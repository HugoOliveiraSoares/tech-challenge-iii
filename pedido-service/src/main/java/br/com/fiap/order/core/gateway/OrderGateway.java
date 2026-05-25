package br.com.fiap.order.core.gateway;

import br.com.fiap.order.core.domain.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência: salvar e buscar pedidos (implementada pelo JPA). */
public interface OrderGateway {
    Order save(Order order);
    Optional<Order> findById(UUID orderId);
    List<Order> findByCustomerId(UUID customerId);
}