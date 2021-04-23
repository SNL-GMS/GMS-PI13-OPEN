package gms.core.dataacquisition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.UnicastProcessor;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverPartition;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * An implementation of {@link AceiReactiveConsumer} that consumes instances of {@link
 * AcquiredChannelEnvironmentIssue} off a Kafka topic.
 */
public class AceiReactiveKafkaConsumer implements AceiReactiveConsumer {

  private static final Logger logger = LoggerFactory.getLogger(AceiReactiveKafkaConsumer.class);

  private final String bootstrapServers;
  private final String topic;
  private final String applicationId;

  // Set to true by calling consume()
  private volatile boolean consuming;
  private ConsumptionReinitiator consumptionReinitiator;
  private Disposable subscriptionDisposable;
  private Consumer<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> booleanConsumer;
  private Consumer<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> analogConsumer;

  /**
   * Constructor
   *
   * @param bootstrapServers addresses and ports for the Kafka servers, separated by commas if more
   * than one.
   * @param topic the topic from which the ACEIs are read
   * @param applicationId used for the group id for consuming the messages from the topic timestamps
   * are older than this, they are ignored.
   */
  public AceiReactiveKafkaConsumer(
      String bootstrapServers,
      String topic,
      String applicationId
  ) {
    Validate.notBlank(bootstrapServers, "bootstrapServers is required");
    Validate.notBlank(topic, "topic is required");
    Validate.notBlank(applicationId, "applicationId is required");

    this.bootstrapServers = bootstrapServers;
    this.topic = topic;
    this.applicationId = applicationId;
  }

  /**
   * Returns whether or not currently consuming from the topic.
   */
  public boolean isConsuming() {
    return consuming;
  }

  /**
   * Called to initiate consumption from the kafka topic.
   *
   * @param booleanConsumer the consumer for the boolean issues. Must not be null.
   * @param analogConsumer the consumer for the analog issues. Must not be null.
   */
  public synchronized void consume(
      Consumer<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> booleanConsumer,
      Consumer<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> analogConsumer
  ) {

    if (consuming) {
      throw new IllegalStateException("already consuming");
    }

    consuming = true;

    this.booleanConsumer = booleanConsumer;
    this.analogConsumer = analogConsumer;

    initiateConsumption();

  }

  private void initiateConsumption() {

    if (subscriptionDisposable != null && !subscriptionDisposable.isDisposed()) {
      subscriptionDisposable.dispose();
      subscriptionDisposable = null;
    }

    if (consumptionReinitiator != null) {
      consumptionReinitiator.deactivate();
    }

    consumptionReinitiator = new ConsumptionReinitiator(Duration.ofSeconds(60L));

    ReceiverOptions<String, String> receiverOptions = ReceiverOptions.create(consumerProperties());

    receiverOptions = receiverOptions.subscription(Collections.singleton(topic));
    receiverOptions = receiverOptions.addAssignListener(
        consumptionReinitiator::onPartitionsAssigned);
    receiverOptions = receiverOptions.addRevokeListener(
        consumptionReinitiator::onPartitionsRevoked);

    Flux<ReceiverRecord<String, String>> kafkaFlux = KafkaReceiver.create(
        receiverOptions).receive();

    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

    UnicastProcessor<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>>
        booleanIssueProcessor = UnicastProcessor.create();
    UnicastProcessor<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>>
        analogIssueProcessor = UnicastProcessor.create();

    FluxSink<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> booleanSink = booleanIssueProcessor.sink();
    FluxSink<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> analogSink = analogIssueProcessor.sink();

    booleanIssueProcessor.subscribe(tuple -> {
      booleanConsumer.accept(tuple);
    });
    analogIssueProcessor.subscribe(tuple -> {
      analogConsumer.accept(tuple);
    });

    this.subscriptionDisposable = kafkaFlux
        .subscribe(
            receiverRecord -> {
              consumptionReinitiator.incrementReceived();
              receiverRecord.receiverOffset().acknowledge();
              // If it has a timestamp, only forward the ACEIs to one of the sinks if its timestamp
              // is sufficiently recent.
              try {
                AcquiredChannelEnvironmentIssue<?> issue = objectMapper.readValue(
                    receiverRecord.value(), AcquiredChannelEnvironmentIssue.class);
                // Route to the appropriate subscriber.
                if (issue instanceof AcquiredChannelEnvironmentIssueBoolean) {
                  booleanSink.next(Tuples.of(issue, receiverRecord.receiverOffset()));
                } else if (issue instanceof AcquiredChannelEnvironmentIssueAnalog) {
                  analogSink.next(Tuples.of(issue, receiverRecord.receiverOffset()));
                }
              } catch (JsonProcessingException e) {
                logger.error("invalid acei message: {}", receiverRecord.value(), e);
              }
            },
            t -> {
              // This might happen if the Kafka server is restarted. Sometimes a restart doesn't
              // abort the connection, sometimes it does.
              logger.error("Error consuming ACEIs", t);
              if (consuming) {
                // No need for a delay to give the kafka server time to come up if it's restarting,
                // the receive call above will block until the server is up.
                initiateConsumption();
              }
            },
            () -> {
              // Shouldn't ever happen, since it's an infinite flux.
              logger.info("Got an onComplete from the receiver flux");
              stop();
            });

      // Causes it monitor an interval flux. If during any interval, nothing has been received on
      // the kafka topic, kafka consumption is restarted.
      consumptionReinitiator.activate();
  }

  /**
   * Called to stop the consumption of ACEIs from the kafka topic.
   */
  public synchronized void stop() {
    if (consuming) {
      try {
        if (subscriptionDisposable != null) {
          subscriptionDisposable.dispose();
        }
        if (consumptionReinitiator != null) {
          consumptionReinitiator.deactivate();
        }
      } finally {
        subscriptionDisposable = null;
        consumptionReinitiator = null;
        consuming = false;
      }
    }
  }

  /**
   * Sets up the consumer properties.
   */
  private Map<String, Object> consumerProperties() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.put(ConsumerConfig.CLIENT_ID_CONFIG, applicationId);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, applicationId);
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    return properties;
  }

  private class ConsumptionReinitiator {

    private final Duration timeout;

    private AtomicLong receivedRecords = new AtomicLong();
    private AtomicReference<Disposable> dispRef = new AtomicReference<>();
    private volatile boolean active;

    ConsumptionReinitiator(Duration timeout) {
      this.timeout = timeout;
    }

    public void incrementReceived() {
      receivedRecords.incrementAndGet();
    }

    public void activate() {
      active = true;
      dispRef.set(Flux.interval(timeout).subscribe(
          // This will execute on a parallel scheduler thread, not on the thread which called
          // this method.
          l -> {
            if (active) {
              // See if any records have been received on the topic since the last timeout.
              long received = receivedRecords.getAndSet(0L);

              // If nothing has been received, we assume the connection may need to be reinitiated.
              // Be sure to set the timeout large enough for this to make sense.
              if (received == 0L) {
                deactivate();
                initiateConsumption();
              }
            }
      }));
    }

    public void deactivate() {
      active = false;
      Disposable disposable = dispRef.get();
      if (disposable != null && !disposable.isDisposed()) {
        disposable.dispose();
      }
    }

    public void onPartitionsAssigned(Collection<ReceiverPartition> partitions) {
      logger.info("The following partitions have been assigned: {}", partitions);
    }

    public void onPartitionsRevoked(Collection<ReceiverPartition> partitions) {
      logger.info("Partitions have been revoked: {}", partitions);
    }
  }
}
