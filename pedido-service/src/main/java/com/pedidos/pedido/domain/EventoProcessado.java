package com.pedidos.pedido.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Idempotencia do consumidor: um eventId processado nunca gera efeito duplo.
 */
@Entity
@Table(name = "eventos_processados")
public class EventoProcessado {

    @Id
    private String eventId;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private Instant processadoEm;

    protected EventoProcessado() {}

    public EventoProcessado(String eventId, String tipo) {
        this.eventId = eventId;
        this.tipo = tipo;
        this.processadoEm = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public String getTipo() {
        return tipo;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }
}
