package gms.integration.steps.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.io.Files;
import gms.integration.steps.Environment;
import gms.integration.util.ConfigFileReaderUtility;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonTestSteps {

  private static final Logger logger = LoggerFactory.getLogger(CommonTestSteps.class);

  private final String BASEPATH = "gms/integration/config";
  private final int MAXDELAY = 20000;

  private static Environment environment;
  private boolean restartBetweenScenarios = true;

  public CommonTestSteps(Environment environment) {
    if (CommonTestSteps.environment == null) {
      CommonTestSteps.environment = environment;
    }
    restartBetweenScenarios = true;
  }

  @Given("the environment should restart between scenarios: {string}")
  public void theEnvironmentShouldBeRestartedBetweenScenarios(String restartBetweenScenarios) {
    this.restartBetweenScenarios = BooleanUtils.toBoolean(restartBetweenScenarios);
  }

  @After
  public void tearDownScenario(){
    logger.info("!!!!TEST CONTAINERS - CHECKING IF RESTART REQUESTED!!!!");
    if (restartBetweenScenarios) {
      logger.info("!!!!TEST CONTAINERS - RESTART IS REQUESTED!!!!");
      this.tearDownEnvironment();
    } else {
      logger.info("!!!!TEST CONTAINERS - RESTART WAS NOT REQUESTED!!!!");
    }
    restartBetweenScenarios = true;
  }

  @Given("^The environment is started with$")
  public void startEnvironmentWithServices(List<String> serviceTypes) {
    if (!environment.isStarted()) {
      environment.startEnvironmentWithServices(serviceTypes);

      if (!restartBetweenScenarios) {
        logger.info("!!!!TEST CONTAINERS - REGISTERING SHUTDOWN!!!!");
        Runtime.getRuntime().addShutdownHook(new Thread(this::tearDownEnvironment));
      }
    }
  }

  public void tearDownEnvironment() {
    environment.tearDownEnvironment();
  }

  @Then("the environment is restarted")
  public void theEnvironmentIsRestarted() {
    environment.restart();
  }

  @Given("The {string} service is healthy")
  public void theGmsServiceIsHealthy(String gmsServiceType) {
    final var deploymentCtxt = environment.deploymentCtxt();
    assertTrue(deploymentCtxt.isServiceCreated(GmsServiceType.valueOf(gmsServiceType)));
    assertTrue(deploymentCtxt.isServiceRunning(GmsServiceType.valueOf(gmsServiceType)));
    assertTrue(deploymentCtxt.isServiceHealthy(GmsServiceType.valueOf(gmsServiceType)));
  }

  @Given("Configuration for {string} component test is loaded")
  public void testConfigurationLoaded(String gmsService) {
    GmsServiceType gmsServiceType = GmsServiceType.valueOf(gmsService);
    Map<String, String> configFilenames = new HashMap<>();

    switch (gmsServiceType) {
      case CONNMAN:
        configFilenames.put("connman.station-parameters", "connmantest.json");
        break;
      case DATAMAN:
        configFilenames.put("dataman.station-parameters", "datamantest.json");
        configFilenames.put("dataframe-receiver.channel-lookup", "cd11-datamantest.json");
        break;
      case RSDF_STREAMS_PROCESSOR:
        configFilenames.put("dataframe-receiver.channel-lookup", "cd11-rsdf-proctest.json");
        break;
    }

    URL url = StepUtils.class.getClassLoader().getResource(BASEPATH);
    logger.info("Test config url = " + url);
    logger.info("Config filenames:");
    configFilenames.keySet().forEach(key -> logger.info(key + " : " + configFilenames.get(key)));

    Path basePath = null;
    try {
      basePath = Paths.get(url.toURI());
    } catch (URISyntaxException e) {
      e.printStackTrace();
      Assert.fail("finding base path of processing configs should not throw an exception: " + e
          .getMessage());
    }

    ConfigFileReaderUtility connmanConfig = new ConfigFileReaderUtility(basePath, configFilenames);

    boolean configsLoaded = false;
    try {
      configsLoaded = connmanConfig.postConfigurationsforTest(environment.deploymentCtxt());
    } catch (IOException e) {
      Assert.fail("/processing-cfg/put should not throw an exception: " + e.getMessage());
    }

    assertTrue(configsLoaded,
        String.format(
            "FAILURE - The %s application could not unmarshall/save a configuration to the DB",
            GmsServiceType.PROCESSING_CONFIG_SERVICE.toString()));

  }

  @Given("The {string} service has been restarted")
  public void theGmsServiceHasBeenRestarted(String gmsServiceType) {
    assertTrue(environment.deploymentCtxt().restartServicewithRetryBackoff(
        GmsServiceType.valueOf(gmsServiceType),MAXDELAY),
        String.format("%s should have been successfully restarted",
            GmsServiceType.valueOf(gmsServiceType)));
  }

  @Given("appropriate json files are mounted and readable from the {string} directory")
  public void appropriateRsdfJsonFilesAreMountedFromDirectory(String dirPath) throws Exception {
    // Final files from local directory
    URL url = StepUtils.class.getClassLoader().getResource(dirPath);
    File monitoredDir = Paths.get(url.toURI()).toFile();
    File[] allFiles = monitoredDir.listFiles();

    // Only read in json files
    String jsonType = "json";
    List<File> jsonFiles = Arrays.stream(allFiles)
        .filter(f -> Files.getFileExtension(f.getName()).equals(jsonType))
        .collect(Collectors.toList());

    assertNotNull(jsonFiles);
    assertTrue(jsonFiles.size() > 0);

    jsonFiles.stream().forEach(f -> {
      logger.info(">>> File: {}", f.getName());
    });
    logger.info(">>> Read {} instances of json files from input resource {}",
        jsonFiles.size(), dirPath);
  }

  @Then("The SoH system is deployed")
  public void theSoHSystemIsDeployed() {
    // Write code here that turns the phrase above into concrete actions
    // Nothing to test here - just return true
    //TODO: If we add more logic to check basic functionality add it here or replace with another call
    //       Used as first step of our system test development.
    assertEquals(1,1);
  }

}

