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
        pendingPayment = new Payment(paymentId, ORDER_ID, CLIENT_ID, TOTAL_AMOUNT,
                PaymentStatus.PENDING, LocalDateTime.now().minusMinutes(10), LocalDateTime.now(), 0);
        approvedPayment = new Payment(paymentId, ORDER_ID, CLIENT_ID, TOTAL_AMOUNT,
                PaymentStatus.APPROVED, LocalDateTime.now().minusMinutes(10), LocalDateTime.now(), 0);
        exhaustedPayment = new Payment(paymentId, ORDER_ID, CLIENT_ID, TOTAL_AMOUNT,
                PaymentStatus.PENDING, LocalDateTime.now().minusMinutes(10), LocalDateTime.now(), 3);
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Nested
        @DisplayName("quando fluxo feliz")
        class HappyPath {

            @Test
            @DisplayName("deve reprocessar pagamento PENDING e aprovar quando Procpag retornar ACCEPTED")
            void deve_AprovarPagamento_Quando_ProcpagRetornarAccepted() {
                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
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
            @DisplayName("deve manter como PENDING quando Procpag retornar PENDING")
            void deve_ManterComoPendente_Quando_ProcpagRetornarPending() {
                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class))).thenReturn("PENDING");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                assertThat(paymentCaptor.getValue().getRetryCount()).isEqualTo(1);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentApproval(any());
            }

            @Test
            @DisplayName("deve passar dados corretos para o Procpag")
            void deve_PassarDadosCorretosParaProcpag() {
                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(procPagGateway).processarPagamento(procPagCaptor.capture());
                var request = procPagCaptor.getValue();
                assertThat(request.getClientId()).isEqualTo(CLIENT_ID);
                assertThat(request.getAmount()).isEqualByComparingTo(TOTAL_AMOUNT);
                assertThat(request.getPaymentId()).isEqualTo(pendingPayment.getPaymentId());
            }

            @Test
            @DisplayName("deve tratar status desconhecido como PENDING")
            void deve_TratarComoPendente_Quando_ProcpagRetornarStatusDesconhecido() {
                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class))).thenReturn("REJECTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                assertThat(paymentCaptor.getValue().getRetryCount()).isEqualTo(1);

                verify(eventGateway).publishPaymentPending(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentApproval(any());
            }

            @Test
            @DisplayName("deve reprocessar multiplos pagamentos pendentes")
            void deve_ReprocessarMultiplosPagamentos() {
                var payment2 = new Payment(UUID.randomUUID(), "order-789", "client-999",
                        BigDecimal.valueOf(50), PaymentStatus.PENDING,
                        LocalDateTime.now().minusMinutes(10), LocalDateTime.now(), 0);

                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(pendingPayment, payment2));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(paymentGateway, times(2)).save(any());
                verify(eventGateway, times(2)).publishPaymentApproval(any(PaymentEvent.class));
            }
        }

        @Nested
        @DisplayName("quando nao ha pagamentos pendentes")
        class NoPendingPayments {

            @Test
            @DisplayName("nao deve fazer nada quando nao houver pagamentos pendentes")
            void nao_DeveFazerNada_Quando_NaoHouverPagamentosPendentes() {
                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(Collections.emptyList());

                useCase.execute();

                verifyNoInteractions(procPagGateway, eventGateway);
                verify(paymentGateway, never()).save(any());
            }
        }

        @Nested
        @DisplayName("quando limite de tentativas (guard clauses removidas)")
        class RetryLimit {

            @Test
            @DisplayName("deve processar pagamento com retryCount excedido (guard clause removida)")
            void deve_Processar_Quando_RetryCountExcederLimite() {
                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(exhaustedPayment));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class))).thenReturn("ACCEPTED");
                when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                useCase.execute();

                verify(procPagGateway).processarPagamento(any(ProcPagRequest.class));
                verify(paymentGateway).save(paymentCaptor.capture());
                assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
                assertThat(paymentCaptor.getValue().getRetryCount()).isEqualTo(4);

                verify(eventGateway).publishPaymentApproval(any(PaymentEvent.class));
                verify(eventGateway, never()).publishPaymentPending(any());
            }

            @Test
            @DisplayName("deve falhar ao processar APPROVED (changeStatusTo bloqueia transicao)")
            void deve_Falhar_Quando_PagamentoJaAprovado() {
                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(approvedPayment));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class))).thenReturn("ACCEPTED");

                useCase.execute();

                verify(procPagGateway).processarPagamento(any(ProcPagRequest.class));
                verify(paymentGateway, never()).save(any());
                verifyNoInteractions(eventGateway);
            }
        }

        @Nested
        @DisplayName("quando erro externo ocorre")
        class ExternalError {

            @Test
            @DisplayName("deve incrementar retryCount e publicar pagamento-pendente quando Procpag lancar PaymentProcessingException")
            void deve_IncrementarRetry_Quando_ProcpagLancarPaymentProcessingException() {
                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class)))
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
            @DisplayName("deve incrementar retryCount e publicar pagamento-pendente quando Procpag lancar ExternalServiceUnavailableException")
            void deve_IncrementarRetry_Quando_ProcpagLancarExternalServiceUnavailableException() {
                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class)))
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
            @DisplayName("deve engolir excecao da publicacao do evento pendente")
            void deve_EngolirExcecao_Quando_PublicacaoEventoFalhar() {
                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(pendingPayment));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class)))
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
            @DisplayName("deve continuar processando outros pagamentos quando um falhar com excecao inesperada")
            void deve_Continuar_Quando_UmPagamentoLancarExcecaoInesperada() {
                var validPayment = new Payment(UUID.randomUUID(), "order-valid", CLIENT_ID,
                        BigDecimal.valueOf(50), PaymentStatus.PENDING,
                        LocalDateTime.now().minusMinutes(10), LocalDateTime.now(), 0);

                when(paymentGateway.findPendingWithRetryCountLessThan3())
                        .thenReturn(List.of(pendingPayment, validPayment));
                when(procPagGateway.processarPagamento(any(ProcPagRequest.class)))
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
