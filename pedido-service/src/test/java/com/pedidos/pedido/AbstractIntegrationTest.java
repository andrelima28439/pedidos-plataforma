package com.pedidos.pedido;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integracao contra o Postgres real do docker-compose (localhost:5432).
 * Sem Testcontainers aqui pelo mesmo motivo da Etapa 2 (Docker 29/Windows
 * Npipe 400). Kafka nao e necessario nos testes: publisher e mockado e o
 * consumer e invocado diretamente (logica idempotente identica a do listener).
 * Reavaliar Testcontainers no CI Linux (Etapa 9).
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {}
