# API Documentation

## auth-service (Porta 8081)

### Autenticação

#### POST /auth/cadastro

Cria um novo usuário.

**Request:**

```json
{
  "name": "João Silva",
  "email": "joao@email.com",
  "password": "senha123",
  "role": "CLIENT"
}
```

**Response (201 Created):**

```json
{
  "userId": "uuid-gerado"
}
```

#### POST /auth/login

Autentica o usuário e retorna token JWT.

**Request:**

```json
{
  "email": "joao@email.com",
  "password": "senha123"
}
```

**Response (200 OK):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**JWT Payload:**

```json
{
  "sub": "userId",
  "role": "CLIENT",
  "exp": 1234567890
}
```

---

## pedido-service (Porta 8082)

### Pedidos

#### POST /pedidos

Cria um novo pedido. Requer autenticação.

**Headers:**

```
Authorization: Bearer {token_jwt}
```

**Request:**

```json
{
  "itens": [
    {
      "productId": "uuid-produto",
      "restaurantId": "uuid-restaurante",
      "amount": 2,
      "price": 25.90
    },
    {
      "productId": "uuid-produto2",
      "restaurantId": "uuid-restaurante2",
      "amount": 1,
      "price": 5.0
    }
  ]
}
```

**Response (201 Created):**

```json
{
  "id": "uuid-pedido",
  "clientId": "uuid-cliente",
  "status": "AGUARDANDO_PAGAMENTO",
  "itens": [...],
  "totalPrice": 56.80,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

#### GET /pedidos/{id}

Consulta um pedido pelo ID. Requer autenticação (apenas o dono pode acessar).

**Headers:**

```
Authorization: Bearer {token_jwt}
```

**Response (200 OK):**

```json
{
  "id": "uuid-pedido",
  "clientId": "uuid-cliente",
  "status": "PAGO",
  "itens": [...],
  "totalPrice": 56.80,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

#### GET /pedidos

Lista todos os pedidos do cliente autenticado.

**Headers:**

```
Authorization: Bearer {token_jwt}
```

**Response (200 OK):**

```json
{
  "orders": [
    {
      "id": "uuid-pedido-1",
      "status": "PAGO",
      "totalPrice": 56.80,
      "createdAt": "2024-01-15T10:30:00Z"
    },
    {
      "id": "uuid-pedido-2",
      "status": "AGUARDANDO_PAGAMENTO",
      "totalPrice": 30.00,
      "createdAt": "2024-01-15T11:00:00Z"
    }
  ]
}
```

---

## pagamento-service (Porta 8083)

O pagamento-service é consumido via Kafka e não expõe endpoints REST públicos para criação de pagamentos.

### Endpoints Internos

#### GET /actuator/health

Health check do serviço.

**Response (200 OK):**

```json
{
  "status": "UP"
}
```

---

## [procpag - Serviço Externo (Porta 8089)](procpag.md)
