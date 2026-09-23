package com.pedidos.pagamento.metrics;

import com.pedidos.pagamento.domain.PagamentoDlq;
import com.pedidos.pagamento.repository.PagamentoDlqRepository;
import com.pedidos.pagamento.repository.PagamentoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Métricas do dashboard Grafana, todas derivadas do Postgres —
 * a mesma fonte do endpoint GET /pagamentos/observabilidade que o
 * frontend consome (números batem por construção, não há cálculo duplo).
 * - pagamento_dlq_pendente: gauge(count PENDENTE)
 * - pagamento_total_24h / pagamento_com_retry_24h: gauges (janela 24h)
 * O counter pagamento_retry_total (tentativas transitorias) é incrementado
 * no PagamentoService a cada retry.
 */
@Component
public class PagamentoMetrics {

    public PagamentoMetrics(PagamentoDlqRepository dlq, PagamentoRepository pagamentos, MeterRegistry registry) {
        registry.gauge("pagamento_dlq_pendente", dlq, r -> (double) r.countByStatus(PagamentoDlq.DlqStatus.PENDENTE));
        registry.gauge("pagamento_total_24h", pagamentos, r ->
                (double) r.countByDataProcessamentoAfter(Instant.now().minus(24, ChronoUnit.HOURS)));
        registry.gauge(
                "pagamento_com_retry_24h",
                pagamentos,
                r -> (double) r.countByDataProcessamentoAfterAndTentativasGreaterThan(
                        Instant.now().minus(24, ChronoUnit.HOURS), 1));
    }
}
