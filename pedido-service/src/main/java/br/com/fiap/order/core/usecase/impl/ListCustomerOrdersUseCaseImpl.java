package br.com.fiap.order.core.usecase.impl;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.core.usecase.ListCustomerOrdersUseCase;
import br.com.fiap.order.core.validation.OrderValidator;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Retorna histórico de pedidos do cliente, ordenado por data. */
@RequiredArgsConstructor
public class ListCustomerOrdersUseCaseImpl implements ListCustomerOrdersUseCase {

    private final OrderGateway orderGateway;

    @Override
    public List<Order> execute(UUID customerId) {
        OrderValidator.validateCustomerId(customerId);
        return orderGateway.findByCustomerId(customerId);
    }
}
