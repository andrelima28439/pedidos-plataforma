package com.pedidos.pagamento.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PagamentoEvents {

    public record ItemEvento(String produtoId, int quantidade, BigDecimal precoUnitario) {}

    public record PedidoCriadoEvent(
            String eventId,
            UUID pedidoId,
            String clienteId,
            BigDecimal valorTotal,
            List<ItemEvento> itens,
            String correlationId) {}

    public record PagamentoResultadoEvent(String eventId, UUID pedidoId, BigDecimal valor, String correlationId) {}

    public record PagamentoDlqEvent(
            String eventId, UUID pedidoId, BigDecimal valor, String motivo, int tentativas, String correlationId) {}
}
