package gms.dataacquisition.cd11.rsdf.processor;

import static com.google.common.base.Preconditions.checkNotNull;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.shared.frameworks.common.annotations.Component;
import gms.shared.frameworks.control.ControlContext;
import gms.shared.frameworks.control.ControlFactory;
import gms.shared.frameworks.osd.coi.waveforms.AcquisitionProtocol;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.util.MissingResourceException;
import javax.ws.rs.Path;

import gms.shared.utilities.kafka.KafkaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class responsible for mapping component information to allow configuration and control
 * frameworks to setup appropriately
 */
@Component("cd11-rsdf-processor")
@Path("/")
public class Cd11RsdfProcessorService {

  private static final Logger logger = LoggerFactory.getLogger(Cd11RsdfProcessorService.class);

  private final Cd11RsdfProcessor cd11RsdfProcessor;

  public Cd11RsdfProcessorService(Cd11RsdfProcessor processor) {
    this.cd11RsdfProcessor = processor;
  }

  public static Cd11RsdfProcessorService create(ControlContext context) {
    checkNotNull(context, "Cannot create Cd11RsdfProcessorService from null context");

    SystemConfig systemConfig = context.getSystemConfig();

    KafkaConfiguration kafkaConfiguration = KafkaConfiguration
        .create(systemConfig);
    DataFrameReceiverConfiguration dataFrameReceiverConfiguration = DataFrameReceiverConfiguration
        .create(AcquisitionProtocol.CD11, context.getProcessingConfigurationRepository(),
            systemConfig);

    return new Cd11RsdfProcessorService(
        toggleExperimental(systemConfig, kafkaConfiguration, dataFrameReceiverConfiguration));
  }

  /**
   * Feature toggle, returning back different implementations of {@link Cd11RsdfProcessor} depending
   * on what is enabled via system configuration
   * @param systemConfig System Configuration client
   * @param kafkaConfiguration Reactor Kafka Configuration client
   * @param dataFrameReceiverConfiguration Dataframe Receiver Configuration client
   * @return Implementation of Cd11RsdfProcessor given the feature toggle
   */
  private static Cd11RsdfProcessor toggleExperimental(SystemConfig systemConfig,
      KafkaConfiguration kafkaConfiguration,
      DataFrameReceiverConfiguration dataFrameReceiverConfiguration) {
    try {
      if (systemConfig.getValueAsBoolean("experimental-enabled")) {
        logger.info("Using experimental processor {}",
            ReactorCd11RsdfProcessor.class.getCanonicalName());

        return ReactorCd11RsdfProcessor
            .create(kafkaConfiguration, dataFrameReceiverConfiguration);
      }

      logger.info("Using non-experimental processor {}",
          KafkaStreamsCd11RsdfProcessor.class.getCanonicalName());

      return KafkaStreamsCd11RsdfProcessor
          .create(kafkaConfiguration, dataFrameReceiverConfiguration);
    } catch (NullPointerException | MissingResourceException | IllegalArgumentException e) {
      logger.warn(String.format("Experimental flag check failed. Defaulting to %s",
          KafkaStreamsCd11RsdfProcessor.class.getCanonicalName()), e);

      return KafkaStreamsCd11RsdfProcessor
          .create(kafkaConfiguration, dataFrameReceiverConfiguration);
    }
  }

  public Cd11RsdfProcessor getCd11RsdfProcessor() {
    return cd11RsdfProcessor;
  }

  public static void main(String[] args) {
    Cd11RsdfProcessor cd11RsdfProcessor = ControlFactory.runService(Cd11RsdfProcessorService.class)
        .getCd11RsdfProcessor();

    cd11RsdfProcessor.run();
  }
}
