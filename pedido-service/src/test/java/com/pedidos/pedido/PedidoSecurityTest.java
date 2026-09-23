package com.pedidos.pedido;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PedidoSecurityTest extends AbstractIntegrationTest {

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
    void semTokenRetorna403EClienteSoVePropriosPedidos() throws Exception {
        mvc.perform(get("/pedidos")).andExpect(status().isForbidden());
        System.out.println("[seguranca] sem token -> 403 OK");

        String tokenA = jwt.gerar("cliente-A");
        String tokenB = jwt.gerar("cliente-B");

        String body = """
                {"itens":[{"produtoId":"p1","quantidade":1,"precoUnitario":10.00}]}""";
        String resp = mvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value("cliente-A"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String pedidoId = resp.split("\"id\":\"")[1].split("\"")[0];
        System.out.println("[seguranca] cliente-A criou pedido " + pedidoId);

        mvc.perform(get("/pedidos").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/pedidos").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        System.out.println("[seguranca] cliente-B nao lista pedido de A (0 itens) OK");

        mvc.perform(get("/pedidos/" + pedidoId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        System.out.println("[seguranca] cliente-B nao acessa detalhe de A (404) OK");

        mvc.perform(get("/pedidos/" + pedidoId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historico.length()").value(2));
        System.out.println("[seguranca] cliente-A acessa proprio pedido com historico=2 OK");
    }
}
