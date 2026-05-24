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
    @DisplayName("deve mapear PaymentEntity para Payment com todos os campos")
    void deve_MapearEntityParaDomain() {
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
    @DisplayName("deve mapear Payment para PaymentEntity com todos os campos")
    void deve_MapearDomainParaEntity() {
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
    @DisplayName("deve lançar NullPointerException quando entity for nula")
    void deve_LancarNPE_Quando_EntityForNull() {
        assertThatThrownBy(() -> paymentMapper.toDomain(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("deve lançar NullPointerException quando domain for nulo")
    void deve_LancarNPE_Quando_DomainForNull() {
        assertThatThrownBy(() -> paymentMapper.toEntity(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("deve mapear corretamente Payment com timestamps nulos")
    void deve_MapearComTimestampsNulos() {
        var entity = new PaymentEntity(paymentId, "order-1", "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, null, null, null);

        Payment result = paymentMapper.toDomain(entity);

        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("deve mapear PaymentEntity com createdAt nulo")
    void deve_MapearComCreatedAtNulo() {
        var entity = new PaymentEntity(UUID.randomUUID(), "order-1", "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, 0, null, null);

        assertThat(entity.isNew()).isTrue();
    }
}
