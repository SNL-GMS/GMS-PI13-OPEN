package gms.shared.frameworks.osd.dao.util;

import gms.shared.frameworks.systemconfig.SystemConfig;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.metamodel.EntityType;
import java.util.Map;
import java.util.Objects;

/**
 * Utility for creating EntityManagerFactory's for JPA. These know about all of the core Data Access
 * Objects for GMS.
 */
public class CoiEntityManagerFactory {

  public static final String UNIT_NAME = "gms";
  public static final String HIBERNATE_CONNECTION_PROP_KEY = "hibernate.connection.url";
  public static final String HIBERNATE_CONNECTION_USERNAME_KEY = "hibernate.connection.username";
  public static final String HIBERNATE_CONNECTION_PASSWORD_KEY = "hibernate.connection.password";
  public static final String HIBERNATE_C3P0_POOL_SIZE_KEY = "hibernate.c3p0.max_size";
  public static final String HIBERNATE_FLUSH_MODE = "hibernate.flushMode";

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
        HIBERNATE_CONNECTION_PROP_KEY, config.getValue("sql_url"),
        HIBERNATE_CONNECTION_USERNAME_KEY, config.getValue("sql_user"),
        HIBERNATE_CONNECTION_PASSWORD_KEY, config.getValue("sql_password"),
        HIBERNATE_C3P0_POOL_SIZE_KEY, config.getValue("c3p0_connection_pool_size"),
        HIBERNATE_FLUSH_MODE, "FLUSH_AUTO"));
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

  public static void main(String[] args) {
    EntityManagerFactory emf = create();
    EntityManager entityManager = emf.createEntityManager();
    for (EntityType<?> entityType : entityManager.getMetamodel().getEntities()) {
      System.out.println(entityType.getName());
    }
  }
}
