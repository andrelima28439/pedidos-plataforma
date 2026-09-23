package com.pedidos.notificacao.repository;

import com.pedidos.notificacao.domain.NotificacaoProcessada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoProcessadaRepository extends JpaRepository<NotificacaoProcessada, String> {}
