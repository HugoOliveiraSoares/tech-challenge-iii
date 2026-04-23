package br.com.fiap.payment.core.domain;

import java.time.LocalDateTime;

public record PaymentPendingEvent(
        String eventType,
        String pedidoId,
        String paymentId,
        String reason,
        LocalDateTime timestamp) {
}