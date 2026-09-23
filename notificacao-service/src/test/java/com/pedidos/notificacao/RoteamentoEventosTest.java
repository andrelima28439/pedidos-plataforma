package com.pedidos.notificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pedidos.notificacao.config.RabbitConfig;
import com.pedidos.notificacao.events.NotificacaoEvents.NotificacaoPayload;
import com.pedidos.notificacao.events.NotificacaoEvents.PagamentoResultadoEvent;
import com.pedidos.notificacao.events.NotificacaoEvents.PedidoCriadoEvent;
import com.pedidos.notificacao.repository.NotificacaoProcessadaRepository;
import com.pedidos.notificacao.service.KafkaConsumers;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Cada tipo de evento Kafka deve cair na fila RabbitMQ correspondente, com o tipo
 * preservado no payload — é esse roteamento que separa as notificações por canal.
 */
class RoteamentoEventosTest extends AbstractIntegrationTest {

    @Autowired
    KafkaConsumers consumers;

    @Autowired
    NotificacaoProcessadaRepository repository;

    @MockBean
    RabbitTemplate rabbit;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    void pedidoCriadoVaiParaFilaPedidoCriado() {
        var evento = new PedidoCriadoEvent(
                "evt-rot-1", UUID.randomUUID(), "demo", new BigDecimal("100.00"), List.of(), "corr-rot-1");

        consumers.onPedidoCriado(evento);

        var payload = unicaPublicacao(RabbitConfig.FILA_PEDIDO_CRIADO);
        assertEquals("pedido.criado", payload.tipo());
        assertEquals("evt-rot-1", payload.eventId());
        assertTrue(payload.mensagem().contains("criado"), "mensagem deve descrever criacao");
        System.out.println("[roteamento] pedido.criado -> " + RabbitConfig.FILA_PEDIDO_CRIADO);
    }

    @Test
    void pagamentoAprovadoVaiParaFilaAprovado() {
        var evento =
                new PagamentoResultadoEvent("evt-rot-2", UUID.randomUUID(), new BigDecimal("100.00"), "corr-rot-2");

        consumers.onAprovado(evento);

        var payload = unicaPublicacao(RabbitConfig.FILA_PAGAMENTO_APROVADO);
        assertEquals("pagamento.aprovado", payload.tipo());
        assertTrue(payload.mensagem().contains("aprovado"), "mensagem deve descrever aprovacao");
        System.out.println("[roteamento] pagamento.aprovado -> " + RabbitConfig.FILA_PAGAMENTO_APROVADO);
    }

    @Test
    void pagamentoRecusadoVaiParaFilaRecusado() {
        var evento =
                new PagamentoResultadoEvent("evt-rot-3", UUID.randomUUID(), new BigDecimal("199.99"), "corr-rot-3");

        consumers.onRecusado(evento);

        var payload = unicaPublicacao(RabbitConfig.FILA_PAGAMENTO_RECUSADO);
        assertEquals("pagamento.recusado", payload.tipo());
        assertTrue(payload.mensagem().contains("recusado"), "mensagem deve descrever recusa");
        System.out.println("[roteamento] pagamento.recusado -> " + RabbitConfig.FILA_PAGAMENTO_RECUSADO);
    }

    private NotificacaoPayload unicaPublicacao(String routingKey) {
        var captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbit, times(1)).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(routingKey), captor.capture());
        assertEquals(1, repository.count(), "roteamento deve registrar idempotencia");
        return (NotificacaoPayload) captor.getValue();
    }
}
