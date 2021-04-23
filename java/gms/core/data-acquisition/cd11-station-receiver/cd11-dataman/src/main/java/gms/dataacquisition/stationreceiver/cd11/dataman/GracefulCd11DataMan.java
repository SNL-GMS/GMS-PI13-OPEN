package gms.dataacquisition.stationreceiver.cd11.dataman;

import static com.google.common.base.Preconditions.checkNotNull;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.Cd11DataConsumerConfig;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.DataManConfig;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.KafkaConnectionConfiguration;
import gms.shared.utilities.javautilities.gracefulthread.GracefulThread;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is designed to start all data communications. It reads the stations.properties file,
 * and spins off a new Cd11DataConsumer thread to talk to the station.
 */
public class GracefulCd11DataMan extends GracefulThread implements Cd11DataMan {

  private static Logger logger = LoggerFactory.getLogger(GracefulCd11DataMan.class);

  private final DataManConfig config;
  private final DataFrameReceiverConfiguration dataFrameReceiverConfig;
  private final ProducerFactory<String, String> producerFactory;
  private final KafkaConnectionConfiguration kafkaConnectionConfiguration;
  private final ConcurrentHashMap<Integer, Cd11DataConsumer> dataConsumerThreads;

  private static final String THREAD_NAME = "CD 1.1 Data Consumer";

  /**
   * Constructor
   *
   * @param config Processing configuration object
   * @param producerFactory Factory to generate Kafka producers
   * @param kafkaConnectionConfiguration Configuration for connecting to kafka brokers
   */
  private GracefulCd11DataMan(DataManConfig config,
                              DataFrameReceiverConfiguration dataFrameReceiverConfig,
                              ProducerFactory<String, String> producerFactory,
                              KafkaConnectionConfiguration kafkaConnectionConfiguration) {

    super(String.format("%s Manager", THREAD_NAME),
        true,
        true);

    this.config = checkNotNull(config);
    this.dataFrameReceiverConfig = dataFrameReceiverConfig;
    this.producerFactory = producerFactory;
    this.kafkaConnectionConfiguration = kafkaConnectionConfiguration;

    // Create a map to store Data Consumer threads.
    this.dataConsumerThreads = new ConcurrentHashMap<>();
  }

  public static GracefulCd11DataMan create(DataManConfig config,
                                           DataFrameReceiverConfiguration dataFrameReceiverConfig,
                                           ProducerFactory<String, String> producerFactory,
                                           KafkaConnectionConfiguration kafkaConnectionConfiguration) {
    checkNotNull(config);
    checkNotNull(producerFactory);
    checkNotNull(kafkaConnectionConfiguration);
    return new GracefulCd11DataMan(config, dataFrameReceiverConfig, producerFactory,
        kafkaConnectionConfiguration);
  }

  /**
   * Starts the Data Consumer Manager.
   */
  @Override
  protected void onStart() {
    // Gets of Data Consumers that need to be spawned.
    List<Cd11DataConsumerConfig> osdListOfDataConsumers = getDataConsumerConfig();

    // Register each Data Consumer.
    for (Cd11DataConsumerConfig dcConfig : osdListOfDataConsumers) {
      logger.info("Registering data consumer for station {} on {}:{}",
          dcConfig.getDataProviderStationName(), dcConfig.getDataConsumerIpAddress(),
          dcConfig.getDataConsumerPort());

      String kafkaClientId = "cd11_rsdf_producer_" + dcConfig.getThreadName();
      this.addDataConsumer(dcConfig, producerFactory.makeProducer(kafkaClientId,
          kafkaConnectionConfiguration));
    }

    // Indicate that this GracefulThread is initialized.
    this.setThreadAsInitialized();

    // Periodically check the status of each thread.
    checkChildThreadStatusLoop();

    shutdownConsumerThreads();
  }

  private void checkChildThreadStatusLoop() {
    while (this.keepThreadRunning()) {
      dataConsumerThreads.values().parallelStream().forEach(dcThread -> {
        // Check that the thread is still running.
        if (!dcThread.isRunning()) {
          logger.warn("Restarting data consumer thread: {}", dcThread.getThreadName());

          // Check for an error message.
          if (dcThread.hasErrorMessage()) {
            // Log the error message.
            logger.error(String.format(
                "Data Consumer thread running on port %d shutdown in error: %s",
                dcThread.getCd11ListeningPort(),
                dcThread.getErrorMessage()));
          }

          // Restart the thread.
          try {
            dcThread.start();
          } catch (Exception e) {
            logger.error("Data Consumer thread failed to start.", e);
          }
        }
      });

      // Sleep for a period of time.
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        logger.error(
            "Data Consumer Manager received an InterruptedException, shutting down gracefully.", e);
        // Restore interrupted state
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  // Shuts down all Data Consumer threads.
  private void shutdownConsumerThreads() {
    for (Cd11DataConsumer dcThread : dataConsumerThreads.values()) {
      dcThread.onStop();
    }
  }

  private List<Cd11DataConsumerConfig> getDataConsumerConfig() {
    return config.getCd11StationParameters().stream()
        .map(singleStationConfig -> {
          if (singleStationConfig.isAcquired()) {
            Cd11DataConsumerConfig dataConsumerConfig = Cd11DataConsumerConfig
                .builderWithDefaults(singleStationConfig.getPort(),
                    singleStationConfig.getStationName())
                .setThreadName(
                    String.format("%s (Station: %s Port: %s)", THREAD_NAME,
                        singleStationConfig.getStationName(), singleStationConfig.getPort()))
                .setStationDisabled(singleStationConfig.isFrameProcessingDisabled())
                .build();
            logger.info("Consumer configuration registered for station {} on port {}",
                singleStationConfig.getStationName(), singleStationConfig.getPort());
            return dataConsumerConfig;
          }
          logger.info(
              "Station {} is not configured to be acquired. Consumer configuration not registered.",
              singleStationConfig.getStationName());
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  //-------------------- Statistics and State Info Methods --------------------

  /**
   * Returns true if a Data Consumer is registered on the given port.
   *
   * @param port Port number.
   * @return True if a data consumer is registered on the given port, false otherwise.
   */
  public boolean isDataConsumerPortRegistered(int port) {
    return dataConsumerThreads.containsKey(port);
  }

  public long getTotalDataFramesReceived(int port) {
    return isDataConsumerPortRegistered(port) ?
        dataConsumerThreads.get(port).getTotalDataFramesReceived() : 0;
  }

  /**
   * Adds a new Data Consumer thread.
   *
   * @param dcConfig Data Consumer configuration.
   * @param kafkaProducer Kafka Producer for RSDFs.
   */
  public void addDataConsumer(Cd11DataConsumerConfig dcConfig,
      Producer<String, String> kafkaProducer) {
    // Check whether a Data Consumer has already been assigned to this port.
    if (dataConsumerThreads.containsKey(dcConfig.getDataConsumerPort())) {
      throw new IllegalArgumentException(String.format(
          "A Data Consumer is already running on port %d.", dcConfig.getDataConsumerPort()));
    }

    dataConsumerThreads.put(dcConfig.getDataConsumerPort(),
        new Cd11DataConsumer(dcConfig,
            dataFrameReceiverConfig,
            kafkaProducer, kafkaConnectionConfiguration.getOutputRsdfTopic()));
  }

  /**
   * Removes an active Data Consumer thread.
   *
   * @param port Local port number that the Data Consumer is running on.
   */
  public void removeDataConsumer(int port) {
    Cd11DataConsumer cd11DataConsumer = dataConsumerThreads.remove(port);

    if (cd11DataConsumer == null) {
      throw new IllegalArgumentException(String.format(
          "Data Consumer on port %d does not exist.", port));
    }

    // Stop the data consumer.
    cd11DataConsumer.stop();
    cd11DataConsumer.waitUntilThreadStops();
  }

  ConcurrentHashMap<Integer, Cd11DataConsumer> getDataConsumerThreads() {
    return this.dataConsumerThreads;
  }

  /**
   * Returns the total number of CD 1.1 Data Consumer threads that are registered.
   *
   * @return Number of registered Data Consumer threads.
   */
  public int getTotalDataConsumerThreads() {
    return dataConsumerThreads.size();
  }

  /**
   * Returns the set of port numbers used by the CD 1.1 Data Consumer.
   *
   * @return List of port numbers in use.
   */
  public Set<Integer> getPorts() {
    return dataConsumerThreads.keySet();
  }

  @Override
  public void execute() {
    this.start();

    // Wait until the thread stops.
    this.waitUntilThreadStops();

    // Check for an error message.
    if (this.hasErrorMessage()) {
      throw new IllegalStateException(this.getErrorMessage());
    }
  }
}
