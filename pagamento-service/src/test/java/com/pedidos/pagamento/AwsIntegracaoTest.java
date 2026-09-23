package com.pedidos.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedidos.pagamento.aws.AltoValorService;
import com.pedidos.pagamento.aws.S3ComprovanteService;
import com.pedidos.pagamento.domain.Pagamento;
import com.pedidos.pagamento.domain.PagamentoStatus;
import com.pedidos.pagamento.events.PagamentoEvents.PedidoCriadoEvent;
import com.pedidos.pagamento.repository.PagamentoDlqRepository;
import com.pedidos.pagamento.repository.PagamentoRepository;
import com.pedidos.pagamento.service.MockGateway;
import com.pedidos.pagamento.service.PagamentoEventPublisher;
import com.pedidos.pagamento.service.PagamentoService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Etapa 6 (secao 7): provas reais e separadas de S3 e SQS no LocalStack.
 * Usa os clientes reais (sem mock) contra http://localhost:4566.
 */
class AwsIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    PagamentoService service;

    @Autowired
    MockGateway gateway;

    @Autowired
    PagamentoRepository pagamentos;

    @Autowired
    PagamentoDlqRepository dlq;

    @Autowired
    S3ComprovanteService s3;

    @Autowired
    AltoValorService altoValor;

    @MockBean
    PagamentoEventPublisher publisher;

    @BeforeEach
    void limpar() {
        dlq.deleteAll();
        pagamentos.deleteAll();
        gateway.setSempreFalhar(false);
        gateway.falharProximas(0);
        // limpa a fila SQS para o teste ser deterministico
        for (var m : altoValor.receberParaTeste()) {
            // receive sem delete deixa visivel de novo; purga via nova chamada nao suportada
            // pelo SDK simples — o suficiente e filtrar pelo eventId abaixo
        }
    }

    @Test
    void s3ComprovanteApareceNoBucket() {
        var evento = new PedidoCriadoEvent(
                "evt-s3-1", UUID.randomUUID(), "cliente-s3", new BigDecimal("250.00"), null, "corr-s3");

        Pagamento pagamento = service.processar(evento);
        assertEquals(PagamentoStatus.APROVADO, pagamento.getStatus());

        var chaves = s3.listarChaves();
        assertTrue(
                chaves.contains("comprovantes/evt-s3-1.json"), "comprovante deveria estar no bucket, chaves=" + chaves);
        System.out.println("[s3] comprovante s3://" + s3.getBucket()
                + "/comprovantes/evt-s3-1.json listado no bucket OK (total=" + chaves.size() + ")");
    }

    @Test
    void sqsAltoValorChegaNaFila() {
        // Regra de negocio: valor >= 1000.00 vai para revisao manual
        assertTrue(
                altoValor.getLimite().compareTo(new BigDecimal("1000.00")) == 0,
                "limite deveria ser 1000.00, mas foi " + altoValor.getLimite());
        System.out.println(
                "[sqs] regra alto-valor: valor >= " + altoValor.getLimite() + " -> fila " + AltoValorService.FILA);

        var eventoAlto = new PedidoCriadoEvent(
                "evt-sqs-alto-1", UUID.randomUUID(), "cliente-sqs", new BigDecimal("1500.00"), null, "corr-sqs-alto");
        service.processar(eventoAlto);

        var mensagens = altoValor.receberParaTeste().stream()
                .filter(m -> m.body().contains("evt-sqs-alto-1"))
                .toList();
        assertEquals(1, mensagens.size(), "mensagem alto-valor deveria estar na fila SQS");
        System.out.println("[sqs] mensagem evt-sqs-alto-1 (1500.00) recebida na fila " + AltoValorService.FILA + " OK: "
                + mensagens.get(0).body());

        var eventoBaixo = new PedidoCriadoEvent(
                "evt-sqs-baixo-1", UUID.randomUUID(), "cliente-sqs", new BigDecimal("100.00"), null, "corr-sqs-baixo");
        service.processar(eventoBaixo);

        var baixo = altoValor.receberParaTeste().stream()
                .filter(m -> m.body().contains("evt-sqs-baixo-1"))
                .toList();
        assertTrue(baixo.isEmpty(), "valor baixo nao deve ir para a fila SQS");
        System.out.println("[sqs] mensagem evt-sqs-baixo-1 (100.00) corretamente NAO enviada a fila");
    }
}
