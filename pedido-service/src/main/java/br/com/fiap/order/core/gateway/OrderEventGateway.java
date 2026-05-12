package br.com.fiap.order.core.gateway;

import br.com.fiap.order.core.domain.Order;

public interface OrderEventGateway {
    void publishOrderCreated(Order order);
}