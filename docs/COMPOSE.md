# Docker Compose - Infraestrutura

## Visão Geral

O arquivo `compose.yml` na raiz do projeto orquestra toda a infraestrutura necessária para executar o sistema localmente: bancos de dados PostgreSQL para cada microsserviço, mensageria Kafka, ferramentas auxiliares e o simulador de processamento de pagamentos externo.

---

## Serviços

| Serviço | Container | Imagem | Porta(s) Host |
|---------|-----------|--------|---------------|
| `procpag` | - | `erickemprobr/procpag:latest` | `8089` |
| `kafka` | `kafka` | `confluentinc/cp-kafka:7.7.0` | `9092`, `9094` |
| `kafka-ui` | `kafka-ui` | `provectuslabs/kafka-ui:latest` | `9000` |
| `auth-db` | `auth-db` | `postgres:15` | `5432` |
| `pedido-db` | `pedido-db` | `postgres:15` | `5433` |
| `pagamento-db` | `pagamento-db` | `postgres:15` | `5434` |
| `kafka-setup` | `kafka-setup` | `confluentinc/cp-kafka:7.7.0` | - |

---

## Banco de Dados

Cada microsserviço possui sua própria instância PostgreSQL dedicada:

| Serviço | Host (no Docker) | Database | Usuário |
|---------|-----------------|----------|---------|
| `auth-db` | `auth-db:5432` | `authdb` | `authdb` |
| `pedido-db` | `pedido-db:5432` | `pedidodb` | `pedidodb` |
| `pagamento-db` | `pagamento-db:5432` | `pagamentodb` | `pagamentodb` |

As portas **5432**, **5433** e **5434** no host são mapeadas para a porta `5432` (padrão PostgreSQL) de cada container, permitindo conectar localmente com ferramentas como DBeaver, psql ou IntelliJ.

As tabelas e migrações são gerenciadas pelo **Flyway** em cada microsserviço — nenhum script SQL de init é necessário no `compose.yml`.

**JDBC URLs para os microsserviços:**

```properties
# auth-service
spring.datasource.url=jdbc:postgresql://auth-db:5432/authdb
spring.datasource.username=authdb
spring.datasource.password=authdb

# pedido-service
spring.datasource.url=jdbc:postgresql://pedido-db:5432/pedidodb
spring.datasource.username=pedidodb
spring.datasource.password=pedidodb

# pagamento-service
spring.datasource.url=jdbc:postgresql://pagamento-db:5432/pagamentodb
spring.datasource.username=pagamentodb
spring.datasource.password=pagamentodb
```

---

## YAML Anchors

Para evitar repetição da configuração comum dos bancos PostgreSQL, foi utilizado o recurso de **YAML anchors**:

```yaml
x-postgres: &postgres-base
  image: postgres:15
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres -d $$POSTGRES_DB"]
    interval: 10s
    timeout: 5s
    retries: 5
  networks:
    - fase3net
```

Cada serviço herda essa base e sobrescreve apenas o que difere:

```yaml
auth-db:
  <<: *postgres-base
  container_name: auth-db
  environment:
    POSTGRES_USER: authdb
    POSTGRES_PASSWORD: authdb
    POSTGRES_DB: authdb
  ports:
    - "5432:5432"
  volumes:
    - auth_data:/var/lib/postgresql/data
```

| Propriedade | Definida no anchor | Definida no serviço |
|-------------|-------------------|---------------------|
| `image` | `postgres:15` | - |
| `healthcheck` | `pg_isready` com `$POSTGRES_DB` | - |
| `networks` | `fase3net` | - |
| `container_name` | - | cada serviço |
| `environment` | - | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| `ports` | - | cada serviço (`5432`, `5433`, `5434`) |
| `volumes` | - | cada serviço |

---

## Healthchecks

Cada container PostgreSQL possui um healthcheck que valida se o database está pronto para conexões:

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres -d $$POSTGRES_DB"]
```

O `$$POSTGRES_DB` é uma variável de ambiente do container que, para cada serviço, aponta para o database específico (`authdb`, `pedidodb`, `pagamentodb`). O healthcheck falha enquanto o PostgreSQL não estiver aceitando conexões no database correto.

---

## Kafka

O cluster Kafka é composto por 3 serviços:

| Serviço | Função |
|---------|--------|
| `kafka` | Broker Kafka single-node (modo KRaft, sem Zookeeper) |
| `kafka-setup` | Container temporário que cria os tópicos na inicialização |
| `kafka-ui` | Interface web para visualizar tópicos, mensagens e consumer groups |

**Tópicos criados automaticamente:**

| Tópico | Partições |
|--------|-----------|
| `pedido-criado` | 1 |
| `pagamento-aprovado` | 1 |
| `pagamento-pendente` | 1 |

Para detalhes sobre schemas de eventos, consumer groups e configuração dos producers/consumers, consulte [KAFKA.md](KAFKA.md).

---

## Procpag

Serviço externo que simula o processamento de pagamentos. Documentação completa em [procpag.md](procpag.md).

---

## Rede

Todos os serviços compartilham a rede `fase3net`, permitindo comunicação direta pelo nome do container:

```
procpag:8089
kafka:9092
auth-db:5432
pedido-db:5432
pagamento-db:5432
```

---

## Volumes

Dados persistentes são armazenados em volumes gerenciados pelo Docker:

| Volume | Montagem | Serviço |
|--------|----------|---------|
| `auth_data` | `/var/lib/postgresql/data` | `auth-db` |
| `pedido_data` | `/var/lib/postgresql/data` | `pedido-db` |
| `pagamento_data` | `/var/lib/postgresql/data` | `pagamento-db` |
| `kafka_data` | `/var/lib/kafka/data` | `kafka` |

Para limpar todos os dados e recomeçar do zero:

```bash
docker compose down -v
```

---

## Comandos Úteis

```bash
# Iniciar todos os serviços
docker compose up -d

# Iniciar apenas um serviço específico
docker compose up -d auth-db

# Verificar o status e health dos serviços
docker compose ps

# Acompanhar logs de todos os serviços
docker compose logs -f

# Acompanhar logs de um serviço específico
docker compose logs -f auth-db

# Parar todos os serviços (mantém volumes)
docker compose down

# Parar todos os serviços e remover volumes (destrutivo)
docker compose down -v

# Reconstruir containers após alterações
docker compose up -d --build

# Executar um comando dentro de um container
docker compose exec auth-db psql -U authdb -d authdb
```
