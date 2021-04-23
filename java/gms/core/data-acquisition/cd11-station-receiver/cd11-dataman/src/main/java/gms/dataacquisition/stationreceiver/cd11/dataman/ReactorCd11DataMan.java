package gms.dataacquisition.stationreceiver.cd11.dataman;


import static com.google.common.base.Preconditions.checkNotNull;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11GapList;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11DataConsumerParameters;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.DataManConfig;
import gms.shared.utilities.kafka.KafkaConfiguration;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpResources;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;

// a collection of bootstrap servers (TCP servers) that use the same loop resources
public class ReactorCd11DataMan implements Cd11DataMan {

  private static Logger logger = LoggerFactory.getLogger(ReactorCd11DataMan.class);
  private final DataManConfig dataManConfig;
  private final DataFrameReceiverConfiguration receiverConfig;
  private final KafkaConfiguration kafkaConfiguration;
  private static final int NUM_RETRIES_PORT_BIND = 100;
  private static final Duration INITIAL_WAIT = Duration.ofSeconds(1);

  private HashMap<Integer, TcpServer> portBootstrapMap;
  private HashMap<Integer, String> portStationMap;

  private static final long CONNECTION_EXPIRED_TIME_LIMIT_SEC = 120;

  private ReactorCd11DataMan(DataManConfig dataManConfig,
      DataFrameReceiverConfiguration receiverConfig,
      KafkaConfiguration kafkaConfiguration) {
    this.dataManConfig = dataManConfig;
    this.receiverConfig = receiverConfig;
    this.kafkaConfiguration = kafkaConfiguration;
    this.portBootstrapMap = new HashMap<>();
    this.portStationMap = new HashMap<>();
    ResourceLeakDetector.setLevel(Level.DISABLED);
  }

  /**
   * Factory method for creating ReactorCd11DataMan
   *
   * @param dataManConfig DataManConfig
   * @param receiverConfig DataFrameReceiverConfiguration
   * @param kafkaConfiguration ReactorKafkaConfiguration
   * @return ReactorCd11DataMan
   */
  public static ReactorCd11DataMan create(DataManConfig dataManConfig,
      DataFrameReceiverConfiguration receiverConfig,
      KafkaConfiguration kafkaConfiguration) {
    checkNotNull(dataManConfig, "Cannot create ReactorCd11DataMan with null DataManConfig.");
    checkNotNull(dataManConfig,
        "Cannot create ReactorCd11DataMan with null DataFrameReceiverConfiguration.");
    checkNotNull(dataManConfig,
        "Cannot create ReactorCd11DataMan with null ReactorKafkaConfiguration.");

    return new ReactorCd11DataMan(dataManConfig, receiverConfig, kafkaConfiguration);
  }

  @Override
  public void execute() {

    List<Cd11DataConsumerParameters> consumerConfigs = this.getAcquiredStationParameters();

    for (Cd11DataConsumerParameters consumerParameters : consumerConfigs) {

      int stationPort = consumerParameters.getPort();
      String stationName = consumerParameters.getStationName();
      TcpServer server = TcpServer.create();
      //set timeout
      server = server.doOnConnection(conn -> conn.addHandler(
          new ReadTimeoutHandler(CONNECTION_EXPIRED_TIME_LIMIT_SEC, TimeUnit.SECONDS)))
          .port(stationPort);

      //set resources
      server = setUpResources(server);

      //get frame factory and gap list for station
      //TODO: Discuss if this needs concurrent protections (likely does, isn't in GracefuDataMan)
      Cd11GapList gapList = Cd11GapListUtility
          .loadGapState(stationName);
      Cd11DataHandler dataHandler = Cd11DataHandler
          .create(stationName, receiverConfig, kafkaConfiguration);

      //set handler
      server = server.handle(
          dataHandler.handlecd11Data(gapList, !consumerParameters.isFrameProcessingDisabled())
      ).doOnConnection(connection -> {
        logger.info(
            "Data Manager connection established on port {}",
            stationPort);

        connection.onDispose(() -> logger
            .info("Data Manager connection closed on station {} - port {}", stationName,
                stationPort));
      });

      portBootstrapMap.put(stationPort, server);
      portStationMap.put(stationPort, stationName);
    }

    this.bind();
  }


  //TODO reevaluate putting all the servers on the same loop resources if there are performance issues
  public TcpServer setUpResources(TcpServer server) {
    return setUpResources(server, TcpResources.get());
  }


  //default max connections is 500, this may need to be changed as stations are added
  public TcpServer setUpResources(TcpServer server, LoopResources loopResources) {
    return server.runOn(loopResources);

  }

  public void bind() {
    TcpServer server;
    for (Map.Entry<Integer, TcpServer> pair : portBootstrapMap.entrySet()) {
      int currentPort = pair.getKey();
      String stationName = portStationMap.get(currentPort);
      server = pair.getValue();

      server.bind()
          .retryWhen(Retry.backoff(NUM_RETRIES_PORT_BIND, INITIAL_WAIT))
          .doOnSuccess(successful -> logger
              .info("Server for station:port - {}:{} bound successfully", stationName, currentPort))
          .doOnError(error -> logger
              .error("Error binding to station:port - {}:{}", stationName, currentPort, error))
          .doOnTerminate(() -> logger
              .info("TcpServer on terminate station:port - {}:{}", stationName, currentPort))
          .doFinally(f -> logger.info("FINALLY: station:port - {}:{}", stationName, currentPort))
          .doOnTerminate(
              () -> logger.info("TERMINATE: station:port - {}:{}", stationName, currentPort))
          .doOnCancel(() -> logger.info("CANCEL: station:port - {}:{}", stationName, currentPort))
          .subscribe();
    }

  }

  public void bindNow() {
    for (TcpServer server : portBootstrapMap.values()) {
      server.bindNow();
    }
  }

  /**
   * Get the parameters that are configured to be acquired
   *
   * @return Consumer parameters configured to be acquired
   */
  private List<Cd11DataConsumerParameters> getAcquiredStationParameters() {
    return dataManConfig.getCd11StationParameters().stream()
        .filter(station -> {
          if (station.isAcquired()) {
            logger.info("Consumer parameters registered for station {} on port {}",
                station.getStationName(), station.getPort());
          } else {
            logger.info(
                "Station {} is not configured to be acquired. Consumer parameters not registered.",
                station.getStationName());
          }
          return station.isAcquired();
        })
        .collect(Collectors.toList());
  }
}
