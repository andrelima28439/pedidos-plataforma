package com.pedidos.notificacao.service;

import static com.pedidos.notificacao.config.RabbitConfig.FILA_PAGAMENTO_APROVADO;
import static com.pedidos.notificacao.config.RabbitConfig.FILA_PAGAMENTO_RECUSADO;
import static com.pedidos.notificacao.config.RabbitConfig.FILA_PEDIDO_CRIADO;

import com.pedidos.notificacao.events.NotificacaoEvents.PagamentoResultadoEvent;
import com.pedidos.notificacao.events.NotificacaoEvents.PedidoCriadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumers {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumers.class);

    private final NotificacaoService service;

    public KafkaConsumers(NotificacaoService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = "${kafka.topicos.pedido-criado:pedido.criado}",
            groupId = "notificacao-service",
            properties = {
                "spring.json.value.default.type=com.pedidos.notificacao.events.NotificacaoEvents$PedidoCriadoEvent"
            })
    public void onPedidoCriado(PedidoCriadoEvent evento) {
        log.info("kafka pedido.criado eventId={} correlationId={}", evento.eventId(), evento.correlationId());
        service.enfileirar(
                evento.eventId(),
                "pedido.criado",
                FILA_PEDIDO_CRIADO,
                evento.pedidoId().toString(),
                evento.correlationId(),
                "Pedido " + evento.pedidoId() + " criado no valor " + evento.valorTotal());
    }

    @KafkaListener(
            topics = "${kafka.topicos.pagamento-aprovado:pagamento.aprovado}",
            groupId = "notificacao-service",
            properties = {
                "spring.json.value.default.type=com.pedidos.notificacao.events.NotificacaoEvents$PagamentoResultadoEvent"
            })
    public void onAprovado(PagamentoResultadoEvent evento) {
        log.info("kafka pagamento.aprovado eventId={} correlationId={}", evento.eventId(), evento.correlationId());
        service.enfileirar(
                evento.eventId(),
                "pagamento.aprovado",
                FILA_PAGAMENTO_APROVADO,
                evento.pedidoId().toString(),
                evento.correlationId(),
                "Pagamento aprovado para o pedido " + evento.pedidoId());
    }

    @KafkaListener(
            topics = "${kafka.topicos.pagamento-recusado:pagamento.recusado}",
            groupId = "notificacao-service",
            properties = {
                "spring.json.value.default.type=com.pedidos.notificacao.events.NotificacaoEvents$PagamentoResultadoEvent"
            })
    public void onRecusado(PagamentoResultadoEvent evento) {
        log.info("kafka pagamento.recusado eventId={} correlationId={}", evento.eventId(), evento.correlationId());
        service.enfileirar(
                evento.eventId(),
                "pagamento.recusado",
                FILA_PAGAMENTO_RECUSADO,
                evento.pedidoId().toString(),
                evento.correlationId(),
                "Pagamento recusado para o pedido " + evento.pedidoId());
    }
}
