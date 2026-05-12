package br.com.fiap.order.core.usecase.impl;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.core.usecase.ListCustomerOrdersUseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListCustomerOrdersUseCaseImpl implements ListCustomerOrdersUseCase {

    private final OrderGateway orderGateway;

    @Override
    public List<Order> execute(Long customerId) {
        return orderGateway.findByCustomerId(customerId);
    }
}