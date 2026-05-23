package br.com.fiap.payment.infra.gateway.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import br.com.fiap.payment.infra.gateway.db.repository.PaymentEntityRepository;

@ExtendWith(MockitoExtension.class)
class PaymentSpringDataGatewayTest {

    @Mock
    private PaymentEntityRepository repository;

    @InjectMocks
    private PaymentSpringDataGateway gateway;

    private PaymentEntity entity;
    private Payment domain;
    private final String orderId = "order-1";
    private final UUID paymentId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        entity = new PaymentEntity(paymentId, orderId, "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, now, now);
        domain = new Payment(paymentId, orderId, "client-1",
                BigDecimal.valueOf(100), PaymentStatus.PENDING, now, now);
    }

    @Test
    @DisplayName("deve retornar Payment mapeado quando findByOrderId encontrar entidade")
    void deve_RetornarPayment_Quando_FindByOrderIdEncontrar() {
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
    @DisplayName("deve retornar Optional.empty quando findByOrderId não encontrar entidade")
    void deve_RetornarEmpty_Quando_FindByOrderIdNaoEncontrar() {
        when(repository.findPaymentByOrderId("order-inexistente"))
                .thenReturn(Optional.empty());

        Optional<Payment> result = gateway.findPaymentByOrderId("order-inexistente");

        assertThat(result).isEmpty();
        verify(repository).findPaymentByOrderId("order-inexistente");
    }

    @Test
    @DisplayName("deve retornar Payment mapeado quando findByOrderIdAndApproved encontrar entidade")
    void deve_RetornarPayment_Quando_FindByOrderIdAndApprovedEncontrar() {
        when(repository.findPaymentByOrderIdAndPaymentStatus(orderId, PaymentStatus.APPROVED))
                .thenReturn(Optional.of(entity));

        Optional<Payment> result = gateway.findPaymentByOrderIdAndApproved(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().getPaymentId()).isEqualTo(paymentId);
        assertThat(result.get().getOrderId()).isEqualTo(orderId);
        verify(repository).findPaymentByOrderIdAndPaymentStatus(orderId, PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("deve retornar Optional.empty quando findByOrderIdAndApproved não encontrar")
    void deve_RetornarEmpty_Quando_FindByOrderIdAndApprovedNaoEncontrar() {
        when(repository.findPaymentByOrderIdAndPaymentStatus(orderId, PaymentStatus.APPROVED))
                .thenReturn(Optional.empty());

        Optional<Payment> result = gateway.findPaymentByOrderIdAndApproved(orderId);

        assertThat(result).isEmpty();
        verify(repository).findPaymentByOrderIdAndPaymentStatus(orderId, PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("deve salvar entidade e retornar Payment mapeado")
    void deve_SalvarERetornarPayment() {
        when(repository.save(any(PaymentEntity.class))).thenReturn(entity);

        Payment result = gateway.save(domain);

        assertThat(result.getPaymentId()).isEqualTo(paymentId);
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(repository).save(any(PaymentEntity.class));
    }
}
