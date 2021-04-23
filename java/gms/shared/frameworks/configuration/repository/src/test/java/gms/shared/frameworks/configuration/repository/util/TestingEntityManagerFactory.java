package gms.shared.frameworks.configuration.repository.util;

import java.util.Map;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

/**
 * Used to create EntityManagerFactory's for use in tests.
 */
public class TestingEntityManagerFactory {

  public static final String UNIT_NAME = "processing-cfg";

  private static final Map<String, String> testProperties = Map.of(
      "hibernate.connection.driver_class", "org.postgresql.Driver",
      "hibernate.connection.url", "jdbc:postgresql://localhost:5432/gms",
      "hibernate.connection.username", "gms_config_application",
      "hibernate.connection.password", "gmsdb:postgres:gms_config_application:good-steel-referees",
      "hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect",
      "hibernate.hbm2ddl.auto", "validate",
      "hibernate.flushMode", "FLUSH_AUTO",
      "hibernate.c3p0.max_size", "10");

  /**
   * Creates an EntityManagerFactory for testing; connects to an in-memory database.
   *
   * @return EntityManagerFactory
   */
  public static javax.persistence.EntityManagerFactory createTesting() {
    try {
      return Persistence
          .createEntityManagerFactory(TestingEntityManagerFactory.UNIT_NAME, TestingEntityManagerFactory.testProperties);
    } catch (PersistenceException e) {
      throw new IllegalArgumentException("Could not create persistence unit " + TestingEntityManagerFactory.UNIT_NAME, e);
    }
  }
}
