package com.pedidos.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pedidos.pagamento.aws.AltoValorService;
import com.pedidos.pagamento.aws.S3ComprovanteService;
import com.pedidos.pagamento.domain.Pagamento;
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
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Prova 1 (secao 5.3): falha transitoria seguida de sucesso no retry,
 * com backoff exponencial visivel no log.
 */
class RetrySucessoTest extends AbstractIntegrationTest {

    @Autowired
    PagamentoService service;

    @Autowired
    MockGateway gateway;

    @Autowired
    PagamentoRepository pagamentos;

    @Autowired
    PagamentoDlqRepository dlq;

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
    void falhaTransitoriaDuasVezesDepoisSucessoNoRetry() {
        gateway.falharProximas(2);
        var evento = new PedidoCriadoEvent(
                "evt-retry-1", UUID.randomUUID(), "cliente-1", new BigDecimal("250.00"), null, "corr-1");

        long inicio = System.currentTimeMillis();
        Pagamento resultado = service.processar(evento);
        long duracao = System.currentTimeMillis() - inicio;

        assertEquals(PagamentoStatus.APROVADO, resultado.getStatus());
        assertEquals(3, resultado.getTentativas(), "2 falhas + 1 sucesso = 3 tentativas");
        assertEquals(0, dlq.count(), "sucesso no retry nao vai para DLQ");
        verify(publisher, times(1)).publicarAprovado(any());
        verify(publisher, never()).publicarDlq(any());
        // backoff 50ms + 100ms = 150ms minimo (com margem)
        assertTrue(duracao >= 140, "backoff exponencial deve atrasar retries, duracao=" + duracao);
        System.out.println("[retry] SUCESSO na tentativa 3 apos 2 falhas transitorias, "
                + "backoffs 50ms+100ms, duracao=" + duracao + "ms, status=APROVADO, dlq=0");
    }

    @Test
    void valorTerminadoEm99ERecusadoSemRetry() {
        var evento = new PedidoCriadoEvent(
                "evt-recusa-99", UUID.randomUUID(), "cliente-2", new BigDecimal("199.99"), null, "corr-2");

        Pagamento resultado = service.processar(evento);

        assertEquals(PagamentoStatus.RECUSADO, resultado.getStatus());
        assertEquals(1, resultado.getTentativas(), "recusa de negocio nao retenta");
        verify(publisher, times(1)).publicarRecusado(any());
        System.out.println("[regra .99] valor 199.99 RECUSADO em 1 tentativa, sem retry nem DLQ");
    }
}
