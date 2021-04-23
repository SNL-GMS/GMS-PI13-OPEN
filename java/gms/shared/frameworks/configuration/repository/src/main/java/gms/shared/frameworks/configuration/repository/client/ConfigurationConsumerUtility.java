package gms.shared.frameworks.configuration.repository.client;

import gms.shared.frameworks.configuration.Configuration;
import gms.shared.frameworks.configuration.ConfigurationRepository;
import gms.shared.frameworks.configuration.ConfigurationResolver;
import gms.shared.frameworks.configuration.Selector;
import gms.shared.frameworks.configuration.util.ObjectSerialization;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility used by client applications to obtain parameters from {@link Configuration}. Resolves
 * parameters to either a field map (map of String to Object) or an instance of a parameters class.
 * Uses a {@link ConfigurationRepository} implementation to retrieve the {@link Configuration}s to
 * resolve.
 */
public class ConfigurationConsumerUtility {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationConsumerUtility.class);

  private static final Duration DEFAULT_CACHE_EXPIRATION = Duration.ofDays(1);

  private final ConfigurationRepository configurationRepository;

  private final Map<String, ConfigurationSelectorCache> configurationCache;

  private final Duration selectorCacheExpiration;

  private ConfigurationConsumerUtility(ConfigurationRepository configurationRepository,
      Duration selectorCacheExpiration) {
    this.configurationRepository = configurationRepository;
    this.configurationCache = new HashMap<>();
    this.selectorCacheExpiration = selectorCacheExpiration;
  }

  /**
   * {@link ConfigurationConsumerUtility} builder
   */
  public static class Builder {

    private final ConfigurationRepository configurationRepository;
    private Duration selectorCacheExpiration;
    private List<String> configurationNamePrefixes = List.of();

    private Builder(ConfigurationRepository configurationRepository) {
      this.configurationRepository = configurationRepository;
      this.selectorCacheExpiration = DEFAULT_CACHE_EXPIRATION;
    }

    /**
     * Sets the configurationNamePrefixes of the {@link Configuration}s the {@link
     * ConfigurationConsumerUtility} will use.
     *
     * @param configurationNamePrefixes name prefixes (key prefixes) of the {@link Configuration}s
     * the ConfigurationConsumerUtility will use, not null
     * @return this {@link Builder}, not null
     * @throws NullPointerException if configurationNamePrefixes is null
     */
    public Builder configurationNamePrefixes(Collection<String> configurationNamePrefixes) {
      Objects
          .requireNonNull(configurationNamePrefixes, "Requires non-null configurationNamePrefixes");
      this.configurationNamePrefixes = new ArrayList<>(configurationNamePrefixes);
      return this;
    }

    public Builder selectorCacheExpiration(Duration selectorCacheExpiration) {
      Objects.requireNonNull(selectorCacheExpiration, "Requires non-null cache expiration Duration");
      this.selectorCacheExpiration = selectorCacheExpiration;
      return this;
    }

    /**
     * Obtains a {@link ConfigurationConsumerUtility} capable of resolving parameters from {@link
     * Configuration}s with the {@link Builder#configurationNamePrefixes}.
     *
     * @return {@link ConfigurationConsumerUtility}, not null
     * @throws IllegalStateException if the {@link ConfigurationRepository} does not have an entry
     * for any of the configurationNamePrefixes.
     */
    public ConfigurationConsumerUtility build() {

      // Add the key prefix used to load global configuration defaults to configurationNamePrefixes
      final List<String> configurationNamePrefixesWithGlobal = Stream.concat(
          configurationNamePrefixes.stream(),
          Stream.of(GlobalConfigurationReferenceResolver.REFERENCED_CONFIGURATION_KEY_PREFIX)
      ).collect(Collectors.toList());

      logger.info("Creating ConfigurationConsumerUtility for Configuration's named : {}",
          configurationNamePrefixesWithGlobal);

      // Initialize ConfigurationConsumerUtility with the Configuration associated with each provided key
      final Collection<Configuration> initialConfigurations = loadConfigurations(
          configurationRepository, configurationNamePrefixesWithGlobal);

      final ConfigurationConsumerUtility configurationConsumerUtility =
          new ConfigurationConsumerUtility(configurationRepository, selectorCacheExpiration);
      configurationConsumerUtility.addConfigurations(initialConfigurations);

      return configurationConsumerUtility;
    }
  }

  /**
   * Obtains a new {@link Builder} for a {@link ConfigurationConsumerUtility} that will use the
   * provided {@link ConfigurationRepository}
   *
   * @param configurationRepository {@link ConfigurationRepository}, not null
   * @return {@link Builder}, not null
   * @throws NullPointerException if configurationRepository is null
   */
  public static Builder builder(ConfigurationRepository configurationRepository) {
    Objects.requireNonNull(configurationRepository, "Requires non-null ConfigurationRepository");
    return new Builder(configurationRepository);
  }

  /**
   * Updates this ConfigurationConsumerUtility to be able to resolve parameters from {@link
   * Configuration}s with the provided configurationNamePrefixes.  Uses the {@link
   * ConfigurationRepository} provided during construction to load the {@link Configuration}s.
   *
   * @param configurationNamePrefixes name prefixes (key prefixes) of the new {@link Configuration}s
   * for the ConfigurationConsumerUtility to use, not null
   * @throws NullPointerException if configurationRepository or configurationNamePrefixes are null
   * @throws IllegalStateException if the {@link ConfigurationRepository} does not have an entry for
   * any of the configurationNamePrefixes..
   */
  public void loadConfigurations(List<String> configurationNamePrefixes) {
    Objects
        .requireNonNull(configurationNamePrefixes, "Requires non-null configurationNamePrefixes");

    logger.info("Loading into ConfigurationConsumerUtility Configuration's named : {}",
        configurationNamePrefixes);

    addConfigurations(loadConfigurations(this.configurationRepository,
        configurationNamePrefixes.stream()
            .filter(k -> !this.configurationCache.containsKey(k))
            .collect(Collectors.toList())
        )
    );
  }

  /**
   * Adds the provided {@link Configuration}s to {@link ConfigurationConsumerUtility#configurationCache}.
   * Uses each {@link Configuration#getName()} for the keys.  Does not overwrite existing mappings.
   *
   * Also loads the global default Configurations using key prefix {@link
   * GlobalConfigurationReferenceResolver#REFERENCED_CONFIGURATION_KEY_PREFIX}
   *
   * @param configurations Configurations to add, not null
   */
  private void addConfigurations(Collection<Configuration> configurations) {

    // Load each configuration that is not already tracked by this ConfigurationConsumerUtility.
    // Dereference configuration references.
    final Consumer<Collection<Configuration>> addConfigurationConsumer = c ->
        c.stream()
            .filter(configuration -> !configurationCache.containsKey(configuration.getName()))
            .map(config -> GlobalConfigurationReferenceResolver
                .resolve(this.configurationCache, config))
            .forEach(config -> configurationCache.put(config.getName(),
                ConfigurationSelectorGuavaCache.create(config, selectorCacheExpiration)));

    // Split configuration reference and non-reference configurations
    final Predicate<String> isGlobalReferenceConfiguration = s -> s
        .startsWith(GlobalConfigurationReferenceResolver.REFERENCED_CONFIGURATION_KEY_PREFIX);

    final Collection<Configuration> globalReferenceConfigurations = configurations.stream()
        .filter(c -> isGlobalReferenceConfiguration.test(c.getName())).collect(
            Collectors.toList());

    final Collection<Configuration> nonReferenceConfigurations =
        configurations.stream().filter(c -> !globalReferenceConfigurations.contains(c)).collect(
            Collectors.toList());

    // First load the reference configurations, then load the remaining configurations
    addConfigurationConsumer.accept(globalReferenceConfigurations);
    addConfigurationConsumer.accept(nonReferenceConfigurations);
  }

  /**
   * Loads {@link Configuration}s with the provided name prefixes from the provided {@link
   * ConfigurationRepository}.
   *
   * @param configurationRepository {@link ConfigurationRepository} implementation providing the
   * Configurations, not null
   * @param configurationNamePrefixes name prefixes (key prefixes) of the {@link Configuration}s to
   * load, not null
   * @return Collection of {@link Configuration}, not null
   */
  private static Collection<Configuration> loadConfigurations(
      ConfigurationRepository configurationRepository,
      Collection<String> configurationNamePrefixes) {

    // Loads a Configuration while keeping track of keys not found in the ConfigurationRepository
    final List<String> missingKeys = new ArrayList<>();
    final Function<String, List<Configuration>> loadConfigFindMissingKeys = prefix -> {
      final List<Configuration> configurations = loadConfiguration(configurationRepository, prefix);

      if (configurations.isEmpty() && !prefix
          .startsWith(GlobalConfigurationReferenceResolver.REFERENCED_CONFIGURATION_KEY_PREFIX)) {

        missingKeys.add(prefix);
      }
      return configurations;
    };

    // Load all of the configurations
    try {
      final List<Configuration> configurations = configurationNamePrefixes.stream()
          .distinct()
          .map(loadConfigFindMissingKeys)
          .flatMap(List::stream)
          .collect(Collectors.toList());

      // Throw an exception if any of the requested keys are missing
      if (!missingKeys.isEmpty()) {
        final String message =
            "No Configuration(s) found for key prefix(es) " + Arrays.toString(missingKeys.toArray());

        logger.error(message);
      }
      return configurations;
    } catch (Exception ex) {
      logger.error("Error loading configurations", ex);
      throw ex;
    }
  }

  /**
   * Loads a list of {@link Configuration} with the provided name prefix from the {@link
   * ConfigurationRepository}.
   *
   * @param configurationRepository {@link ConfigurationRepository} implementation providing the
   * Configuration, not null
   * @param configurationNamePrefix names prefix (key prefix) of the {@link Configuration}s to load,
   * not null
   * @return list of {@link Configuration}, not null
   */
  private static List<Configuration> loadConfiguration(
      ConfigurationRepository configurationRepository,
      String configurationNamePrefix) {

    // TODO: setup watch for updates to configurationNamePrefix
    return new ArrayList<>(configurationRepository.getKeyRange(configurationNamePrefix));
  }

  /**
   * Uses the provided {@link Selector}s to resolve parameters from the {@link Configuration} with
   * the provided name.  Returns the resolved parameters as an instance of the provided
   * parametersClass
   *
   * @param configurationName name of the Configuration to resolve
   * @param selectors {@link Selector}s describing how to resolve the Configuration
   * @param parametersClass class type of the resolved parameters, not null
   * @param <T> type of the parametersClass
   * @return Instance of T (the parametersClass) containing the resolved parameters, not null
   * @throws NullPointerException if configurationName, selectors, or parametersClass are null
   * @throws IllegalArgumentException if this ConfigurationConsumerUtility does not have a
   * Configuration with the provided name
   * @throws IllegalArgumentException if the resolved parameters cannot be used to construct an
   * instance of T (the parametersClass)
   * @see ConfigurationResolver#resolve(Configuration, List) for details of the resolution
   * algorithm.
   */
  public <T> T resolve(String configurationName, List<Selector> selectors,
      Class<T> parametersClass) {

    Objects.requireNonNull(parametersClass, "Cannot resolve Configuration to null parametersClass");

    // Resolve and construct parametersClass instance.
    // resolve() call is not inlined in SerializationUtility.fromFieldMap call since both calls
    // produce IllegalArgumentException but only the SerializationUtility's exception is caught and
    // rethrown.
    final Map<String, Object> resolvedFieldMap = resolve(configurationName, selectors);
    try {
      return ObjectSerialization.fromFieldMap(resolvedFieldMap, parametersClass);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Resolved Configuration is not a valid instance of " + parametersClass.getCanonicalName(),
          e);
    }
  }

  /**
   * Uses the provided {@link Selector}s to resolve parameters from the {@link Configuration} with
   * the provided name.  Returns the resolved parameters in a field map.
   *
   * @param configurationName name of the Configuration to resolve
   * @param selectors {@link Selector}s describing how to resolve the Configuration
   * @return field map containing the resolved parameters, not null
   * @throws NullPointerException if configurationName or selectors are null
   * @throws IllegalArgumentException if this ConfigurationConsumerUtility does not have a
   * Configuration with the provided name
   * @see ConfigurationResolver#resolve(Configuration, List) for details of the resolution
   * algorithm.
   */
  public Map<String, Object> resolve(String configurationName, List<Selector> selectors) {
    Objects.requireNonNull(configurationName,
        "Cannot resolve Configuration for null configurationName");
    Objects.requireNonNull(selectors, "Cannot resolve Configuration for null selectors");

    ConfigurationSelectorCache configuration = configurationCache.get(configurationName);
    if(configuration == null){
      loadConfigurations(Collections.singletonList(configurationName));
    }

    return Optional.ofNullable(configurationCache.get(configurationName))
        .orElseThrow(() -> new IllegalArgumentException(
            "No Configuration named " + configurationName
                + " is in this ConfigurationConsumerUtility"))
        .resolveFieldMap(selectors);
  }

}
