package br.com.fiap.payment.infra.gateway.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import br.com.fiap.payment.core.domain.OrderEvent;
import br.com.fiap.payment.core.domain.PaymentEvent;

/**
 * Configuração Kafka com estratégia de retry multicamada.
 *
 * <p>
 * O sistema possui 3 camadas independentes de retry, cada uma atuando em um
 * nível diferente.
 * Elas são complementares, não redundantes:
 *
 * <ol>
 * <li><b>Producer retry</b> ({@code RETRIES_CONFIG = 3}): retry de
 * infraestrutura para envio
 * de mensagens ao broker Kafka. Atua em falhas de rede temporárias. Se
 * exaurido, a
 * exceção propaga para o caller do {@code KafkaTemplate.send()}.</li>
 * <li><b>Consumer retry</b> ({@link DefaultErrorHandler} +
 * {@link FixedBackOff}(5s, 3)):
 * reentrega a mensagem ao listener se ele lançar uma exceção. Após 3 tentativas
 * com
 * 5s de intervalo, a mensagem é enviada para a DLQ {@code pedido-criado-dlq}.
 * Exceções do tipo {@link IllegalArgumentException} não são retentadas.</li>
 * <li><b>Resilience4j Retry</b> ({@code procPagRetry}): configurado em
 * {@code application.properties} com 3 tentativas e 5s de espera. Atua na
 * chamada
 * HTTP externa ao Procpag dentro do listener.</li>
 * </ol>
 *
 * <p>
 * <b>Interação entre as camadas:</b>
 * 
 * <pre>
 * Kafka Consumer retry (entrega da mensagem ao listener)
 *   └─ Resilience4j retry (chamada HTTP ao Procpag)
 *        └─ Producer retry (envio do evento de resultado ao broker)
 * </pre>
 *
 * O Resilience4j retry e o Kafka consumer retry atuam em escopos diferentes — o
 * primeiro
 * retenta a chamada HTTP externa; o segundo retenta a entrega da mensagem
 * Kafka. O producer
 * retry garante que os eventos de resultado ({@code pagamento-aprovado} /
 * {@code pagamento-pendente})
 * cheguem ao broker mesmo sob instabilidade de rede.
 *
 * @see br.com.fiap.payment.infra.gateway.http.ProcPagHttpGateway
 * @see <a href=
 *      "../../../../../../../../../../docs/resilience.html">docs/resilience.md</a>
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private Map<String, Object> producerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return props;
    }

    @Bean
    public ProducerFactory<String, PaymentEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }

    @Bean
    public KafkaTemplate<String, PaymentEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaDlqTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfig()));
    }

    private Map<String, Object> consumerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "br.com.fiap.payment.core.domain");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }

    @Bean
    public ConsumerFactory<String, OrderEvent> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfig());
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaDlqTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaDlqTemplate,
                (record, ex) -> new TopicPartition("pedido-criado-dlq", record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(5000L, 3));
        handler.addNotRetryableExceptions(IllegalArgumentException.class, DeserializationException.class);
        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> kafkaListenerContainerFactory(
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
