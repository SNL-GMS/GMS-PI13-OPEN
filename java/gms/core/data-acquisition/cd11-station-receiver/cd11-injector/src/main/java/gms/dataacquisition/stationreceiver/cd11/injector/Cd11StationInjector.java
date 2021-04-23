package gms.dataacquisition.stationreceiver.cd11.injector;

import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.injector.configuration.StationInjectorConfig;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;


/**
 * Main orchestrator for the station injection.
 * <p>
 * Through handlers sets up injection pre-requisites, including establishing the CD1.1 connection
 * and ordering the data, and then delegates to the handler that meters the injection.
 * <p>
 * Handles higher-order concerns like datastream looping.
 */
public class Cd11StationInjector {

  private final StationInjectorConfig config;
  private final Vertx vertx;
  private final Cd11FrameClientele frameClientele;

  private final Logger logger;

  private Cd11FrameClient connectionManager;
  private InjectionDataHandler dataHandler;

  /**
   * Constructor
   *
   * @param config Configuration of the station injector
   * @param vertx  handle on the vertx instance for timer scheduling
   */
  Cd11StationInjector(StationInjectorConfig config, Vertx vertx) {
    this.config = config;
    this.vertx = vertx;
    this.frameClientele = new Cd11FrameClientele(config.getFrameCreator(),
        config.getFrameDestination(), vertx);

    this.logger = LoggerFactory
        .getLogger(String.format("%s|%s", Cd11StationInjector.class, config.getStationName()));
  }

  /**
   * Creates a single consolidated data stream from all files to be injected
   *
   * @param path root directory of this station's data
   */
  private Flowable<RawStationDataFrame> setupDataStream(String path) {
    InjectionFileHandler fileHandler = new InjectionFileHandler();
    try {
      return fileHandler.createConsolidatedDataStream(path);
    } catch (IOException ex) {
      logger.error("Error reading RSDF Flowable from filesystem", ex);
      stopInjector();
      return null;
    }
  }

  /**
   * Restarts the injector
   *
   * @param seconds how many seconds from now to restart
   */
  public void restartInjector(int seconds) {
    logger.info("Restarting injector");
    this.stopInjector();
    Instant now = Instant.now();
    Instant startTime = now.compareTo(config.getTargetStartTime()) > 0 ? now : config.getTargetStartTime();
    vertx.setTimer((long) seconds * 1000, res -> this.startInjector(startTime));
  }

  /**
   * Sets up the CD1.1 connection, creating a {@link Cd11FrameClient} if successful
   */
  private Future<Cd11FrameClient> setupConnection() {
    // Set socket options
    return frameClientele.establishCd11Connection(InetAddresses.toAddrString(config.getConsumerAddress()),
        config.getConsumerPort(), config.getStationName())
        .onSuccess(connectionResult -> {
          connectionManager = connectionResult;
          logger.info("Successful connection to {}:{}", config.getConsumerAddress(),
              config.getConsumerPort());
        })
        .onFailure(cause -> {
          logger.error("Failed to connect to data consumer at {}:{}", config.getConsumerAddress(),
              config.getConsumerPort());
          restartInjector(10);
        });
  }

  private void loopData() {
    logger.info("Looping data stream");

    // Calculate how data handler should change the sequence numbers
    // Since we never reset the sequence number bookkeeping this offset will
    // increase every loop.  So if the data has sequence numbers 5,6,7, the second loop
    // will have offset 3 to produce 8,9,10; third loop offset 6, etc
    long sequenceNumberOffset =
        connectionManager.getHighestSentSequenceNumber() - connectionManager
            .getLowestSentSequenceNumber() + 1;

    Instant startTime = Instant.now().plusSeconds(5);
    Flowable<RawStationDataFrame> rsdfFlowable = setupDataStream(config.getStationDataPath());
    setupDataHandler(startTime, sequenceNumberOffset, rsdfFlowable, connectionManager)
        .onComplete(res -> {
          if (res.succeeded()) {
            logger.info("Successfully looped the data stream");
          } else {
            logger.error("Failed to loop data stream.  Exiting");
            stopInjector();
          }
        });
  }

  /**
   * Sets up the Injection Data Handler that will convert, meter, and inject the data
   *
   * @param startTime injection start time
   * @param sequenceNumberOffset how much to add to the sequence number (used in data stream
   * looping)
   */
  private Future<Void> setupDataHandler(Instant startTime, long sequenceNumberOffset,
      Flowable<RawStationDataFrame> rsdfFlowable, Cd11FrameClient connectionManager) {
    // TODO handle the restart variant
    Promise<Void> promise = Promise.promise();

    dataHandler = new InjectionDataHandler(rsdfFlowable, startTime, config.getDataReferenceTime(),
        sequenceNumberOffset, config.getMeterDataStream(), connectionManager, vertx);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> dataHandler.close()));
    
    dataHandler.start(res -> {
      if (res.succeeded()) {
        if (config.getLoopDataStream()) {
          loopData();
        } else {
          stopInjector();
        }
      } else if (res.failed()) {
        restartInjector(10);
      }
    });

    promise.complete();
    return promise.future();
  }

  /**
   * Starts the injection, sequencing data stream creation, connection creation and injection
   * handler creation
   */
  public Future<Void> startInjector(Instant startTime) {
    logger.info("Injection will commence at {} for station {}", startTime, config.getStationName());

    final long sequenceNumberOffset = 0;

    return setupConnection().flatMap(client ->
          setupDataHandler(startTime, sequenceNumberOffset, setupDataStream(config.getStationDataPath()), client));
  }

  /**
   * Stops the injector
   */
  private void stopInjector() {
    logger.info("Stopping injector");
    if (dataHandler != null) {
      dataHandler.stop();
      dataHandler = null;
    }
    if (connectionManager != null) {
      connectionManager.close("Injector shutting down");
      connectionManager = null;
    }
  }

}

