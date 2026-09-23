package com.pedidos.pagamento;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integracao contra o Postgres real do docker-compose.
 * Publisher Kafka mockado (verificamos o topico correto via Mockito);
 * gateway e real e controlado pelos testes para simular falhas.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {}
