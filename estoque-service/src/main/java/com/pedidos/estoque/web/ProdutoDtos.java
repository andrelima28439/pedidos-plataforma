package com.pedidos.estoque.web;

import com.pedidos.estoque.domain.Produto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ProdutoDtos {

    public record CriarProdutoRequest(
            @NotBlank String nome,
            String descricao,
            @NotNull @DecimalMin("0.01") BigDecimal preco,
            @Min(0) int quantidadeDisponivel,
            String categoria) {
        public Produto toEntity() {
            return new Produto(nome, descricao, preco, quantidadeDisponivel, categoria);
        }
    }

    public record QuantidadeRequest(@Min(1) int quantidade) {}

    public record ProdutoResponse(
            String id,
            String nome,
            String descricao,
            BigDecimal preco,
            int quantidadeDisponivel,
            int quantidadeReservada,
            int quantidadeLivre,
            String categoria,
            Long version) {
        public static ProdutoResponse from(Produto p) {
            return new ProdutoResponse(
                    p.getId(),
                    p.getNome(),
                    p.getDescricao(),
                    p.getPreco(),
                    p.getQuantidadeDisponivel(),
                    p.getQuantidadeReservada(),
                    p.getQuantidadeLivre(),
                    p.getCategoria(),
                    p.getVersion());
        }
    }
}
