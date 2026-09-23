package com.pedidos.pagamento.service;

import com.pedidos.pagamento.aws.AltoValorService;
import com.pedidos.pagamento.aws.S3ComprovanteService;
import com.pedidos.pagamento.domain.Pagamento;
import com.pedidos.pagamento.domain.PagamentoDlq;
import com.pedidos.pagamento.domain.PagamentoStatus;
import com.pedidos.pagamento.events.PagamentoEvents.PagamentoDlqEvent;
import com.pedidos.pagamento.events.PagamentoEvents.PagamentoResultadoEvent;
import com.pedidos.pagamento.events.PagamentoEvents.PedidoCriadoEvent;
import com.pedidos.pagamento.repository.PagamentoDlqRepository;
import com.pedidos.pagamento.repository.PagamentoRepository;
import com.pedidos.pagamento.service.MockGateway.TransientGatewayException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoRepository pagamentos;
    private final PagamentoDlqRepository dlq;
    private final MockGateway gateway;
    private final PagamentoEventPublisher publisher;
    private final S3ComprovanteService s3;
    private final AltoValorService altoValor;
    private final MeterRegistry metrics;
    private final int maxTentativas;
    private final long backoffBaseMs;

    public PagamentoService(
            PagamentoRepository pagamentos,
            PagamentoDlqRepository dlq,
            MockGateway gateway,
            PagamentoEventPublisher publisher,
            S3ComprovanteService s3,
            AltoValorService altoValor,
            MeterRegistry metrics,
            @Value("${pagamento.max-tentativas:3}") int maxTentativas,
            @Value("${pagamento.backoff-base-ms:50}") long backoffBaseMs) {
        this.pagamentos = pagamentos;
        this.dlq = dlq;
        this.gateway = gateway;
        this.publisher = publisher;
        this.s3 = s3;
        this.altoValor = altoValor;
        this.metrics = metrics;
        this.maxTentativas = maxTentativas;
        this.backoffBaseMs = backoffBaseMs;
    }

    /**
     * Processa pedido.criado com retry exponencial (50ms, 100ms, ...) e DLQ.
     * Idempotente por eventId: reprocessar o mesmo evento nao duplica pagamento.
     */
    @Transactional
    public Pagamento processar(PedidoCriadoEvent evento) {
        MDC.put("correlationId", evento.correlationId());
        try {
            return processarInterno(evento);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private Pagamento processarInterno(PedidoCriadoEvent evento) {
        var existente = pagamentos.findByEventId(evento.eventId());
        if (existente.isPresent()) {
            log.info("evento duplicado ignorado eventId={}", evento.eventId());
            return existente.get();
        }

        int tentativa = 0;
        while (true) {
            tentativa++;
            try {
                MockGateway.Resultado resultado = gateway.cobrar(evento.valorTotal());
                PagamentoStatus status = resultado == MockGateway.Resultado.APROVADO
                        ? PagamentoStatus.APROVADO
                        : PagamentoStatus.RECUSADO;
                Pagamento pagamento = new Pagamento(
                        evento.eventId(),
                        evento.pedidoId(),
                        evento.valorTotal(),
                        status,
                        tentativa,
                        evento.correlationId());
                pagamentos.save(pagamento);

                var resultadoEvento = new PagamentoResultadoEvent(
                        evento.eventId(), evento.pedidoId(), evento.valorTotal(), evento.correlationId());
                if (status == PagamentoStatus.APROVADO) {
                    publisher.publicarAprovado(resultadoEvento);
                    // Etapa 6 AWS: comprovante S3 + fila alto-valor SQS (best-effort)
                    try {
                        s3.salvarComprovante(pagamento);
                    } catch (Exception e) {
                        log.warn("[s3] falha ao salvar comprovante eventId={}: {}", evento.eventId(), e.getMessage());
                    }
                    try {
                        altoValor.enviarParaRevisao(
                                evento.eventId(),
                                evento.pedidoId().toString(),
                                evento.valorTotal(),
                                evento.correlationId());
                    } catch (Exception e) {
                        log.warn("[sqs] falha ao enviar alto-valor eventId={}: {}", evento.eventId(), e.getMessage());
                    }
                } else {
                    publisher.publicarRecusado(resultadoEvento);
                }
                log.info(
                        "[pagamento] sucesso na tentativa {} eventId={} status={}",
                        tentativa,
                        evento.eventId(),
                        status);
                return pagamento;
            } catch (TransientGatewayException e) {
                metrics.counter("pagamento_retry_total").increment();
                log.warn(
                        "[retry] tentativa {} falhou (transitoria) eventId={} pedidoId={}: {}",
                        tentativa,
                        evento.eventId(),
                        evento.pedidoId(),
                        e.getMessage());
                if (tentativa >= maxTentativas) {
                    return levarParaDlq(evento, tentativa, e.getMessage());
                }
                long backoff = backoffBaseMs * (1L << (tentativa - 1));
                log.info("[retry] backoff exponencial {}ms antes da tentativa {}", backoff, tentativa + 1);
                dormir(backoff);
            }
        }
    }

    private Pagamento levarParaDlq(PedidoCriadoEvent evento, int tentativas, String motivo) {
        Pagamento pagamento = new Pagamento(
                evento.eventId(),
                evento.pedidoId(),
                evento.valorTotal(),
                PagamentoStatus.DLQ,
                tentativas,
                evento.correlationId());
        pagamentos.save(pagamento);
        dlq.save(new PagamentoDlq(
                evento.eventId(), evento.pedidoId(), evento.valorTotal(), motivo, tentativas, evento.correlationId()));
        publisher.publicarDlq(new PagamentoDlqEvent(
                evento.eventId(), evento.pedidoId(), evento.valorTotal(), motivo, tentativas, evento.correlationId()));
        log.error("[dlq] eventId={} caiu na DLQ apos {} tentativas: {}", evento.eventId(), tentativas, motivo);
        return pagamento;
    }

    /**
     * Reprocessa manualmente uma mensagem da DLQ (secao 5.2).
     * Espera-se que o gateway esteja saudavel de novo; usa o mesmo retry.
     */
    @Transactional
    public Pagamento reprocessarDlq(String eventId) {
        PagamentoDlq pendente = dlq.findById(eventId).orElseThrow(() -> new DlqNaoEncontradaException(eventId));
        if (pendente.getStatus() != PagamentoDlq.DlqStatus.PENDENTE) {
            throw new IllegalStateException("DLQ ja reprocessada: " + eventId);
        }
        // Remove o registro antigo de pagamento DLQ para reprocessar do zero
        pagamentos.findByEventId(eventId).ifPresent(pagamentos::delete);
        dlq.delete(pendente);

        var evento = new PedidoCriadoEvent(
                eventId, pendente.getPedidoId(), null, pendente.getValor(), null, pendente.getCorrelationId());
        Pagamento resultado = processar(evento);
        // Marca a DLQ original como reprocessada (recria como historico)
        PagamentoDlq historico = new PagamentoDlq(
                eventId,
                pendente.getPedidoId(),
                pendente.getValor(),
                pendente.getMotivo(),
                resultado.getTentativas(),
                pendente.getCorrelationId());
        historico.setStatus(PagamentoDlq.DlqStatus.REPROCESSADO);
        dlq.save(historico);
        log.info("[dlq] eventId={} reprocessada com sucesso status={}", eventId, resultado.getStatus());
        return resultado;
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrompido durante backoff", e);
        }
    }

    public static class DlqNaoEncontradaException extends RuntimeException {
        public DlqNaoEncontradaException(String eventId) {
            super("DLQ nao encontrada: " + eventId);
        }
    }
}
