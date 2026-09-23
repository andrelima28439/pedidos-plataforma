package com.pedidos.notificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.pedidos.notificacao.config.RabbitConfig;
import com.pedidos.notificacao.events.NotificacaoEvents.NotificacaoPayload;
import com.pedidos.notificacao.repository.NotificacaoProcessadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Prova secao 6: Dead-letter queue do RabbitMQ (nao do Kafka) configurada
 * e funcionando para falhas de envio.
 */
class RabbitDlqTest extends AbstractIntegrationTest {

    @Autowired
    RabbitTemplate rabbit;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Autowired
    NotificacaoProcessadaRepository repository;

    @Autowired
    @Qualifier("filaPedidoCriado")
    Queue filaPedidoCriado;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
        purgar(RabbitConfig.FILA_PEDIDO_CRIADO);
        purgar(RabbitConfig.DLQ_PEDIDO_CRIADO);
    }

    private void purgar(String fila) {
        try {
            rabbit.execute(channel -> {
                channel.queuePurge(fila);
                return null;
            });
        } catch (Exception e) {
            // fila pode nao existir ainda na primeira execucao
        }
    }

    @Test
    void filaTemDlxConfiguradaEfalhaDeEnvioCaiNaDlq() {
        // 1. Configuracao: bean da fila principal tem DLX apontando para notificacao.dlx
        assertEquals(RabbitConfig.DLX, filaPedidoCriado.getArguments().get("x-dead-letter-exchange"));
        assertEquals(
                RabbitConfig.FILA_PEDIDO_CRIADO, filaPedidoCriado.getArguments().get("x-dead-letter-routing-key"));
        System.out.println("[dlq-config] " + RabbitConfig.FILA_PEDIDO_CRIADO + " tem x-dead-letter-exchange="
                + RabbitConfig.DLX + " OK");

        // 2. Funcionamento: mensagem com FAIL falha no envio e cai na DLQ
        // (RabbitConsumers consome a fila principal e faz reject sem requeue)
        var payload = new NotificacaoPayload(
                "evt-fail-dlq-1", "pedido.criado", "pedido-fail-1", "corr-fail-1", "FAIL simulado");
        rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.FILA_PEDIDO_CRIADO, payload);
        System.out.println("[dlq] mensagem FAIL publicada na fila principal, aguardando reject -> DLQ...");

        NotificacaoPayload daDlq = null;
        var deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            Object recebido = rabbit.receiveAndConvert(RabbitConfig.DLQ_PEDIDO_CRIADO, 1000);
            if (recebido instanceof NotificacaoPayload p) {
                daDlq = p;
                break;
            }
        }
        assertNotNull(daDlq, "mensagem com falha deveria ter caido na DLQ em 15s");
        assertEquals("evt-fail-dlq-1", daDlq.eventId());
        System.out.println("[dlq] mensagem FAIL chegou na " + RabbitConfig.DLQ_PEDIDO_CRIADO + " eventId="
                + daDlq.eventId() + " — DLQ RabbitMQ funcionando (distinta da DLQ Kafka do pagamento)");
    }
}
