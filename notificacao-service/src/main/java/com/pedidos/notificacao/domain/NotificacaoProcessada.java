package com.pedidos.notificacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notificacoes_processadas")
public class NotificacaoProcessada {

    @Id
    private String eventId;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private Instant processadaEm;

    protected NotificacaoProcessada() {}

    public NotificacaoProcessada(String eventId, String tipo) {
        this.eventId = eventId;
        this.tipo = tipo;
        this.processadaEm = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public String getTipo() {
        return tipo;
    }

    public Instant getProcessadaEm() {
        return processadaEm;
    }
}
