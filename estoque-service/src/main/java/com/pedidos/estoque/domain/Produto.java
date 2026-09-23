package com.pedidos.estoque.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "produtos")
public class Produto {

    @Id
    private String id;

    private String nome;
    private String descricao;
    private BigDecimal preco;
    private int quantidadeDisponivel;
    private int quantidadeReservada;
    private String categoria;

    @Version
    private Long version;

    public Produto() {}

    public Produto(String nome, String descricao, BigDecimal preco, int quantidadeDisponivel, String categoria) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.quantidadeReservada = 0;
        this.categoria = categoria;
    }

    @JsonIgnore
    public int getQuantidadeLivre() {
        return quantidadeDisponivel - quantidadeReservada;
    }

    /** Reserva N unidades: bloqueia sem decrementar o estoque fisico. */
    public void reservar(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade deve ser maior que zero");
        }
        if (getQuantidadeLivre() < quantidade) {
            throw new EstoqueInsuficienteException(
                    "estoque insuficiente: livre=" + getQuantidadeLivre() + ", solicitado=" + quantidade);
        }
        this.quantidadeReservada += quantidade;
    }

    /** Libera uma reserva anterior (pagamento recusado). */
    public void liberar(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade deve ser maior que zero");
        }
        if (quantidadeReservada < quantidade) {
            throw new IllegalArgumentException(
                    "nao ha reserva suficiente para liberar: reservada=" + quantidadeReservada);
        }
        this.quantidadeReservada -= quantidade;
    }

    /** Confirma a reserva: decrementa o estoque de fato (pagamento aprovado). */
    public void confirmar(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade deve ser maior que zero");
        }
        if (quantidadeReservada < quantidade) {
            throw new IllegalArgumentException(
                    "nao ha reserva suficiente para confirmar: reservada=" + quantidadeReservada);
        }
        this.quantidadeReservada -= quantidade;
        this.quantidadeDisponivel -= quantidade;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public int getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    public void setQuantidadeDisponivel(int quantidadeDisponivel) {
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    public int getQuantidadeReservada() {
        return quantidadeReservada;
    }

    public void setQuantidadeReservada(int quantidadeReservada) {
        this.quantidadeReservada = quantidadeReservada;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
