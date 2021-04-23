package gms.dataacquisition.cd11.rsdf.processor;

import static com.google.common.base.Preconditions.checkState;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiSerde;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import gms.shared.utilities.kafka.KafkaConfiguration;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of an {@link Cd11RsdfProcessor} that processes data utilizing the {@link
 * KafkaStreams} streaming paradigm
 */
public class KafkaStreamsCd11RsdfProcessor implements Cd11RsdfProcessor {

  private static final Logger logger = LoggerFactory.getLogger(KafkaStreamsCd11RsdfProcessor.class);

  private final KafkaConfiguration kafkaConfiguration;
  private final DataFrameReceiverConfiguration receiverConfiguration;
  private final Duration streamsCloseTimeout;
  private final String applicationId;

  public KafkaStreamsCd11RsdfProcessor(KafkaConfiguration kafkaConfiguration,
                                       DataFrameReceiverConfiguration receiverConfiguration) {
    this.kafkaConfiguration = kafkaConfiguration;
    this.receiverConfiguration = receiverConfiguration;
    this.applicationId = this.kafkaConfiguration.getApplicationId();
    this.streamsCloseTimeout = Duration.ofMillis(
        this.kafkaConfiguration.getStreamsCloseTimeoutMs());
  }

  public static KafkaStreamsCd11RsdfProcessor create(
      KafkaConfiguration kafkaConfiguration,
      DataFrameReceiverConfiguration dataFrameReceiverConfiguration) {
    Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
      logger.error("Exception encountered, triggering processor shutdown", e);
      System.exit(1);
    });

    return new KafkaStreamsCd11RsdfProcessor(kafkaConfiguration,
        dataFrameReceiverConfiguration);
  }


  @Override
  public void run() {
    logger.info("{} configuration: {}", applicationId, kafkaConfiguration);

    Topology topology = buildKafkaTopology();
    Properties kafkaProperties = buildKafkaProperties();

    AdminClient adminClient = AdminClient.create(kafkaProperties);
    validateKafkaState(adminClient,
        kafkaConfiguration.getNumberOfVerificationAttempts());

    try (KafkaStreams streams = new KafkaStreams(topology, kafkaProperties)) {

      CountDownLatch latch = new CountDownLatch(1);
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        logger.info("Shutting down KafkaStreams {} with timeout {}...", applicationId,
            streamsCloseTimeout);
        streams.close(streamsCloseTimeout);
        latch.countDown();
      }));

      streams.setUncaughtExceptionHandler((t, e) -> {
        logger
            .error("A KafkaStreams thread has halted unexpectedly, triggering processor shutdown",
                e);
        System.exit(1);
      });

      streams.start();
      latch.await();
    } catch (Exception e) {
      logger.error(String.format("Error running %s", applicationId), e);
      System.exit(1);
    }
  }

  Topology buildKafkaTopology() {
    Cd11StationSohExtractParser sohParser = Cd11StationSohExtractParser
        .create(receiverConfiguration);

    StreamsBuilder builder = new StreamsBuilder();

    String inputRsdfTopic = kafkaConfiguration.getInputRsdfTopic();
    KStream<String, RawStationDataFrame> rsdfSource = builder
        .stream(inputRsdfTopic,
            Consumed.with(Serdes.String(), new CoiSerde<>(RawStationDataFrame.class)));

    configureSohStreams(rsdfSource, sohParser);

    return builder.build();
  }

  Properties buildKafkaProperties() {
    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfiguration.getBootstrapServers());
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
        kafkaConfiguration.getKeySerializer());
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
        kafkaConfiguration.getValueSerializer());
    props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
        LogAndContinueExceptionHandler.class);
    props.put(StreamsConfig.RETRIES_CONFIG, kafkaConfiguration.getConnectionRetryCount());
    props.put(StreamsConfig.RETRY_BACKOFF_MS_CONFIG, kafkaConfiguration.getRetryBackoffMs());

    return props;
  }

  void validateKafkaState(AdminClient adminClient, int numberOfRetries) {
    // ask the broker which topics have been created
    final RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
        .withBackoff(1, 60, ChronoUnit.SECONDS)
        .withMaxAttempts(numberOfRetries)
        .handle(List.of(ExecutionException.class, IllegalStateException.class,
            InterruptedException.class))
        .onFailedAttempt(e -> logger.warn(
            "The necessary topics do not exist: {}, checking again",
            e));
    Failsafe.with(retryPolicy).run(() -> checkIfConsumerTopicExists(adminClient));
  }

  private void checkIfConsumerTopicExists(AdminClient adminClient)
      throws ExecutionException, InterruptedException {
    String rsdfInputTopic = kafkaConfiguration.getInputRsdfTopic();
    String outputAcquiredChannelSohTopic = kafkaConfiguration
        .getOutputAcquiredChannelSohTopic();
    String outputStationSohInputTopic = kafkaConfiguration.getOutputStationSohInputTopic();
    ListTopicsResult existingTopics = adminClient.listTopics();
    Set<String> topicNames = existingTopics.names().get();
    Set<String> expectedTopicNames = Set
        .of(rsdfInputTopic, outputAcquiredChannelSohTopic, outputStationSohInputTopic);

    checkState(topicNames.containsAll(expectedTopicNames));
  }

  private void configureSohStreams(KStream<String, RawStationDataFrame> rsdfStream,
      Cd11StationSohExtractParser sohParser) {

    // Get StationSohInput
    KStream<String, AcquiredStationSohExtract> stationSohInputKStream = rsdfStream
        .mapValues(rsdf -> tryParseStationSohExtract(sohParser, rsdf))
        .filter((k, v) -> v.isPresent())
        .mapValues(Optional::get);

    // Produce StationSohInput
    String outputSohInputTopic = kafkaConfiguration.getOutputStationSohInputTopic();
    stationSohInputKStream
        .to(outputSohInputTopic,
            Produced.valueSerde(new CoiSerde<>(AcquiredStationSohExtract.class)));

    // Produce AcquiredChannelSoh
    String outputAcquiredChannelSohTopic = kafkaConfiguration
        .getOutputAcquiredChannelSohTopic();
    stationSohInputKStream
        .flatMapValues((k, sohInput) -> sohInput.getAcquiredChannelEnvironmentIssues())
        .to(outputAcquiredChannelSohTopic,
            Produced.valueSerde(new CoiSerde<>(AcquiredChannelEnvironmentIssue.class)));
  }

  private static Optional<AcquiredStationSohExtract> tryParseStationSohExtract(
      Cd11StationSohExtractParser sohParser,
      RawStationDataFrame rsdf) {
    try {
      return Optional.of(sohParser.parseStationSohExtract(rsdf));
    } catch (IOException e) {
      logger.warn(
          "IO error while parsing StationSohExtract, skipping message and resuming processing, cause: {}",
          e.getMessage());
      return Optional.empty();
    } catch (IllegalArgumentException e) {
      logger.warn(
          "Malformed data in RawStationDataFrame while parsing frame {} for station {}, skipping message and resuming processing, cause: {}",
          rsdf.getId(), rsdf.getMetadata().getStationName(), e.getMessage());
      return Optional.empty();
    }
  }
}
