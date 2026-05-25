package br.com.fiap.order.core.usecase.impl;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderStatus;
import br.com.fiap.order.core.exception.InvalidOrderException;
import br.com.fiap.order.core.exception.OrderNotFoundException;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.core.usecase.MarkOrderAsPendingPaymentUseCase;
import br.com.fiap.order.core.validation.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class MarkOrderAsPendingPaymentUseCaseImpl implements MarkOrderAsPendingPaymentUseCase {

    private final OrderGateway orderGateway;

    @Override
    public void execute(UUID orderId) {
        OrderValidator.validateOrderId(orderId);

        Order order = orderGateway.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Pedido {} já está pago. Ignorando evento de pagamento pendente.", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            log.info("Pedido {} já está pendente de pagamento. Ignorando evento duplicado.", orderId);
            return;
        }

        if (!OrderValidator.canTransitionToPendingPayment(order.getStatus())) {
            throw new InvalidOrderException(
                    "Cannot mark order as pending payment from status: " + order.getStatus());
        }

        order.markAsPendingPayment();
        orderGateway.save(order);
    }
}
