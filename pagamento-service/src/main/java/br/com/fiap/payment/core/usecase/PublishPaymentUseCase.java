package br.com.fiap.payment.core.usecase;

import br.com.fiap.payment.core.domain.PaymentEvent;

public interface PublishPaymentUseCase {
    void execute(PaymentEvent event);
}
