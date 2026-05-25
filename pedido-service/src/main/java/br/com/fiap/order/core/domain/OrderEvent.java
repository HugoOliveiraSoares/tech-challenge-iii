package br.com.fiap.order.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderEvent(
        String orderId,
        String clientId,
        BigDecimal totalAmount,
        LocalDateTime timestamp) {
}
