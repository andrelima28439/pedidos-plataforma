package com.pedidos.pedido.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "pedido_historico")
public class PedidoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    private PedidoStatus de;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PedidoStatus para;

    @Column(nullable = false)
    private Instant em;

    private String motivo;

    protected PedidoHistorico() {}

    public PedidoHistorico(Pedido pedido, PedidoStatus de, PedidoStatus para, String motivo) {
        this.pedido = pedido;
        this.de = de;
        this.para = para;
        this.em = Instant.now();
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public PedidoStatus getDe() {
        return de;
    }

    public PedidoStatus getPara() {
        return para;
    }

    public Instant getEm() {
        return em;
    }

    public String getMotivo() {
        return motivo;
    }
}
