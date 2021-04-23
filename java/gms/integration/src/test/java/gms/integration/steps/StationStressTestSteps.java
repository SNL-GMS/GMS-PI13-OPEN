package gms.integration.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class StationStressTestSteps {

  private Environment environment;

  public StationStressTestSteps(Environment environment) {
    this.environment = environment;
  }

  @When("I run {int} stations for {int} minutes")
  public void iRunStationsForMinutes(Integer int1, Integer int2) {
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.java.PendingException();
  }

  @Then("I can verify that there is less than {int} lag in the soh.rsdf topic")
  public void iCanVerifyThatThereIsLessThanLagInTheRsdfTopic(Integer int1) {
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.java.PendingException();
  }

  @Then("I can verify that there is less than {int} lag in the soh.extract topic")
  public void iCanVerifyThatThereIsLessThanLagInTheSohExtractTopic(Integer int1) {
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.java.PendingException();
  }

  @Then("I can verify that there is less than {int} lag in the soh.station-soh topic")
  public void iCanVerifyThatThereIsLessThanLagInTheStationSohTopic(Integer int1) {
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.java.PendingException();
  }

  @Then("None of the services crashed during the transmission")
  public void noneOfTheServicesCrashedDuringTheTransmission() {
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.java.PendingException();
  }

}
