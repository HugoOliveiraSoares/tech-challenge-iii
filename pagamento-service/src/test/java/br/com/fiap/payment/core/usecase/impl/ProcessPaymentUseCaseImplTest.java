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
        @DisplayName("quando fluxo feliz")
        class HappyPath {

            @Test
            @DisplayName("deve aprovar pagamento e publicar pagamento-aprovado quando Procpag retornar ACCEPTED")
            void deve_AprovarPagamento_Quando_ProcpagRetornarAccepted() {
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
            @DisplayName("deve manter pagamento como PENDING e publicar pagamento-pendente quando Procpag retornar PENDING")
            void deve_ManterComoPendente_Quando_ProcpagRetornarPending() {
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
            @DisplayName("deve tratar status desconhecido como PENDING quando Procpag retornar status inesperado")
            void deve_TratarComoPendente_Quando_ProcpagRetornarStatusDesconhecido() {
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
            @DisplayName("deve reprocessar pagamento pendente existente quando pedido já possui pagamento PENDING")
            void deve_ReprocessPaymentPendente_Quando_PagamentoExistentePendente() {
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
            @DisplayName("deve passar dados corretos para o Procpag")
            void deve_PassarDadosCorretosParaProcpag() {
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
        @DisplayName("quando validação falha")
        class Validation {

            @Test
            @DisplayName("deve lançar IllegalArgumentException quando totalAmount for zero")
            void deve_LancarIllegalArgumentException_Quando_TotalAmountForZero() {
                var event = new OrderEvent(ORDER_ID, CLIENT_ID, BigDecimal.ZERO, LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Valor do pedido deve ser positivo");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("deve lançar IllegalArgumentException quando totalAmount for negativo")
            void deve_LancarIllegalArgumentException_Quando_TotalAmountForNegativo() {
                var event = new OrderEvent(ORDER_ID, CLIENT_ID, BigDecimal.valueOf(-10), LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Valor do pedido deve ser positivo");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("deve lançar IllegalArgumentException quando clientId for nulo")
            void deve_LancarIllegalArgumentException_Quando_ClientIdForNulo() {
                var event = new OrderEvent(ORDER_ID, null, TOTAL_AMOUNT, LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID do cliente não pode ser vazio");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("deve lançar IllegalArgumentException quando clientId for vazio")
            void deve_LancarIllegalArgumentException_Quando_ClientIdForVazio() {
                var event = new OrderEvent(ORDER_ID, "  ", TOTAL_AMOUNT, LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID do cliente não pode ser vazio");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("deve lançar IllegalArgumentException quando orderId for nulo")
            void deve_LancarIllegalArgumentException_Quando_OrderIdForNulo() {
                var event = new OrderEvent(null, CLIENT_ID, TOTAL_AMOUNT, LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID do pedido não pode ser vazio");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("deve lançar IllegalArgumentException quando orderId for vazio")
            void deve_LancarIllegalArgumentException_Quando_OrderIdForVazio() {
                var event = new OrderEvent("", CLIENT_ID, TOTAL_AMOUNT, LocalDateTime.now());

                assertThatThrownBy(() -> useCase.execute(event))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID do pedido não pode ser vazio");

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }

            @Test
            @DisplayName("deve lançar NullPointerException quando event for nulo")
            void deve_LancarNullPointerException_Quando_EventForNulo() {
                assertThatThrownBy(() -> useCase.execute(null))
                        .isInstanceOf(NullPointerException.class);

                verifyNoInteractions(paymentGateway, procPagGateway, eventGateway);
            }
        }

        @Nested
        @DisplayName("quando idempotência")
        class Idempotency {

            @Test
            @DisplayName("deve ignorar evento duplicado quando pagamento já foi aprovado")
            void deve_IgnorarEventoDuplicado_Quando_PagamentoJaAprovado() {
                when(paymentGateway.findPaymentByOrderId(ORDER_ID))
                        .thenReturn(Optional.of(approvedPayment));

                useCase.execute(validEvent);

                verify(paymentGateway).findPaymentByOrderId(ORDER_ID);
                verifyNoMoreInteractions(paymentGateway);
                verifyNoInteractions(procPagGateway, eventGateway);
            }
        }

        @Nested
        @DisplayName("quando erro externo ocorre")
        class ExternalError {

            @Test
            @DisplayName("deve manter como PENDING e publicar pagamento-pendente quando Procpag lançar PaymentProcessingException")
            void deve_PublicarPagamentoPendente_Quando_ProcpagLancarPaymentProcessingException() {
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
            @DisplayName("deve manter como PENDING e publicar pagamento-pendente quando Procpag lançar ExternalServiceUnavailableException")
            void deve_PublicarPagamentoPendente_Quando_ProcpagLancarExternalServiceUnavailableException() {
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
            @DisplayName("deve propagar exceção inesperada sem tratar quando Procpag lançar RuntimeException")
            void deve_PropagarExcecaoInesperada_Quando_ProcpagLancarExcecaoGenerica() {
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
            @DisplayName("deve engolir exceção da publicação quando handleFailure falhar ao publicar evento pendente")
            void deve_EngolirExcecao_Quando_PublicacaoEventoPendenteFalhar() {
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
