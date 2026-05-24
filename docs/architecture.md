# Arquitetura do Sistema

## Visão Geral

Sistema de pedidos online para restaurante, implementado com **microsserviços em Java 21 + Spring Boot 3.2.5**,
seguindo os princípios de **Clean Architecture**. A comunicação assíncrona entre serviços é feita via **Apache Kafka**,
e o processamento de pagamentos delega a um serviço externo (**Procpag**) com resiliência via **Resilience4j**.

### Estado dos Módulos

| Módulo | Porta | Situação |
|--------|-------|----------|
| `auth-service` | 8081 | **Stub** — apenas `pom.xml` e diretório de migração vazio |
| `pedido-service` | 8082 | **Stub** — apenas `pom.xml` e diretório de migração vazio |
| `pagamento-service` | 8083 | **Implementado** — domínio, casos de uso, gateways (DB/HTTP/Kafka), testes |
| `procpag` (externo) | 8089 | **Fornecido** — simulador de processadora de pagamentos |

---

## Diagrama de Contexto (C4 — Nível 1)

```mermaid
C4Context
  title Contexto do Sistema - Tech Challenge III

  Person(cliente, "Cliente", "Consumidor do restaurante")
  Person(dono, "Dono", "Administrador do restaurante")

  System(auth, "auth-service", "Autenticação e cadastro de usuários com JWT")
  System(pedido, "pedido-service", "Gestão de pedidos e itens")
  System(pagamento, "pagamento-service", "Processamento de pagamentos via eventos")
  System_Ext(procpag, "Procpag", "Processador externo de pagamentos (simulado)")
  System_Ext(kafka, "Apache Kafka", "Mensageria assíncrona entre serviços")

  Rel(cliente, auth, "Cadastra-se e faz login")
  Rel(cliente, pedido, "Cria e consulta pedidos")
  Rel(pedido, kafka, "Publica pedido-criado")
  Rel(kafka, pagamento, "Consome pedido-criado")
  Rel(pagamento, kafka, "Publica pagamento-aprovado / pagamento-pendente")
  Rel(pagamento, procpag, "POST /requisicao para processar pagamento")
```

---

## Diagrama de Containers (C4 — Nível 2)

```mermaid
C4Container
  title Containers do Sistema

  Person(cliente, "Cliente", "Usuário do sistema")

  Container_Boundary(auth_svc, "auth-service") {
    Container(auth_api, "Auth REST API", "Spring Boot", "Endpoints de cadastro e login")
    ContainerDb(auth_db, "auth-db", "PostgreSQL 15", "Dados de usuários e credenciais")
  }

  Container_Boundary(pedido_svc, "pedido-service") {
    Container(pedido_api, "Pedido REST API", "Spring Boot", "Endpoints de criação e consulta de pedidos")
    Container(pedido_consumer, "Pedido Consumer", "Spring Kafka", "Consome eventos de pagamento")
    ContainerDb(pedido_db, "pedido-db", "PostgreSQL 15", "Dados de pedidos e itens")
  }

  Container_Boundary(pagamento_svc, "pagamento-service") {
    Container(pagamento_consumer, "Pagamento Consumer", "Spring Kafka", "Consome pedido-criado")
    Container(pagamento_usecase, "ProcessPaymentUseCase", "Spring Service", "Orquestra validação, procpag e persistência")
    Container(pagamento_retry_usecase, "RetryPendingPaymentsUseCase", "Spring Service", "Reprocessa pagamentos pendentes")
    Container(pagamento_scheduler, "PaymentRetryScheduler", "Spring Scheduler", "Agenda reprocessamento a cada 60s")
    Container(pagamento_http, "ProcPagHttpGateway", "RestClient + Resilience4j", "Chama POST /requisicao com retry e circuit breaker")
    Container(pagamento_producer, "Pagamento Producer", "Spring Kafka", "Publica pagamento-aprovado / pagamento-pendente")
    ContainerDb(pagamento_db, "pagamento-db", "PostgreSQL 15", "Dados de pagamentos")
  }

  Container_Ext(procpag_ext, "Procpag", "Serviço externo", "POST /requisicao, GET /requisicao/:id")
  Container_Ext(kafka_broker, "Kafka", "Confluent Kafka 7.7.0", "Tópicos: pedido-criado, pagamento-aprovado, pagamento-pendente, pedido-criado-dlq")

  Rel(cliente, auth_api, "POST /auth/cadastro, /auth/login", "JSON")
  Rel(cliente, pedido_api, "POST /pedidos, GET /pedidos", "JWT Bearer")
  Rel(auth_api, auth_db, "JPA/Hibernate", "JDBC")
  Rel(pedido_api, kafka_broker, "Publica pedido-criado", "JSON")
  Rel(kafka_broker, pedido_consumer, "Consome pagamento-aprovado / pagamento-pendente", "JSON")
  Rel(kafka_broker, pagamento_consumer, "Consome pedido-criado", "JSON")
  Rel(pagamento_consumer, pagamento_usecase, "execute(OrderEvent)", "Spring DI")
  Rel(pagamento_usecase, pagamento_http, "processarPagamento(ProcPagRequest)", "Spring DI")
  Rel(pagamento_http, procpag_ext, "POST /requisicao", "HTTP JSON")
  Rel(pagamento_usecase, pagamento_db, "save / findByOrderId", "JPA/Hibernate")
  Rel(pagamento_usecase, pagamento_producer, "publishPaymentApproval / publishPaymentPending", "Spring DI")
  Rel(pagamento_producer, kafka_broker, "Publica eventos de pagamento", "JSON")
  Rel(pagamento_scheduler, pagamento_retry_usecase, "execute()", "Spring DI")
  Rel(pagamento_retry_usecase, pagamento_http, "processarPagamento(ProcPagRequest)", "Spring DI")
  Rel(pagamento_retry_usecase, pagamento_db, "findPendingPayments / save", "JPA/Hibernate")
  Rel(pagamento_retry_usecase, pagamento_producer, "publishPaymentApproval / publishPaymentPending", "Spring DI")
  Rel(pedido_api, pedido_db, "JPA/Hibernate", "JDBC")
```

---

## Stack Tecnológico

| Componente | Tecnologia | Versão |
|------------|------------|--------|
| Linguagem | Java | 21 |
| Framework | Spring Boot | 3.2.5 |
| Segurança | Spring Security + OAuth2 Resource Server + JWT | - |
| Mensageria | Apache Kafka (KRaft mode, sem Zookeeper) | 7.7.0 |
| Resiliência | Resilience4j (Retry + Circuit Breaker) | 2.2.0 |
| Banco de Dados | PostgreSQL | 15+ |
| Migrações | Flyway | 10.10.0 |
| Build | Maven (multi-module) | - |
| HTTP Client | Spring RestClient | - |

---

## Estrutura de Diretórios (Clean Architecture)

```
src/
├── main/
│   ├── java/
│   │   └── br/com/fiap/
│   │       └── [servico]/
│   │           ├── core/
│   │           │   ├── domain/         # Entidades, records, enums, regras de negócio
│   │           │   ├── dto/            # DTOs para transicionar entre camadas
│   │           │   ├── exception/      # Exceções de negócio e sistema
│   │           │   ├── gateway/        # Interfaces de portas (saída)
│   │           │   ├── usecase/        # Casos de uso (entrada)
│   │           ├── infra/
│   │           │   ├── controller/     # Endpoints REST
│   │           │   ├── gateway/
│   │           │   │   ├── db/         # Implementação JPA (entity, repository, mapper)
│   │   │   │   ├── http/       # Clientes HTTP para APIs externas
│   │   │   │   ├── kafka/      # Producers e consumers Kafka
│   │   │   │   └── scheduler/  # Agendadores (retry worker, etc.)
│   │           │   └── security/       # Configuração Spring Security / OAuth2
│   │           └── [servico]Application.java
│   └── resources/
│       ├── application.properties      # (formato .properties, não .yml)
│       └── db/migration/               # Migrations Flyway
└── test/
    └── java/
        └── br/com/fiap/[servico]/      # Testes unitários e de integração
```

> **Nota:** O pacote real do `pagamento-service` é `br.com.fiap.payment` (inglês), não `br.com.fiap.pagamento`.

---

## Camadas

```mermaid
graph TD
  subgraph "Core (regras de negócio)"
    DOM[Domain<br/>Entidades + Enums + Records]
    UC[UseCase<br/>Casos de uso]
    GW[Gateway Interfaces<br/>Portas de saída]
    EXC[Exception<br/>Exceções de negócio]
  end

  subgraph "Infra (adaptadores)"
    CTRL[Controller<br/>REST endpoints]
    DB[Gateway/DB<br/>JPA repositories]
    HTTP[Gateway/HTTP<br/>Clientes externos]
    KFK[Gateway/Kafka<br/>Producers/Consumers]
    SCH[Gateway/Scheduler<br/>PaymentRetryScheduler]
    SEC[Security<br/>Spring Security / OAuth2]
  end

  subgraph "External"
    PG["PostgreSQL<br/>Banco de dados"]
    KP["Apache Kafka<br/>Mensageria"]
    EXT["Procpag<br/>Serviço externo"]
  end

  UC --> DOM
  UC --> GW
  GW --> DB
  GW --> HTTP
  GW --> KFK
  CTRL --> UC
  DB --> PG
  HTTP --> EXT
  KFK --> KP
  SCH --> UC
  SEC -.-> CTRL
```

### Controller

- Exposição de endpoints REST
- Validação de input (DTO → domínio)
- Extração de claims do JWT (`clientId`)
- Tratamento de exceções HTTP
- *(Implementado apenas no pagamento-service, que expõe `/actuator/health`)*

### Use Cases

- Orquestração de operações de negócio
- Chamada a portas (gateways) definidas no domínio
- Transações e tratamento de erros
- **Implementado:** `ProcessPaymentUseCaseImpl` e `RetryPendingPaymentsUseCaseImpl` no pagamento-service

### Domain

- Entidades (`Payment`) com regras de transição de estado (forward-only)
- Records (`OrderEvent`, `PaymentEvent`, `ProcPagRequest`)
- Enums (`PaymentStatus: APPROVED | PENDING`)
- Interfaces de gateway (`PaymentGateway`, `ProcPagGateway`, `PaymentEventGateway`)
- Exceções específicas (`OrderAlreadyCreatedException`, `PaymentProcessingException`, `ExternalServiceUnavailableException`)

### Infra

- **db/**: `PaymentEntity` (JPA), `PaymentEntityRepository`, `PaymentMapper`, `PaymentSpringDataGateway`
- **http/**: `ProcPagHttpGateway` com `RestClient` + Resilience4j (Retry + Circuit Breaker)
- **kafka/**: `PaymentKafkaConsumer` (consome `pedido-criado`), `PaymentKafkaGateway` (publica resultados), `KafkaConfig` (DLQ configurada)
- **scheduler/**: `PaymentRetryScheduler` (reprocessamento agendado de pagamentos pendentes via `@Scheduled`)
- **security/**: Diretório preparado, aguardando implementação

---

## Fluxo de Dados — Processamento de Pagamento

### Caminho Feliz

```mermaid
sequenceDiagram
  participant PS as pedido-service
  participant Kafka as Apache Kafka
  participant Consumer as PaymentKafkaConsumer
  participant UseCase as ProcessPaymentUseCaseImpl
  participant DB as PostgreSQL (pagamento-db)
  participant HTTP as ProcPagHttpGateway
  participant Procpag as Procpag (externo)
  participant Producer as PaymentKafkaGateway

  PS->>Kafka: Publica pedido-criado (OrderEvent)
  Kafka->>Consumer: Consome OrderEvent
  Consumer->>UseCase: execute(orderEvent)

  UseCase->>UseCase: validateEvent(event)
  Note over UseCase: totalAmount > 0<br/>clientId not blank

  UseCase->>DB: findPaymentByOrderIdAndApproved(orderId)
  DB-->>UseCase: Optional.empty() (idempotência)

  UseCase->>UseCase: Cria Payment(PENDING, UUID.randomUUID())
  UseCase->>HTTP: processarPagamento(ProcPagRequest)

  HTTP->>Procpag: POST /requisicao
  Note over HTTP: @CircuitBreaker + @Retry
  Procpag-->>HTTP: 201 {status: "ACCEPTED"}

  HTTP-->>UseCase: "ACCEPTED"
  UseCase->>UseCase: mapProcpagStatus → APPROVED
  UseCase->>UseCase: payment.changeStatusTo(APPROVED)
  UseCase->>DB: save(payment)

  UseCase->>Producer: publishPaymentApproval(PaymentEvent)
  Producer->>Kafka: Publica pagamento-aprovado
```

### Fluxo de Resiliência (Falha do Procpag)

```mermaid
sequenceDiagram
  participant Kafka as Apache Kafka
  participant Consumer as PaymentKafkaConsumer
  participant UseCase as ProcessPaymentUseCaseImpl
  participant HTTP as ProcPagHttpGateway
  participant Procpag as Procpag
  participant Producer as PaymentKafkaGateway

  Kafka->>Consumer: Consome OrderEvent
  Consumer->>UseCase: execute(orderEvent)

  UseCase->>HTTP: processarPagamento(request)

  Note over HTTP,Procpag: Tentativa 1 (Retry)
  HTTP->>Procpag: POST /requisicao
  Procpag-->>HTTP: 408 Timeout

  Note over HTTP,Procpag: Tentativa 2 (Retry)
  HTTP->>Procpag: POST /requisicao
  Procpag-->>HTTP: 502 Bad Gateway

  Note over HTTP,Procpag: Tentativa 3 (Retry)
  HTTP->>Procpag: POST /requisicao
  Procpag-->>HTTP: 408 Timeout

  Note over HTTP: Circuit Breaker avalia<br/>(50% falha → OPEN)
  HTTP->>HTTP: requisicaoFallback()
  Note over HTTP: Lança ExternalServiceUnavailableException

  HTTP-->>UseCase: ExternalServiceUnavailableException
  UseCase->>UseCase: handleFailure()
  Note over UseCase: Status continua PENDING

  UseCase->>Producer: publishPaymentPending(PaymentEvent)
  Producer->>Kafka: Publica pagamento-pendente
```

### Dead Letter Queue (DLQ)

Eventos inválidos ou não processados após retries são enviados ao tópico `pedido-criado-dlq`:

```mermaid
flowchart LR
  A[pedido-criado] --> B[Kafka Consumer]
  B --> C{Processa}
  C -- erro retryável --> D[Retry: 3x com 5s]
  D --> B
  C -- erro não retryável --> E[DLQ: pedido-criado-dlq]
  D -- exaustão --> E
  C -- sucesso --> F[Ack manual]
```

---

### Reprocessamento Agendado (Retry Worker)

O `PaymentRetryScheduler` executa a cada 60s (configurável) e reprocessa pagamentos com status `PENDING` e `retry_count < 3`:

```mermaid
sequenceDiagram
  participant Scheduler as PaymentRetryScheduler
  participant UseCase as RetryPendingPaymentsUseCaseImpl
  participant DB as PostgreSQL (pagamento-db)
  participant HTTP as ProcPagHttpGateway
  participant Procpag as Procpag (externo)
  participant Producer as PaymentKafkaGateway

  Note over Scheduler: A cada ${payment.retry.scheduled-interval}ms
  Scheduler->>UseCase: execute()
  UseCase->>DB: findPendingPayments()
  DB-->>UseCase: List<Payment> PENDING

  loop Para cada pagamento pendente
    UseCase->>HTTP: processarPagamento(ProcPagRequest)
    HTTP->>Procpag: POST /requisicao
    Note over HTTP: @CircuitBreaker + @Retry

    alt Sucesso no Procpag
      Procpag-->>HTTP: 201 {status}
      HTTP-->>UseCase: status
      UseCase->>UseCase: mapProcpagStatus(status)
      UseCase->>DB: save(payment) (retryCount++)

      alt APPROVED
        UseCase->>Producer: publishPaymentApproval(PaymentEvent)
        Producer->>Kafka: pagamento-aprovado
      else PENDING
        UseCase->>Producer: publishPaymentPending(PaymentEvent)
        Producer->>Kafka: pagamento-pendente
      end

    else Falha (Procpag indisponível)
      HTTP-->>UseCase: PaymentProcessingException / ExternalServiceUnavailableException
      UseCase->>DB: save(payment) (retryCount++)
      UseCase->>Producer: publishPaymentPending(PaymentEvent)
      Producer->>Kafka: pagamento-pendente
    end
  end
```

---

## Banco de Dados

Cada microsserviço possui seu próprio banco PostgreSQL dedicado, gerenciado pelo Flyway.

### auth-service (auth-db)

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- CLIENTE, OWNER
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### pedido-service (pedido-db)

```sql
CREATE TABLE pedidos (
    id UUID PRIMARY KEY,
    cliente_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL, -- AGUARDANDO_PAGAMENTO, PAGO, PENDENTE_PAGAMENTO, CANCELADO
    valor_total DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pedido_itens (
    id UUID PRIMARY KEY,
    pedido_id UUID REFERENCES pedidos(id),
    produto_id UUID NOT NULL,
    nome_produto VARCHAR(255) NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL
);
```

### pagamento-service (pagamento-db)

```sql
CREATE TABLE payment (
    payment_id UUID PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    client_id VARCHAR(255) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL, -- APPROVED, PENDING
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_status_created_at ON payment(payment_status, created_at);
```

O `PaymentStatus` possui apenas **dois valores** (definidos no enum `br.com.fiap.payment.core.domain.PaymentStatus`):

| Valor | Descrição |
|-------|-----------|
| `APPROVED` | Pagamento aprovado pelo processador externo |
| `PENDING` | Pagamento pendente (ainda não processado ou falhou) |

**Controle de tentativas:** A coluna `retry_count` (INTEGER, default 0) controla quantas vezes o pagamento foi reprocessado. O worker agendado só seleciona pagamentos com `retry_count < 3` (configurável via `payment.retry.max-attempts`).

**Regra de transição de status** (forward-only):
- `PENDING → APPROVED` — permitido
- `PENDING → PENDING` — permitido (reprocessamento)
- `APPROVED → *` — **bloqueado** (lança `IllegalStateException`)

> Para detalhes completos do modelo, consulte [data-model.md](data-model.md).

---

## Eventos Kafka

| Tópico | Producer | Consumer | Schema |
|--------|----------|----------|--------|
| `pedido-criado` | pedido-service | pagamento-service | `OrderEvent(orderId, clientId, totalAmount, timestamp)` |
| `pagamento-aprovado` | pagamento-service | pedido-service | `PaymentEvent(orderId, paymentId, amount, timestamp)` |
| `pagamento-pendente` | pagamento-service | pedido-service | `PaymentEvent(orderId, paymentId, amount, timestamp)` |
| `pedido-criado-dlq` | pagamento-service (DLQ) | — | `OrderEvent` original |

> Para detalhes completos sobre schemas, consumer groups e configuração, consulte [KAFKA.md](KAFKA.md).

---

## Variáveis de Ambiente por Serviço

### auth-service

```properties
SERVER_PORT=8081
SPRING_DATASOURCE_URL=jdbc:postgresql://auth-db:5432/authdb
SPRING_DATASOURCE_USERNAME=authdb
SPRING_DATASOURCE_PASSWORD=authdb
JWT_SECRET=${JWT_SECRET:default-secret-key}
JWT_EXPIRATION=86400000
```

### pedido-service

```properties
SERVER_PORT=8082
SPRING_DATASOURCE_URL=jdbc:postgresql://pedido-db:5432/pedidodb
SPRING_DATASOURCE_USERNAME=pedidodb
SPRING_DATASOURCE_PASSWORD=pedidodb
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://auth-service:8081
```

### pagamento-service

```properties
SERVER_PORT=8083
SPRING_DATASOURCE_URL=jdbc:postgresql://pagamento-db:5432/pagamentodb
SPRING_DATASOURCE_USERNAME=pagamentodb
SPRING_DATASOURCE_PASSWORD=pagamentodb
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_CONSUMER_GROUP_ID=pagamento-group
PROCPAG_URL=http://procpag:8089
KAFKA_TOPIC_PAGAMENTO_APROVADO=pagamento-aprovado
KAFKA_TOPIC_PAGAMENTO_PENDENTE=pagamento-pendente
KAFKA_TOPIC_PEDIDO_CRIADO=pedido-criado
KAFKA_TOPIC_PEDIDO_CRIADO_DLQ=pedido-criado-dlq
KAFKA_PUBLISH_TIMEOUT=30
PAYMENT_RETRY_SCHEDULED_INTERVAL=60000
PAYMENT_RETRY_MAX_ATTEMPTS=3
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://auth-service:8081
```

### procpag (fornecido)

```properties
SERVER_PORT=8089
```

---

## Resiliência (Resilience4j)

Aplicada exclusivamente no `pagamento-service` para chamadas HTTP ao Procpag:

| Padrão | Nome | Configuração |
|--------|------|-------------|
| **Circuit Breaker** | `procPagCircuitBreaker` | sliding-window=10, min-calls=5, threshold=50%, wait=30s |
| **Retry** | `procPagRetry` | max-attempts=3, wait=5s |

A ordem de execução é: **Circuit Breaker → Retry** (aspectos configurados com `circuitBreakerAspectOrder=1`, `retryAspectOrder=2`).

O fallback (`requisicaoFallback`) lança `ExternalServiceUnavailableException`, que é capturada pelo `ProcessPaymentUseCaseImpl` para persistir o status `PENDING` e publicar `pagamento-pendente`.

> Para detalhes completos, consulte [resilience.md](resilience.md).
