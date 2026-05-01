package br.com.fiap.payment.core.gateway;

import br.com.fiap.payment.core.domain.PaymentEvent;

public interface PaymentEventGateway {

    void publishPaymentApproval(PaymentEvent paymentEvent);

    void publishPaymentPending(PaymentEvent paymentEvent);

}
