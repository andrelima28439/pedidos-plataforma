package com.pedidos.pagamento.repository;

import com.pedidos.pagamento.domain.Pagamento;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
    Optional<Pagamento> findByEventId(String eventId);

    long countByDataProcessamentoAfter(Instant desde);

    long countByDataProcessamentoAfterAndTentativasGreaterThan(Instant desde, int tentativas);
}
