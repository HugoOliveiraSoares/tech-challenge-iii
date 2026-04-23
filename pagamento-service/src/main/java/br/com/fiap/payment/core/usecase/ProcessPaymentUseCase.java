package br.com.fiap.payment.core.usecase;

import br.com.fiap.payment.core.domain.PedidoCreatedEvent;

public interface ProcessPaymentUseCase {
    void execute(PedidoCreatedEvent event);
}