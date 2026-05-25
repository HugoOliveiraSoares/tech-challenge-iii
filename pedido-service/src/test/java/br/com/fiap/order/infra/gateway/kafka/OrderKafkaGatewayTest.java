package br.com.fiap.order.infra.gateway.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.fiap.order.core.domain.OrderEvent;
import br.com.fiap.order.support.OrderTestFixtures;

@ExtendWith(MockitoExtension.class)
class OrderKafkaGatewayTest {

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @InjectMocks
    private OrderKafkaGateway gateway;

    @Captor
    private ArgumentCaptor<OrderEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gateway, "pedidoCriadoTopic", "pedido-criado");
        ReflectionTestUtils.setField(gateway, "publishTimeoutSeconds", 30);
    }

    @Test
    @DisplayName("should publish pedido-criado event")
    void shouldPublishOrderCreatedEvent() {
        var order = OrderTestFixtures.sampleOrder();
        var sendResult = mockSendResult();
        when(kafkaTemplate.send(eq("pedido-criado"), eq(order.getId().toString()), eventCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        gateway.publishOrderCreated(order);

        verify(kafkaTemplate).send(eq("pedido-criado"), eq(order.getId().toString()), eventCaptor.capture());
        OrderEvent event = eventCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(event.orderId()).isEqualTo(order.getId().toString());
        org.assertj.core.api.Assertions.assertThat(event.clientId()).isEqualTo(order.getCustomerId().toString());
        org.assertj.core.api.Assertions.assertThat(event.totalAmount()).isEqualByComparingTo(order.getTotalAmount());
    }

    @Test
    @DisplayName("should throw IllegalStateException when publish fails")
    void shouldThrowWhenPublishFails() {
        var order = OrderTestFixtures.sampleOrder();
        var future = new CompletableFuture<SendResult<String, OrderEvent>>();
        future.completeExceptionally(new ExecutionException(new RuntimeException("broker down")));
        when(kafkaTemplate.send(eq("pedido-criado"), eq(order.getId().toString()), eventCaptor.capture())).thenReturn(future);

        assertThatThrownBy(() -> gateway.publishOrderCreated(order))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Falha ao publicar evento Kafka");
    }

    @SuppressWarnings("unchecked")
    private SendResult<String, OrderEvent> mockSendResult() {
        ProducerRecord<String, OrderEvent> record =
                new ProducerRecord<>("pedido-criado", OrderTestFixtures.ORDER_ID.toString(), mock(OrderEvent.class));
        return mock(SendResult.class);
    }
}
