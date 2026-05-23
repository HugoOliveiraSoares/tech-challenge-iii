# Fluxos Principais — pagamento-service

## Fluxo 1: Processamento de Pagamento (Caminho Feliz)

### Descrição

Quando um pedido é criado no `pedido-service`, um evento `pedido-criado` é publicado no Kafka. O `pagamento-service` consome esse evento, processa o pagamento junto ao processador externo Procpag e publica o resultado.

### Diagrama de Sequência

```mermaid
sequenceDiagram
  participant PS as pedido-service
  participant Kafka as Apache Kafka
  participant Consumer as PaymentKafkaConsumer
  participant UseCase as ProcessPaymentUseCaseImpl
  participant DB as PostgreSQL
  participant HTTP as ProcPagHttpGateway
  participant Procpag as Procpag (externo)
  participant Producer as PaymentKafkaGateway

  PS->>Kafka: Publica pedido-criado
  Kafka->>Consumer: Consome OrderEvent
  Consumer->>UseCase: execute(orderEvent)

  UseCase->>UseCase: validateEvent(event)
  Note over UseCase: totalAmount > 0<br/>clientId not blank

  UseCase->>DB: findPaymentByOrderIdAndApproved(orderId)
  DB-->>UseCase: Optional.empty() (idempotência)

  UseCase->>UseCase: Cria Payment(PENDING)
  UseCase->>HTTP: processarPagamento(ProcPagRequest)
  HTTP->>Procpag: POST /requisicao
  Procpag-->>HTTP: 201 {status: "ACCEPTED"}

  HTTP-->>UseCase: "ACCEPTED"
  UseCase->>UseCase: mapProcpagStatus("ACCEPTED") → APPROVED
  UseCase->>UseCase: payment.changeStatusTo(APPROVED)

  UseCase->>DB: save(payment)
  DB-->>UseCase: payment persisted

  UseCase->>Producer: publishPaymentApproval(PaymentEvent)
  Producer->>Kafka: Publica pagamento-aprovado
```

### Diagrama de Decisão

```mermaid
flowchart TD
  A[Recebe OrderEvent] --> B{Evento válido?}
  B -- Não --> C[Rejeita + DLQ]
  B -- Sim --> D{Pedido já processado idempotência}
  D -- Sim --> E[Ignora early return]
  D -- Não --> F[Cria Payment PENDING]
  F --> G[Chama ProcPag<br/>POST /requisicao]
  G --> H{Resposta?}
  H -- ACCEPTED --> I[Status → APPROVED]
  H -- PENDING --> J[Status → PENDING]
  H -- outro --> K[Status → PENDING<br/>log warning]
  I --> L[Salva Payment]
  J --> L
  K --> L
  L --> M{Status final?}
  M -- APPROVED --> N[Publica pagamento-aprovado]
  M -- PENDING --> O[Publica pagamento-pendente]
```

## Fluxo 2: Falha no Processamento (Resiliência)

### Descrição

Quando o Procpag retorna erro ou está indisponível, o Resilience4j entra em ação com Retry e Circuit Breaker. Após exaustão das tentativas, o fallback publica um evento `pagamento-pendente`.

### Diagrama de Sequência

```mermaid
sequenceDiagram
  actor User as pedido-service
  participant Kafka
  participant Consumer as PaymentKafkaConsumer
  participant UseCase as ProcessPaymentUseCaseImpl
  participant HTTP as ProcPagHttpGateway
  participant Procpag as Procpag
  participant Producer as PaymentKafkaGateway

  User->>Kafka: Publica pedido-criado
  Kafka->>Consumer: Consome OrderEvent
  Consumer->>UseCase: execute(orderEvent)

  UseCase->>UseCase: validate + idempotency check
  UseCase->>UseCase: Cria Payment(PENDING)

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
  Note over UseCase: Status continua PENDING<br/>(não estava APPROVED)

  UseCase->>DB: save(payment)
  UseCase->>Producer: publishPaymentPending(PaymentEvent)
  Producer->>Kafka: Publica pagamento-pendente
```

### Diagrama de Decisão — Tratamento de Erros

```mermaid
flowchart TD
  A[Chama ProcPag] --> B{Sucesso?}
  B -- Sim --> C[Processa status normalmente]
  B -- Não --> D{Payment já APPROVED?}
  D -- Sim --> E[Loga erro + retorna <br/> não reverte status]
  D -- Não --> F[Mantém PENDING]
  F --> G[Salva Payment PENDING]
  G --> H[Publica pagamento-pendente]
  H --> I[Graceful degradation]

  C --> J{Status ProcPag?}
  J -- ACCEPTED --> K[Avança para APPROVED]
  J -- PENDING --> F
  J -- outro --> F
```

## Fluxo 3: Dead Letter Queue (DLQ)

### Descrição

Eventos que falham no processamento (validação ou erros não recuperáveis) vão para a DLQ após as tentativas de retry do Kafka consumer.

```mermaid
flowchart LR
  A[pedido-criado] --> B[Kafka Consumer]
  B --> C{Processa}
  C -- erro (retryável) --> D[Retry: 3x com 5s]
  D --> B
  C -- erro (não retryável) --> E["DLQ: pedido-criado-dlq"]
  D -- exaustão --> E
  C -- sucesso --> F[Ack manual]
```

### Configuração do Error Handler

- **Dead Letter Publishing**: `DeadLetterPublishingRecoverer` → tópico `pedido-criado-dlq`
- **Backoff**: `FixedBackOff(5000L, 3)` — 5 segundos de intervalo, 3 tentativas
- **Exceções não retryáveis**: `IllegalArgumentException` (validação de evento)
- **Modo de acknowledgment**: manual (`Acknowledgment.acknowledge()`)
