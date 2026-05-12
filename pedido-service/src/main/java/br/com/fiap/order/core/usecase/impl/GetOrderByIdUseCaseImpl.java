package br.com.fiap.order.core.usecase.impl;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.exception.OrderNotFoundException;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.core.usecase.GetOrderByIdUseCase;
import br.com.fiap.order.core.validation.OrderValidator;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Busca pedido e garante que pertence ao cliente do token JWT. */
@RequiredArgsConstructor
public class GetOrderByIdUseCaseImpl implements GetOrderByIdUseCase {

    private final OrderGateway orderGateway;

    @Override
    public Order execute(UUID orderId, UUID customerId) {
        OrderValidator.validateOrderId(orderId);
        OrderValidator.validateCustomerId(customerId);

        Order order = orderGateway.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        OrderValidator.validateOwnership(order, customerId);

        return order;
    }
}
