package br.com.fiap.order.infra.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID clientId,
        String restaurantId,
        String status,
        List<OrderItemResponse> itens,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
