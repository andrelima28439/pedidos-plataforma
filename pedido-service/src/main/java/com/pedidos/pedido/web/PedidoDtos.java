package com.pedidos.pedido.web;

import com.pedidos.pedido.domain.Pedido;
import com.pedidos.pedido.domain.PedidoHistorico;
import com.pedidos.pedido.domain.PedidoStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PedidoDtos {

    public record ItemRequest(
            @NotBlank String produtoId,
            @Min(1) int quantidade,
            @NotNull @DecimalMin("0.01") BigDecimal precoUnitario) {}

    public record CriarPedidoRequest(@NotEmpty List<@Valid ItemRequest> itens) {}

    public record HistoricoResponse(PedidoStatus de, PedidoStatus para, Instant em, String motivo) {
        static HistoricoResponse from(PedidoHistorico h) {
            return new HistoricoResponse(h.getDe(), h.getPara(), h.getEm(), h.getMotivo());
        }
    }

    public record PedidoResponse(
            UUID id,
            String clienteId,
            BigDecimal valorTotal,
            PedidoStatus status,
            Instant criadoEm,
            List<ItemResponse> itens,
            List<HistoricoResponse> historico) {
        static PedidoResponse from(Pedido p) {
            return new PedidoResponse(
                    p.getId(),
                    p.getClienteId(),
                    p.getValorTotal(),
                    p.getStatus(),
                    p.getCriadoEm(),
                    p.getItens().stream()
                            .map(i -> new ItemResponse(i.getProdutoId(), i.getQuantidade(), i.getPrecoUnitario()))
                            .toList(),
                    p.getHistorico().stream().map(HistoricoResponse::from).toList());
        }
    }

    public record ItemResponse(String produtoId, int quantidade, BigDecimal precoUnitario) {}
}
