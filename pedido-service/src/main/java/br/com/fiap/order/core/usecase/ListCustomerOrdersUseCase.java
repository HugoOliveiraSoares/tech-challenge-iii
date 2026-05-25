package br.com.fiap.order.core.usecase;

import br.com.fiap.order.core.domain.Order;

import java.util.List;
import java.util.UUID;

/** Caso de uso: listar todos os pedidos do cliente autenticado. */
public interface ListCustomerOrdersUseCase {
    List<Order> execute(UUID customerId);
}