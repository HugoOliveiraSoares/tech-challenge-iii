package br.com.fiap.order.core.usecase.impl;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.exception.OrderNotFoundException;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.core.usecase.MarkOrderAsPendingPaymentUseCase;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class MarkOrderAsPendingPaymentUseCaseImpl implements MarkOrderAsPendingPaymentUseCase {

    private final OrderGateway orderGateway;

    @Override
    public void execute(UUID orderId) {
        Order order = orderGateway.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        order.markAsPendingPayment();
        orderGateway.save(order);
    }
}
