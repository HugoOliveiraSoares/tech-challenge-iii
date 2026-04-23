package br.com.fiap.payment.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoCreatedEvent(
        String eventType,
        String pedidoId,
        String clientId,
        BigDecimal totalAmount,
        List<OrderItem> items,
        LocalDateTime timestamp) {
}