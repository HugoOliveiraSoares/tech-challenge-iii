package br.com.fiap.payment.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
public class Payment {

    private UUID paymentId;
    private String orderId;
    private String clientId;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer retryCount;

    @Builder
    private Payment(UUID paymentId, String orderId, String clientId, BigDecimal totalAmount,
            PaymentStatus paymentStatus, LocalDateTime createdAt, LocalDateTime updatedAt,
            Integer retryCount) {
        if (paymentId == null) {
            throw new IllegalArgumentException("The paymentId can't be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("The orderId can't be null");
        }
        if (clientId == null) {
            throw new IllegalArgumentException("The clientId can't be null");
        }
        if (totalAmount == null) {
            throw new IllegalArgumentException("The totalAmount can't be null");
        }
        if (paymentStatus == null) {
            throw new IllegalArgumentException("The paymentStatus can't be null");
        }
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.clientId = clientId;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.retryCount = retryCount;
    }

    public static Payment createPending(String orderId, String clientId, BigDecimal totalAmount) {
        return Payment.builder()
                .paymentId(UUID.randomUUID())
                .orderId(orderId)
                .clientId(clientId)
                .totalAmount(totalAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public void changeStatusTo(PaymentStatus newStatus) {
        if (this.paymentStatus == PaymentStatus.APPROVED) {
            throw new IllegalStateException(
                    "Status cannot transition from APPROVED to " + newStatus);
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("The paymentStatus can't be null");
        }
        this.paymentStatus = newStatus;
    }

    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }

}
