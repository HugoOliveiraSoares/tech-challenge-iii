package br.com.fiap.order.infra.controller.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String name,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal) {
}
