package gms.integration.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.integration.util.ServiceUtility;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.osd.coi.channel.Channel;

import gms.shared.frameworks.osd.dao.util.CoiEntityManagerFactory;
import gms.shared.frameworks.soh.repository.SohRepository;
import gms.shared.frameworks.soh.repository.capabilityrollup.CapabilitySohRollupRepositoryJpa;
import gms.shared.frameworks.soh.repository.channel.ChannelRepositoryJpa;
import gms.shared.frameworks.soh.repository.performancemonitoring.PerformanceMonitoringRepositoryJpa;
import gms.shared.frameworks.soh.repository.performancemonitoring.StationSohRepositoryJpa;
import gms.shared.frameworks.soh.repository.performancemonitoring.StationSohRepositoryQueryViewJpa;
import gms.shared.frameworks.soh.repository.preferences.UserPreferencesRepositoryJpa;
import gms.shared.frameworks.soh.repository.sohstatuschange.SohStatusChangeRepositoryJpa;
import gms.shared.frameworks.soh.repository.station.StationGroupRepositoryJpa;
import gms.shared.frameworks.soh.repository.station.StationRepositoryJpa;
import gms.shared.frameworks.soh.repository.stationreference.*;
import gms.shared.frameworks.soh.repository.systemmessage.SystemMessageRepositoryJpa;
import gms.shared.frameworks.soh.repository.transferredfile.RawStationDataFrameRepositoryJpa;
import gms.shared.frameworks.soh.repository.transferredfile.RawStationDataFrameRepositoryQueryViewJpa;
import gms.shared.frameworks.soh.repository.transferredfile.TransferredFileRepositoryJpa;
import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.cucumber.java.en.When;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class FrameworkOsdServiceComponentTestSteps {
  private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkOsdServiceComponentTestSteps.class);

  private List<Channel> channels;

  private final Environment environment;

  public FrameworkOsdServiceComponentTestSteps(Environment environment) {
    this.environment = environment;
  }

  @Given("The Framework OSD Service container is created")
  public void theFrameworkOsdServiceContainerIsCreated() {
    final String rootUrl = String.format("%s%s:%d", ServiceUtility.URL_PREFIX,
            environment.deploymentCtxt().getServiceHost(GmsServiceType.OSD_SERVICE),
            environment.deploymentCtxt().getServicePort(GmsServiceType.OSD_SERVICE));
    final RetryPolicy<Boolean> retryPolicy = new RetryPolicy<Boolean>()
            .withBackoff(50, 1000, ChronoUnit.MILLIS)
            .withMaxAttempts(10)
            .handle(List.of(Exception.class))
            .onFailedAttempt(e -> System.out.println("Failed service request, will try again..."));

    Boolean alive = Failsafe.with(retryPolicy).get(() -> StepUtils.isServiceAlive(rootUrl));

    assertTrue(alive,
        String.format("FAILURE - The %s application does not appear to be running",
            ServiceUtility.FRAMEWORKS_OSD_SERVICE));
  }
  @When("I connect to the database via the OSD interface")
  public void testConnectToPostgresViaOSDInterface() {
    HashMap<String, String> testProperties = new HashMap<>(Map.of(
       "hibernate.connection.provider_class",  "org.hibernate.connection.C3P0ConnectionProvider",
            "hibernate.connection.driver_class", "org.postgresql.Driver",
        "hibernate.connection.url", this.environment.deploymentCtxt().getJdbcUrl(),
        "hibernate.connection.username", "gms_soh_application",
        "hibernate.connection.password", "gmsdb:postgres:gms_soh_application:smidgeons-offerers-reducers",
        "hibernate.default_schema", "gms_soh",
        "hibernate.dialect", "org.hibernate.dialect.PostgreSQL10Dialect",
        "hibernate.hbm2ddl.auto", "validate",
            "hibernate.flushMode", "FLUSH_AUTO"));
    testProperties.putAll(Map.of(
            "hibernate.c3p0.min_size", "5",
            "hibernate.c3p0.max_size", "20",
            "hibernate.c3p0.acquire_increment", "5",
            "hibernate.c3p0.timeout", "1800",
            "hibernate.c3p0.unreturnedConnectionTimeout", "1800",
            "hibernate.c3p0.max_statements", "50",
            "hibernate.c3p0.debugUnreturnedConnectionStackTraces", "true"
    ));
    var emf = CoiEntityManagerFactory.create(testProperties);
    environment.setSohRepositoryInterface(SohRepository.from(
        new CapabilitySohRollupRepositoryJpa(emf),
        new ChannelRepositoryJpa(emf),
        new PerformanceMonitoringRepositoryJpa(emf),
        new RawStationDataFrameRepositoryJpa(emf),
        new RawStationDataFrameRepositoryQueryViewJpa(emf),
        new ReferenceChannelRepositoryJpa(emf),
        new ReferenceNetworkRepositoryJpa(emf),
        new ReferenceResponseRepositoryJpa(emf),
        new ReferenceSensorRepositoryJpa(emf),
        new ReferenceSiteRepositoryJpa(emf),
        new ReferenceStationRepositoryJpa(emf),
        new SohStatusChangeRepositoryJpa(emf),
        new StationGroupRepositoryJpa(emf),
        new StationRepositoryJpa(emf),
        new StationSohRepositoryJpa(emf),
        new StationSohRepositoryQueryViewJpa(emf, new StationRepositoryJpa(emf)),
        new SystemMessageRepositoryJpa(emf),
        new TransferredFileRepositoryJpa(emf),
        new UserPreferencesRepositoryJpa(emf)));
  }

  @And("I attempt to get all Channels")
  public void testOsdChannelQuery() {
    long start = System.nanoTime();
    this.channels = environment.getSohRepositoryInterface().retrieveChannels(List.of());
    long end = System.nanoTime();
    long elapsed = end - start;
    LOGGER.info("---- SQL Retrieve All Channels: ----");
    LOGGER.info("Elapsed time (ns): {}", elapsed);
    LOGGER.info("Elapsed time (ms): {}", elapsed/1000000);
  }

  @Then("I get a non-empty list of channels")
  public void verifyNonEmptyChannelRetrieval() {
    // TODO: Need to make this better
    Assertions.assertTrue(this.channels.size() > 0);
  }

  @Then("The Frameworks OSD Service works")
  public void itWorks() {
    // TODO: Need to know how to tie
    assertTrue(true);
  }

}
