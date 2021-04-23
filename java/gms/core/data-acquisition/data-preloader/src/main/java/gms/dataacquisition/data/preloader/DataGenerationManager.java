package gms.dataacquisition.data.preloader;

import gms.dataacquisition.data.preloader.generator.GmsPreloaderException;
import gms.shared.frameworks.soh.repository.SohRepository;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import gms.shared.frameworks.systemconfig.SystemConfig;
import org.apache.commons.cli.HelpFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataGenerationManager {

  private static final Logger logger = LoggerFactory.getLogger(DataGenerationManager.class);

  public static void main(String[] args) {
    if (args.length == 0) {
      HelpFormatter helpFormatter = new HelpFormatter();
      helpFormatter.printHelp("data-preloader", DatasetGeneratorOptions.options, true);
    } else {
      final var generationSpec = DatasetGeneratorOptions.parse(args);
      final SohRepository sohRepository = SohRepositoryFactory
          .createSohRepository(SystemConfig.create("preloader"));

      logger.info("~~~STARTING DATA LOADER~~~");

      try {
        var dataGenerator = DataGeneratorFactory.getDataGenerator(generationSpec, sohRepository);
        dataGenerator.run();
      } catch (Exception e) {
        final var error = new GmsPreloaderException("Failed to run data generator", e);
        logger.error(error.getMessage(), error);
        throw error;
      }

      logger.info("~~~FINISHED DATA LOADER~~~");

      System.exit(0);
    }
  }
}
