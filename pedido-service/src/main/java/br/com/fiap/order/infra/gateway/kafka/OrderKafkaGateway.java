package br.com.fiap.order.infra.gateway.kafka;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderEvent;
import br.com.fiap.order.core.gateway.OrderEventGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Producer Kafka: publica OrderEvent no tópico pedido-criado. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaGateway implements OrderEventGateway {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${kafka.topic.pedido-criado}")
    private String pedidoCriadoTopic;

    @Value("${kafka.publish.timeout:30}")
    private int publishTimeoutSeconds;

    @Override
    public void publishOrderCreated(Order order) {
        OrderEvent event = new OrderEvent(
                order.getId().toString(),
                order.getCustomerId().toString(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
        publish(event);
    }

    private void publish(OrderEvent event) {
        try {
            kafkaTemplate.send(pedidoCriadoTopic, event.orderId(), event)
                    .get(publishTimeoutSeconds, TimeUnit.SECONDS);
            log.info("Evento pedido-criado publicado para pedido {}", event.orderId());
        } catch (ExecutionException | TimeoutException e) {
            log.error("Falha ao publicar pedido-criado para pedido {}: {}", event.orderId(), e.getMessage(), e);
            throw new IllegalStateException("Falha ao publicar evento Kafka", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrompida ao publicar evento Kafka", e);
        }
    }
}
