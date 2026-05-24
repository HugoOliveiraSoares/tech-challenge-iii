package br.com.fiap.payment.infra.gateway.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.fiap.payment.core.domain.PaymentEvent;
import br.com.fiap.payment.core.exception.PaymentProcessingException;

@ExtendWith(MockitoExtension.class)
class PaymentKafkaGatewayTest {

    @Mock
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @InjectMocks
    private PaymentKafkaGateway gateway;

    private PaymentEvent paymentEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gateway, "pagamentoAprovadoTopic", "pagamento-aprovado");
        ReflectionTestUtils.setField(gateway, "pagamentoPendenteTopic", "pagamento-pendente");
        ReflectionTestUtils.setField(gateway, "publishTimeoutSeconds", 30);

        paymentEvent = new PaymentEvent(
                "order-123",
                "pay-456",
                BigDecimal.valueOf(150.00),
                LocalDateTime.now());
    }

    @Test
    void publishPaymentApprovalShouldPublishToAprovadoTopic() {
        var sendResult = mockSendResult("pagamento-aprovado", paymentEvent);
        when(kafkaTemplate.send("pagamento-aprovado", paymentEvent.orderId(), paymentEvent))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        gateway.publishPaymentApproval(paymentEvent);

        verify(kafkaTemplate).send("pagamento-aprovado", paymentEvent.orderId(), paymentEvent);
    }

    @Test
    void publishPaymentPendingShouldPublishToPendenteTopic() {
        var sendResult = mockSendResult("pagamento-pendente", paymentEvent);
        when(kafkaTemplate.send("pagamento-pendente", paymentEvent.orderId(), paymentEvent))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        gateway.publishPaymentPending(paymentEvent);

        verify(kafkaTemplate).send("pagamento-pendente", paymentEvent.orderId(), paymentEvent);
    }

    @Test
    void publishShouldThrowPaymentProcessingExceptionOnBrokerError() {
        var future = new CompletableFuture<SendResult<String, PaymentEvent>>();
        future.completeExceptionally(new ExecutionException("Broker offline", new RuntimeException()));

        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class)))
                .thenReturn(future);

        assertThatThrownBy(() -> gateway.publishPaymentApproval(paymentEvent))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Falha ao publicar evento Kafka");
    }

    @Test
    void publishShouldThrowPaymentProcessingExceptionOnTimeout() {
        var future = new CompletableFuture<SendResult<String, PaymentEvent>>();
        future.completeExceptionally(new TimeoutException("Timed out waiting for broker"));

        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class)))
                .thenReturn(future);

        assertThatThrownBy(() -> gateway.publishPaymentApproval(paymentEvent))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Falha ao publicar evento Kafka");
    }

    @Test
    void publishShouldRestoreInterruptFlagOnInterruptedException() {
        var neverCompletingFuture = new CompletableFuture<SendResult<String, PaymentEvent>>();

        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentEvent.class)))
                .thenReturn(neverCompletingFuture);

        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> gateway.publishPaymentApproval(paymentEvent))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Thread interrompida ao publicar evento Kafka");

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }

    private SendResult<String, PaymentEvent> mockSendResult(String topic, PaymentEvent event) {
        var serializer = new StringSerializer();
        var record = new ProducerRecord<>(topic, event.orderId(), event);
        var metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        return new SendResult<>(record, metadata);
    }
}
