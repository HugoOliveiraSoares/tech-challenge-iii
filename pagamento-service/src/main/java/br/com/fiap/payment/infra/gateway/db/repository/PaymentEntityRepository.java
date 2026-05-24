package br.com.fiap.payment.infra.gateway.db.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.fiap.payment.core.domain.PaymentStatus;
import br.com.fiap.payment.infra.gateway.db.entity.PaymentEntity;

public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findPaymentByOrderId(String orderId);

    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentStatus = :status AND p.retryCount < :count")
    List<PaymentEntity> findByPaymentStatusAndRetryCount(
            @Param("status") PaymentStatus status,
            @Param("count") int count);
}
