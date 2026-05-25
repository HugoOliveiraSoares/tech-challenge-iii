package br.com.fiap.order.infra.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Resumo do pedido na listagem (sem itens). */
public record OrderSummaryResponse(
        UUID id,
        String status,
        BigDecimal totalPrice,
        LocalDateTime createdAt) {
}
