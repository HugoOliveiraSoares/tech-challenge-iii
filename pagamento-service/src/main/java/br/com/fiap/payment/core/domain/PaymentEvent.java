package br.com.fiap.payment.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEvent(
        String orderId,
        String paymentId,
        BigDecimal amount,
        LocalDateTime timestamp) {
}
