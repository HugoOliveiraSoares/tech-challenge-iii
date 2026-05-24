package br.com.fiap.payment.infra.gateway.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import br.com.fiap.payment.core.domain.OrderEvent;
import br.com.fiap.payment.core.usecase.ProcessPaymentUseCase;

@ExtendWith(MockitoExtension.class)
class PaymentKafkaConsumerTest {

    @Mock
    private ProcessPaymentUseCase processPaymentUseCase;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private PaymentKafkaConsumer consumer;

    @Test
    @DisplayName("should execute use case and acknowledge when event received")
    void shouldExecuteUseCaseAndAcknowledge() {
        var event = new OrderEvent("order-1", "client-1", BigDecimal.TEN, LocalDateTime.now());

        consumer.consumeOrderEvent(event, ack);

        verify(processPaymentUseCase).execute(event);
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("should propagate exception without ack when use case throws exception")
    void shouldPropagateException_When_UseCaseThrowsException() {
        doThrow(new RuntimeException("Erro inesperado"))
                .when(processPaymentUseCase).execute(any());

        var event = new OrderEvent("order-1", "client-1", BigDecimal.TEN, LocalDateTime.now());

        assertThatThrownBy(() -> consumer.consumeOrderEvent(event, ack))
                .isInstanceOf(RuntimeException.class);

        verify(processPaymentUseCase).execute(event);
        verify(ack, never()).acknowledge();
    }
}
