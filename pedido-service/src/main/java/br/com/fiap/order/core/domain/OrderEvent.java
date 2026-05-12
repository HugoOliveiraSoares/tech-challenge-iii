package br.com.fiap.order.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Payload publicado no tópico Kafka pedido-criado (consumido pelo pagamento-service). */
public record OrderEvent(
        String orderId,
        String clientId,
        BigDecimal totalAmount,
        LocalDateTime timestamp) {
}
