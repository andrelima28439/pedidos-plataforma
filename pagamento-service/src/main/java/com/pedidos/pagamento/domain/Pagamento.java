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
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private UUID pedidoId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PagamentoStatus status;

    @Column(nullable = false)
    private int tentativas;

    @Column(nullable = false)
    private Instant dataProcessamento;

    private String correlationId;

    protected Pagamento() {}

    public Pagamento(
            String eventId,
            UUID pedidoId,
            BigDecimal valor,
            PagamentoStatus status,
            int tentativas,
            String correlationId) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.pedidoId = pedidoId;
        this.valor = valor;
        this.status = status;
        this.tentativas = tentativas;
        this.dataProcessamento = Instant.now();
        this.correlationId = correlationId;
    }

    public UUID getId() {
        return id;
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

    public PagamentoStatus getStatus() {
        return status;
    }

    public void setStatus(PagamentoStatus status) {
        this.status = status;
    }

    public int getTentativas() {
        return tentativas;
    }

    public void setTentativas(int tentativas) {
        this.tentativas = tentativas;
    }

    public Instant getDataProcessamento() {
        return dataProcessamento;
    }

    public void setDataProcessamento(Instant dataProcessamento) {
        this.dataProcessamento = dataProcessamento;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
