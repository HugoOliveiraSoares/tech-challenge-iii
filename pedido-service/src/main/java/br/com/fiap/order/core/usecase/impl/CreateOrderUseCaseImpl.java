package br.com.fiap.order.core.usecase.impl;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderItem;
import br.com.fiap.order.core.gateway.OrderEventGateway;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.core.usecase.CreateOrderUseCase;
import br.com.fiap.order.core.validation.OrderValidator;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {

    private final OrderGateway orderGateway;
    private final OrderEventGateway orderEventGateway;

    @Override
    public Order execute(UUID customerId, String restaurantId, List<OrderItem> items) {
        OrderValidator.validateCustomerId(customerId);
        String normalizedRestaurantId = OrderValidator.normalizeRestaurantId(restaurantId);
        List<OrderItem> normalizedItems = OrderValidator.normalizeItems(items);

        Order order = new Order(
                UUID.randomUUID(),
                customerId,
                normalizedRestaurantId,
                normalizedItems
        );

        Order savedOrder = orderGateway.save(order);
        orderEventGateway.publishOrderCreated(savedOrder);

        return savedOrder;
    }
}
