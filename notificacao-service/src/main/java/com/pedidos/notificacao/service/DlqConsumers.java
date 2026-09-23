package com.pedidos.notificacao.service;

import static com.pedidos.notificacao.config.RabbitConfig.DLQ_PAGAMENTO_APROVADO;
import static com.pedidos.notificacao.config.RabbitConfig.DLQ_PAGAMENTO_RECUSADO;
import static com.pedidos.notificacao.config.RabbitConfig.DLQ_PEDIDO_CRIADO;

import com.pedidos.notificacao.events.NotificacaoEvents.NotificacaoPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Em producao apenas loga as DLQs para acao manual.
 * Desabilitado nos testes (profile test) para que as mensagens da DLQ
 * possam ser inspecionadas via RabbitTemplate.receive.
 */
@Component
@Profile("!test")
public class DlqConsumers {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumers.class);

    @RabbitListener(queues = DLQ_PEDIDO_CRIADO)
    public void onDlqPedidoCriado(NotificacaoPayload payload) {
        log.error(
                "DLQ RabbitMQ pedido-criado eventId={} correlationId={} — requer acao manual",
                payload.eventId(),
                payload.correlationId());
    }

    @RabbitListener(queues = DLQ_PAGAMENTO_APROVADO)
    public void onDlqAprovado(NotificacaoPayload payload) {
        log.error(
                "DLQ RabbitMQ pagamento-aprovado eventId={} correlationId={} — requer acao manual",
                payload.eventId(),
                payload.correlationId());
    }

    @RabbitListener(queues = DLQ_PAGAMENTO_RECUSADO)
    public void onDlqRecusado(NotificacaoPayload payload) {
        log.error(
                "DLQ RabbitMQ pagamento-recusado eventId={} correlationId={} — requer acao manual",
                payload.eventId(),
                payload.correlationId());
    }
}
