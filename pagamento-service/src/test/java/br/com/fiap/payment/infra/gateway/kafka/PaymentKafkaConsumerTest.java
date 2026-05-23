package br.com.fiap.payment.infra.gateway.kafka;

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
    @DisplayName("deve executar use case e confirmar acknowledgment quando evento recebido")
    void deve_ExecutarUseCaseEConfirmarAck() {
        var event = new OrderEvent("order-1", "client-1", BigDecimal.TEN, LocalDateTime.now());

        consumer.consumeOrderEvent(event, ack);

        verify(processPaymentUseCase).execute(event);
        verify(ack).acknowledge();
    }
}
