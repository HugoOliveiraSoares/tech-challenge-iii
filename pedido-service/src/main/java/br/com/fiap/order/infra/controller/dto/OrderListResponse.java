package br.com.fiap.order.infra.controller.dto;

import java.util.List;

/** Wrapper da listagem GET /pedidos. */
public record OrderListResponse(List<OrderSummaryResponse> orders) {
}
