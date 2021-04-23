package gms.core.dataacquisition;

import gms.shared.frameworks.control.ControlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AceiMergeService {

  private static Logger logger = LoggerFactory.getLogger(AceiMergeService.class);

  public static void main(String[] args) {
    logger.info("Initializing AceiMergeService...");
    AceiMergeProcessor processor = ControlFactory.runService(AceiMergeProcessor.class);

    logger.info("Starting AceiMergeService...");
    processor.start();
  }

}
