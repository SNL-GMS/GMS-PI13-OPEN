package gms.core.performancemonitoring.ssam.application;

import gms.core.performancemonitoring.ssam.control.StationSohAnalysisManagerControl;
import gms.shared.frameworks.control.ControlFactory;
import gms.shared.frameworks.service.ServiceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Kicks off consumer producer threads
 */
public class StationSohAnalysisManagerApplication {

  private static final Logger logger = LoggerFactory.getLogger(StationSohAnalysisManagerApplication.class);

  public static void main(String[] args) {

    logger.info("Starting StationSohAnalysisManagerApplication");

    final StationSohAnalysisManagerControl stationSohAnalysisManager =
        ControlFactory.createControl(StationSohAnalysisManagerControl.class);

    logger.info("Starting the StationSohAnalysisManager control");

    // Start any tasks, such as long-running tasks performed on background threads,
    // lengthy startup initializations, etc..
    new Thread(() -> stationSohAnalysisManager.start()).start();

    logger.info("Starting HTTP service threads");

    ServiceGenerator.runService(
        stationSohAnalysisManager,
        stationSohAnalysisManager.getSystemConfig());
  }

}