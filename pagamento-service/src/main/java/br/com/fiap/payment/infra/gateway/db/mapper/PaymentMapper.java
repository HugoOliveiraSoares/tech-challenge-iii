package br.com.fiap.payment.infra.gateway.db.mapper;

import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.infra.gateway.db.entity.PaymentEntity;

public class PaymentMapper {

    private PaymentMapper() {
    }

    public static Payment toDomain(PaymentEntity entity) {
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

    public static PaymentEntity toEntity(Payment domain) {
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
