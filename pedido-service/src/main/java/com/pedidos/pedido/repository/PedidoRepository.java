package com.pedidos.pedido.repository;

import com.pedidos.pedido.domain.Pedido;
import com.pedidos.pedido.domain.PedidoStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
    List<Pedido> findByClienteIdOrderByCriadoEmDesc(String clienteId);

    long countByStatus(PedidoStatus status);
}
