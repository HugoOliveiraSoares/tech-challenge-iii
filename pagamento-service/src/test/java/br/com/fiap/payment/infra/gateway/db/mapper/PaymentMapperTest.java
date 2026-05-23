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

    private final UUID paymentId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    @DisplayName("deve mapear PaymentEntity para Payment com todos os campos")
    void deve_MapearEntityParaDomain() {
        var entity = new PaymentEntity(paymentId, "order-1", "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, 0, now, now);

        Payment result = PaymentMapper.toDomain(entity);

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
        var domain = new Payment(paymentId, "order-1", "client-1",
                BigDecimal.valueOf(100), PaymentStatus.APPROVED, now, now, 0);

        PaymentEntity result = PaymentMapper.toEntity(domain);

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
        assertThatThrownBy(() -> PaymentMapper.toDomain(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("deve lançar NullPointerException quando domain for nulo")
    void deve_LancarNPE_Quando_DomainForNull() {
        assertThatThrownBy(() -> PaymentMapper.toEntity(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("deve mapear corretamente Payment com timestamps nulos")
    void deve_MapearComTimestampsNulos() {
        var entity = new PaymentEntity(paymentId, "order-1", "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, null, null, null);

        Payment result = PaymentMapper.toDomain(entity);

        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("deve mapear PaymentEntity com paymentId nulo")
    void deve_MapearComPaymentIdNulo() {
        var entity = new PaymentEntity(null, "order-1", "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, 0, now, now);

        assertThatThrownBy(() -> PaymentMapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The paymentId can't be null");

        // PaymentEntity pode ter paymentId nulo (new entity)
        assertThat(entity.isNew()).isTrue();
    }
}
