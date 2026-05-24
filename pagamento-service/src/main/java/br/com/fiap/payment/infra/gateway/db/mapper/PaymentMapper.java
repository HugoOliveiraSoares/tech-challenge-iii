package br.com.fiap.payment.infra.gateway.db.mapper;

import org.springframework.stereotype.Component;

import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.infra.gateway.db.entity.PaymentEntity;

@Component
public class PaymentMapper {

    public Payment toDomain(PaymentEntity entity) {
        return Payment.builder()
                .paymentId(entity.getPaymentId())
                .orderId(entity.getOrderId())
                .clientId(entity.getClientId())
                .totalAmount(entity.getTotalAmount())
                .paymentStatus(entity.getPaymentStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .retryCount(entity.getRetryCount())
                .build();
    }

    public PaymentEntity toEntity(Payment domain) {
        return new PaymentEntity(
                domain.getPaymentId(),
                domain.getOrderId(),
                domain.getClientId(),
                domain.getTotalAmount(),
                domain.getPaymentStatus(),
                domain.getRetryCount(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}
