package br.com.fiap.order.core.usecase.impl;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderItem;
import br.com.fiap.order.core.gateway.OrderEventGateway;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.core.usecase.CreateOrderUseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {

    private final OrderGateway orderGateway;
    private final OrderEventGateway orderEventGateway;

    @Override
    public Order execute(Long customerId, String restaurantId, List<OrderItem> items) {
        Order order = new Order(
                UUID.randomUUID(),
                customerId,
                restaurantId,
                items
        );

        Order savedOrder = orderGateway.save(order);
        orderEventGateway.publishOrderCreated(savedOrder);

        return savedOrder;
    }
}