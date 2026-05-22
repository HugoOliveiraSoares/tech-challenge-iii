package br.com.fiap.payment.infra.gateway.db.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.payment.core.domain.PaymentStatus;
import br.com.fiap.payment.infra.gateway.db.entity.PaymentEntity;

public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findPaymentByOrderIdAndPaymentStatus(String orderId, PaymentStatus status);

}
