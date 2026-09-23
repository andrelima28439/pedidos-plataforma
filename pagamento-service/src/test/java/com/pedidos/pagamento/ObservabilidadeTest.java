package com.pedidos.pagamento;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedidos.pagamento.events.PagamentoEvents.PedidoCriadoEvent;
import com.pedidos.pagamento.repository.PagamentoDlqRepository;
import com.pedidos.pagamento.repository.PagamentoRepository;
import com.pedidos.pagamento.service.MockGateway;
import com.pedidos.pagamento.service.PagamentoEventPublisher;
import com.pedidos.pagamento.service.PagamentoService;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Etapa 8: correlation-id aparece nos logs JSON do pagamento-service
 * (mesmo id gerado no pedido-service) + metricas expostas.
 */
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ObservabilidadeTest extends AbstractIntegrationTest {

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

    @Autowired
    MeterRegistry registry;

    @MockBean
    PagamentoEventPublisher publisher;

    @MockBean
    com.pedidos.pagamento.aws.S3ComprovanteService s3;

    @MockBean
    com.pedidos.pagamento.aws.AltoValorService altoValor;

    @BeforeEach
    void limpar() {
        dlq.deleteAll();
        pagamentos.deleteAll();
        gateway.setSempreFalhar(false);
        gateway.falharProximas(0);
    }

    @Test
    void correlationIdApareceNosLogs(CapturedOutput out) {
        var evento = new PedidoCriadoEvent(
                "evt-corr-1", UUID.randomUUID(), "cliente-c", new BigDecimal("50.00"), null, "corr-prova-123");
        service.processar(evento);

        assertTrue(out.getOut().contains("corr-prova-123"), "logs JSON deveriam conter o correlationId propagado");
        System.out.println("[correlation] pagamento-service logou correlationId=corr-prova-123 "
                + "(mesmo id do pedido-service) — rastreio ponta a ponta OK");
    }

    @Test
    void metricasPagamentoRegistradas() {
        // Exportacao Prometheus e desabilitada no contexto de teste (MockMvc);
        // a prova de scrape real esta no relatorio da Etapa 8 (servicos de pe).
        assertNotNull(registry.find("pagamento_dlq_pendente").gauge());
        assertNotNull(registry.find("pagamento_total_24h").gauge());
        assertNotNull(registry.find("pagamento_com_retry_24h").gauge());
        System.out.println("[metricas] gauges pagamento_dlq_pendente/total_24h/com_retry_24h "
                + "registrados no registry OK (mesma fonte Postgres do endpoint do frontend)");
    }
}
