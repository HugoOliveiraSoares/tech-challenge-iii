package br.com.fiap.order.infra.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank String restaurantId,
        @NotEmpty @Valid List<CreateOrderItemRequest> itens) {
}
