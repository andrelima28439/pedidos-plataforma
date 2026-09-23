package com.pedidos.pedido.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PedidoEvents {

    public record ItemEvento(String produtoId, int quantidade, BigDecimal precoUnitario) {}

    public record PedidoCriadoEvent(
            String eventId,
            UUID pedidoId,
            String clienteId,
            BigDecimal valorTotal,
            List<ItemEvento> itens,
            String correlationId) {}

    public record PagamentoResultadoEvent(String eventId, UUID pedidoId, BigDecimal valor, String correlationId) {}
}
