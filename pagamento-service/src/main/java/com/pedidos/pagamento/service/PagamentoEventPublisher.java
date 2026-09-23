package com.pedidos.pagamento.service;

import com.pedidos.pagamento.events.PagamentoEvents.PagamentoDlqEvent;
import com.pedidos.pagamento.events.PagamentoEvents.PagamentoResultadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PagamentoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PagamentoEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;
    private final String topicoAprovado;
    private final String topicoRecusado;
    private final String topicoDlq;

    public PagamentoEventPublisher(
            KafkaTemplate<String, Object> kafka,
            @Value("${kafka.topicos.pagamento-aprovado:pagamento.aprovado}") String topicoAprovado,
            @Value("${kafka.topicos.pagamento-recusado:pagamento.recusado}") String topicoRecusado,
            @Value("${kafka.topicos.pagamento-dlq:pagamento.dlq}") String topicoDlq) {
        this.kafka = kafka;
        this.topicoAprovado = topicoAprovado;
        this.topicoRecusado = topicoRecusado;
        this.topicoDlq = topicoDlq;
    }

    public void publicarAprovado(PagamentoResultadoEvent evento) {
        log.info("publicando pagamento.aprovado pedidoId={} eventId={}", evento.pedidoId(), evento.eventId());
        kafka.send(topicoAprovado, evento.pedidoId().toString(), evento);
    }

    public void publicarRecusado(PagamentoResultadoEvent evento) {
        log.info("publicando pagamento.recusado pedidoId={} eventId={}", evento.pedidoId(), evento.eventId());
        kafka.send(topicoRecusado, evento.pedidoId().toString(), evento);
    }

    public void publicarDlq(PagamentoDlqEvent evento) {
        log.warn(
                "publicando pagamento.dlq pedidoId={} eventId={} motivo={}",
                evento.pedidoId(),
                evento.eventId(),
                evento.motivo());
        kafka.send(topicoDlq, evento.pedidoId().toString(), evento);
    }
}
