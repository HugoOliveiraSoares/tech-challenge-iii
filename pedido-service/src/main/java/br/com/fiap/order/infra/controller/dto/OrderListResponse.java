package br.com.fiap.order.infra.controller.dto;

import java.util.List;

public record OrderListResponse(List<OrderSummaryResponse> orders) {
}
