# Decisões Técnicas (ADRs)

Registro das decisões de arquitetura e seus motivos.

## Infraestrutura

### ADR-001: Kafka + RabbitMQ coexistindo
- **Contexto:** dois modelos de mensageria necessários: streaming de eventos de domínio (com replay) e filas ponto-a-ponto de notificação.
- **Decisão:** Kafka para eventos de domínio (pedido.criado, pagamento.*) com replay e DLQ por tópico; RabbitMQ para notificações ponto-a-ponto (fila por tipo) com DLQ nativa e management UI.
- **Motivo:** Kafka escala melhor para event streaming entre 4 serviços; RabbitMQ é mais simples para fan-out de notificações e demonstra familiaridade com os dois modelos.

### ADR-002: MongoDB vs Postgres
- **Contexto:** catálogo de produtos (schema flexível, leitura rápida) vs dados transacionais (pedidos, pagamentos, estoque reservado — onde ACID importa).
- **Decisão:** MongoDB para catálogo; Postgres para dados transacionais. O `estoque-service` usa MongoDB para `Produto` (id, nome, descrição, preço, disponível, reservada, categoria, version).
- **Motivo:** cada dado no banco cujo modelo combina com ele; Postgres guarda pedidos, pagamentos, DLQs e tabelas de idempotência.

### ADR-003: Redis — o que é cacheado e TTL
- **Contexto:** quais leituras cachear e com qual TTL, sem vender com dado obsoleto.
- **Decisão:** `GET /produtos` e `GET /produtos/{id}` cacheados no Redis com TTL de **60s**.
- **Motivo:** catálogo muda pouco, mas reserva/confirma/liberar alteram visibilidade de estoque; mutações fazem `evict` total. 60s equilibra hit-rate e frescor sem complexidade de invalidação fina.

### ADR-004: Maven multi-módulo + Java 21 + Spring Boot 3.2.5
- **Contexto:** build tool em aberto (Maven ou Gradle); Java 21 (LTS) como versão alvo.
- **Decisão:** `pom.xml` pai na raiz (herda de `spring-boot-starter-parent`), módulos por serviço; Java 21 LTS.
- **Motivo:** decisão mais comum no ecossistema; parent-first garante versões consistentes. Testes de integração rodam contra Mongo/Redis reais do `docker-compose` (localhost:27017/6379) em vez de Testcontainers: o cliente Java do Testcontainers 1.19.x (gerenciado pelo Boot 3.2.5) falha no Docker Desktop no Windows (`Npipe 400`, `Could not find a valid Docker environment`). Usar a infra real mantém o teste "de verdade" (nada mockado). Reavaliar no CI Linux.

## estoque-service

### ADR-005: Reserva com lock otimista (@Version)
- **Contexto:** Mongo standalone (docker-compose) não tem replica-set para transações multi-documento; duas reservas simultâneas não podem vender além do estoque.
- **Decisão:** `Produto.version` com `@Version` do Spring Data MongoDB em vez de transação.
- **Motivo:** lock otimista resolve a condição de corrida (duas reservas simultâneas → 1 sucesso + 1x 409, sem oversell) e é o padrão mais simples no ecossistema Spring. Coberto por teste de concorrência.

## pedido-service

### ADR-006: JWT próprio em vez de Keycloak
- **Contexto:** autenticação sem Keycloak — o foco do projeto é resiliência de eventos, não SSO.
- **Decisão:** emissão própria com Spring Security + JJWT (`POST /auth/token` com `{clienteId}`), sem Keycloak.
- **Motivo:** o mais simples no ecossistema Boot e suficiente para isolar pedidos por cliente. Limitações: sem refresh, sem RBAC real (ver README).

### ADR-007: Reserva síncrona via REST (pedido -> estoque)
- **Contexto:** reserva via REST síncrono antes do evento vs saga 100% orientada a evento.
- **Decisão:** `POST /pedidos` reserva via REST no `estoque-service` antes de publicar `pedido.criado` (orquestração simples).
- **Motivo:** mais simples de demonstrar e testar; falha de reserva retorna 409 imediato sem evento órfão. Compensação via `confirmar`/`liberar` no consumo do pagamento.

### ADR-008: SSE em vez de WebSocket (GET /pedidos/stream)
- **Contexto:** empurrar atualizações servidor → cliente sem polling (SSE unidirecional vs WebSocket bidirecional).
- **Decisão:** `SseEmitter` para o frontend receber `CRIADO -> AGUARDANDO -> PAGO/RECUSADO` sem polling.
- **Motivo:** fluxo é só servidor → cliente; SSE é nativo no Boot/WebMVC e mais simples que WebSocket. Emitters por `clienteId` notificados no `criar` e no consumer. Teste `SseQueryTokenTest` cobre o token via query string.

### ADR-009: Idempotência via tabela eventos_processados
- **Contexto:** consumers Kafka têm redelivery; reprocessar não pode duplicar efeito. Testcontainers indisponível no Windows (mesmo motivo do ADR-004).
- **Decisão:** `eventos_processados(eventId PK)` checada no início do consumer; segundo delivery do mesmo `eventId` retorna sem efeito (sem novo histórico, sem nova chamada ao estoque).
- **Motivo:** garantia transacional simples. Testes com Postgres real do compose + consumer invocado 2x com o mesmo `eventId`; listeners sobem contra o broker real. No CI (Linux) vale reavaliar Testcontainers padrão.

## pagamento-service

### ADR-010: Retry exponencial + DLQ em Kafka e Postgres
- **Contexto:** o gateway pode falhar de forma transitória (retry) ou definitiva (DLQ); é preciso demonstrar os dois fluxos sem infra externa.
- **Decisão:** `max-tentativas=3`, backoff `50ms * 2^(n-1)` (50ms, 100ms) no `PagamentoService`; esgotou → salva `pagamentos(DLQ)` + `pagamento_dlq(PENDENTE)` e publica `pagamento.dlq`; reprocesso via `POST /pagamentos/dlq/{eventId}/reprocessar` (reprocessa e marca `REPROCESSADO`).
- **Motivo:** mock de gateway com `valores terminados em .99 → RECUSADO` demonstra os dois fluxos; falhas transitórias injetáveis (`falharProximas(N)` / `sempreFalhar`) exercitam retry e DLQ. Testes com Postgres real do compose e publisher mockado: 2 falhas + sucesso na 3ª (duração ≥150ms); 3 falhas → DLQ + reprocessar → APROVADO.

## notificacao-service

### ADR-011: Kafka in, RabbitMQ out, DLQ RabbitMQ distinta
- **Contexto:** notificações ponto-a-ponto por tipo, com DLQ própria (distinta da DLQ Kafka do pagamento).
- **Decisão:** consumers Kafka (`pedido.criado`, `pagamento.*`) enfileiram em `notificacao.exchange` (direct) com 1 fila por tipo; cada fila tem `x-dead-letter-exchange=notificacao.dlx` + DLQ própria; `Jackson2JsonMessageConverter` no RabbitMQ.
- **Motivo:** idempotência via `notificacoes_processadas(eventId)` (2ª entrega ignorada, coberto por teste); `EnvioService` simula envio com log + `correlationId`, e payload com `FAIL` lança `AmqpRejectAndDontRequeueException` → broker roteia para a DLQ (coberto por teste). DLQs logadas para ação manual em produção.

## Frontend

### ADR-012: React + SSE + observabilidade simples
- **Contexto:** UI com catálogo, carrinho e pedidos em tempo real, mais painel de DLQ/retry antes do Prometheus.
- **Decisão:** Vite + React + react-router (`frontend/`, `.env.example` com `VITE_*`); 4 rotas (catálogo paginado, carrinho/criar, meus-pedidos SSE, observabilidade com auto-refresh 15s); loading/erro em todas as chamadas.
- **Motivo:** `EventSource` em `GET /pedidos/stream?token=` — `JwtAuthFilter` aceita `?token=` além de `Bearer` (EventSource não envia headers); `SseEmitter` por `clienteId`. Observabilidade via `GET /pagamentos/observabilidade` (`dlqPendente`, `total24h`, `comRetry24h`, `taxaRetry`); CORS liberado para `http://localhost:5173` nos serviços usados pelo browser.

## AWS via LocalStack

### ADR-013: S3 comprovante + SQS alto-valor + Lambda resumo
- **Contexto:** demonstrar S3/SQS/Lambda sem conta AWS real.
- **Decisão:** `pagamento-service` com SDK AWS v2 apontando para `AWS_ENDPOINT` (LocalStack); S3 `comprovantes-pagamentos/comprovantes/{eventId}.json` no APROVADO (best-effort, não quebra o pagamento); SQS `pedidos-alto-valor` para valor **>= 1000.00** (revisão manual/antifraude — documentado no README); Lambda Python `lambda/resumo_diario.py` (`resumo-diario-pedidos`, handler direto + Records SQS) com event source mapping SQS→Lambda.
- **Motivo:** testes de integração verificam comprovante listado no bucket e regra da fila (1500.00 recebida, 100.00 não enviada); script `python lambda/prova_lambda.py` demonstra função ativa, invoke direto e mapping criado. AWS real: ver README "Limitações conhecidas".

## Observabilidade

### ADR-014: Prometheus + Grafana + logs JSON + correlation-id
- **Contexto:** métricas por serviço, dashboard e rastreio ponta a ponta de um pedido pelos logs.
- **Decisão:** `micrometer-registry-prometheus` + `logstash-logback-encoder` nos 4 serviços; `logback-spring.xml` com `LogstashEncoder` (JSON, inclui MDC + `service`); `CorrelationIdFilter` (header `X-Correlation-Id`, gera se ausente e devolve no response); MDC `correlationId` = pedidoId no `criar` e nos consumers/publishers.
- **Motivo:** métricas custom com **fonte única no Postgres** (`pedidos_por_status`, `pagamento_dlq_pendente` / `total_24h` / `com_retry_24h` via mesmas queries do endpoint do frontend) + counter `pagamento_retry_total`. Infra `prometheus` + `grafana` com datasource e dashboard provisionados (`grafana/`); dashboard **Pedidos Plataforma** em JSON no repo, sem configuração manual. Bug real pego no E2E: consumers sem `default.type` + producer com type headers quebravam a desserialização entre serviços; fix com `ADD_TYPE_INFO_HEADERS=false` e `default.type` por listener. Nota de teste: export Prometheus é desabilitado no contexto `@SpringBootTest`; testes verificam o registry e o scrape é validado com os serviços em execução.

## CI/CD

### ADR-015: Compose em vez de Testcontainers
- **Contexto:** Testcontainers falha no Windows (ADR-004/009); no CI Linux (ubuntu-latest) o Docker é nativo. Linter do frontend em aberto (ESLint vs alternativa do template).
- **Decisão:** pipeline (`.github/workflows/ci.yml`, jobs `backend`/`frontend`/`docker`) sobe a infra com `docker compose up` e roda `mvn -B verify` (31 testes + JaCoCo), `spotless:check`, `npm ci/lint/test/build` e `docker build` das 4 imagens.
- **Motivo:** os testes falam com a infra do compose via TCP/localhost, idêntico no Windows e no Actions; migrar para a API Testcontainers daria mais isolamento ao custo de rewrite, sem ganho comportamental; o compose ainda valida o próprio `docker-compose.yml` no CI. Linter: template Vite entrega `oxlint` (`npm run lint`, 0 erros) — mesmo propósito do ESLint, zero config. Formatter backend: Spotless `palantirJavaFormat`.

## Collection Postman

### ADR-016: Postman + endpoint admin DEV para forçar DLQ
- **Contexto:** demonstrar o ciclo consultar DLQ → reprocessar sob demanda.
- **Decisão:** collection `postman/pedidos-plataforma.postman_collection.json` com os 5 fluxos + `3b` (teste automatizado com polling: pedido `RECUSADO` e `quantidadeReservada == 0`) + `A1` (helper); `POST /pagamentos/admin/gateway/falhar-proximas` (DEV-ONLY, nunca expor em prod) permite encher a DLQ sob demanda.
- **Motivo:** cobre caminho feliz, recusa com liberação de estoque e DLQ de ponta a ponta sem mexer no código.
