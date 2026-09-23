package com.pedidos.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedidos.pagamento.aws.AltoValorService;
import com.pedidos.pagamento.aws.S3ComprovanteService;
import com.pedidos.pagamento.domain.Pagamento;
import com.pedidos.pagamento.domain.PagamentoDlq;
import com.pedidos.pagamento.domain.PagamentoStatus;
import com.pedidos.pagamento.events.PagamentoEvents.PedidoCriadoEvent;
import com.pedidos.pagamento.repository.PagamentoDlqRepository;
import com.pedidos.pagamento.repository.PagamentoRepository;
import com.pedidos.pagamento.service.MockGateway;
import com.pedidos.pagamento.service.PagamentoEventPublisher;
import com.pedidos.pagamento.service.PagamentoService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova 2 (secao 5.3 + 5.2): falha definitiva apos N tentativas cai na DLQ
 * (topico pagamento.dlq + tabela), e o endpoint de reprocessar funciona.
 */
@AutoConfigureMockMvc
class DlqReprocessarTest extends AbstractIntegrationTest {

    @Autowired
    PagamentoService service;

    @Autowired
    MockGateway gateway;

    @Autowired
    PagamentoRepository pagamentos;

    @Autowired
    PagamentoDlqRepository dlq;

    @Autowired
    MockMvc mvc;

    @MockBean
    PagamentoEventPublisher publisher;

    @MockBean
    S3ComprovanteService s3;

    @MockBean
    AltoValorService altoValor;

    @BeforeEach
    void limpar() {
        dlq.deleteAll();
        pagamentos.deleteAll();
        gateway.setSempreFalhar(false);
        gateway.falharProximas(0);
    }

    @Test
    void falhaDefinitivaCaiNaDlqEReprocessarFunciona() throws Exception {
        gateway.setSempreFalhar(true);
        var evento = new PedidoCriadoEvent(
                "evt-dlq-1", UUID.randomUUID(), "cliente-9", new BigDecimal("100.00"), null, "corr-9");

        Pagamento resultado = service.processar(evento);

        assertEquals(PagamentoStatus.DLQ, resultado.getStatus());
        assertEquals(3, resultado.getTentativas(), "max-tentativas=3");
        assertEquals(1, dlq.count(), "1 registro PENDENTE na DLQ");
        PagamentoDlq registro = dlq.findById("evt-dlq-1").orElseThrow();
        assertEquals(PagamentoDlq.DlqStatus.PENDENTE, registro.getStatus());
        verify(publisher, times(1)).publicarDlq(any());
        System.out.println("[dlq] eventId=evt-dlq-1 caiu na DLQ apos 3 tentativas, "
                + "topico pagamento.dlq publicado, tabela PENDENTE=1");

        mvc.perform(get("/pagamentos/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        System.out.println("[dlq] GET /pagamentos/dlq lista 1 PENDENTE");

        // Gateway volta ao normal: reprocessar deve aprovar e sair da DLQ
        gateway.setSempreFalhar(false);
        mvc.perform(post("/pagamentos/dlq/evt-dlq-1/reprocessar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.eventId").value("evt-dlq-1"));
        verify(publisher, times(1)).publicarAprovado(any());

        PagamentoDlq apos = dlq.findById("evt-dlq-1").orElseThrow();
        assertEquals(PagamentoDlq.DlqStatus.REPROCESSADO, apos.getStatus());
        System.out.println("[dlq] POST /pagamentos/dlq/evt-dlq-1/reprocessar -> APROVADO, "
                + "DLQ marcada REPROCESSADO, mensagem saiu da fila pendente");
    }
}
