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
  UseCase->>HTTP: processPayment(ProcPagRequest)
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

  UseCase->>HTTP: processPayment(request)

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
  Note over HTTP: Retorna "PENDING" (fallback real)

  HTTP-->>UseCase: "PENDING"
  Note over UseCase: mapProcpagStatus → PENDING

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

---

## Fluxo 4: Reprocessamento Agendado de Pagamentos Pendentes

### Descrição

O `PaymentRetryScheduler` executa periodicamente (a cada 60s por padrão) e delega ao `RetryPendingPaymentsUseCaseImpl` o reprocessamento de pagamentos que ficaram com status `PENDING`. O caso de uso consulta o banco por pagamentos com `retryCount < 3` e tenta processá-los novamente junto ao Procpag. Cada tentativa incrementa o `retryCount`, independentemente de sucesso ou falha.

### Diagrama de Sequência

```mermaid
sequenceDiagram
  participant Scheduler as PaymentRetryScheduler
  participant UseCase as RetryPendingPaymentsUseCaseImpl
  participant DB as PostgreSQL
  participant HTTP as ProcPagHttpGateway
  participant Procpag as Procpag (externo)
  participant Producer as PaymentKafkaGateway
  participant Kafka as Apache Kafka

  Note over Scheduler: A cada ${payment.retry.scheduled-interval}ms
  Scheduler->>UseCase: execute()
  UseCase->>DB: findPendingPayments()
  DB-->>UseCase: List<Payment> PENDING

  loop Para cada pagamento pendente
    UseCase->>HTTP: processPayment(ProcPagRequest)
    HTTP->>Procpag: POST /requisicao
    Note over HTTP: @CircuitBreaker + @Retry

    alt Sucesso no Procpag
      Procpag-->>HTTP: 201 {status}
      HTTP-->>UseCase: status do Procpag
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
      UseCase->>UseCase: handleRetryFailure()
      UseCase->>DB: save(payment) (retryCount++)
      UseCase->>Producer: publishPaymentPending(PaymentEvent)
      Producer->>Kafka: pagamento-pendente
    end
  end
```

### Diagrama de Decisão

```mermaid
flowchart TD
  A[Scheduler dispara<br/>a cada 60s] --> B[Busca pagamentos<br/>PENDING com retry < 3]
  B --> C{Encontrou<br/>pendentes?}
  C -- Não --> A
  C -- Sim --> D[Seleciona próximo<br/>pagamento]
  D --> E[Chama Procpag<br/>POST /requisicao]
  E --> F{Sucesso?}
  F -- Sim --> G{Status Procpag?}
  G -- ACCEPTED --> H[Status → APPROVED]
  G -- PENDING --> I[Status → PENDING]
  G -- outro --> J[Status → PENDING<br/>log warning]
  F -- Não --> I
  H --> K[Incrementa retryCount]
  I --> K
  J --> K
  K --> L[Salva Payment]
  L --> M{Status final?}
  M -- APPROVED --> N[Publica pagamento-aprovado]
  M -- PENDING --> O[Publica pagamento-pendente]
  N --> C
  O --> C
```
