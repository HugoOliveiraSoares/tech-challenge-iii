package br.com.fiap.payment.infra.gateway.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.core.domain.PaymentStatus;
import br.com.fiap.payment.infra.gateway.db.entity.PaymentEntity;
import br.com.fiap.payment.infra.gateway.db.mapper.PaymentMapper;
import br.com.fiap.payment.infra.gateway.db.repository.PaymentEntityRepository;

import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentSpringDataGatewayTest {

    @Mock
    private PaymentEntityRepository repository;

    private PaymentSpringDataGateway gateway;

    private PaymentEntity entity;
    private Payment domain;
    private final String orderId = "order-1";
    private final UUID paymentId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        entity = new PaymentEntity(paymentId, orderId, "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, 0, now, now);
        domain = Payment.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .clientId("client-1")
                .totalAmount(BigDecimal.valueOf(100))
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .retryCount(0)
                .build();
        gateway = new PaymentSpringDataGateway(repository, new PaymentMapper());
        ReflectionTestUtils.setField(gateway, "maxRetryAttempts", 3);
    }

    @Test
    @DisplayName("should return mapped Payment when findByOrderId finds entity")
    void shouldReturnPayment_When_FindByOrderIdFinds() {
        when(repository.findPaymentByOrderId(orderId)).thenReturn(Optional.of(entity));

        Optional<Payment> result = gateway.findPaymentByOrderId(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().getPaymentId()).isEqualTo(paymentId);
        assertThat(result.get().getOrderId()).isEqualTo(orderId);
        assertThat(result.get().getClientId()).isEqualTo("client-1");
        assertThat(result.get().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.get().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.get().getCreatedAt()).isEqualTo(now);
        assertThat(result.get().getUpdatedAt()).isEqualTo(now);
        verify(repository).findPaymentByOrderId(orderId);
    }

    @Test
    @DisplayName("should return Optional.empty when findByOrderId does not find entity")
    void shouldReturnEmpty_When_FindByOrderIdNotFound() {
        when(repository.findPaymentByOrderId("order-inexistente"))
                .thenReturn(Optional.empty());

        Optional<Payment> result = gateway.findPaymentByOrderId("order-inexistente");

        assertThat(result).isEmpty();
        verify(repository).findPaymentByOrderId("order-inexistente");
    }

    @Test
    @DisplayName("should return mapped Payment when findByOrderIdAndApproved finds entity")
    void shouldReturnPayment_When_FindByOrderIdAndApprovedFinds() {
        when(repository.findPaymentByOrderIdAndPaymentStatus(orderId, PaymentStatus.APPROVED))
                .thenReturn(Optional.of(entity));

        Optional<Payment> result = gateway.findPaymentByOrderIdAndApproved(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().getPaymentId()).isEqualTo(paymentId);
        assertThat(result.get().getOrderId()).isEqualTo(orderId);
        verify(repository).findPaymentByOrderIdAndPaymentStatus(orderId, PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("should return Optional.empty when findByOrderIdAndApproved does not find")
    void shouldReturnEmpty_When_FindByOrderIdAndApprovedNotFound() {
        when(repository.findPaymentByOrderIdAndPaymentStatus(orderId, PaymentStatus.APPROVED))
                .thenReturn(Optional.empty());

        Optional<Payment> result = gateway.findPaymentByOrderIdAndApproved(orderId);

        assertThat(result).isEmpty();
        verify(repository).findPaymentByOrderIdAndPaymentStatus(orderId, PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("should save entity and return mapped Payment")
    void shouldSaveAndReturnPayment() {
        when(repository.save(any(PaymentEntity.class))).thenReturn(entity);

        Payment result = gateway.save(domain);

        assertThat(result.getPaymentId()).isEqualTo(paymentId);
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(repository).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("should return list of mapped Payments when findPendingPayments finds entities")
    void shouldReturnPaymentList_When_FindPendingPaymentsFinds() {
        when(repository.findByPaymentStatusAndRetryCount(PaymentStatus.PENDING, 3))
                .thenReturn(List.of(entity));

        List<Payment> result = gateway.findPendingPayments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaymentId()).isEqualTo(paymentId);
        assertThat(result.get(0).getOrderId()).isEqualTo(orderId);
        verify(repository).findByPaymentStatusAndRetryCount(PaymentStatus.PENDING, 3);
    }

    @Test
    @DisplayName("should return empty list when findPendingPayments finds no entities")
    void shouldReturnEmptyList_When_FindPendingPaymentsNotFound() {
        when(repository.findByPaymentStatusAndRetryCount(PaymentStatus.PENDING, 3))
                .thenReturn(Collections.emptyList());

        List<Payment> result = gateway.findPendingPayments();

        assertThat(result).isEmpty();
        verify(repository).findByPaymentStatusAndRetryCount(PaymentStatus.PENDING, 3);
    }
}
