package br.com.fiap.order.infra.gateway.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import br.com.fiap.order.core.domain.PaymentEvent;
import br.com.fiap.order.core.usecase.MarkOrderAsPaidUseCase;
import br.com.fiap.order.core.usecase.MarkOrderAsPendingPaymentUseCase;
import br.com.fiap.order.support.OrderTestFixtures;

@ExtendWith(MockitoExtension.class)
class OrderKafkaConsumerTest {

    @Mock
    private MarkOrderAsPaidUseCase markOrderAsPaidUseCase;

    @Mock
    private MarkOrderAsPendingPaymentUseCase markOrderAsPendingPaymentUseCase;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private OrderKafkaConsumer consumer;

    private final PaymentEvent paymentEvent = new PaymentEvent(
            OrderTestFixtures.ORDER_ID.toString(),
            "pay-123",
            BigDecimal.valueOf(51.80),
            LocalDateTime.now());

    @Test
    @DisplayName("should mark order as paid and acknowledge on pagamento-aprovado")
    void shouldMarkAsPaidAndAcknowledge() {
        consumer.consumePaymentApproved(paymentEvent, ack);

        verify(markOrderAsPaidUseCase).execute(OrderTestFixtures.ORDER_ID);
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("should mark order as pending and acknowledge on pagamento-pendente")
    void shouldMarkAsPendingAndAcknowledge() {
        consumer.consumePaymentPending(paymentEvent, ack);

        verify(markOrderAsPendingPaymentUseCase).execute(OrderTestFixtures.ORDER_ID);
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("should propagate exception without ack when mark paid fails")
    void shouldPropagateExceptionOnPaymentApproved() {
        doThrow(new RuntimeException("erro"))
                .when(markOrderAsPaidUseCase).execute(OrderTestFixtures.ORDER_ID);

        assertThatThrownBy(() -> consumer.consumePaymentApproved(paymentEvent, ack))
                .isInstanceOf(RuntimeException.class);

        verify(ack, never()).acknowledge();
    }

    @Test
    @DisplayName("should propagate exception without ack when mark pending fails")
    void shouldPropagateExceptionOnPaymentPending() {
        doThrow(new RuntimeException("erro"))
                .when(markOrderAsPendingPaymentUseCase).execute(OrderTestFixtures.ORDER_ID);

        assertThatThrownBy(() -> consumer.consumePaymentPending(paymentEvent, ack))
                .isInstanceOf(RuntimeException.class);

        verify(ack, never()).acknowledge();
    }
}
