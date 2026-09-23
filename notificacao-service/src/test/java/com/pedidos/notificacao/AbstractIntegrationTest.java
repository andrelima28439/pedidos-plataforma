package com.pedidos.notificacao;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integracao contra Postgres + RabbitMQ + Kafka reais do docker-compose.
 * Sem Testcontainers API (mesmo motivo Windows/Npipe das etapas anteriores).
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {}
