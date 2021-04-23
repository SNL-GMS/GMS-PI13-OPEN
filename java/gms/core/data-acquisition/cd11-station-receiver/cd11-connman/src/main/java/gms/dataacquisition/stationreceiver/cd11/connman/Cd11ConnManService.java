package gms.dataacquisition.stationreceiver.cd11.connman;

import static com.google.common.base.Preconditions.checkNotNull;

import gms.dataacquisition.stationreceiver.cd11.connman.configuration.Cd11ConnManConfig;
import gms.shared.frameworks.common.annotations.Component;
import gms.shared.frameworks.control.ControlContext;
import gms.shared.frameworks.control.ControlFactory;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.util.MissingResourceException;
import javax.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("connman")
@Path("/")
public class Cd11ConnManService {

  private static final Logger logger = LoggerFactory.getLogger(Cd11ConnManService.class);

  private final Cd11ConnMan cd11ConnMan;

  private Cd11ConnManService(Cd11ConnMan connMan) {
    this.cd11ConnMan = connMan;
  }

  /**
   * Create {@link Cd11ConnManService} from a {@link ControlContext}
   *
   * @param context the control context, not null
   * @return an instance of Cd11ConnManService
   */
  public static Cd11ConnManService create(ControlContext context) {
    checkNotNull(context, "Cannot create Cd11ConnManService from null context");

    SystemConfig systemConfig = context.getSystemConfig();

    return new Cd11ConnManService(
            toggleExperimental(systemConfig,
                    Cd11ConnManConfig.create(context.getProcessingConfigurationConsumerUtility(),
                            systemConfig.getValueAsInt("cd11-dataconsumer-baseport"))));
  }

  /**
   * Feature toggle, returning back different implementations of {@link Cd11ConnMan} depending
   * on what is enabled via system configuration
   * @param systemConfig System Configuration client
   * @param cd11ConnManConfig Cd11 ConnMan configuration
   * @return Implementation of Cd11RsdfProcessor given the feature toggle
   */
  private static Cd11ConnMan toggleExperimental(SystemConfig systemConfig,
                                                      Cd11ConnManConfig cd11ConnManConfig) {

    try {
      if (systemConfig.getValueAsBoolean("experimental-enabled")) {
        logger.info("Using experimental connman {}",
                ReactorCd11ConnMan.class.getCanonicalName());

        return ReactorCd11ConnMan
                .create(systemConfig, cd11ConnManConfig);
      }
      logger.info("Using non-experimental processor {}",
              GracefulCd11ConnMan.class.getCanonicalName());

      return GracefulCd11ConnMan
              .create(systemConfig, cd11ConnManConfig);
    } catch (NullPointerException | MissingResourceException | IllegalArgumentException e) {
      logger.warn(String.format("Experimental flag check failed. Defaulting to %s",
              GracefulCd11ConnMan.class.getCanonicalName()), e);

      return GracefulCd11ConnMan
              .create(systemConfig, cd11ConnManConfig);
    }
  }

  public Cd11ConnMan getCd11ConnMan() {
    return cd11ConnMan;
  }

  public static void main(String[] args) {
    Cd11ConnMan cd11ConnMan = ControlFactory.runService(Cd11ConnManService.class)
            .getCd11ConnMan();

    cd11ConnMan.start();
  }
}
