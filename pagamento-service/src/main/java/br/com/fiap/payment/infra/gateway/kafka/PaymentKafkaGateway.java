package br.com.fiap.payment.infra.gateway.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import br.com.fiap.payment.core.domain.PaymentEvent;
import br.com.fiap.payment.core.gateway.PaymentEventGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaGateway implements PaymentEventGateway {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${kafka.topic.pagamento-aprovado}")
    private String pagamentoAprovadoTopic;

    @Value("${kafka.topic.pagamento-pendente}")
    private String pagamentoPendenteTopic;

    @Override
    public void publishPaymentApproval(PaymentEvent paymentEvent) {
        publish(pagamentoAprovadoTopic, paymentEvent);
    }

    @Override
    public void publishPaymentPending(PaymentEvent paymentEvent) {
        publish(pagamentoPendenteTopic, paymentEvent);
    }

    private void publish(String topic, PaymentEvent event) {
        try {
            kafkaTemplate.send(topic, event.orderId(), event);
            log.info("Evento publicado em {} para pedido {}", topic, event.orderId());
        } catch (Exception e) {
            log.error("Falha ao publicar evento em {} para pedido {}: {}",
                    topic, event.orderId(), e.getMessage(), e);
        }
    }
}
