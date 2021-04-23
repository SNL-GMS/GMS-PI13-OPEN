package gms.integration.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.integration.util.ServiceUtility;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.configuration.Configuration;
import gms.shared.frameworks.configuration.ConfigurationOption;
import gms.shared.frameworks.configuration.Constraint;
import gms.shared.frameworks.configuration.Operator;
import gms.shared.frameworks.configuration.Operator.Type;
import gms.shared.frameworks.configuration.constraints.BooleanConstraint;
import gms.shared.frameworks.configuration.constraints.DefaultConstraint;
import gms.shared.frameworks.configuration.constraints.DoubleRange;
import gms.shared.frameworks.configuration.constraints.NumericRangeConstraint;
import gms.shared.frameworks.configuration.constraints.NumericScalarConstraint;
import gms.shared.frameworks.configuration.constraints.PhaseConstraint;
import gms.shared.frameworks.configuration.constraints.StringConstraint;
import gms.shared.frameworks.configuration.constraints.TimeOfDayRange;
import gms.shared.frameworks.configuration.constraints.TimeOfDayRangeConstraint;
import gms.shared.frameworks.configuration.constraints.TimeOfYearRange;
import gms.shared.frameworks.configuration.constraints.TimeOfYearRangeConstraint;
import gms.shared.frameworks.configuration.constraints.WildcardConstraint;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.Assert;
import org.testcontainers.junit.jupiter.Testcontainers;

//Given The Framework Processing Configuration Service is alive
//        When Processing Configuration has been loaded
//        And I request a configuration with the Configuration Name as the request body
//        Then I should retrieve the correct configuration

@Testcontainers
public class ProcessingConfigurationServiceComponentTestSteps {

  private Optional<String> returnedConfiguration;
  private Environment environment;

  public ProcessingConfigurationServiceComponentTestSteps(
      Environment environment) {
    this.environment = environment;
  }

  @Given("The Framework Processing Configuration Service is alive")
  public void initPCService() {

    final String rootUrl = String.format("%s%s:%d", ServiceUtility.URL_PREFIX,
        this.environment.deploymentCtxt().getServiceHost(GmsServiceType.PROCESSING_CONFIG_SERVICE),
        this.environment.deploymentCtxt().getServicePort(GmsServiceType.PROCESSING_CONFIG_SERVICE));
    final RetryPolicy<Boolean> retryPolicy = new RetryPolicy<Boolean>()
        .withBackoff(5, 1000, ChronoUnit.MILLIS)
        .withMaxAttempts(1)
        .handle(List.of(Exception.class))
        .onFailedAttempt(e -> System.out.println("Failed service request, will try again..."));

    Boolean alive = Failsafe.with(retryPolicy).get(() -> {
      boolean isAlive = StepUtils.isServiceAlive(rootUrl);
      return isAlive;
    });

    assertTrue(alive,
        String.format("FAILURE - The %s application does not appear to be running",
            GmsServiceType.PROCESSING_CONFIG_SERVICE.toString()));
  }

  @When("Processing Configuration has been loaded")
  public void loadProcessingConfiguration() {

    Configuration configuration = createConfiguration();

    final String rootUrl = String.format("%s%s:%d", ServiceUtility.URL_PREFIX,
        this.environment.deploymentCtxt().getServiceHost(GmsServiceType.PROCESSING_CONFIG_SERVICE),
        this.environment.deploymentCtxt().getServicePort(GmsServiceType.PROCESSING_CONFIG_SERVICE));
    Optional<String> retVal = null;
    try {
      retVal = StepUtils.postDataToEndpoint(rootUrl + "/processing-cfg/put", configuration);
    } catch (IOException e) {
      Assert.fail("/processing-cfg/put should not throw an exception: " + e.getMessage());
    }
    assertTrue(retVal.isPresent(),
        String.format(
            "FAILURE - The %s application could not unmarshall/save a configuration to the DB",
            GmsServiceType.PROCESSING_CONFIG_SERVICE.toString()));

  }

  @And("I request a configuration with the Configuration Name as the request body")
  public void requestConfiguration() {
    final String rootUrl = String.format("%s%s:%d", ServiceUtility.URL_PREFIX,
        this.environment.deploymentCtxt().getServiceHost(GmsServiceType.PROCESSING_CONFIG_SERVICE),
        this.environment.deploymentCtxt().getServicePort(GmsServiceType.PROCESSING_CONFIG_SERVICE));
    String configurationName = "test-config";
    try {
      returnedConfiguration = StepUtils
          .postDataToEndpoint(rootUrl + "/processing-cfg/get", configurationName);
    } catch (IOException e) {
      Assert.fail("/processing-cfg/get should not throw an exception: " + e.getMessage());
    }

    assertTrue(!returnedConfiguration.get().isBlank(),
        "/processing-cfg/get  with requestBody: " + configurationName
            + " should return a configuration");
  }

  @Then("I should retrieve the correct configuration")
  public void IShouldBeTold() throws IOException {
    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

    Configuration configuration = objectMapper
        .readValue(returnedConfiguration.get(), Configuration.class);
    assertTrue(configuration.getName().equalsIgnoreCase("test-config"),
        String.format(
            "FAILURE - The %s application could not read/return a configuration from the DB",
            GmsServiceType.PROCESSING_CONFIG_SERVICE.toString()));
  }

  private Configuration createConfiguration() {
    Collection<ConfigurationOption> configurationOptions = new ArrayList<>();
    List<Constraint> constraints = new ArrayList<>();
    Operator operatorIn = Operator.from(Type.IN, false);
    Operator operatorEq = Operator.from(Type.EQ, false);

    constraints.add(BooleanConstraint.from("test1", true, 1));
    constraints.add(DefaultConstraint.from());
    constraints.add(NumericRangeConstraint.from("test2", operatorIn, DoubleRange.from(0, 1), 1));
    constraints.add(NumericScalarConstraint.from("test3", operatorEq, 0, 1));
    constraints.add(PhaseConstraint.from("test4", operatorEq, new LinkedHashSet<>(), 1));
    constraints.add(StringConstraint.from("test5", operatorEq, new LinkedHashSet<>(), 1));
    constraints.add(TimeOfDayRangeConstraint
        .from("test6", operatorIn, TimeOfDayRange.from(LocalTime.now(), LocalTime.now()), 1));
    constraints.add(
        TimeOfYearRangeConstraint.from("test7", operatorIn,
            TimeOfYearRange.from(LocalDateTime.now(), LocalDateTime.now()), 1));
    constraints.add(WildcardConstraint.from("test8"));

    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("some-string", "some-value");
    ConfigurationOption co = ConfigurationOption
        .from("config-option-test", constraints, parameters);
    configurationOptions.add(co);

    return Configuration.from(
        "test-config", configurationOptions
    );
  }
}



