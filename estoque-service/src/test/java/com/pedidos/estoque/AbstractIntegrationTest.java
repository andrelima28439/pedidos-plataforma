package com.pedidos.estoque;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste de integracao contra a infra real do docker-compose
 * (MongoDB localhost:27017 + Redis localhost:6379).
 *
 * Motivo: Testcontainers 1.19.x (gerenciado pelo Boot 3.2.5) nao e compativel
 * com o Docker Desktop 29.x no Windows (Npipe 400). Usar os servicos reais
 * do compose mantem o teste "de verdade" (nada mockado) sem contornar a
 * condicao de corrida. Pedido-service avaliara upgrade do Testcontainers.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {}
