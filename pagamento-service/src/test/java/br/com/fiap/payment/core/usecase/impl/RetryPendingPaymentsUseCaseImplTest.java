package br.com.fiap.payment.core.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.core.domain.PaymentEvent;
import br.com.fiap.payment.core.domain.PaymentStatus;
import br.com.fiap.payment.core.domain.ProcPagRequest;
import br.com.fiap.payment.core.exception.ExternalServiceUnavailableException;
import br.com.fiap.payment.core.exception.PaymentProcessingException;
import br.com.fiap.payment.core.gateway.PaymentEventGateway;
import br.com.fiap.payment.core.gateway.PaymentGateway;
import br.com.fiap.payment.core.gateway.ProcPagGateway;

@ExtendWith(MockitoExtension.class)
class RetryPendingPaymentsUseCaseImplTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private ProcPagGateway procPagGateway;

    @Mock
    private PaymentEventGateway eventGateway;

    @InjectMocks
    private RetryPendingPaymentsUseCaseImpl useCase;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    @Captor
    private ArgumentCaptor<ProcPagRequest> procPagCaptor;

    @Captor
    private ArgumentCaptor<PaymentEvent> eventCaptor;

    private static final String ORDER_ID = "order-123";
    private static final String CLIENT_ID = "client-456";
    private static final BigDecimal TOTAL_AMOUNT = BigDecimal.valueOf(100.00);

    private Payment pendingPayment;
    private Payment approvedPayment;
    private Payment exhaustedPayment;

    @BeforeEach
    void setUp() {
        var paymentId = UUID.randomUUID();
        pendingPayment = Payment.builder()
                .paymentId(paymentId)
                .orderId(ORDER_ID)
                .clientId(CLIENT_ID)
                .totalAmount(TOTAL_AMOUNT)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .updatedAt(LocalDateTime.now())
                .retryCount(0)
                .build();
        approvedPayment = Payment.builder()
                .paymentId(paymentId)
                .orderId(ORDER_ID)
                .clientId(CLIENT_ID)
                .totalAmount(TOTAL_AMOUNT)
                .paymentStatus(PaymentStatus.APPROVED)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .updatedAt(LocalDateTime.now())
                .retryCount(0)
                .build();
        exhaustedPayment = Payment.builder()
                .paymentId(paymentId)
                .orderId(ORDER_ID)
                .clientId(CLIENT_ID)
                .totalAmount(TOTAL_AMOUNT)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .updatedAt(LocalDateTime.now())
                .retryCount(3)
                .build();
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Nested
        @DisplayName("when happy path")
        class HappyPath {

            @Test
            @DisplayName("should reprocess PENDING payment and approve when Procpag returns ACCEPTED")
            void shouldApprovePayment_When_ProcpagReturnsAccepted() {
                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
                assertThat(paymentCaptor.getValue().getRetryCount()).isEqualTo(1);

                verify(eventGateway).publishPaymentApproval(eventCaptor.capture());
                var publishedEvent = eventCaptor.getValue();
                assertThat(publishedEvent.orderId()).isEqualTo(ORDER_ID);
                assertThat(publishedEvent.amount()).isEqualByComparingTo(TOTAL_AMOUNT);

                verify(eventGateway, never()).publishPaymentPending(any());
            }

            @Test
            @DisplayName("should keep as PENDING when Procpag returns PENDING")
            void shouldKeepPending_When_ProcpagReturnsPending() {
                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("PENDING");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                assertThat(paymentCaptor.getValue().getRetryCount()).isEqualTo(1);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentApproval(any());
            }

            @Test
            @DisplayName("should pass correct data to Procpag")
            void shouldPassCorrectDataToProcpag() {
                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(procPagGateway).processPayment(procPagCaptor.capture());
                var request = procPagCaptor.getValue();
                assertThat(request.clientId()).isEqualTo(CLIENT_ID);
                assertThat(request.amount()).isEqualByComparingTo(TOTAL_AMOUNT);
                assertThat(request.paymentId()).isEqualTo(pendingPayment.getPaymentId());
            }

            @Test
            @DisplayName("should treat unknown status as PENDING")
            void shouldTreatAsPending_When_ProcpagReturnsUnknownStatus() {
                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("REJECTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                assertThat(paymentCaptor.getValue().getRetryCount()).isEqualTo(1);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentApproval(any());
            }

            @Test
            @DisplayName("should reprocess multiple pending payments")
            void shouldReprocessMultiplePayments() {
                var payment2 = Payment.builder()
                        .paymentId(UUID.randomUUID())
                        .orderId("order-789")
                        .clientId("client-999")
                        .totalAmount(BigDecimal.valueOf(50))
                        .paymentStatus(PaymentStatus.PENDING)
                        .createdAt(LocalDateTime.now().minusMinutes(10))
                        .updatedAt(LocalDateTime.now())
                        .retryCount(0)
                        .build();

                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(pendingPayment, payment2));
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(paymentGateway, times(2)).save(any());
                verify(eventGateway, times(2)).publishPaymentApproval(any(PaymentEvent.class));
            }
        }

        @Nested
        @DisplayName("when no pending payments")
        class NoPendingPayments {

            @Test
            @DisplayName("should do nothing when there are no pending payments")
            void shouldDoNothing_When_NoPendingPayments() {
                when(paymentGateway.findPendingPayments())
                        .thenReturn(Collections.emptyList());

                useCase.execute();

                verifyNoInteractions(procPagGateway, eventGateway);
                verify(paymentGateway, never()).save(any());
            }
        }

        @Nested
        @DisplayName("when retry limit (guard clauses removed)")
        class RetryLimit {

            @Test
            @DisplayName("should process payment with exceeded retryCount (guard clause removed)")
            void shouldProcess_When_RetryCountExceedsLimit() {
                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(exhaustedPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(procPagGateway).processPayment(any(ProcPagRequest.class));
                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
                assertThat(paymentCaptor.getValue().getRetryCount()).isEqualTo(4);

                verify(eventGateway).publishPaymentApproval(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentPending(any());
            }

            @Test
            @DisplayName("should fail to process APPROVED (changeStatusTo blocks transition)")
            void shouldFail_When_PaymentAlreadyApproved() {
                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(approvedPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("ACCEPTED");

                useCase.execute();

                verify(procPagGateway).processPayment(any(ProcPagRequest.class));
                verify(paymentGateway, never()).save(any());
                verifyNoInteractions(eventGateway);
            }
        }

        @Nested
        @DisplayName("when external error occurs")
        class ExternalError {

            @Test
            @DisplayName("should increment retryCount and publish pagamento-pendente when Procpag throws PaymentProcessingException")
            void shouldIncrementRetry_When_ProcpagThrowsPaymentProcessingException() {
                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class)))
                        .thenThrow(new PaymentProcessingException("Falha no processamento"));
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                assertThat(paymentCaptor.getValue().getRetryCount()).isEqualTo(1);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentApproval(any());
            }

            @Test
            @DisplayName("should increment retryCount and publish pagamento-pendente when Procpag throws ExternalServiceUnavailableException")
            void shouldIncrementRetry_When_ProcpagThrowsExternalServiceUnavailableException() {
                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class)))
                        .thenThrow(new ExternalServiceUnavailableException(
                                "Servico indisponivel", new RuntimeException()));
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                assertThat(paymentCaptor.getValue().getRetryCount()).isEqualTo(1);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentApproval(any());
            }

            @Test
            @DisplayName("should swallow exception from pending event publication")
            void shouldSwallowException_When_PendingEventPublishingFails() {
                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class)))
                        .thenThrow(new PaymentProcessingException("Falha no processamento"));
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
                doThrow(new RuntimeException("Kafka offline"))
                        .when(eventGateway).publishPaymentPending(any(PaymentEvent.class));

                useCase.execute();

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                assertThat(paymentCaptor.getValue().getRetryCount()).isEqualTo(1);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
            }

            @Test
            @DisplayName("should continue processing other payments when one fails with unexpected exception")
            void shouldContinue_When_OnePaymentThrowsUnexpectedException() {
                var validPayment = Payment.builder()
                        .paymentId(UUID.randomUUID())
                        .orderId("order-valid")
                        .clientId(CLIENT_ID)
                        .totalAmount(BigDecimal.valueOf(50))
                        .paymentStatus(PaymentStatus.PENDING)
                        .createdAt(LocalDateTime.now().minusMinutes(10))
                        .updatedAt(LocalDateTime.now())
                        .retryCount(0)
                        .build();

                when(paymentGateway.findPendingPayments())
                        .thenReturn(List.of(pendingPayment, validPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class)))
                        .thenThrow(new RuntimeException("Erro inesperado"))
                        .thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(paymentGateway).save(any());
                verify(eventGateway).publishPaymentApproval(any(PaymentEvent.class));
            }
        }
    }
}
