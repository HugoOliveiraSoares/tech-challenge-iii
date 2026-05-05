package br.com.fiap.payment.infra.gateway.db.mapper;

import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.infra.gateway.db.entity.PaymentEntity;

public class PaymentMapper {
    public static Payment toDomain(PaymentEntity entity) {
        return new Payment(
                entity.getPaymentId(),
                entity.getOrderId(),
                entity.getClientId(),
                entity.getTotalAmount(),
                entity.getPaymentStatus());
    }

    public static PaymentEntity toEntity(Payment domain) {
        return new PaymentEntity(
                domain.getPaymentId(),
                domain.getOrderId(),
                domain.getClientId(),
                domain.getTotalAmount(),
                domain.getPaymentStatus());
    }
}
