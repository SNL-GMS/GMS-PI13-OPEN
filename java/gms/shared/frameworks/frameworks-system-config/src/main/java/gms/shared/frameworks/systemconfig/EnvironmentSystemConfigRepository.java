package gms.shared.frameworks.systemconfig;

import java.util.Optional;
import java.lang.System;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class providing retrieval of system configuration values from environment variables.
 */
public class EnvironmentSystemConfigRepository implements SystemConfigRepository {

  private static final Logger logger = LoggerFactory.getLogger(EnvironmentSystemConfigRepository.class);

  /** Instantiate a EnvironmentSystemConfigRepository */
  private EnvironmentSystemConfigRepository() {
    /* Nothing to do */
  }

  /**
   * Environment-specific implementation of get.
   *
   * Since config values may contain the '.' and '-' characters which
   * are unsupported as environment variables names, we will look for
   * an environment variable that matches a transformed version of the
   * key.
   *
   * Specifically, the environment variable name will
   * - Be prefixed with 'GMS_CONFIG_'
   * - Be converted to all capital letters.
   * - Dashes will be translated to single-underscores.
   * - Dots will be translated to double-underscores.
   *
   * For example, the key 'foo.bar-baz' will correspond to the
   * environment variable 'GMS_CONFIG_FOO__BAR_BAZ'.
   *
   * @param key key name to return the value for from this repository
   * @return value of key if present, empty Optional if not found
   */
  @Override
  public Optional<String> get(String key) {

    String environmentVariableName = "GMS_CONFIG_" + key.toUpperCase().replace("-", "_").replace(".", "__");
    return Optional.ofNullable(System.getenv(environmentVariableName));
  }

  /**
   * Get the name of this system configuration repository as a string.
   *
   * @return name of the form "environment"
   */
  @Override
  public String toString() {
    return "environment";
  }

  /** Construct a builder for an EnvironmentSystemConfigurationRepository. */
  public static Builder builder() {
    return new EnvironmentSystemConfigRepository.Builder();
  }

  /** Builder for an EnvironmentSystemConfigurationRepository. */
  public static class Builder {
    
    /**
     * Finish construction of a new EnvironmentSystemConfigRepository
     *
     * @return newly constructed EnvironmentSystemConfigRepository
     */
    public EnvironmentSystemConfigRepository build() {
      return new EnvironmentSystemConfigRepository();
    }
  }
}
