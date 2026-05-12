package br.com.fiap.order.core.usecase;

import br.com.fiap.order.core.domain.Order;

import java.util.UUID;


public interface GetOrderByIdUseCase {
    Order execute(UUID orderId, Long customerId);
}