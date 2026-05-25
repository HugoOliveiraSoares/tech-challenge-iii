package br.com.fiap.order.infra.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderItemRequest(
        @NotNull Long productId,
        @NotBlank String name,
        @NotNull @Min(1) Integer quantity,
        @NotNull @Positive BigDecimal price) {
}
