package com.pedidos.pedido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

import com.pedidos.pedido.events.PedidoEvents.PedidoCriadoEvent;
import com.pedidos.pedido.repository.EventoProcessadoRepository;
import com.pedidos.pedido.repository.PedidoRepository;
import com.pedidos.pedido.service.EstoqueClient;
import com.pedidos.pedido.service.PedidoEventPublisher;
import com.pedidos.pedido.service.PedidoService;
import com.pedidos.pedido.web.PedidoDtos.CriarPedidoRequest;
import com.pedidos.pedido.web.PedidoDtos.ItemRequest;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Etapa 8: correlation-id (pedidoId) propagado no evento + metrica exposta.
 */
@AutoConfigureMockMvc
class ObservabilidadeTest extends AbstractIntegrationTest {

    @Autowired
    PedidoService pedidos;

    @Autowired
    PedidoRepository pedidoRepository;

    @Autowired
    EventoProcessadoRepository eventos;

    @Autowired
    MockMvc mvc;

    @Autowired
    MeterRegistry registry;

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
    void eventoCarregaCorrelationIdIgualAoPedidoId() {
        var req = new CriarPedidoRequest(List.of(new ItemRequest("p1", 1, new BigDecimal("10.00"))));
        var pedido = pedidos.criar("cliente-corr", req);

        var captor = ArgumentCaptor.forClass(PedidoCriadoEvent.class);
        verify(publisher).publicarPedidoCriado(captor.capture());
        assertEquals(pedido.getId().toString(), captor.getValue().correlationId());
        System.out.println("[correlation] pedido-service publicou pedido.criado com correlationId="
                + captor.getValue().correlationId() + " (= pedidoId) — propaga para pagamento-service");
    }

    @Test
    void metricaPedidosPorStatusRegistrada() {
        // Exportacao Prometheus e desabilitada no contexto de teste (MockMvc);
        // a prova de scrape real esta no relatorio da Etapa 8 (servicos de pe).
        // Aqui garantimos o registro da metrica custom com a tag por status.
        assertNotNull(registry.find("pedidos_por_status").tags("status", "PAGO").gauge());
        assertNotNull(registry.find("pedidos_por_status")
                .tags("status", "AGUARDANDO_PAGAMENTO")
                .gauge());
        System.out.println("[metricas] gauge pedidos_por_status{status} registrado no registry OK "
                + "(scrape /actuator/prometheus provado com servicos reais na Etapa 8)");
    }
}
