package gms.shared.frameworks.configuration.repository.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.google.common.cache.Cache;
import com.google.common.collect.Lists;
import gms.shared.frameworks.configuration.Configuration;
import gms.shared.frameworks.configuration.ConfigurationResolver;
import gms.shared.frameworks.configuration.Selector;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import mockit.MockUp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigurationSelectorGuavaCacheTests {

  @Mock
  Configuration mockConfiguration;

  @Test
  void testUpdatesCache() throws NoSuchFieldException, IllegalAccessException {
    ConfigurationSelectorGuavaCache cache = ConfigurationSelectorGuavaCache
        .create(mockConfiguration, Duration.ofDays(1));

    Map<String, Object> testFieldMap = Map.of("TEST", "TEST");

    new MockUp<ConfigurationResolver>() {
      @mockit.Mock
      public Map<String, Object> resolve(Configuration configuration, List<Selector> selectors) {
        return testFieldMap;
      }
    };

    Field field = cache.getClass()
        .getDeclaredField("fieldMapCache");
    field.setAccessible(true);
    var fieldMapCache = (Cache<Integer, Map<String, Object>>) field.get(cache);

    List<Selector> dummySelectors = Collections.emptyList();
    assertNull(fieldMapCache.getIfPresent(Set.copyOf(dummySelectors)));

    assertEquals(testFieldMap, cache.resolveFieldMap(dummySelectors));
    assertEquals(testFieldMap, fieldMapCache.getIfPresent(Set.copyOf(dummySelectors)));
  }

  @Test
  void testResolveSameSelectors()
      throws NoSuchFieldException, IllegalAccessException {
    ConfigurationSelectorGuavaCache cache = ConfigurationSelectorGuavaCache
        .create(mockConfiguration, Duration.ofDays(1));

    Map<String, Object> testFieldMap = Map.of("TEST", "TEST");

    new MockUp<ConfigurationResolver>() {
      @mockit.Mock
      public Map<String, Object> resolve(Configuration configuration, List<Selector> selectors) {
        return testFieldMap;
      }
    };

    Field field = cache.getClass()
        .getDeclaredField("fieldMapCache");
    field.setAccessible(true);
    var fieldMapCache = (Cache<Integer, Map<String, Object>>) field.get(cache);

    List<Selector> selectors = List.of(
        Selector.from("TEST", 1),
        Selector.from("TEST2", "TEST"));

    assertNull(fieldMapCache.getIfPresent(Set.copyOf(selectors)));

    assertEquals(testFieldMap, cache.resolveFieldMap(selectors));
    assertEquals(testFieldMap, fieldMapCache.getIfPresent(Set.copyOf(selectors)));

    List<Selector> reverse = Lists.reverse(selectors);
    assertEquals(testFieldMap, cache.resolveFieldMap(reverse));
    assertEquals(1, fieldMapCache.size());
  }

  @Test
  void testResolveDifferentSelectorsSameFieldMap()
      throws NoSuchFieldException, IllegalAccessException {
    ConfigurationSelectorGuavaCache cache = ConfigurationSelectorGuavaCache
        .create(mockConfiguration, Duration.ofDays(1));

    Map<String, Object> testFieldMap = Map.of("TEST", "TEST");

    new MockUp<ConfigurationResolver>() {
      @mockit.Mock
      public Map<String, Object> resolve(Configuration configuration, List<Selector> selectors) {
        return testFieldMap;
      }
    };

    Field fieldMapCacheField = cache.getClass()
        .getDeclaredField("fieldMapCache");
    fieldMapCacheField.setAccessible(true);
    var fieldMapCache = (Cache<Integer, Map<String, Object>>) fieldMapCacheField.get(cache);

    Field uniqueFieldMapsField = cache.getClass()
        .getDeclaredField("uniqueFieldMaps");
    uniqueFieldMapsField.setAccessible(true);
    var uniqueFieldMaps = (Map<Map<String, Object>, Map<String, Object>>) uniqueFieldMapsField
        .get(cache);

    List<Selector> selectors1 = List.of(
        Selector.from("TEST", 1));
    List<Selector> selectors2 = List.of(
        Selector.from("TEST", 2));

    assertEquals(testFieldMap, cache.resolveFieldMap(selectors1));
    assertEquals(testFieldMap, cache.resolveFieldMap(selectors2));

    var cachedMap1 = fieldMapCache.getIfPresent(Set.copyOf(selectors1));
    var cachedMap2 = fieldMapCache.getIfPresent(Set.copyOf(selectors2));
    assertEquals(testFieldMap, cachedMap1);
    assertEquals(testFieldMap, cachedMap2);

    assertEquals(2, fieldMapCache.size());
    assertEquals(1, uniqueFieldMaps.size());
    assertSame(cachedMap1, uniqueFieldMaps.get(testFieldMap));
    assertSame(cachedMap2, uniqueFieldMaps.get(testFieldMap));
  }

  @ParameterizedTest
  @MethodSource("exceptionSource")
  void testResolutionFailureThrowsException(RuntimeException exception) {
    ConfigurationSelectorGuavaCache cache = ConfigurationSelectorGuavaCache
        .create(mockConfiguration, Duration.ofDays(1));

    Map<String, Object> testFieldMap = Map.of("TEST", "TEST");

    new MockUp<ConfigurationResolver>() {
      @mockit.Mock
      public Map<String, Object> resolve(Configuration configuration, List<Selector> selectors) {
        throw exception;
      }
    };

    assertThrows(exception.getClass(), () -> cache.resolveFieldMap(List.of()));
  }

  static Stream<Arguments> exceptionSource() {
    return Stream.of(
        arguments(new NullPointerException()),
        arguments(new IllegalArgumentException()),
        arguments(new IllegalStateException())
    );
  }
}