package gms.shared.utilities.kafka.reactor;

import gms.shared.utilities.kafka.KafkaConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reactor Kafka Options factory for building sender and receiver
 */
public class ReactorKafkaFactory {

    private KafkaConfiguration kafkaConfiguration;

    // Constructor
    public ReactorKafkaFactory(KafkaConfiguration kafkaConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
    }

    public KafkaSender<String, String> makeSender(String senderName) {
        return KafkaSender.create(senderOptions(senderName));
    }

    public KafkaReceiver<String, String> makeReceiver() {
        return KafkaReceiver.create(receiverOptions(
                Collections.singleton(kafkaConfiguration.getInputRsdfTopic())));
    }

    /**
     * Build the Kafka Reactor SenderOptions
     * @param senderName - every kafkasender needs a unique client-id
     * @return SenderOptions
     */
    private SenderOptions<String, String> senderOptions(String senderName) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfiguration.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, senderName);
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, kafkaConfiguration.getTransactionTimeout());
        props.put(ProducerConfig.ACKS_CONFIG, kafkaConfiguration.getAcks());
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, kafkaConfiguration.getDeliveryTimeout());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return SenderOptions.create(props);
    }

    /**
     * Build receiver options using the reactor kafka configuration
      * @return ReceiverOptions
     */
    public ReceiverOptions<String, String> receiverOptions() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfiguration.getBootstrapServers());
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConfiguration.getApplicationId());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfiguration.getApplicationId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, kafkaConfiguration.getSessionTimeout());
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, kafkaConfiguration.getMaxPollInterval());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaConfiguration.getMaxPollRecords());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConfiguration.getAutoCommit());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, kafkaConfiguration.getHeartbeatInterval());

        return ReceiverOptions.create(props);
    }

    public ReceiverOptions<String, String> receiverOptions(Collection<String> topics) {
        return receiverOptions()
                .consumerProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .commitBatchSize(10)
                .commitInterval(Duration.ofSeconds(15L))
                .subscription(topics);
    }
}
