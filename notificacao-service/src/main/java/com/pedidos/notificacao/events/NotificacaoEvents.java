package com.pedidos.notificacao.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class NotificacaoEvents {

    public record ItemEvento(String produtoId, int quantidade, BigDecimal precoUnitario) {}

    public record PedidoCriadoEvent(
            String eventId,
            UUID pedidoId,
            String clienteId,
            BigDecimal valorTotal,
            List<ItemEvento> itens,
            String correlationId) {}

    public record PagamentoResultadoEvent(String eventId, UUID pedidoId, BigDecimal valor, String correlationId) {}

    /** Payload interno publicado no RabbitMQ. */
    public record NotificacaoPayload(
            String eventId, String tipo, String pedidoId, String correlationId, String mensagem) {}
}
