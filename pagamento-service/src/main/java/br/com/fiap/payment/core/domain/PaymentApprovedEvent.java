package br.com.fiap.payment.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentApprovedEvent(
        String eventType,
        String pedidoId,
        String paymentId,
        BigDecimal amount,
        LocalDateTime timestamp) {
}