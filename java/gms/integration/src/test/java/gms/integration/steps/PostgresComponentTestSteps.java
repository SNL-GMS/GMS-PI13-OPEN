package gms.integration.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;


import gms.shared.frameworks.test.utils.services.GmsServiceType;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class PostgresComponentTestSteps {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostgresComponentTestSteps.class);
  private final String GMS_DB_TEST_USER = "gms_read_only";
  private final String GMS_DB_TEST_PASSWORD = "gmsdb:postgres:gms_read_only:humoured-tempered-furious-lion";
  private Connection conn;
  private ResultSet resultSet;
  
  private Environment environment;

  public PostgresComponentTestSteps(Environment environment) {
    this.environment = environment;
  }

  @Given("The postgres service is healthy")
  public void thePostgresServiceIsHealthy() {
    // verify the container is created and is running
    try {

      assertTrue(this.environment.deploymentCtxt().isServiceCreated(GmsServiceType.POSTGRES_SERVICE));
      assertTrue(this.environment.deploymentCtxt().isServiceRunning(GmsServiceType.POSTGRES_SERVICE));
      Properties props = new Properties();
      props.setProperty("user", GMS_DB_TEST_USER);
      props.setProperty("password", GMS_DB_TEST_PASSWORD);
      props.setProperty("ssl", "false");
      conn = DriverManager.getConnection(this.environment.deploymentCtxt().getJdbcUrl(), props);
    } catch (SQLException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  @When("I query the database schema objects")
  public void iQueryTheDatabaseSchemaObjects() {
    String listAllSchemasQuery = "SELECT schema_name FROM information_schema.schemata where schema_name like '%gms%'";
    try {
      Instant start = Instant.now();
      this.resultSet = conn.prepareStatement(listAllSchemasQuery).executeQuery();
      Instant end = Instant.now();
      long elapsed = Duration.between(start, end).toMillis();
      LOGGER.info("---- Execute SQL Query: ----");
      LOGGER.info("Elapsed time (ms): {}", elapsed);
    } catch (SQLException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  @Then("I should get the expected list of schemas")
  public void iShouldGetTheExpectedListOfSchemas() {
    var schemaList = List.of("gms_soh", "gms_config", "gms_session", "gms_soh_test");

    try {
      Instant start = Instant.now();
      while (resultSet.next()) {
        String resultCol1 = resultSet.getString(1);

        Assertions.assertTrue(schemaList.contains(resultCol1));

      }
      Instant end = Instant.now();
      long elapsed = Duration.between(start, end).toMillis();
      LOGGER.info("---- SQL Result Unpack: ----");
      LOGGER.info("Elapsed time (ms): {}", elapsed);
    } catch (SQLException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }
}
