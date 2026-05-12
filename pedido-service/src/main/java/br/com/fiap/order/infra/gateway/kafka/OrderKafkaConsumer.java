package br.com.fiap.order.infra.gateway.kafka;

import br.com.fiap.order.core.domain.PaymentEvent;
import br.com.fiap.order.core.usecase.MarkOrderAsPaidUseCase;
import br.com.fiap.order.core.usecase.MarkOrderAsPendingPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Consumer Kafka: escuta pagamento-aprovado e pagamento-pendente para atualizar status. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private final MarkOrderAsPaidUseCase markOrderAsPaidUseCase;
    private final MarkOrderAsPendingPaymentUseCase markOrderAsPendingPaymentUseCase;

    /** Pagamento confirmado → pedido vira PAGO. */
    @KafkaListener(
            topics = "${kafka.topic.pagamento-aprovado}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentKafkaListenerContainerFactory")
    public void consumePaymentApproved(PaymentEvent event, Acknowledgment ack) {
        log.info("Pagamento aprovado recebido para pedido {}", event.orderId());
        markOrderAsPaidUseCase.execute(UUID.fromString(event.orderId()));
        ack.acknowledge();
    }

    /** Pagamento falhou ou está em retry → pedido vira PENDENTE_PAGAMENTO. */
    @KafkaListener(
            topics = "${kafka.topic.pagamento-pendente}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentKafkaListenerContainerFactory")
    public void consumePaymentPending(PaymentEvent event, Acknowledgment ack) {
        log.info("Pagamento pendente recebido para pedido {}", event.orderId());
        markOrderAsPendingPaymentUseCase.execute(UUID.fromString(event.orderId()));
        ack.acknowledge();
    }
}
