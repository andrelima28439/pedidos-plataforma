package com.pedidos.pedido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pedidos.pedido.domain.Pedido;
import com.pedidos.pedido.domain.PedidoStatus;
import com.pedidos.pedido.events.PedidoEvents.PagamentoResultadoEvent;
import com.pedidos.pedido.repository.EventoProcessadoRepository;
import com.pedidos.pedido.repository.PedidoRepository;
import com.pedidos.pedido.service.EstoqueClient;
import com.pedidos.pedido.service.PagamentoResultadoConsumer;
import com.pedidos.pedido.service.PedidoEventPublisher;
import com.pedidos.pedido.service.PedidoService;
import com.pedidos.pedido.web.PedidoDtos.CriarPedidoRequest;
import com.pedidos.pedido.web.PedidoDtos.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Secao 4.5 do roteiro: publicar o mesmo evento duas vezes NAO pode
 * duplicar efeito (status, historico e chamadas ao estoque exatamente 1x).
 */
class PagamentoIdempotenciaTest extends AbstractIntegrationTest {

    @Autowired
    PedidoService pedidos;

    @Autowired
    PagamentoResultadoConsumer consumer;

    @Autowired
    PedidoRepository pedidoRepository;

    @Autowired
    EventoProcessadoRepository eventos;

    @MockBean
    EstoqueClient estoque;

    @MockBean
    PedidoEventPublisher publisher;

    @BeforeEach
    void limpar() {
        pedidoRepository.deleteAll();
        eventos.deleteAll();
    }

    @Test
    void mesmoEventoDuasVezesNaoDuplicaEfeito() {
        var req = new CriarPedidoRequest(List.of(new ItemRequest("prod-9", 1, new BigDecimal("99.90"))));
        Pedido pedido = pedidos.criar("cliente-idem", req);
        assertEquals(PedidoStatus.AGUARDANDO_PAGAMENTO, pedido.getStatus());

        var evento = new PagamentoResultadoEvent(
                "evt-duplicado-123",
                pedido.getId(),
                new BigDecimal("99.90"),
                pedido.getId().toString());

        consumer.handleAprovadoForTest(evento);
        Pedido aposPrimeira = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(PedidoStatus.PAGO, aposPrimeira.getStatus());
        assertEquals(3, aposPrimeira.getHistorico().size());
        assertEquals(1, eventos.count());
        System.out.println("[idempotencia] 1a entrega: PAGO historico=3 eventos=1 confirmar=1x");

        consumer.handleAprovadoForTest(evento);
        Pedido aposSegunda = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(PedidoStatus.PAGO, aposSegunda.getStatus());
        assertEquals(3, aposSegunda.getHistorico().size(), "segunda entrega nao pode criar historico duplicado");
        assertEquals(1, eventos.count(), "segunda entrega nao pode duplicar evento processado");
        verify(estoque, times(1)).confirmar("prod-9", 1);
        System.out.println(
                "[idempotencia] 2a entrega (mesmo eventId): ignorada, historico=3 eventos=1 confirmar ainda 1x");
    }
}
