package com.pedidos.pedido;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedidos.pedido.repository.EventoProcessadoRepository;
import com.pedidos.pedido.repository.PedidoRepository;
import com.pedidos.pedido.security.JwtService;
import com.pedidos.pedido.service.EstoqueClient;
import com.pedidos.pedido.service.PedidoEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova Etapa 7: EventSource (SSE) nao envia headers Authorization,
 * por isso o filtro aceita ?token= na query (GET /pedidos/stream usa isso).
 */
@AutoConfigureMockMvc
class SseQueryTokenTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwt;

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
    void queryTokenAutenticaSemHeader() throws Exception {
        String token = jwt.gerar("cliente-sse");
        // sem header, com ?token= -> deve autenticar (200, nao 403)
        mvc.perform(get("/pedidos").param("token", token)).andExpect(status().isOk());
        System.out.println("[sse] GET /pedidos?token=... -> 200 sem header Authorization OK "
                + "(EventSource do navegador usa esse caminho para /pedidos/stream)");
    }
}
