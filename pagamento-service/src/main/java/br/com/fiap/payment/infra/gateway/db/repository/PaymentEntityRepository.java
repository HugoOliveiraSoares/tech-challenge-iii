package br.com.fiap.payment.infra.gateway.db.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.payment.infra.gateway.db.entity.PaymentEntity;

public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findPaymentByOrderId(String orderId);

}
