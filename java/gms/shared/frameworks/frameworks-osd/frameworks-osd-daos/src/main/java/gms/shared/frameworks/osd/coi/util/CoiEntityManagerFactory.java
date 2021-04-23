package gms.shared.frameworks.osd.coi.util;

import gms.shared.frameworks.systemconfig.SystemConfig;
import java.util.Map;
import java.util.Objects;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

/**
 * Utility for creating EntityManagerFactory's for JPA. These know about all of the core Data Access
 * Objects for GMS.
 */
public class CoiEntityManagerFactory {

  public static final String UNIT_NAME = "gms";

  /**
   * Creates an EntityManagerFactory with defaults.
   *
   * @return EntityManagerFactory
   */
  public static EntityManagerFactory create() {
    return create(Map.of());
  }

  /**
   * Creates an EntityManagerFactory using system configuration.
   *
   * @return EntityManagerFactory
   */
  public static EntityManagerFactory create(SystemConfig config) {
    return create(Map.of(
        "hibernate.connection.url", config.getValue("sql_url"),
        "hibernate.connection.username", config.getValue("sql_user"),
        "hibernate.connection.password", config.getValue("sql_password"),
        "hibernate.c3p0.max_size", config.getValue("c3p0_connection_pool_size")));
  }

  /**
   * Creates an EntityManagerFactory with the specified property overrides. These are given directly
   * to the JPA provider; they can be used to override things like the URL of the database.
   *
   * @param propertiesOverrides a map of properties to override and their values
   * @return EntityManagerFactory
   */
  public static EntityManagerFactory create(Map<String, String> propertiesOverrides) {
    Objects.requireNonNull(propertiesOverrides, "Property overrides cannot be null");
    try {
      return Persistence.createEntityManagerFactory(UNIT_NAME, propertiesOverrides);
    } catch (PersistenceException e) {
      throw new IllegalArgumentException("Could not create persistence unit " + UNIT_NAME, e);
    }
  }
}
