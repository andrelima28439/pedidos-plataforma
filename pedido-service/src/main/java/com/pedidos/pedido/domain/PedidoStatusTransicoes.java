package com.pedidos.pedido.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Transicoes de estado explicitas do agregado Pedido.
 * Nao e um enum solto: toda mudanca passa por aqui.
 */
public final class PedidoStatusTransicoes {

    private static final Map<PedidoStatus, Set<PedidoStatus>> PERMITIDAS = new EnumMap<>(PedidoStatus.class);

    static {
        PERMITIDAS.put(PedidoStatus.CRIADO, EnumSet.of(PedidoStatus.AGUARDANDO_PAGAMENTO, PedidoStatus.CANCELADO));
        PERMITIDAS.put(
                PedidoStatus.AGUARDANDO_PAGAMENTO,
                EnumSet.of(PedidoStatus.PAGO, PedidoStatus.RECUSADO, PedidoStatus.CANCELADO));
        PERMITIDAS.put(PedidoStatus.PAGO, EnumSet.noneOf(PedidoStatus.class));
        PERMITIDAS.put(PedidoStatus.RECUSADO, EnumSet.noneOf(PedidoStatus.class));
        PERMITIDAS.put(PedidoStatus.CANCELADO, EnumSet.noneOf(PedidoStatus.class));
    }

    private PedidoStatusTransicoes() {}

    public static void validar(PedidoStatus atual, PedidoStatus proximo) {
        Set<PedidoStatus> permitidos = PERMITIDAS.getOrDefault(atual, Set.of());
        if (!permitidos.contains(proximo)) {
            throw new TransicaoInvalidaException(atual, proximo);
        }
    }

    public static class TransicaoInvalidaException extends RuntimeException {
        public TransicaoInvalidaException(PedidoStatus atual, PedidoStatus proximo) {
            super("transicao invalida: " + atual + " -> " + proximo);
        }
    }
}
