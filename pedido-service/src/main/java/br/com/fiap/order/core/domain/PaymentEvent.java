package br.com.fiap.order.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEvent(
        String orderId,
        String paymentId,
        BigDecimal amount,
        LocalDateTime timestamp) {
}
