package br.com.fiap.order.core.usecase;

import br.com.fiap.order.core.domain.Order;

import java.util.UUID;


/** Caso de uso: buscar pedido por ID (somente se for do cliente logado). */
public interface GetOrderByIdUseCase {
    Order execute(UUID orderId, UUID customerId);
}