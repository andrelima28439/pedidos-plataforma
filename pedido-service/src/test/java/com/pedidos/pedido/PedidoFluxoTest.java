package com.pedidos.pedido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

class PedidoFluxoTest extends AbstractIntegrationTest {

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

    private Pedido criarPedido(String cliente) {
        var req = new CriarPedidoRequest(List.of(
                new ItemRequest("prod-1", 2, new BigDecimal("100.00")),
                new ItemRequest("prod-2", 1, new BigDecimal("50.00"))));
        return pedidos.criar(cliente, req);
    }

    @Test
    void criarReservaEPublicaEventoEAprovacaoVaiParaPago() {
        Pedido pedido = criarPedido("cliente-1");

        assertEquals(PedidoStatus.AGUARDANDO_PAGAMENTO, pedido.getStatus());
        assertEquals(new BigDecimal("250.00"), pedido.getValorTotal());
        assertEquals(2, pedido.getHistorico().size());
        verify(estoque, times(1)).reservar("prod-1", 2);
        verify(estoque, times(1)).reservar("prod-2", 1);
        verify(publisher, times(1)).publicarPedidoCriado(any());
        System.out.println("[fluxo] pedido criado AGUARDANDO_PAGAMENTO valor=250.00 historico=2");

        var evento = new PagamentoResultadoEvent(
                "evt-aprov-1",
                pedido.getId(),
                new BigDecimal("250.00"),
                pedido.getId().toString());
        consumer.handleAprovadoForTest(evento);

        Pedido pago = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(PedidoStatus.PAGO, pago.getStatus());
        assertEquals(3, pago.getHistorico().size());
        verify(estoque, times(1)).confirmar("prod-1", 2);
        verify(estoque, times(1)).confirmar("prod-2", 1);
        assertEquals(1, eventos.count());
        System.out.println("[fluxo] pagamento.aprovado -> PAGO historico=3 confirmar=1x por item");
    }

    @Test
    void pagamentoRecusadoLiberaEstoqueEVaiParaRecusado() {
        Pedido pedido = criarPedido("cliente-2");

        var evento = new PagamentoResultadoEvent(
                "evt-rec-1",
                pedido.getId(),
                new BigDecimal("250.00"),
                pedido.getId().toString());
        consumer.handleRecusadoForTest(evento);

        Pedido recusado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(PedidoStatus.RECUSADO, recusado.getStatus());
        verify(estoque, times(1)).liberar("prod-1", 2);
        verify(estoque, times(1)).liberar("prod-2", 1);
        verify(estoque, never()).confirmar(anyString(), anyInt());
        System.out.println("[fluxo] pagamento.recusado -> RECUSADO liberar=1x por item");
    }
}
