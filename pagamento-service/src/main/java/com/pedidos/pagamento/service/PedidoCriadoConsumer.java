package com.pedidos.pagamento.service;

import com.pedidos.pagamento.events.PagamentoEvents.PedidoCriadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoCriadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoCriadoConsumer.class);

    private final PagamentoService service;

    public PedidoCriadoConsumer(PagamentoService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = "${kafka.topicos.pedido-criado:pedido.criado}",
            groupId = "pagamento-service",
            properties = {
                "spring.json.value.default.type=com.pedidos.pagamento.events.PagamentoEvents$PedidoCriadoEvent"
            })
    public void onPedidoCriado(PedidoCriadoEvent evento) {
        MDC.put("correlationId", evento.correlationId());
        try {
            log.info(
                    "recebido pedido.criado pedidoId={} eventId={} correlationId={}",
                    evento.pedidoId(),
                    evento.eventId(),
                    evento.correlationId());
            service.processar(evento);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
