package br.com.fiap.payment.core.usecase;

import br.com.fiap.payment.core.domain.PaymentPendingEvent;

public interface PublishPaymentPendingUseCase {
    void execute(PaymentPendingEvent event);
}