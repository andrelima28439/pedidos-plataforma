package com.pedidos.pagamento.repository;

import com.pedidos.pagamento.domain.PagamentoDlq;
import com.pedidos.pagamento.domain.PagamentoDlq.DlqStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoDlqRepository extends JpaRepository<PagamentoDlq, String> {
    List<PagamentoDlq> findByStatusOrderByCriadoEmDesc(DlqStatus status);

    long countByStatus(DlqStatus status);
}
