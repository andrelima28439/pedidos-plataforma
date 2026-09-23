package com.pedidos.notificacao.service;

import static com.pedidos.notificacao.config.RabbitConfig.FILA_PAGAMENTO_APROVADO;
import static com.pedidos.notificacao.config.RabbitConfig.FILA_PAGAMENTO_RECUSADO;
import static com.pedidos.notificacao.config.RabbitConfig.FILA_PEDIDO_CRIADO;

import com.pedidos.notificacao.events.NotificacaoEvents.NotificacaoPayload;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitConsumers {

    private final EnvioService envio;

    public RabbitConsumers(EnvioService envio) {
        this.envio = envio;
    }

    @RabbitListener(queues = FILA_PEDIDO_CRIADO)
    public void onPedidoCriado(NotificacaoPayload payload) {
        envio.enviar(payload);
    }

    @RabbitListener(queues = FILA_PAGAMENTO_APROVADO)
    public void onAprovado(NotificacaoPayload payload) {
        envio.enviar(payload);
    }

    @RabbitListener(queues = FILA_PAGAMENTO_RECUSADO)
    public void onRecusado(NotificacaoPayload payload) {
        envio.enviar(payload);
    }
}
