package com.pedidos.pagamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pagamento_dlq")
public class PagamentoDlq {

    @Id
    private String eventId;

    @Column(nullable = false)
    private UUID pedidoId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 1000)
    private String motivo;

    @Column(nullable = false)
    private int tentativas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DlqStatus status;

    @Column(nullable = false)
    private Instant criadoEm;

    private String correlationId;

    protected PagamentoDlq() {}

    public PagamentoDlq(
            String eventId, UUID pedidoId, BigDecimal valor, String motivo, int tentativas, String correlationId) {
        this.eventId = eventId;
        this.pedidoId = pedidoId;
        this.valor = valor;
        this.motivo = motivo;
        this.tentativas = tentativas;
        this.status = DlqStatus.PENDENTE;
        this.criadoEm = Instant.now();
        this.correlationId = correlationId;
    }

    public enum DlqStatus {
        PENDENTE,
        REPROCESSADO
    }

    public String getEventId() {
        return eventId;
    }

    public UUID getPedidoId() {
        return pedidoId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getMotivo() {
        return motivo;
    }

    public int getTentativas() {
        return tentativas;
    }

    public DlqStatus getStatus() {
        return status;
    }

    public void setStatus(DlqStatus status) {
        this.status = status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
