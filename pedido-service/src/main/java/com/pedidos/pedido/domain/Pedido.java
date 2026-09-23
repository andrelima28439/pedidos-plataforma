package com.pedidos.pedido.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String clienteId;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PedidoItem> itens = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PedidoStatus status;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PedidoHistorico> historico = new ArrayList<>();

    @Column(nullable = false)
    private Instant criadoEm;

    @Version
    private Long version;

    protected Pedido() {}

    public Pedido(String clienteId, List<PedidoItem> itens) {
        this.id = UUID.randomUUID();
        this.clienteId = clienteId;
        this.itens = new ArrayList<>(itens);
        this.itens.forEach(i -> i.setPedido(this));
        this.valorTotal = itens.stream()
                .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.status = PedidoStatus.CRIADO;
        this.criadoEm = Instant.now();
        registrarHistorico(null, PedidoStatus.CRIADO, "pedido criado");
    }

    public void avancarPara(PedidoStatus proximo, String motivo) {
        PedidoStatusTransicoes.validar(this.status, proximo);
        PedidoStatus anterior = this.status;
        this.status = proximo;
        registrarHistorico(anterior, proximo, motivo);
    }

    private void registrarHistorico(PedidoStatus de, PedidoStatus para, String motivo) {
        PedidoHistorico h = new PedidoHistorico(this, de, para, motivo);
        this.historico.add(h);
    }

    public UUID getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public List<PedidoItem> getItens() {
        return itens;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public PedidoStatus getStatus() {
        return status;
    }

    public List<PedidoHistorico> getHistorico() {
        return historico;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
