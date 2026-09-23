package com.pedidos.pedido;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pedidos.pedido.domain.PedidoStatus;
import com.pedidos.pedido.domain.PedidoStatusTransicoes;
import com.pedidos.pedido.domain.PedidoStatusTransicoes.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;

class TransicaoStatusTest {

    @Test
    void transicoesValidasPassam() {
        assertDoesNotThrow(
                () -> PedidoStatusTransicoes.validar(PedidoStatus.CRIADO, PedidoStatus.AGUARDANDO_PAGAMENTO));
        assertDoesNotThrow(() -> PedidoStatusTransicoes.validar(PedidoStatus.AGUARDANDO_PAGAMENTO, PedidoStatus.PAGO));
        assertDoesNotThrow(
                () -> PedidoStatusTransicoes.validar(PedidoStatus.AGUARDANDO_PAGAMENTO, PedidoStatus.RECUSADO));
        System.out.println("[transicao] validas OK");
    }

    @Test
    void canceladoNaoPodeIrParaPago() {
        assertThrows(
                TransicaoInvalidaException.class,
                () -> PedidoStatusTransicoes.validar(PedidoStatus.CANCELADO, PedidoStatus.PAGO));
        assertThrows(
                TransicaoInvalidaException.class,
                () -> PedidoStatusTransicoes.validar(PedidoStatus.PAGO, PedidoStatus.CANCELADO));
        assertThrows(
                TransicaoInvalidaException.class,
                () -> PedidoStatusTransicoes.validar(PedidoStatus.CRIADO, PedidoStatus.PAGO));
        System.out.println("[transicao] CANCELADO->PAGO e CRIADO->PAGO recusadas corretamente");
    }
}
