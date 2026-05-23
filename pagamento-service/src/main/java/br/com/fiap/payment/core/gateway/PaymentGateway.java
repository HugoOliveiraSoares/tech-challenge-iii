package br.com.fiap.payment.core.gateway;

import java.util.Optional;

import br.com.fiap.payment.core.domain.Payment;

public interface PaymentGateway {

    Optional<Payment> findPaymentByOrderIdAndApproved(String orderId);

    Optional<Payment> findPaymentByOrderId(String orderId);

    Payment save(Payment payment);

}
