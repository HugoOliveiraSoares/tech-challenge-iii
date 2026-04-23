package br.com.fiap.payment.core.usecase;

import br.com.fiap.payment.core.domain.PaymentApprovedEvent;

public interface PublishPaymentApprovedUseCase {
    void execute(PaymentApprovedEvent event);
}