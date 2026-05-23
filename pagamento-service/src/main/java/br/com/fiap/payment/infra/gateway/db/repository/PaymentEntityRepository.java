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

    Optional<PaymentEntity> findPaymentByOrderIdAndPaymentStatus(String orderId, PaymentStatus status);

    Optional<PaymentEntity> findPaymentByOrderId(String orderId);

    default List<PaymentEntity> findPendingWithRetryCountLessThan3() {
        return findByPaymentStatusAndRetryCount(PaymentStatus.PENDING, 3);
    }

    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentStatus = :status AND p.retryCount < :count")
    List<PaymentEntity> findByPaymentStatusAndRetryCount(
            @Param("status") PaymentStatus status,
            @Param("count") int count);
}
