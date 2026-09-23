package com.pedidos.notificacao.service;

import static com.pedidos.notificacao.config.RabbitConfig.EXCHANGE;

import com.pedidos.notificacao.domain.NotificacaoProcessada;
import com.pedidos.notificacao.events.NotificacaoEvents.NotificacaoPayload;
import com.pedidos.notificacao.repository.NotificacaoProcessadaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrada idempotente: mesmo eventId duas vezes publica no RabbitMQ
 * exatamente uma vez. A segunda chamada e ignorada (sem efeito).
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final NotificacaoProcessadaRepository repository;
    private final RabbitTemplate rabbit;

    public NotificacaoService(NotificacaoProcessadaRepository repository, RabbitTemplate rabbit) {
        this.repository = repository;
        this.rabbit = rabbit;
    }

    @Transactional
    public boolean enfileirar(
            String eventId, String tipo, String routingKey, String pedidoId, String correlationId, String mensagem) {
        if (repository.existsById(eventId)) {
            log.info("notificacao duplicada ignorada eventId={} tipo={}", eventId, tipo);
            return false;
        }
        repository.save(new NotificacaoProcessada(eventId, tipo));
        var payload = new NotificacaoPayload(eventId, tipo, pedidoId, correlationId, mensagem);
        rabbit.convertAndSend(EXCHANGE, routingKey, payload);
        log.info(
                "notificacao enfileirada eventId={} tipo={} fila={} correlationId={}",
                eventId,
                tipo,
                routingKey,
                correlationId);
        return true;
    }
}
