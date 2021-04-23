package gms.dataacquisition.stationreceiver.cd11.dataman;

import static com.google.common.base.Preconditions.checkNotNull;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.DataManConfig;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.KafkaConnectionConfiguration;
import gms.shared.frameworks.common.annotations.Component;
import gms.shared.frameworks.control.ControlContext;
import gms.shared.frameworks.control.ControlFactory;
import gms.shared.frameworks.osd.coi.waveforms.AcquisitionProtocol;
import gms.shared.frameworks.systemconfig.SystemConfig;
import gms.shared.utilities.kafka.KafkaConfiguration;
import java.util.MissingResourceException;
import javax.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Service class responsible for mapping component information to allow configuration and control
 * frameworks to setup appropriately
 */
@Component("dataman")
@Path("/")
public class Cd11DataManControlService {

  private static Logger logger = LoggerFactory.getLogger(Cd11DataManControlService.class);

  private final Cd11DataMan dataMan;

  public Cd11DataManControlService(Cd11DataMan dataMan) {
    this.dataMan = dataMan;
  }

  public static Cd11DataManControlService create(ControlContext context) {
    checkNotNull(context, "Cannot create Cd11DataManService from null context");

    SystemConfig systemConfig = context.getSystemConfig();

    // DataManConfig setup using processing config
    DataManConfig dataManConfig = DataManConfig
        .create(context.getProcessingConfigurationConsumerUtility(),
            systemConfig.getValueAsInt("cd11-dataconsumer-baseport"));

    // DataFrameReceiverConfiguration setup using processing and system config
    DataFrameReceiverConfiguration dataFrameReceiverConfiguration = DataFrameReceiverConfiguration
        .create(AcquisitionProtocol.CD11, context.getProcessingConfigurationRepository(),
            systemConfig);

    // ReactorKafkaConfiguration using system config
    KafkaConfiguration kafkaConfiguration = KafkaConfiguration
            .create(systemConfig);

    return new Cd11DataManControlService(
        toggleExperimental(systemConfig, dataManConfig, dataFrameReceiverConfiguration, kafkaConfiguration));
  }

  /**
   * Feature toggle, returning back different implementations of {@link Cd11DataMan} depending on
   * what is enabled via system configuration
   *
   * @param systemConfig System Configuration client
   * @param dataManConfig Processing Configuration client
   * @param dataFrameReceiverConfiguration Dataframe Receiver Configuration client
   * @return Implementation of Cd11RsdfProcessor given the feature toggle
   */
  private static Cd11DataMan toggleExperimental(SystemConfig systemConfig,
                                                DataManConfig dataManConfig,
                                                DataFrameReceiverConfiguration dataFrameReceiverConfiguration,
                                                KafkaConfiguration kafkaConfiguration) {
    try {
      if (systemConfig.getValueAsBoolean("experimental-enabled")) {
        logger.info("Using experimental processor {}",
            "TODO: Defaulting to non-experimental until experimental implemented");

        return ReactorCd11DataMan.create(dataManConfig, dataFrameReceiverConfiguration, kafkaConfiguration);
      }
      logger.info("Using non-experimental processor {}",
          GracefulCd11DataMan.class.getCanonicalName());

      return GracefulCd11DataMan.create(
          dataManConfig,
          dataFrameReceiverConfiguration,
          new KafkaProducerFactory<>(),
          KafkaConnectionConfiguration.create(systemConfig));
    } catch (NullPointerException | MissingResourceException | IllegalArgumentException e) {

      logger.warn(String.format("Experimental flag check failed. Defaulting to %s",
          GracefulCd11DataMan.class.getCanonicalName()), e);
      return GracefulCd11DataMan.create(
          dataManConfig,
          dataFrameReceiverConfiguration,
          new KafkaProducerFactory<>(),
          KafkaConnectionConfiguration.create(systemConfig));
    }
  }

  public Cd11DataMan getDataMan() {
    return dataMan;
  }

  public static void main(String[] args) {
    Cd11DataMan dataMan = ControlFactory.runService(Cd11DataManControlService.class).getDataMan();

    try {
      dataMan.execute();
    } catch (Exception e) {
      logger.error("DataMan encountered an unrecoverable exception: ", e);
      System.exit(1);
    }
  }
}
