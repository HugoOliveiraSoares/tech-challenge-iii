package br.com.fiap.payment.core.domain;

import java.math.BigDecimal;

import lombok.Getter;

@Getter
public class Payment {

    private Long paymentId;
    private String orderId;
    private String clientId;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;

    public Payment(String orderId, String clientId, BigDecimal totalAmount,
            PaymentStatus paymentStatus) {
        this.setOrderId(orderId);
        this.setClientId(clientId);
        this.setTotalAmount(totalAmount);
        this.setPaymentStatus(paymentStatus);
    }

    public Payment(Long paymentId, String orderId, String clientId, BigDecimal totalAmount,
            PaymentStatus paymentStatus) {
        this.setPaymentId(paymentId);
        this.setOrderId(orderId);
        this.setClientId(clientId);
        this.setTotalAmount(totalAmount);
        this.setPaymentStatus(paymentStatus);
    }

    public void changeStatusTo(PaymentStatus newStatus) {
        if (this.paymentStatus == PaymentStatus.APPROVED) {
            throw new IllegalStateException(
                    "Status cannot transition from APPROVED to " + newStatus);
        }
        this.setPaymentStatus(newStatus);
    }

    private void setPaymentId(Long paymentId) {
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

}
