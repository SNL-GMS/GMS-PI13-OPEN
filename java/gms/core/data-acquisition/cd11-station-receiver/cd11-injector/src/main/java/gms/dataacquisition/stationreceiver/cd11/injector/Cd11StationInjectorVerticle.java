package gms.dataacquisition.stationreceiver.cd11.injector;

import gms.dataacquisition.stationreceiver.cd11.injector.configuration.StationInjectorConfig;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import java.nio.channels.ClosedChannelException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verticle for the station injector
 */
public class Cd11StationInjectorVerticle extends AbstractVerticle {

  private Logger logger;

  @Override
  public void start() {
    // Start the station injection verticle

    // Read in config

    ConfigRetriever retriever = ConfigRetriever.create(vertx);
    retriever.getConfig(json -> {

      JsonObject config = json.result();
      String stationName = config.getString("stationName");

      logger = LoggerFactory
              .getLogger(String.format("%s|%s", Cd11StationInjectorVerticle.class, stationName));

      try {
        StationInjectorConfig injectorConfig = config.mapTo(StationInjectorConfig.class);

        Cd11StationInjector injector = new Cd11StationInjector(injectorConfig, vertx);

        Instant startTime = injectorConfig.getTargetStartTime();
        // Start the station injection setup
        injector.startInjector(startTime)
            .onComplete(res -> {
                if (res.failed() && res.cause() instanceof ClosedChannelException) {
                  injector.restartInjector(10);
                }
            });
      } catch (Exception e) {
        logger.error("Problem setting up injector.  Terminating vertx...");
        vertx.close(v -> logger.error("Vert.x shut down due to fatal problem while setting up injector", e));
      }
    });
  }
}

