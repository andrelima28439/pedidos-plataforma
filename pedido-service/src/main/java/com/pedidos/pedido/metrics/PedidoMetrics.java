package com.pedidos.pedido.metrics;

import com.pedidos.pedido.domain.PedidoStatus;
import com.pedidos.pedido.repository.PedidoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Taxa de pedidos por status (dashboard Grafana).
 * Gauges com supplier no Postgres — mesma fonte do painel do frontend.
 */
@Component
public class PedidoMetrics {

    public PedidoMetrics(PedidoRepository repository, MeterRegistry registry) {
        for (PedidoStatus status : PedidoStatus.values()) {
            registry.gauge("pedidos_por_status", List.of(Tag.of("status", status.name())), repository, r ->
                    (double) r.countByStatus(status));
        }
    }
}
