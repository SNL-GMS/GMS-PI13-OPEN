package gms.dataacquisition.stationreceiver.cd11.injector;

import gms.dataacquisition.stationreceiver.cd11.injector.configuration.InjectionConfig;
import gms.dataacquisition.stationreceiver.cd11.injector.configuration.RepeaterConfig;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Cd11MainVerticle extends AbstractVerticle {

  private static final String INJECTOR_MODE = "injector";
  private static final String REPEATER_MODE = "repeater";

  private static Logger logger = LoggerFactory.getLogger(Cd11MainVerticle.class);

  /**
   * Determines the operation mode defined in config, and either starts the injection manager
   * or the repeater manager, mapping the base config as applicable
   */
  @Override
  public void start() {
    //Don't see a way around loading the env here, to get the path to the config for the next initialization
    //(which needs the path explicitly)
    Runtime.getRuntime().addShutdownHook(new Thread(vertx::close));
    ConfigRetriever topRetriever = ConfigRetriever.create(vertx.getDelegate());
    topRetriever.getConfig(topJson -> {
      if (topJson.failed()) {
        logger.error("JSON config failed to load; exiting.", topJson.cause());
        vertx.close(
                v -> logger.error("Vert.x shut down due to fatal problem loading config"));
        return;
      }

      // the vert.x default config store precedence is config > sys > env so it needs to be overridden here
      String path = topJson.result().getString("CD11_INJECTOR_CONFIG_PATH");
      if (path == null) {
        logger.error("Missing environment variable CD11_INJECTOR_CONFIG_PATH");
        vertx.close(
                v -> logger.error("Vert.x shut down due to fatal problem loading config"));
        return;
      }
      ConfigStoreOptions fileStore = new ConfigStoreOptions()
              .setType("file")
              .setOptional(false)
              .setConfig(new JsonObject().put("path", path));
      ConfigStoreOptions envVarsStore = new ConfigStoreOptions().setType("env");

      //ordering here causes config file options to be overridden by the ENV vars
      ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore).addStore(envVarsStore);

      ConfigRetriever retriever = ConfigRetriever.create(vertx.getDelegate(), options);

      retriever.getConfig(json -> {
        if (json.failed()) {
          logger.error("JSON config failed to load; exiting.", json.cause());
          vertx.close(
                  v -> logger.error("Vert.x shut down due to fatal problem loading config"));
          return;
        }

        JsonObject managerConfig = json.result();
        String mode = managerConfig.getString("INJECTOR_OPERATION_MODE");
        if (mode == null || mode.isBlank()) {
          mode = managerConfig.getString("injectorOperationMode");
        }
        Validate.validState(mode.equalsIgnoreCase(INJECTOR_MODE)
                || mode.equalsIgnoreCase(REPEATER_MODE));

        startInjectorOrRepeater(mode, managerConfig);
      });
    });
  }

  private void startInjectorOrRepeater(String mode, JsonObject config) {
    if (mode.equalsIgnoreCase(INJECTOR_MODE)) {
      logger.info("Operation mode has been identified as INJECTOR");
      try {
        InjectionConfig injectionConfig = config.mapTo(InjectionConfig.class);

        Cd11InjectionManager injectionManager = new Cd11InjectionManager(injectionConfig, vertx);
        injectionManager.start();
      } catch (Exception e) {
        logger.error("Problem setting up injector.  Terminating vert.x...");
        vertx.close(v -> logger
                .error("Vert.x shut down due to fatal problem while setting up injector", e));
      }
    } else if (mode.equalsIgnoreCase(REPEATER_MODE)) {
      logger.info("Operation mode has been identified as REPEATER");
      try {
        RepeaterConfig repeaterConfig = config.mapTo(RepeaterConfig.class);

        logger.info("Initializing Repeater Manager Verticle...");
        Cd11RepeaterManager repeaterManager = new Cd11RepeaterManager(repeaterConfig, vertx);
        repeaterManager.start();
        logger.info("Repeater Manager Verticle successfully started");
        Runtime.getRuntime().addShutdownHook(new Thread(repeaterManager::close));
      } catch (Exception e) {
        logger.error("Problem setting up repeater manager.  Terminating vert.x...");
        vertx.close(v -> logger
                .error("Vert.x shut down due to fatal problem while setting up repeater manager", e));
      }
    } else {
      logger.error("Not operating in repeater nor injector mode due to invalid state, exiting vertx...");
      vertx.close(v -> logger
              .error("Vert.x shut down due to fatal problem while setting up injector or repeater"));
    }
  }
}
