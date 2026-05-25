package br.com.fiap.order.core.gateway;

import br.com.fiap.order.core.domain.Order;

/** Porta para publicar evento pedido-criado no Kafka. */
public interface OrderEventGateway {
    void publishOrderCreated(Order order);
}