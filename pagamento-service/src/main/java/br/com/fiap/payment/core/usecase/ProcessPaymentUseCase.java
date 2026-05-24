package br.com.fiap.payment.core.usecase;

import br.com.fiap.payment.core.domain.OrderEvent;

public interface ProcessPaymentUseCase {
    void execute(OrderEvent event);
}
