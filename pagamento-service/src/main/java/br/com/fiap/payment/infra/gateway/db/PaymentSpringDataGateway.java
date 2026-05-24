package br.com.fiap.payment.infra.gateway.db;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.core.domain.PaymentStatus;
import br.com.fiap.payment.core.gateway.PaymentGateway;
import br.com.fiap.payment.infra.gateway.db.mapper.PaymentMapper;
import br.com.fiap.payment.infra.gateway.db.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentSpringDataGateway implements PaymentGateway {

    @Value("${payment.retry.max-attempts:3}")
    private int maxRetryAttempts;

    private final PaymentEntityRepository paymentEntityRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public Optional<Payment> findPaymentByOrderIdAndApproved(String orderId) {
        return paymentEntityRepository
                .findPaymentByOrderIdAndPaymentStatus(orderId, PaymentStatus.APPROVED)
                .map(paymentMapper::toDomain);

    }

    @Override
    public Payment save(Payment payment) {
        var savedEntity = paymentEntityRepository.save(paymentMapper.toEntity(payment));
        return paymentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Payment> findPaymentByOrderId(String orderId) {
        return paymentEntityRepository
                .findPaymentByOrderId(orderId)
                .map(paymentMapper::toDomain);
    }

    @Override
    public List<Payment> findPendingPayments() {
        return paymentEntityRepository
                .findByPaymentStatusAndRetryCount(PaymentStatus.PENDING, maxRetryAttempts)
                .stream()
                .map(paymentMapper::toDomain)
                .toList();
    }

}
