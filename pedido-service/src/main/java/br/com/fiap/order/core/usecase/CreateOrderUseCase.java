package br.com.fiap.order.core.usecase;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderItem;
import java.util.List;
import java.util.UUID;

public interface CreateOrderUseCase {
    Order execute(UUID customerId, String restaurantId, List<OrderItem> items);
}