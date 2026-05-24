package br.com.fiap.payment.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PaymentTest {

    private Payment createPendingPayment() {
        return Payment.createPending("order-1", "client-1", BigDecimal.valueOf(100));
    }

    private Payment createApprovedPayment() {
        return Payment.builder()
                .paymentId(UUID.randomUUID())
                .orderId("order-1")
                .clientId("client-1")
                .totalAmount(BigDecimal.valueOf(100))
                .paymentStatus(PaymentStatus.APPROVED)
                .retryCount(0)
                .build();
    }

    @Test
    void changeStatusTo_FromPendingToApproved_ShouldWork() {
        var payment = createPendingPayment();

        payment.changeStatusTo(PaymentStatus.APPROVED);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void changeStatusTo_FromApprovedToPending_ShouldThrowException() {
        var payment = createApprovedPayment();

        assertThatThrownBy(() -> payment.changeStatusTo(PaymentStatus.PENDING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Status cannot transition from APPROVED to PENDING");
    }

    @Test
    void changeStatusTo_FromApprovedToApproved_ShouldThrowException() {
        var payment = createApprovedPayment();

        assertThatThrownBy(() -> payment.changeStatusTo(PaymentStatus.APPROVED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Status cannot transition from APPROVED to APPROVED");
    }

    @Test
    void changeStatusTo_FromPendingToPending_ShouldWork() {
        var payment = createPendingPayment();

        payment.changeStatusTo(PaymentStatus.PENDING);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}
