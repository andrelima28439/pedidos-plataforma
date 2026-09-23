package com.pedidos.pedido.repository;

import com.pedidos.pedido.domain.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {}
