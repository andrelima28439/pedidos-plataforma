package com.pedidos.notificacao.service;

import com.pedidos.notificacao.events.NotificacaoEvents.NotificacaoPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Component;

/**
 * Simula o envio real (e-mail/SMS/push).
 * Log estruturado com correlation-id para rastrear o pedido de ponta a ponta.
 *
 * Como seria com provedor real: aqui entraria o client (ex: SES, Twilio,
 * Firebase) com timeout + retry curto; falha definitiva lancaria excecao
 * para cair na DLQ do RabbitMQ, de onde o time reprocessa manualmente.
 *
 * Para os testes: payload com mensagem contendo "FAIL" simula falha de envio
 * e forca o reject sem requeue (vai para a DLQ).
 */
@Component
public class EnvioService {

    private static final Logger log = LoggerFactory.getLogger(EnvioService.class);

    public void enviar(NotificacaoPayload payload) {
        if (payload.mensagem() != null && payload.mensagem().contains("FAIL")) {
            log.error(
                    "falha simulada de envio eventId={} tipo={} correlationId={}",
                    payload.eventId(),
                    payload.tipo(),
                    payload.correlationId());
            throw new AmqpRejectAndDontRequeueException("falha simulada de envio");
        }
        log.info(
                "notificacao enviada eventId={} tipo={} pedidoId={} correlationId={} mensagem={}",
                payload.eventId(),
                payload.tipo(),
                payload.pedidoId(),
                payload.correlationId(),
                payload.mensagem());
    }
}
