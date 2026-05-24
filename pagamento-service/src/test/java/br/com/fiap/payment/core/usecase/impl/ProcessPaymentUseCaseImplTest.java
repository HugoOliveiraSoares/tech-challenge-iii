package br.com.fiap.payment.core.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
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

import br.com.fiap.payment.core.domain.OrderEvent;
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
class ProcessPaymentUseCaseImplTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private ProcPagGateway procPagGateway;

    @Mock
    private PaymentEventGateway eventGateway;

    @InjectMocks
    private ProcessPaymentUseCaseImpl useCase;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    @Captor
    private ArgumentCaptor<ProcPagRequest> procPagCaptor;

    @Captor
    private ArgumentCaptor<PaymentEvent> eventCaptor;

    private static final String ORDER_ID = "order-123";
    private static final String CLIENT_ID = "client-456";
    private static final BigDecimal TOTAL_AMOUNT = BigDecimal.valueOf(100.00);

    private OrderEvent validEvent;
    private Payment pendingPayment;
    private Payment approvedPayment;

    @BeforeEach
    void setUp() {
        validEvent = new OrderEvent(ORDER_ID, CLIENT_ID, TOTAL_AMOUNT, LocalDateTime.now());

        var paymentId = UUID.randomUUID();
        pendingPayment = Payment.builder()
                .paymentId(paymentId)
                .orderId(ORDER_ID)
                .clientId(CLIENT_ID)
                .totalAmount(TOTAL_AMOUNT)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .retryCount(0)
                .build();
        approvedPayment = Payment.builder()
                .paymentId(paymentId)
                .orderId(ORDER_ID)
                .clientId(CLIENT_ID)
                .totalAmount(TOTAL_AMOUNT)
                .paymentStatus(PaymentStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .retryCount(0)
                .build();
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Nested
        @DisplayName("when happy path")
        class HappyPath {

            @Test
            @DisplayName("should approve payment and publish pagamento-aprovado when Procpag returns ACCEPTED")
            void shouldApprovePayment_When_ProcpagReturnsAccepted() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.empty());
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute(validEvent);

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);

                verify(eventGateway).publishPaymentApproval(eventCaptor.capture());
                var publishedEvent = eventCaptor.getValue();
                assertThat(publishedEvent.orderId()).isEqualTo(ORDER_ID);
                assertThat(publishedEvent.amount()).isEqualByComparingTo(TOTAL_AMOUNT);
                assertThat(publishedEvent.paymentId()).isEqualTo(
                        paymentCaptor.getValue().getPaymentId().toString());

                verify(eventGateway, never()).publishPaymentPending(any());
            }

            @Test
            @DisplayName("should keep payment as PENDING and publish pagamento-pendente when Procpag returns PENDING")
            void shouldKeepPending_When_ProcpagReturnsPending() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.empty());
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("PENDING");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute(validEvent);

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentApproval(any());
            }

            @Test
            @DisplayName("should treat unknown status as PENDING when Procpag returns unexpected status")
            void shouldTreatAsPending_When_ProcpagReturnsUnknownStatus() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.empty());
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("REJECTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute(validEvent);

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentApproval(any());
            }

            @Test
            @DisplayName("should reprocess existing pending payment when order already has a PENDING payment")
            void shouldReprocessPendingPayment_When_ExistingPaymentIsPending() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID))
                        .thenReturn(Optional.of(pendingPayment));
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute(validEvent);

                verify(paymentGateway).findPaymentByOrderId(ORDER_ID);
                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentId())
                        .isEqualTo(pendingPayment.getPaymentId());
                assertThat(paymentCaptor.getValue().getPaymentStatus())
                        .isEqualTo(PaymentStatus.APPROVED);

                verify(eventGateway).publishPaymentApproval(any(PaymentEvent.class));
            }

            @Test
            @DisplayName("should pass correct data to Procpag")
            void shouldPassCorrectDataToProcpag() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.empty());
                when(procPagGateway.processPayment(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute(validEvent);

                verify(procPagGateway).processPayment(procPagCaptor.capture());
                var request = procPagCaptor.getValue();
                assertThat(request.clientId()).isEqualTo(CLIENT_ID);
                assertThat(request.amount()).isEqualByComparingTo(TOTAL_AMOUNT);
                assertThat(request.paymentId()).isNotNull();
            }
        }

        @Nested
        @DisplayName("when validation fails")
        class Validation {

            @Test
            @DisplayName("should throw IllegalArgumentException when totalAmount is zero")
            void shouldThrowIllegalArgumentException_When_TotalAmountIsZero() {
                var event = new OrderEvent(ORDER_ID, CLIENT_ID, BigDecimal.ZERO, LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Valor do pedido deve ser positivo");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("should throw IllegalArgumentException when totalAmount is negative")
            void shouldThrowIllegalArgumentException_When_TotalAmountIsNegative() {
                var event = new OrderEvent(ORDER_ID, CLIENT_ID, BigDecimal.valueOf(-10), LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Valor do pedido deve ser positivo");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("should throw IllegalArgumentException when clientId is null")
            void shouldThrowIllegalArgumentException_When_ClientIdIsNull() {
                var event = new OrderEvent(ORDER_ID, null, TOTAL_AMOUNT, LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID do cliente não pode ser vazio");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("should throw IllegalArgumentException when clientId is blank")
            void shouldThrowIllegalArgumentException_When_ClientIdIsBlank() {
                var event = new OrderEvent(ORDER_ID, "  ", TOTAL_AMOUNT, LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID do cliente não pode ser vazio");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("should throw IllegalArgumentException when orderId is null")
            void shouldThrowIllegalArgumentException_When_OrderIdIsNull() {
                var event = new OrderEvent(null, CLIENT_ID, TOTAL_AMOUNT, LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID do pedido não pode ser vazio");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("should throw IllegalArgumentException when orderId is blank")
            void shouldThrowIllegalArgumentException_When_OrderIdIsBlank() {
                var event = new OrderEvent("", CLIENT_ID, TOTAL_AMOUNT, LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID do pedido não pode ser vazio");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("should throw NullPointerException when event is null")
            void shouldThrowNullPointerException_When_EventIsNull() {
                assertThatThrownBy(() -> useCase.execute(null))
                        .isInstanceOf(NullPointerException.class);

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }
        }

        @Nested
        @DisplayName("when idempotency")
        class Idempotency {

            @Test
            @DisplayName("should ignore duplicate event when payment is already approved")
            void shouldIgnoreDuplicateEvent_When_PaymentAlreadyApproved() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID))
                        .thenReturn(Optional.of(approvedPayment));

                useCase.execute(validEvent);

                verify(paymentGateway).findPaymentByOrderId(ORDER_ID);
                verifyNoMoreInteractions(paymentGateway);
                verifyNoInteractions(procPagGateway, eventGateway);
            }
        }

        @Nested
        @DisplayName("when external error occurs")
        class ExternalError {

            @Test
            @DisplayName("should keep as PENDING and publish pagamento-pendente when Procpag throws PaymentProcessingException")
            void shouldPublishPendingPayment_When_ProcpagThrowsPaymentProcessingException() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.empty());
                when(procPagGateway.processPayment(any(ProcPagRequest.class)))
                        .thenThrow(new PaymentProcessingException("Falha no processamento"));
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute(validEvent);

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus())
                        .isEqualTo(PaymentStatus.PENDING);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentApproval(any());
            }

            @Test
            @DisplayName("should keep as PENDING and publish pagamento-pendente when Procpag throws ExternalServiceUnavailableException")
            void shouldPublishPendingPayment_When_ProcpagThrowsExternalServiceUnavailableException() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.empty());
                when(procPagGateway.processPayment(any(ProcPagRequest.class)))
                        .thenThrow(new ExternalServiceUnavailableException(
                                "Serviço indisponível", new RuntimeException()));
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute(validEvent);

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus())
                        .isEqualTo(PaymentStatus.PENDING);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentApproval(any());
            }

            @Test
            @DisplayName("should propagate unexpected exception without handling when Procpag throws RuntimeException")
            void shouldPropagateUnexpectedException_When_ProcpagThrowsGenericException() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.empty());
                when(procPagGateway.processPayment(any(ProcPagRequest.class)))
                        .thenThrow(new RuntimeException("Erro inesperado"));

                assertThatThrownBy(() -> useCase.execute(validEvent))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Erro inesperado");

                verify(paymentGateway).findPaymentByOrderId(ORDER_ID);
                verify(paymentGateway, never()).save(any());
                verifyNoInteractions(eventGateway);
            }

            @Test
            @DisplayName("should swallow publication exception when handleFailure fails to publish pending event")
            void shouldSwallowException_When_PendingEventPublishingFails() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.empty());
                when(procPagGateway.processPayment(any(ProcPagRequest.class)))
                        .thenThrow(new PaymentProcessingException("Falha no processamento"));
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
                doThrow(new RuntimeException("Kafka offline"))
                        .when(eventGateway).publishPaymentPending(any(PaymentEvent.class));

                useCase.execute(validEvent);

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus())
                        .isEqualTo(PaymentStatus.PENDING);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
            }

        }
    }
}
