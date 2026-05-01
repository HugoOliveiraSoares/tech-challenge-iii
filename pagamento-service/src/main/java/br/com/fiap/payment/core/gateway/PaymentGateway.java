package br.com.fiap.payment.core.gateway;

import java.util.Optional;

import br.com.fiap.payment.core.domain.Payment;

public interface PaymentGateway {

    Optional<Payment> findPaymentByOrderId(String paymentId);

    Payment save(Payment payment);

}
