package gms.core.performancemonitoring.soh.application;

import gms.core.performancemonitoring.soh.control.StationSohControl;
import gms.shared.frameworks.control.ControlFactory;
import gms.shared.frameworks.service.ServiceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kicks off the producer and consumer threads for taking station state of health and running
 * summary calculations on it.
 */
public class StationSohApplication {

  private static final Logger logger = LoggerFactory.getLogger(StationSohApplication.class);

  public static void main(String[] args) {

    logger.info("Starting StationSohApplication");

    try {

      final var stationSohControl = ControlFactory.createControl(StationSohControl.class);

      logger.info("Starting consumer and producer threads");
      new Thread(stationSohControl::start).start();

      ServiceGenerator.runService(stationSohControl, stationSohControl.getSystemConfig());
      logger.info("Starting HTTP service threads");

    } catch (Exception e) {
      // Found that in the integration test, the control is created before the other containers
      // can supply the configuration. This results in an IllegalArgumentException. But, for some
      // reason, the application does not exit cleanly even though the only non-daemon thread is
      // the main thread. Remedy this by calling System.exit(), so a container restart is
      // triggered.
      logger.error("Error starting up the StationSohControl", e);
      System.exit(1);
    }
  }

}
