package br.com.fiap.order.core.usecase;

import br.com.fiap.order.core.domain.Order;

import java.util.List;
import java.util.UUID;

public interface ListCustomerOrdersUseCase {
    List<Order> execute(UUID customerId);
}