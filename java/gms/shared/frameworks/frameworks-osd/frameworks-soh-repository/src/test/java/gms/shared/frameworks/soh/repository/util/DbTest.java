package gms.shared.frameworks.soh.repository.util;

import gms.shared.frameworks.osd.coi.station.StationTestFixtures;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.dao.util.CoiEntityManagerFactory;
import gms.shared.frameworks.soh.repository.station.StationGroupRepositoryJpa;
import gms.shared.frameworks.soh.repository.station.StationRepositoryJpa;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Tag("component")
public class DbTest {

  protected static final int POSTGRES_PORT = 5432;
  protected static final String ciDockerRegistryEnvVarName = "CI_DOCKER_REGISTRY";
  protected static final String dockerImageTagEnvVarName = "DOCKER_IMAGE_TAG";
  protected static final String GMS_DB_USER = "gms_test";
  protected static final String GMS_DB_PASSWORD = "test";
  protected static final String POSTGRES_DB = "gms";
  protected static Map<String, String> props;
  protected static EntityManagerFactory entityManagerFactory;

  @Container
  protected static PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
      String.format("%s/gms-common/postgres:%s",
          System.getenv(ciDockerRegistryEnvVarName),
          System.getenv(dockerImageTagEnvVarName)))
      .withDatabaseName(POSTGRES_DB)
      .withUsername(GMS_DB_USER)
      .withPassword(GMS_DB_PASSWORD);



  @BeforeAll
  protected static void setUp() {

    container
        .withEnv(Map.of(
            "POSTGRES_INITDB_ARGS",
            "--data-checksums -A --auth=scram-sha-256 --auth-host=scram-sha-256 --auth-local=scram-sha-256",
            "POSTGRES_HOST_AUTH_METHOD", "scram-sha-256"
        ))
        .withImagePullPolicy(PullPolicy.defaultPolicy())
        .addExposedPort(POSTGRES_PORT);
    container.start();
    final var jdbcUrl = container.getJdbcUrl() + "&reWriteBatchedInserts=true";
    props = Map.ofEntries(
        Map.entry("hibernate.connection.driver_class", "org.postgresql.Driver"),
        Map.entry("hibernate.connection.url", jdbcUrl),
        Map.entry("hibernate.connection.username", GMS_DB_USER),
        Map.entry("hibernate.connection.password", GMS_DB_PASSWORD),
        Map.entry("hibernate.default_schema", "gms_soh"),
        Map.entry("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect"),
        Map.entry("hibernate.hbm2ddl.auto", "validate"),
        Map.entry("hibernate.flushMode", "FLUSH_AUTO"),
        Map.entry("hibernate.jdbc.batch_size", "50"),
        Map.entry("hibernate.order_inserts", "true"),
        Map.entry("hibernate.order_updates", "true"),
        Map.entry("hibernate.jdbc.batch_versioned_data", "true")
        //Map.entry("hibernate.generate_statistics", "true"),
        //Map.entry("hibernate.show_sql", "true")
    );
    entityManagerFactory = CoiEntityManagerFactory.create(props);
    new StationRepositoryJpa(entityManagerFactory)
        .storeStations(List.of(UtilsTestFixtures.STATION, TestFixtures.station));
    new StationGroupRepositoryJpa(entityManagerFactory)
        .storeStationGroups(List.of(UtilsTestFixtures.STATION_GROUP,
            StationTestFixtures.getStationGroup()));
  }

  @AfterAll
  static void tearDown() {
    entityManagerFactory.close();
  }
}
