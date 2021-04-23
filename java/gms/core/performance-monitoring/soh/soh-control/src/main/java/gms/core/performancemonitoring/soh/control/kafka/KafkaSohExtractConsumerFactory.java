package gms.core.performancemonitoring.soh.control.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.core.performancemonitoring.soh.control.api.StationSohMonitoringResultsFluxPair;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.util.function.Tuple2;

/**
 * "Factory" for creating a consumer that consumes a pair of Set of AcquiredStationSohExtract
 * and collection of receiver offsets. The consumer is responsible for commiting the offsets,
 * once it has determined that its task has completed.
 */
public class KafkaSohExtractConsumerFactory {

  private static final Logger logger = LogManager.getLogger(KafkaSohExtractConsumerFactory.class);

  /**
   * and interface to extend Consumer, just so we arent carrying the complex type around.
   */
  public interface SohExtractKafkaConsumer extends
      Consumer<Tuple2<Set<AcquiredStationSohExtract>,
          Collection<ReceiverOffset>>> {

  }

  private final KafkaSender<String, String> kafkaSender;
  private final String stationSohOutputTopic;
  private final String capabilitySohRollupOutputTopic;

  private final Function<Set<AcquiredStationSohExtract>, StationSohMonitoringResultsFluxPair>
      resultsPublisher;

  private final ObjectMapper objectMapper;

  /**
   * Constructor which takes senders. This constructor is meant for testing with mock senders.
   *
   * @param kafkaSender The KafkaSender for StationSoh
   * @param stationSohOutputTopic The KafkaSender for CapabilitySohRollup
   * @param stationSohOutputTopic the StationSoh Kafka output topic
   * @param capabilitySohRollupOutputTopic the CapabilitySohRollup kafka output topic
   * @param resultsPublisher The publisher that publishes computed state-of-health. Needs to be a Mono
   */
  public KafkaSohExtractConsumerFactory(
      KafkaSender<String, String> kafkaSender,
      String stationSohOutputTopic,
      String capabilitySohRollupOutputTopic,
      Function<Set<AcquiredStationSohExtract>, StationSohMonitoringResultsFluxPair> resultsPublisher) {

    this.objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

    this.kafkaSender = kafkaSender;
    this.resultsPublisher = resultsPublisher;
    this.stationSohOutputTopic = stationSohOutputTopic;
    this.capabilitySohRollupOutputTopic = capabilitySohRollupOutputTopic;

  }

  /**
   * Get a consumer which takes the tuple of SOH set and offsets, applies resultsPublisher to them,
   * sends them, and then commits the Soh extract offsets.
   * @return SohExtractKafkaConsumer
   */
  public SohExtractKafkaConsumer getConsumer() {

    return asseSetAndOffsetsTuple -> {

      Set<AcquiredStationSohExtract> extracts = asseSetAndOffsetsTuple.getT1();
      Collection<ReceiverOffset> offsets = asseSetAndOffsetsTuple.getT2();
      if (logger.isDebugEnabled()) {
        logger.debug("Handling {} extracts", extracts.size());
      }

      var monitoringResults = resultsPublisher.apply(extracts);

      sendMessages(
          StationSoh.class,
          monitoringResults.getStationSohPublisher(),
          kafkaSender,
          stationSohOutputTopic,
          "StationSoh"
      );


      sendMessages(
          CapabilitySohRollup.class,
          monitoringResults.getCapabilitySohRollupPublisher(),
          kafkaSender,
          capabilitySohRollupOutputTopic,
          "CapabilitySohRollup"
      );

      commitOffsets(offsets);

    };
  }

  /**
   * Send dataFlux to a Kafka topic
   * @param dataFlux Flux with data to send
   * @param sender KafkaSender to use
   * @param topic topic to post to
   * @param messageType message type, used for logging
   * @param <T> type of dataFlux to send
   */
  private <T> void sendMessages(
      Class<T> dataType,
      Flux<T> dataFlux,
      KafkaSender<String, String> sender,
      String topic,
      String messageType) {

      Flux<SenderRecord<String, String, Class<T>>> senderRecordFlux = dataFlux
          .map(item -> {
            try {
              return Optional.of(objectMapper.writeValueAsString(item));
            } catch (JsonProcessingException e) {
              Exceptions.propagate(e);
            }
            // Needs to be here for the sake of compilation, but should be unreachable unless
            // serialization to JSON throws something other than a JsonProcessingException.
            return Optional.<String>empty();
          }).filter(Optional::isPresent)
          .map(Optional::get)
          .map(json -> new ProducerRecord<String, String>(topic, json))
          .map(producerRecord -> SenderRecord.create(producerRecord, dataType));

      sender.send(senderRecordFlux)
          .subscribe(
              senderResult -> {
                // noop intended
              },
              throwable -> {
                if (logger.isErrorEnabled()) {
                  logger.error("Sending messages of type {} messages failed",
                      messageType, throwable);
                }
              },
              () -> {
                if (logger.isDebugEnabled()) {
                  logger.debug("Sending of messages of type {} complete",
                      messageType);
                }
              }
          );
  }

  /**
   * Commit offsets to the Soh extract Kafka service
   * @param offsets offsets to commit
   */
  private void commitOffsets(Collection<ReceiverOffset> offsets) {
    int size = offsets != null ? offsets.size() : 0;
    if (size > 0) {
      offsets.forEach(offset ->
          offset.commit().subscribe(
              v -> { /* noop, shouldn't activate */ },
              // If an exception is thrown.
              throwable ->
                logger.error("Error committing offset {} of topic partition {}",
                    offset.offset(), offset.topicPartition(), throwable),
              // What should usually happen.
              () -> {}
          )
      );
    }
  }
}
