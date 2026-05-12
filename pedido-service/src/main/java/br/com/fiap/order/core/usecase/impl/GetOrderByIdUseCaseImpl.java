package br.com.fiap.order.core.usecase.impl;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.exception.OrderNotFoundException;
import br.com.fiap.order.core.exception.UnauthorizedCustomerException;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.core.usecase.GetOrderByIdUseCase;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class GetOrderByIdUseCaseImpl implements GetOrderByIdUseCase {

    private final OrderGateway orderGateway;

    @Override
    public Order execute(UUID orderId, Long customerId) {
        Order order = orderGateway.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedCustomerException("Customer is not allowed to access this order");
        }

        return order;
    }
}