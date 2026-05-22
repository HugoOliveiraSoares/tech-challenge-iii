package br.com.fiap.payment.infra.gateway.db.mapper;

import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.infra.gateway.db.entity.PaymentEntity;

public class PaymentMapper {

    private PaymentMapper() {
    }

    public static Payment toDomain(PaymentEntity entity) {
        return new Payment(
                entity.getPaymentId(),
                entity.getOrderId(),
                entity.getClientId(),
                entity.getTotalAmount(),
                entity.getPaymentStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static PaymentEntity toEntity(Payment domain) {
        return new PaymentEntity(
                domain.getPaymentId(),
                domain.getOrderId(),
                domain.getClientId(),
                domain.getTotalAmount(),
                domain.getPaymentStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}
