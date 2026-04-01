# Processamento de Pagamentos

Serviço disponibilizado pelos professores que faz o processamento de pagamentos.
O serviço eventualmente estara disponivel.

## Endpoints

### POST /requisicao

Envia uma requisição de pagamento.

**Corpo da Requisição:**

```json
{
  "pagamento_id": "string",
  "cliente_id": "string",
  "valor": 1000
}
```

**Respostas:**

- `201 Created` - Pagamento aceito
- `408 Request Timeout` - Timeout (simulado aleatoriamente)
- `502 Bad Gateway` - Erro no gateway (simulado aleatoriamente)

### GET /requisicao/:id

Consulta o status de um pagamento pelo ID.

**Parâmetros:**

- `id` (path) - ID do pagamento

**Resposta:**

```json
{
  "pagamento_id": "string",
  "status": "pago"
}
```

### Swagger

Para ver os endpoints disponiveis, acesse `localhost:8089/openapi.yml`
ou acesse [procpag.yml](swagger/procpag.yml)
