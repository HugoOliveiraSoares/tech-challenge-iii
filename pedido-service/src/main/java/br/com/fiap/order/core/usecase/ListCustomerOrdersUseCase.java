package br.com.fiap.order.core.usecase;

import br.com.fiap.order.core.domain.Order;

import java.util.List;

public interface ListCustomerOrdersUseCase {
    List<Order> execute(Long customerId);
}