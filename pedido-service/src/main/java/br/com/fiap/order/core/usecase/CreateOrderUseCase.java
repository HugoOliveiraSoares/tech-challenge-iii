package br.com.fiap.order.core.usecase;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderItem;
import java.util.List;

public interface CreateOrderUseCase {
    Order execute(Long customerId, String restaurantId, List<OrderItem> items);
}