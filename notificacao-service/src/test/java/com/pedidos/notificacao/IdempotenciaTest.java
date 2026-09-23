package com.pedidos.notificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pedidos.notificacao.config.RabbitConfig;
import com.pedidos.notificacao.repository.NotificacaoProcessadaRepository;
import com.pedidos.notificacao.service.NotificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Prova secao 6: mesma mensagem duas vezes nao gera notificacao duplicada.
 */
class IdempotenciaTest extends AbstractIntegrationTest {

    @Autowired
    NotificacaoService service;

    @Autowired
    NotificacaoProcessadaRepository repository;

    @MockBean
    RabbitTemplate rabbit;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    void mesmaMensagemDuasVezesPublicaUmaVez() {
        boolean primeira = service.enfileirar(
                "evt-notif-1", "pedido.criado", RabbitConfig.FILA_PEDIDO_CRIADO, "pedido-1", "corr-1", "Pedido criado");
        boolean segunda = service.enfileirar(
                "evt-notif-1", "pedido.criado", RabbitConfig.FILA_PEDIDO_CRIADO, "pedido-1", "corr-1", "Pedido criado");

        assertTrue(primeira, "primeira entrega deve enfileirar");
        assertFalse(segunda, "segunda entrega (mesmo eventId) deve ser ignorada");
        assertEquals(1, repository.count(), "tabela idempotencia deve ter 1 registro");
        verify(rabbit, times(1))
                .convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.FILA_PEDIDO_CRIADO), any(Object.class));
        System.out.println("[idempotencia] 1a entrega enfileirou, 2a (mesmo eventId) ignorada, "
                + "rabbit.convertAndSend 1x, tabela=1");
    }
}
