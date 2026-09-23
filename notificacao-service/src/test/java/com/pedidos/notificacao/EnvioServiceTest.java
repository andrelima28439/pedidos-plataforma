package com.pedidos.notificacao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pedidos.notificacao.events.NotificacaoEvents.NotificacaoPayload;
import com.pedidos.notificacao.service.EnvioService;
import com.pedidos.notificacao.service.RabbitConsumers;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * O envio simula o provedor real: payload normal sai sem erro; payload com "FAIL"
 * representa falha definitiva e deve rejeitar sem requeue (o broker roteia para a DLQ).
 * Cada listener de fila delega para o mesmo envio.
 */
class EnvioServiceTest extends AbstractIntegrationTest {

    @Autowired
    EnvioService envio;

    @Autowired
    RabbitConsumers consumers;

    @Test
    void payloadNormalEnviaSemErroNosTresListeners() {
        var payload = new NotificacaoPayload("evt-env-1", "pedido.criado", "pedido-1", "corr-env-1", "Pedido criado");

        assertDoesNotThrow(() -> consumers.onPedidoCriado(payload));
        assertDoesNotThrow(() -> consumers.onAprovado(payload));
        assertDoesNotThrow(() -> consumers.onRecusado(payload));
        System.out.println("[envio] payload normal aceito nos 3 listeners");
    }

    @Test
    void payloadFailRejeitaSemRequeue() {
        var payload =
                new NotificacaoPayload("evt-env-2", "pagamento.aprovado", "pedido-2", "corr-env-2", "FAIL simulado");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> envio.enviar(payload));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumers.onAprovado(payload));
        System.out.println("[envio] payload FAIL rejeitado sem requeue (vai para a DLQ)");
    }
}
