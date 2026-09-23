package com.pedidos.pedido.service;

import com.pedidos.pedido.domain.EventoProcessado;
import com.pedidos.pedido.domain.Pedido;
import com.pedidos.pedido.domain.PedidoStatus;
import com.pedidos.pedido.events.PedidoEvents.PagamentoResultadoEvent;
import com.pedidos.pedido.repository.EventoProcessadoRepository;
import com.pedidos.pedido.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PagamentoResultadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PagamentoResultadoConsumer.class);

    private final PedidoRepository pedidos;
    private final EventoProcessadoRepository eventos;
    private final EstoqueClient estoque;
    private final PedidoStreamService stream;

    public PagamentoResultadoConsumer(
            PedidoRepository pedidos,
            EventoProcessadoRepository eventos,
            EstoqueClient estoque,
            PedidoStreamService stream) {
        this.pedidos = pedidos;
        this.eventos = eventos;
        this.estoque = estoque;
        this.stream = stream;
    }

    @KafkaListener(
            topics = "${kafka.topicos.pagamento-aprovado:pagamento.aprovado}",
            groupId = "pedido-service",
            properties = {
                "spring.json.value.default.type=com.pedidos.pedido.events.PedidoEvents$PagamentoResultadoEvent"
            })
    public void onAprovado(PagamentoResultadoEvent evento) {
        aplicar(evento, PedidoStatus.PAGO, "pagamento.aprovado", true);
    }

    @KafkaListener(
            topics = "${kafka.topicos.pagamento-recusado:pagamento.recusado}",
            groupId = "pedido-service",
            properties = {
                "spring.json.value.default.type=com.pedidos.pedido.events.PedidoEvents$PagamentoResultadoEvent"
            })
    public void onRecusado(PagamentoResultadoEvent evento) {
        aplicar(evento, PedidoStatus.RECUSADO, "pagamento.recusado", false);
    }

    @Transactional
    public void aplicar(PagamentoResultadoEvent evento, PedidoStatus destino, String tipo, boolean aprovado) {
        MDC.put("correlationId", evento.correlationId());
        try {
            log.info(
                    "recebido {} pedidoId={} eventId={} correlationId={}",
                    tipo,
                    evento.pedidoId(),
                    evento.eventId(),
                    evento.correlationId());

            // Idempotencia: mesmo eventId duas vezes nao duplica efeito
            if (eventos.existsById(evento.eventId())) {
                log.info("evento duplicado ignorado eventId={}", evento.eventId());
                return;
            }

            Pedido pedido = pedidos.findById(evento.pedidoId()).orElse(null);
            if (pedido == null) {
                log.warn(
                        "pedido nao encontrado para evento {} — registra como processado para nao reprocessar",
                        evento.eventId());
                eventos.save(new EventoProcessado(evento.eventId(), tipo));
                return;
            }

            // Se o pedido ja esta no estado final, so registra idempotencia (sem efeito duplo)
            if (pedido.getStatus() == destino) {
                eventos.save(new EventoProcessado(evento.eventId(), tipo));
                return;
            }

            pedido.avancarPara(destino, tipo + " eventId=" + evento.eventId());
            pedidos.save(pedido);

            // Efeito colateral no estoque (confirmar ou liberar) — exatamente 1x por eventId
            for (var item : pedido.getItens()) {
                if (aprovado) {
                    estoque.confirmar(item.getProdutoId(), item.getQuantidade());
                } else {
                    estoque.liberar(item.getProdutoId(), item.getQuantidade());
                }
            }

            eventos.save(new EventoProcessado(evento.eventId(), tipo));
            stream.publicar(pedido.getClienteId(), pedido.getId(), pedido.getStatus());
            log.info("pedido {} atualizado para {} (eventId={})", pedido.getId(), destino, evento.eventId());
        } finally {
            MDC.remove("correlationId");
        }
    }

    // Helper para testes chamarem sem broker
    public void handleAprovadoForTest(PagamentoResultadoEvent evento) {
        aplicar(evento, PedidoStatus.PAGO, "pagamento.aprovado", true);
    }

    public void handleRecusadoForTest(PagamentoResultadoEvent evento) {
        aplicar(evento, PedidoStatus.RECUSADO, "pagamento.recusado", false);
    }
}
