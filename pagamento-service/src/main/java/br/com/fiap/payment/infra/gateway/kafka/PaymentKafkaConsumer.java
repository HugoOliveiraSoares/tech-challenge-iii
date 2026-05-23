package br.com.fiap.payment.infra.gateway.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import br.com.fiap.payment.core.domain.OrderEvent;
import br.com.fiap.payment.core.usecase.ProcessPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final ProcessPaymentUseCase processPaymentUseCase;

    @KafkaListener(topics = "${kafka.topic.pedido-criado}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderEvent(OrderEvent orderEvent, Acknowledgment ack) {
        log.info("Evento recebido do pedido {}", orderEvent.orderId());
        processPaymentUseCase.execute(orderEvent);
        ack.acknowledge();
    }
}
