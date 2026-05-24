package br.com.fiap.payment.infra.gateway.db.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.core.domain.PaymentStatus;
import br.com.fiap.payment.infra.gateway.db.entity.PaymentEntity;

class PaymentMapperTest {

    private final PaymentMapper paymentMapper = new PaymentMapper();
    private final UUID paymentId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    @DisplayName("should map PaymentEntity to Payment with all fields")
    void shouldMapEntityToDomain() {
        var entity = new PaymentEntity(paymentId, "order-1", "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, 0, now, now);

        Payment result = paymentMapper.toDomain(entity);

        assertThat(result.getPaymentId()).isEqualTo(paymentId);
        assertThat(result.getOrderId()).isEqualTo("order-1");
        assertThat(result.getClientId()).isEqualTo("client-1");
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        assertThat(result.getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should map Payment to PaymentEntity with all fields")
    void shouldMapDomainToEntity() {
        var domain = Payment.builder()
                .paymentId(paymentId)
                .orderId("order-1")
                .clientId("client-1")
                .totalAmount(BigDecimal.valueOf(100))
                .paymentStatus(PaymentStatus.APPROVED)
                .createdAt(now)
                .updatedAt(now)
                .retryCount(0)
                .build();

        PaymentEntity result = paymentMapper.toEntity(domain);

        assertThat(result.getPaymentId()).isEqualTo(paymentId);
        assertThat(result.getOrderId()).isEqualTo("order-1");
        assertThat(result.getClientId()).isEqualTo("client-1");
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        assertThat(result.getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should throw NullPointerException when entity is null")
    void shouldThrowNPE_When_EntityIsNull() {
        assertThatThrownBy(() -> paymentMapper.toDomain(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should throw NullPointerException when domain is null")
    void shouldThrowNPE_When_DomainIsNull() {
        assertThatThrownBy(() -> paymentMapper.toEntity(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should correctly map Payment with null timestamps")
    void shouldMapWithNullTimestamps() {
        var entity = new PaymentEntity(paymentId, "order-1", "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, null, null, null);

        Payment result = paymentMapper.toDomain(entity);

        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("should map PaymentEntity with null createdAt")
    void shouldMapWithNullCreatedAt() {
        var entity = new PaymentEntity(UUID.randomUUID(), "order-1", "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, 0, null, null);

        assertThat(entity.isNew()).isTrue();
    }
}
