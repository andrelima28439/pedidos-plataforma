package com.pedidos.pagamento.web;

import com.pedidos.pagamento.domain.Pagamento;
import com.pedidos.pagamento.domain.PagamentoDlq;
import com.pedidos.pagamento.repository.PagamentoDlqRepository;
import com.pedidos.pagamento.repository.PagamentoRepository;
import com.pedidos.pagamento.service.PagamentoService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final PagamentoRepository pagamentos;
    private final PagamentoDlqRepository dlq;
    private final PagamentoService service;

    public PagamentoController(PagamentoRepository pagamentos, PagamentoDlqRepository dlq, PagamentoService service) {
        this.pagamentos = pagamentos;
        this.dlq = dlq;
        this.service = service;
    }

    @GetMapping
    public List<PagamentoDto> listar() {
        return pagamentos.findAll().stream().map(PagamentoDto::from).toList();
    }

    @GetMapping("/dlq")
    public List<DlqDto> listarDlq() {
        return dlq.findAll().stream().map(DlqDto::from).toList();
    }

    @PostMapping("/dlq/{eventId}/reprocessar")
    public PagamentoDto reprocessar(@PathVariable String eventId) {
        return PagamentoDto.from(service.reprocessarDlq(eventId));
    }

    /** Agregador simples para o painel de observabilidade do frontend (secao 8). */
    @GetMapping("/observabilidade")
    public ObservabilidadeDto observabilidade() {
        Instant desde = Instant.now().minus(24, ChronoUnit.HOURS);
        long total24h = pagamentos.countByDataProcessamentoAfter(desde);
        long comRetry24h = pagamentos.countByDataProcessamentoAfterAndTentativasGreaterThan(desde, 1);
        long dlqPendente = dlq.countByStatus(PagamentoDlq.DlqStatus.PENDENTE);
        double taxaRetry = total24h == 0 ? 0.0 : (double) comRetry24h / total24h;
        return new ObservabilidadeDto(dlqPendente, total24h, comRetry24h, taxaRetry);
    }

    public record ObservabilidadeDto(long dlqPendente, long total24h, long comRetry24h, double taxaRetry) {}

    public record PagamentoDto(UUID id, String eventId, UUID pedidoId, String status, int tentativas, String valor) {
        static PagamentoDto from(Pagamento p) {
            return new PagamentoDto(
                    p.getId(),
                    p.getEventId(),
                    p.getPedidoId(),
                    p.getStatus().name(),
                    p.getTentativas(),
                    p.getValor().toPlainString());
        }
    }

    public record DlqDto(String eventId, UUID pedidoId, String motivo, int tentativas, String status) {
        static DlqDto from(PagamentoDlq d) {
            return new DlqDto(
                    d.getEventId(),
                    d.getPedidoId(),
                    d.getMotivo(),
                    d.getTentativas(),
                    d.getStatus().name());
        }
    }
}
