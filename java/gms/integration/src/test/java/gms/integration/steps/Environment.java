package gms.integration.steps;

import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.test.utils.containers.GmsDeploymentContainer;
import gms.shared.frameworks.test.utils.services.GmsServiceType;
import java.io.File;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Environment} is a class that follows the Singleton Context pattern commonly used when
 * attempting to share between Cucumber TestStep files (allowing for better Step Organization).
 * Specifically, this abstract class's purpose is to share a common instance of {@link
 * GmsDeploymentContainer} amongst the different TestSteps file (which is required when testing
 * things like service health, liveness, etc.)
 */
public class Environment {

  private static final Logger logger = LoggerFactory.getLogger(Environment.class);

  private static GmsDeploymentContainer container;
  private SohRepositoryInterface sohRepo;

  private boolean started = false;
  private List<String> serviceTypes = null;

  public Environment() {
    if (container == null) {
      container = new GmsDeploymentContainer(
          UUID.randomUUID().toString(),
          new File(
              Thread.currentThread().getContextClassLoader()
                  .getResource("gms/integration/compose-files/docker-compose-dev.yml")
                  .getPath()));
    }
  }

  public GmsDeploymentContainer deploymentCtxt() {
    return container;
  }

  public boolean isStarted(){
    return started;
  }

  // Getter/setter for the SohRepositoryInterface
  public SohRepositoryInterface getSohRepositoryInterface() {
    return sohRepo;
  }

  public void setSohRepositoryInterface(SohRepositoryInterface sohRepo) {
    this.sohRepo = sohRepo;
  }

  public void startEnvironmentWithServices(List<String> serviceTypes) {
    if (!started) {
      if (this.serviceTypes == null) {
        this.serviceTypes = serviceTypes;
      }
      var gmsServiceTypes = serviceTypes.stream().map(GmsServiceType::valueOf)
          .toArray(GmsServiceType[]::new);
      logger.info("!!!!TEST CONTAINERS - STARTING!!!!");
      container.withServices(gmsServiceTypes)
          .start();
      started = true;
      logger.info("!!!!TEST CONTAINERS - STARTED!!!!");
    }
  }

  public void tearDownEnvironment() {
    final var ryukDisabled = System.getenv("TESTCONTAINERS_RYUK_DISABLED");
    if (ryukDisabled == null || ryukDisabled.equals("false")) {
      shutdownEnvironment();
    }
  }

  private void shutdownEnvironment() {
    if (container != null) {
      logger.info("!!!!TEST CONTAINERS - SHUTTING DOWN!!!!");
      container.close();
      started = false;
      container = null;
    }
    logger.info("!!!!TEST CONTAINERS - SHUTDOWN!!!!");
  }

  public void restart() {
    this.shutdownEnvironment();
    this.startEnvironmentWithServices(serviceTypes);
  }
}
