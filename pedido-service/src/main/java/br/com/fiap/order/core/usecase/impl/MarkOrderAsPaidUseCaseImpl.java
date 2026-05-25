package br.com.fiap.order.core.usecase.impl;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderStatus;
import br.com.fiap.order.core.exception.InvalidOrderException;
import br.com.fiap.order.core.exception.OrderNotFoundException;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.core.usecase.MarkOrderAsPaidUseCase;
import br.com.fiap.order.core.validation.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class MarkOrderAsPaidUseCaseImpl implements MarkOrderAsPaidUseCase {

    private final OrderGateway orderGateway;

    @Override
    public void execute(UUID orderId) {
        OrderValidator.validateOrderId(orderId);

        Order order = orderGateway.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Pedido {} já está pago. Ignorando evento duplicado.", orderId);
            return;
        }

        if (!OrderValidator.canTransitionToPaid(order.getStatus())) {
            throw new InvalidOrderException(
                    "Cannot mark order as paid from status: " + order.getStatus());
        }

        order.markAsPaid();
        orderGateway.save(order);
    }
}
