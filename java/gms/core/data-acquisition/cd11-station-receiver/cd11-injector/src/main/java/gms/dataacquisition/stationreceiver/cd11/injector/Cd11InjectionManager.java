package gms.dataacquisition.stationreceiver.cd11.injector;

import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.injector.configuration.InjectionConfig;
import gms.dataacquisition.stationreceiver.cd11.injector.configuration.StationConfig;
import gms.dataacquisition.stationreceiver.cd11.injector.configuration.StationInjectorConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cd11InjectionManager {

  private static final Logger logger = LoggerFactory.getLogger(Cd11InjectionManager.class);

  private final InjectionConfig config;
  private final Vertx vertx;
  private final Cd11FrameClientele frameClientele;

  Cd11InjectionManager(InjectionConfig config, Vertx vertx) {
    this.vertx = vertx;
    this.config = config;
    this.frameClientele = new Cd11FrameClientele(config.getFrameCreator(),
        config.getFrameDestination(), vertx);
  }

  /**
   * Start the injection manager, which performs a handshake with connman (if defined in config),
   * and collects station data file locations, which are then provided to the actual CD1.1
   * injector.
   */
  void start() {

    // If discovering station data nested in some base directory
    if (config.getUseBasePathDiscovery()) {
      traverseBasePathAndStartInjectors();
    } else {
      // Base directory not used; assuming individual stations provided via config
      loopStationsAndStartInjectors();
    }
  }

  /**
   * Using the base path provided in config, traverse the path and discover sub-directories with
   * RSDF data, and start the respective injector(s) for this data
   */
  private void traverseBasePathAndStartInjectors() {
    // Obtain all the station directories, and use them to create respective injectors
    Promise<Void> promise = Promise.promise();
    getStationsFromBasepath().compose(stations -> {
      Instant startTime = Instant.now().plusSeconds(config.getPreStartDelaySeconds());

      // is using connman to determine consumer locations
      if (config.getUseConnman()) {
        for (String stationID : stations) {
          logger.info("{} station is available for injecting data", stationID);
          useConnmanAndStartInjector(startTime, stationID,
              Paths.get(config.getRsdfDiscoveryBasePath(), stationID).toString());
        }
      } else {
        // not using connman: assuming consumer locations specified in config
        logger.info("Not using connman; only using stations explicitly defined via config");

        loopStationsAndStartInjectors();
      }

      promise.complete();
      return promise.future();
    }).setHandler(ar -> {
      if (ar.succeeded()) {
        logger.info("Successfully traversed base path for stations");
      } else {
        logger.error("Failed to traverse base path for stations", ar.cause());
      }
    });
  }

  /**
   * Use connman to negotiate the location of a consumer for the station's data
   *
   * @param stationId the stationId of the station that has data for injection
   * @param dataPath path to the station's RSDF data
   */
  private void useConnmanAndStartInjector(Instant startTime, String stationId, String dataPath) {
    io.vertx.reactivex.core.Future<Cd11ConnectionResponseFrame> cd11ConnectionResponseFrameFuture = frameClientele
        .establishCd11Connection(config.getConnManAddress(), config.getConnManPort(), stationId)
        .flatMap(Cd11FrameClient::sendConnectionRequest);
    cd11ConnectionResponseFrameFuture
        .flatMap(responseFrame -> assembleStationInjectorConfig(responseFrame, dataPath, stationId,
            startTime))
        .onComplete(
            injectorConfigResult -> {
              if (injectorConfigResult.succeeded()) {
                StationInjectorConfig stationInjectorConfig = injectorConfigResult.result();
                deployInjector(stationInjectorConfig, stationId);
              } else {
                logger.info("Failed to connect to Connman on {}:{} for {} station",
                    config.getConnManAddress(), config.getConnManPort(), stationId);
              }
            });
  }

  /**
   * Iterate over all the stations provided (whether defined explicitly, or discovered by traversing
   * from the base path), in addition to using overarching config provided
   */
  private void loopStationsAndStartInjectors() {
    Instant startTime = Instant.now().plusSeconds(config.getPreStartDelaySeconds());
    for (StationConfig stationConfig : config.getStationConfigs()) {
      //If a valid location is NOT specified, AND we're using connman, AND the station is enabled
      if (stationConfig.getConsumerAddress() == null
              && stationConfig.getConsumerPort() == null
              && stationConfig.isEnabled()
              && config.getUseConnman()) {
        useConnmanAndStartInjector(startTime, stationConfig.getId(), stationConfig.getDataPath());
        //If the station is enabled, and a valid location is specified in config
      } else if (stationConfig.isEnabled()) {
        StationInjectorConfig stationInjectorConfig = assembleStationInjectorConfig(
            stationConfig.getConsumerAddress(), stationConfig.getConsumerPort(),
            stationConfig.getDataPath(), stationConfig.getId(), startTime);
        deployInjector(stationInjectorConfig, stationConfig.getId());
      } else {
        logger.info("Station {} is DISABLED in config; not starting respective injector",
                stationConfig.getId());
      }
    }
  }

  /**
   * Deploy the actual verticle for the injector, with config
   *
   * @param cfg config to supply to the injector verticle (some combination of config.json config,
   * and injected or generated config)
   */
  private void deployInjector(StationInjectorConfig cfg, String stationName) {
    logger.info("Deploying injector for station {}", stationName);
    vertx.deployVerticle(
        "gms.dataacquisition.stationreceiver.cd11.injector.Cd11StationInjectorVerticle",
        new DeploymentOptions().setConfig(JsonObject.mapFrom(cfg)), ress -> {
          if (ress.succeeded()) {
            logger.info("Cd11 Station Injector for {} started", stationName);
          } else {
            logger.error("Failed to deploy injector for station {}", stationName, ress.cause());
          }
        });
  }

  /**
   * From a basepath, discover subdirectories that contain RSDF data (or currently, ANY files) e.g:
   * /BASEDIR -> ASAR -> ASAR1.json -> ASAR2.json -> BOSA ...
   *
   * @return List of all stations contained within the base directory (strictly IDs/names, NOT
   * paths)
   */

  private Future<List<String>> getStationsFromBasepath() {
    Promise<List<String>> stationsPromise = Promise.promise();

    List<String> dirContents = vertx.getDelegate().fileSystem().readDirBlocking(
        config.getRsdfDiscoveryBasePath());
    List<String> stations = new ArrayList<>();

    for (String absPath : dirContents) {

      try (Stream<Path> dirContentsStream = Files.list(Paths.get(absPath))) {
        String stationName = Paths.get(absPath).getFileName().toString();
        if (dirContentsStream.findAny().isPresent()) {
          logger
                  .info("Station {} directory {} has content; adding to results", stationName, absPath);
          stations.add(stationName);
        } else {
          logger.info("Skipping directory {}: no data to inject", absPath);
        }
      } catch (NotDirectoryException e) {
        logger.info("Skipping {}: not a directory", absPath);
      } catch (IOException e) {
        logger.warn("Problem with directory {}", absPath, e);
      }
    }
    stationsPromise.complete(stations);
    return stationsPromise.future();
  }

  private Future<StationInjectorConfig> assembleStationInjectorConfig(
      Cd11ConnectionResponseFrame responseFrame, String dataPath, String stationId,
      Instant startTime) {
    String ip = InetAddresses.fromInteger(responseFrame.ipAddress).toString();
    if (ip.charAt(0) == '/') {
      ip = ip.substring(1);
    }
    int port = responseFrame.port;
    logger.info("{} injector has received {}:{} as the destination address", stationId, ip, port);

    StationInjectorConfig injectorConfig = assembleStationInjectorConfig(
        InetAddresses.forString(ip), port, dataPath, stationId, startTime);
    return Future.succeededFuture(injectorConfig);
  }

  private StationInjectorConfig assembleStationInjectorConfig(InetAddress consumerAddress,
      Integer consumerPort, String dataPath, String stationName, Instant startTime) {

    return StationInjectorConfig.builder()
        .setConsumerAddress(consumerAddress)
        .setConsumerPort(consumerPort)
        .setStationDataPath(dataPath)
        .setStationName(stationName)
        .setDataReferenceTime(config.getReferenceTime())
        .setTargetStartTime(startTime)
        .setFrameCreator(config.getFrameCreator())
        .setFrameDestination(config.getFrameDestination())
        .setLoopDataStream(config.getLoopDataStream())
        .setMaxConcurrentOpenFiles(config.getMaxConcurrentOpenFiles())
        .build();
  }
}


