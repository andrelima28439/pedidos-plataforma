package com.pedidos.notificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedidos.notificacao.domain.NotificacaoProcessada;
import com.pedidos.notificacao.events.NotificacaoEvents.NotificacaoPayload;
import com.pedidos.notificacao.repository.NotificacaoProcessadaRepository;
import com.pedidos.notificacao.service.DlqConsumers;
import com.pedidos.notificacao.web.NotificacaoController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Observabilidade das DLQs: listar processadas e medir o tamanho das 3 DLQs
 * (usa o broker real; filas vazias retornam 0, nunca erro). Os consumers de DLQ
 * só logam para ação manual — aqui se verifica que não quebram com payload real.
 */
class DlqObservabilidadeTest extends AbstractIntegrationTest {

    @Autowired
    NotificacaoController controller;

    @Autowired
    NotificacaoProcessadaRepository repository;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    void processadasListaRegistros() {
        repository.save(new NotificacaoProcessada("evt-obs-1", "pedido.criado"));

        var lista = controller.processadas();

        assertEquals(1, lista.size());
        assertEquals("evt-obs-1", lista.get(0).getEventId());
        System.out.println("[observabilidade] /notificacoes/processadas lista 1 registro");
    }

    @Test
    void dlqTamanhosRetornaContadores() {
        var tamanhos = controller.dlqTamanhos();

        assertTrue(tamanhos.pedidoCriado() >= 0, "DLQ pedido-criado deve responder");
        assertTrue(tamanhos.pagamentoAprovado() >= 0, "DLQ pagamento-aprovado deve responder");
        assertTrue(tamanhos.pagamentoRecusado() >= 0, "DLQ pagamento-recusado deve responder");
        System.out.println("[observabilidade] /notificacoes/dlq/tamanhos = " + tamanhos);
    }

    @Test
    void dlqConsumersLogamSemQuebrar() {
        var consumers = new DlqConsumers();
        var payload = new NotificacaoPayload("evt-obs-2", "pedido.criado", "pedido-2", "corr-obs-2", "msg");

        consumers.onDlqPedidoCriado(payload);
        consumers.onDlqAprovado(payload);
        consumers.onDlqRecusado(payload);
        System.out.println("[observabilidade] DlqConsumers processaram 3 DLQs (log p/ acao manual)");
    }
}
