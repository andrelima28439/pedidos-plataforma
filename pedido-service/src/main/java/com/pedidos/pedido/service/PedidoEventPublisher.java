package com.pedidos.pedido.service;

import com.pedidos.pedido.events.PedidoEvents.PedidoCriadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;
    private final String topicoPedidoCriado;

    public PedidoEventPublisher(
            KafkaTemplate<String, Object> kafka,
            @Value("${kafka.topicos.pedido-criado:pedido.criado}") String topicoPedidoCriado) {
        this.kafka = kafka;
        this.topicoPedidoCriado = topicoPedidoCriado;
    }

    public void publicarPedidoCriado(PedidoCriadoEvent evento) {
        log.info(
                "publicando pedido.criado pedidoId={} eventId={} correlationId={}",
                evento.pedidoId(),
                evento.eventId(),
                evento.correlationId());
        kafka.send(topicoPedidoCriado, evento.pedidoId().toString(), evento);
    }
}
