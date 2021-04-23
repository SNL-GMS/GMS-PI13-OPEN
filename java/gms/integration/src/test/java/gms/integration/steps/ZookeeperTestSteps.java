package gms.integration.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ZookeeperTestSteps {
  private Environment environment;

  public ZookeeperTestSteps(Environment environment) {
    this.environment = environment;
  }

  @Given("Zookeeper is up and running")
  public void testZookeeperIsRunning() {
    assertTrue(this.environment.deploymentCtxt().isServiceCreated(GmsServiceType.ZOOKEEPER));
    assertTrue(this.environment.deploymentCtxt().isServiceRunning(GmsServiceType.ZOOKEEPER));
  }
}
