package gms.dataacquisition.cd11.rsdf.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.utilities.kafka.KafkaConfiguration;
import gms.shared.utilities.kafka.reactor.ReactorKafkaFactory;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.kafka.sender.TransactionManager;

/**
 * Implementation of an {@link Cd11RsdfProcessor} that processes data in a reactive paradigm using
 * the Reactor framework
 */
public class ReactorCd11RsdfProcessor implements Cd11RsdfProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ReactorCd11RsdfProcessor.class);
  private static final ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();

  private final KafkaConfiguration kafkaConfiguration;
  private final Cd11StationSohExtractParser sohParser;

  private final KafkaSender<String, String> recordSender;
  private final KafkaReceiver<String, String> receiver;
  private final TransactionManager transactionManager;

  public ReactorCd11RsdfProcessor(KafkaConfiguration kafkaConfiguration,
      DataFrameReceiverConfiguration receiverConfiguration) {
    this.kafkaConfiguration = kafkaConfiguration;
    ReactorKafkaFactory reactorKafkaFactory = new ReactorKafkaFactory(kafkaConfiguration);
    sohParser = Cd11StationSohExtractParser.create(receiverConfiguration);

    recordSender = reactorKafkaFactory.makeSender(kafkaConfiguration.getApplicationId());
    receiver = reactorKafkaFactory.makeReceiver();
    transactionManager = recordSender.transactionManager();
  }

  /**
   * Factory method for creating the processor
   *
   * @param kafkaConfiguration             Reactor kafka configuration retrieved from System config
   * @param dataFrameReceiverConfiguration Receiver configuration responsible for mapping packet
   *                                       information and channel information
   * @return The processor
   */
  public static ReactorCd11RsdfProcessor create(
      KafkaConfiguration kafkaConfiguration,
      DataFrameReceiverConfiguration dataFrameReceiverConfiguration) {

    return new ReactorCd11RsdfProcessor(kafkaConfiguration, dataFrameReceiverConfiguration);
  }

  @Override
  public void run() {
    this.executeRsdfFlux();
  }

  public void close() {
    if (recordSender != null) {
      recordSender.close();
    }
  }

  /**
   * Generates the appropriate Flux that performs exactly-once reads from the rsdf topic, parses the
   * messages and generates SenderRecords, and transactionally sends out the groups of parsed
   * messages such that each collection of messages parsed for an individual rsdf are sent out
   * together.
   */
  public void executeRsdfFlux() {
    flux()
        .subscribe(
            this::handleSenderResult,
            this::handleError,
            this::shutdown);
  }

  private void handleSenderResult(SenderResult<String> result) {
      RecordMetadata metadata = result.recordMetadata();
      String corrdata = result.correlationMetadata();
      if (metadata != null && corrdata != null) {
        logger.info(
                "Successfully published result {} from rsdf {}",
                metadata,
                corrdata);
      }
  }

  /**
   * Helper method for building up the processing flux
   * @return The processing flux
   */
  private Flux<SenderResult<String>> flux() {
    return receiver.receiveExactlyOnce(transactionManager)
        .publishOn(Schedulers.boundedElastic())
        .map(this::records)
        .publishOn(transactionManager.scheduler())
        .concatMap(this::sendAndCommit)
        .onErrorResume(this::abortTransaction)
        .doOnCancel(this::close);
  }

  private Flux<SenderResult<String>> sendAndCommit(
          Flux<SenderRecord<String, String, String>> senderRecordFlux) {
    logger.debug("Sending and committing batch");
    return recordSender.send(senderRecordFlux)
            .concatWith(transactionManager.commit());
  }

  private <V> Mono<V> abortTransaction(Throwable e) {
    logger.warn("Aborting transaction due to error", e);
    return transactionManager.abort().then(Mono.error(e));
  }

  private void handleError(Throwable e) {
    logger.error("Irrecoverable error in rsdf processor", e);
    close();
  }

  private void shutdown() {
    logger.info("Shutting down Rsdf Processor...");
    close();
    System.exit(1);
  }

  protected Flux<SenderRecord<String, String, String>> records(
      Flux<ConsumerRecord<String, String>> rsdfRecordFlux) {
    logger.debug("Parsing rsdf batch into sender records");
    return rsdfRecordFlux
        .publishOn(Schedulers.boundedElastic())
        .flatMap(this::parseRsdf);
  }

  /**
   * Higher-level method for creating {@link AcquiredStationSohExtract} and {@link AcquiredChannelEnvironmentIssue} kafka sender records from an input
   * {@link RawStationDataFrame} consumer record.
   * @param rsdfRecord Consumer record to parse
   * @return Flux of parsed sender records
   */
  private Publisher<? extends SenderRecord<String, String, String>> parseRsdf(
      ConsumerRecord<String, String> rsdfRecord) {
    try {
      RawStationDataFrame rsdf = mapper.readValue(rsdfRecord.value(), RawStationDataFrame.class);
      AcquiredStationSohExtract extract = sohParser.parseStationSohExtract(rsdf);

      String stationName = rsdf.getMetadata().getStationName();

      return sohExtractRecord(stationName, extract, rsdfRecord.offset())
          .concatWith(aceiRecords(extract, rsdfRecord.offset()));
    } catch (JsonProcessingException e) {
      logger.error("Mapping exception:", e);

      return Mono.error(e);
    } catch (IOException | IllegalArgumentException e) {
      logger.error("IO exception:", e);

      return Mono.error(e);
    }
  }

  /**
   * Creates a Kafka {@link SenderRecord} for an {@link AcquiredStationSohExtract}, serializing it to JSON and assigning it
   * to the appropriate topic
   * @param stationName Name of the station for this station soh
   * @param stationSohExtract station soh extract to publish
   * @return Kafka record to send
   */
  private Flux<SenderRecord<String, String, String>> sohExtractRecord(
      String stationName, AcquiredStationSohExtract stationSohExtract, long readOffset) {
    AtomicInteger extractCounter = new AtomicInteger(0);
    String baseKey = stationName.concat("-extract-");

    return writeJson(stationSohExtract)
        .flatMapMany(extractJson -> {
          String kafkaKey = baseKey
              .concat(Long.toString(readOffset))
              .concat("-")
              .concat(Integer.toString(extractCounter.getAndIncrement()));

          return Flux.just(SenderRecord
              .create(new ProducerRecord<>(kafkaConfiguration.getOutputStationSohInputTopic(),
                  kafkaKey, extractJson), kafkaKey));
        });
  }

  /**
   * Builds a flux of {@link AcquiredChannelEnvironmentIssue} {@link SenderRecord}s from the input
   * {@link AcquiredStationSohExtract}
   * @param stationSohExtract Input soh extract
   * @param readOffset Kafka offset from the input topic related to this parsed ACEI
   * @return Flux of all sender records for this soh extract
   */
  private Flux<SenderRecord<String, String, String>> aceiRecords(
      AcquiredStationSohExtract stationSohExtract, long readOffset) {
    AtomicInteger aceiCounter = new AtomicInteger(0);
    String baseKey = "acei-";

    return Flux.fromIterable(stationSohExtract.getAcquiredChannelEnvironmentIssues())
        .flatMap(acei -> {
          String kafkaKey = baseKey
              .concat(acei.getChannelName())
              .concat("-")
              .concat(Long.toString(readOffset))
              .concat("-")
              .concat(Integer.toString(aceiCounter.getAndIncrement()));

          return aceiRecord(kafkaKey, acei);
        });
  }

  /**
   * Creates a Kafka {@link SenderRecord} for an ACEI, serializing it to JSON and assigning it
   * to the appropriate topic
   * @param kafkaKey Message key
   * @param acei {@link AcquiredChannelEnvironmentIssue} channel issue to publish
   * @return Kafka record to send
   */
  private Flux<SenderRecord<String, String, String>> aceiRecord(String kafkaKey,
      AcquiredChannelEnvironmentIssue<?> acei) {

    return writeJson(acei)
        .flatMapMany(aceiJson -> Flux.just(SenderRecord
            .create(new ProducerRecord<>(kafkaConfiguration.getOutputAcquiredChannelSohTopic(),
                kafkaKey, aceiJson), kafkaKey)));
  }

  /**
   * Reactor-friendly json serialization via {@link ObjectMapper}
   * @param obj Object to serialize
   * @return a Mono of the serialized JSON object, or an error Mono if an error occurred
   */
  private Mono<String> writeJson(Object obj) {
    try {
      return Mono.just(mapper.writeValueAsString(obj));
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }
}
