package br.com.fiap.order.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Payload consumido dos tópicos pagamento-aprovado e pagamento-pendente. */
public record PaymentEvent(
        String orderId,
        String paymentId,
        BigDecimal amount,
        LocalDateTime timestamp) {
}
