package br.com.fiap.payment.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Payment {

    private UUID paymentId;
    private String orderId;
    private String clientId;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Setter
    private Integer retryCount;

    public Payment(UUID paymentId, String orderId, String clientId, BigDecimal totalAmount,
            PaymentStatus paymentStatus) {
        this.setPaymentId(paymentId);
        this.setOrderId(orderId);
        this.setClientId(clientId);
        this.setTotalAmount(totalAmount);
        this.setPaymentStatus(paymentStatus);
        this.retryCount = 0;
    }

    public Payment(UUID paymentId, String orderId, String clientId, BigDecimal totalAmount,
            PaymentStatus paymentStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.setPaymentId(paymentId);
        this.setOrderId(orderId);
        this.setClientId(clientId);
        this.setTotalAmount(totalAmount);
        this.setPaymentStatus(paymentStatus);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
        this.retryCount = 0;
    }

    public Payment(UUID paymentId, String orderId, String clientId, BigDecimal totalAmount,
            PaymentStatus paymentStatus, LocalDateTime createdAt, LocalDateTime updatedAt,
            Integer retryCount) {
        this.setPaymentId(paymentId);
        this.setOrderId(orderId);
        this.setClientId(clientId);
        this.setTotalAmount(totalAmount);
        this.setPaymentStatus(paymentStatus);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
        this.retryCount = retryCount;
    }

    public void changeStatusTo(PaymentStatus newStatus) {
        if (this.paymentStatus == PaymentStatus.APPROVED) {
            throw new IllegalStateException(
                    "Status cannot transition from APPROVED to " + newStatus);
        }
        this.setPaymentStatus(newStatus);
    }

    private void setPaymentId(UUID paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("The paymentId can't be null");
        }
        this.paymentId = paymentId;
    }

    private void setOrderId(String orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("The orderId can't be null");
        }
        this.orderId = orderId;
    }

    private void setClientId(String clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("The clientId can't be null");
        }
        this.clientId = clientId;
    }

    private void setTotalAmount(BigDecimal totalAmount) {
        if (totalAmount == null) {
            throw new IllegalArgumentException("The totalAmount can't be null");
        }
        this.totalAmount = totalAmount;
    }

    private void setPaymentStatus(PaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            throw new IllegalArgumentException("The paymentStatus can't be null");
        }
        this.paymentStatus = paymentStatus;
    }

    private void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
