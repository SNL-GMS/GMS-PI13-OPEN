package gms.shared.frameworks.configuration.repository.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import gms.shared.frameworks.configuration.Configuration;
import gms.shared.frameworks.configuration.ConfigurationOption;
import gms.shared.frameworks.configuration.ConfigurationRepository;
import gms.shared.frameworks.configuration.Operator;
import gms.shared.frameworks.configuration.Operator.Type;
import gms.shared.frameworks.configuration.Selector;
import gms.shared.frameworks.configuration.constraints.NumericScalarConstraint;
import gms.shared.frameworks.configuration.constraints.WildcardConstraint;
import gms.shared.frameworks.configuration.repository.FooParameters;
import gms.shared.frameworks.configuration.repository.TestUtilities;
import gms.shared.frameworks.configuration.util.ObjectSerialization;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.exceptions.misusing.InvalidUseOfMatchersException;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigurationConsumerUtilityTests {

  private static final String configurationKey = "[component-name]-configuration";

  private static final FooParameters fooParamsDefaults = FooParameters.from(100, "string100", true);
  private static final Map<String, Object> fooParamsDefaultsMap = ObjectSerialization
      .toFieldMap(fooParamsDefaults);

  private static final NumericScalarConstraint snrIs5 = NumericScalarConstraint
      .from("snr", Operator.from(Type.EQ, false), 5.0, 100);

  private static final ConfigurationOption configOptDefault = ConfigurationOption
      .from("SNR-5", List.of(WildcardConstraint.from("snr")), fooParamsDefaultsMap);

  private static final ConfigurationOption configOptSnrIs5 = ConfigurationOption
      .from("SNR-5", List.of(snrIs5), Map.of("a", 10));

  private Configuration configurationSnrIs5 = Configuration
      .from(configurationKey, List.of(configOptDefault, configOptSnrIs5));

  private static final Duration defaultSelectorCacheExpiration = Duration.ofDays(1);

  @Mock
  private ConfigurationRepository configurationRepository;

  /**
   * Mocks configurationRepository to return an List of Configurations containing
   * configurationSnrIs5 when queried with configurationKey.  Does not return any configurations for
   * the global configuration key prefix.
   */
  private void mockGmsConfigurationToReturnPresentConfigurationItemNoGlobalDefaults() {
    Mockito.when(configurationRepository.getKeyRange(Mockito.anyString()))
        .thenAnswer(invocation -> {
          Object argument = invocation.getArguments()[0];
          if (argument.equals(configurationKey) || argument.equals("unknown-key")) {
            return List.of(configurationSnrIs5);
          } else if (argument
              .equals(GlobalConfigurationReferenceResolver.REFERENCED_CONFIGURATION_KEY_PREFIX)) {
            return List.of();
          }
          throw new InvalidUseOfMatchersException(
              String.format("Argument %s does not match", argument)
          );
        });
  }

  private ConfigurationConsumerUtility getClientUtilMockGmsConfig() {
    mockGmsConfigurationToReturnPresentConfigurationItemNoGlobalDefaults();
    return ConfigurationConsumerUtility.builder(configurationRepository)
        .configurationNamePrefixes(List.of(configurationKey)).build();
  }

  private ConfigurationConsumerUtility getClientUtilOverrideExpiration(Duration expiration) {
    return ConfigurationConsumerUtility.builder(configurationRepository)
        .selectorCacheExpiration(expiration)
        .configurationNamePrefixes(List.of(configurationKey)).build();
  }

  /**
   * Mocks configurationRepository to return an empty List when queried with configurationKey
   */
  private void mockGmsConfigurationToReturnEmptyListNoGlobalDefaults() {
    Mockito.when(configurationRepository.getKeyRange(Mockito.anyString()))
        .thenAnswer(invocation -> {
          Object argument = invocation.getArguments()[0];
          if (argument.equals(configurationKey)) {
            return List.of();
          } else if (argument
              .equals(GlobalConfigurationReferenceResolver.REFERENCED_CONFIGURATION_KEY_PREFIX)) {
            return List.of();
          }
          throw new InvalidUseOfMatchersException(
              String.format("Argument %s does not match", argument)
          );
        });
  }

  @Test
  void testBuildNoKeys() {
    final ConfigurationConsumerUtility clientUtility =
        ConfigurationConsumerUtility.builder(configurationRepository).build();

    // When no keys are provided only the globally reference configuration should be loaded
    verify(configurationRepository, Mockito.times(1)).getKeyRange(Mockito.anyString());
    verify(configurationRepository, Mockito.times(1))
        .getKeyRange(GlobalConfigurationReferenceResolver.REFERENCED_CONFIGURATION_KEY_PREFIX);

    // TODO: verify watches are setup

    assertNotNull(clientUtility);
  }

  @Test
  void testBuildNullConfigurationRepositoryExpectNullPointerException() {
    TestUtilities.expectExceptionAndMessage(
        () -> ConfigurationConsumerUtility.builder(null),
        NullPointerException.class, "Requires non-null ConfigurationRepository");
  }

  @Test
  void testBuildLoadsKeys() {
    final ConfigurationConsumerUtility clientUtility = getClientUtilMockGmsConfig();

    verify(configurationRepository, Mockito.times(1)).getKeyRange(configurationKey);

    // TODO: verify watches are setup
    assertNotNull(clientUtility);
  }

  @Test
  void testBuildWithOrWithoutExpiration() throws NoSuchFieldException, IllegalAccessException {
    ConfigurationConsumerUtility clientUtility = getClientUtilMockGmsConfig();
    Field field = clientUtility.getClass()
        .getDeclaredField("selectorCacheExpiration");
    field.setAccessible(true);
    var selectorCacheExpiration = (Duration) field.get(clientUtility);
    assertEquals(defaultSelectorCacheExpiration, selectorCacheExpiration);

    Duration expirationOverride = Duration.ofSeconds(1);
    ConfigurationConsumerUtility clientUtilityExpirationOverride = getClientUtilOverrideExpiration(
        expirationOverride);
    field = clientUtilityExpirationOverride.getClass()
        .getDeclaredField("selectorCacheExpiration");
    field.setAccessible(true);
    selectorCacheExpiration = (Duration) field.get(clientUtilityExpirationOverride);
    assertEquals(expirationOverride, selectorCacheExpiration);
  }

  @Test
  void testBuildNullConfigurationKeysExpectNullPointerException() {
    TestUtilities.expectExceptionAndMessage(
        () -> ConfigurationConsumerUtility.builder(configurationRepository)
            .configurationNamePrefixes(null),
        NullPointerException.class, "Requires non-null configurationNamePrefixes"
    );
  }

  @Test
  void testBuildLoadsGlobalDefaults() {
    mockGmsConfigurationToReturnPresentConfigurationItemNoGlobalDefaults();

    ConfigurationConsumerUtility.builder(configurationRepository)
        .configurationNamePrefixes(List.of(configurationKey))
        .build();

    verify(configurationRepository, Mockito.times(1))
        .getKeyRange(GlobalConfigurationReferenceResolver.REFERENCED_CONFIGURATION_KEY_PREFIX);
  }

  @Test
  void testGlobalDefaultsCanBeEmpty() {
    mockGmsConfigurationToReturnPresentConfigurationItemNoGlobalDefaults();

    Assertions.assertDoesNotThrow(() ->
        ConfigurationConsumerUtility.builder(configurationRepository)
            .configurationNamePrefixes(List.of(configurationKey))
            .build());
  }

  @Test
  void testLoadConfigurations() {
    mockGmsConfigurationToReturnPresentConfigurationItemNoGlobalDefaults();

    final ConfigurationConsumerUtility clientUtility = ConfigurationConsumerUtility
        .builder(configurationRepository)
        .build();
    assertNotNull(clientUtility);

    verify(configurationRepository, Mockito.times(0)).getKeyRange(configurationKey);

    clientUtility.loadConfigurations(List.of(configurationKey));
    verify(configurationRepository, Mockito.times(1)).getKeyRange(configurationKey);
    Mockito.verifyNoMoreInteractions(configurationRepository);

    // TODO: verify watches are setup
  }

  @Test
  void testLoadConfigurationsDoesNotReloadExistingConfigurations() {
    final ConfigurationConsumerUtility clientUtility = getClientUtilMockGmsConfig();
    clientUtility.loadConfigurations(List.of(configurationKey));
    Mockito.verifyNoMoreInteractions(configurationRepository);
  }

  @Test
  void testLoadConfigurationsValidatesParameters() {
    final ConfigurationConsumerUtility clientUtility = getClientUtilMockGmsConfig();

    TestUtilities.expectExceptionAndMessage(
        () -> clientUtility.loadConfigurations(null),
        NullPointerException.class,
        "Requires non-null configurationNamePrefixes"
    );
  }

  private static class NamedInt {

    @JsonProperty
    private int named;

    @JsonCreator
    private NamedInt(
        @JsonProperty("named") int named) {
      this.named = named;
    }
  }

  @Test
  void testResolveToFieldMap() {
    final Map<String, Object> resolvedParamsFieldMap = getClientUtilMockGmsConfig()
        .resolve(configurationKey, List.of(Selector.from("snr", -5.0)));

    assertAll(
        () -> assertNotNull(resolvedParamsFieldMap),
        () -> assertEquals(fooParamsDefaultsMap, resolvedParamsFieldMap)
    );
  }

  @Test
  void testResolveUnknownConfigurationKeyExpectIllegalArgumentException() {
    final String unknownKey = "unknown-key";

    TestUtilities.expectExceptionAndMessage(
        () -> getClientUtilMockGmsConfig().resolve(unknownKey, List.of(), Number.class),
        IllegalArgumentException.class, "No Configuration named " + unknownKey + " is in this"
    );
  }

  @Test
  void testResolveToObjectFromClass() {
    final FooParameters resolvedParams = getClientUtilMockGmsConfig()
        .resolve(configurationKey, List.of(Selector.from("snr", -5.0)), FooParameters.class);

    assertAll(
        () -> assertNotNull(resolvedParams),
        () -> assertEquals(fooParamsDefaults, resolvedParams)
    );
  }

  @Test
  void testParameterClassNotCreatableExpectIllegalArgumentException() {
    TestUtilities.expectExceptionAndMessage(
        () -> getClientUtilMockGmsConfig().resolve(configurationKey, List.of(), Number.class),
        IllegalArgumentException.class,
        "Resolved Configuration is not a valid instance of " + Number.class.getCanonicalName()
    );
  }

  @Test
  void testResolveNullConfigurationNameExpectNullPointerException() {
    TestUtilities.expectExceptionAndMessage(
        () -> getClientUtilMockGmsConfig().resolve(null, List.of()),
        NullPointerException.class,
        "Cannot resolve Configuration for null configurationName"
    );
  }

  @Test
  void testResolveNullSelectorsExpectNullPointerException() {
    TestUtilities.expectExceptionAndMessage(
        () -> getClientUtilMockGmsConfig().resolve(configurationKey, null),
        NullPointerException.class,
        "Cannot resolve Configuration for null selectors"
    );
  }

  @Test
  void testResolveNullParametersClassExpectNullPointerException() {
    TestUtilities.expectExceptionAndMessage(
        () -> getClientUtilMockGmsConfig().resolve(configurationKey, List.of(), null),
        NullPointerException.class,
        "Cannot resolve Configuration to null parametersClass"
    );
  }
}
