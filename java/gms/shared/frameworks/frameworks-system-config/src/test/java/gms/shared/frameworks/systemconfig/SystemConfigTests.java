package gms.shared.frameworks.systemconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.common.config.ServerConfig;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SystemConfigTests {

  private static String testConfigDir =
      SystemConfigTests.class.getClassLoader().getResource("test-config/").getPath();

  private static String basicConfigFilename = testConfigDir + "basic.properties";
  private static String componentConfigFilename = testConfigDir + "component.properties";
  private static String overrideConfigFilename = testConfigDir + "override.properties";

  private static SystemConfig basicConfig;

  @BeforeAll
  static void testSetup() {
    basicConfig = createConfig("component-name", basicConfigFilename);
  }

  @Test
  void testGetValue() {
    assertEquals("jabberwocky", basicConfig.getValue("string-parameter1"));
    assertEquals("twas brillig and the slithy toves", basicConfig.getValue("string-parameter2"));
    assertEquals("42", basicConfig.getValue("integer-parameter1"));
  }

  @Test
  void testMissingValue() {
    assertThrows(MissingResourceException.class, () -> basicConfig.getValue("string-parameter3"));
  }

  @Test
  void testGetValueAsInt() {
    assertEquals(42, basicConfig.getValueAsInt("integer-parameter1"));
    assertEquals(-19, basicConfig.getValueAsInt("integer-parameter2"));
  }

  @Test()
  void testGetNonIntValueAsInt() {
    assertThrows(NumberFormatException.class, () -> basicConfig.getValueAsInt("string-parameter1"));
  }

  @Test()
  void testGetLongValueAsInt() {
    assertThrows(NumberFormatException.class, () -> basicConfig.getValueAsInt("long-parameter"));
  }

  @Test
  void testGetValueAsLong() {
    assertEquals(2147483648L, basicConfig.getValueAsLong("long-parameter"));
  }

  @Test()
  void testGetNonLongValueAsLong() {
    assertThrows(
        NumberFormatException.class, () -> basicConfig.getValueAsLong("string-parameter1"));
  }

  @Test
  void testGetValueAsDouble() {
    assertEquals(3.1415, basicConfig.getValueAsDouble("double-parameter"));
  }

  @Test()
  void testGetNonDoubleValueAsDouble() {
    assertThrows(
        NumberFormatException.class, () -> basicConfig.getValueAsDouble("string-parameter1"));
  }

  @Test
  void testGetValueAsBoolean() {
    assertTrue(basicConfig.getValueAsBoolean("boolean-true-parameter1"));
    assertTrue(basicConfig.getValueAsBoolean("boolean-true-parameter2"));
    assertTrue(basicConfig.getValueAsBoolean("boolean-true-parameter3"));
    assertTrue(basicConfig.getValueAsBoolean("boolean-true-parameter4"));
    assertTrue(basicConfig.getValueAsBoolean("boolean-true-parameter5"));

    assertFalse(basicConfig.getValueAsBoolean("boolean-false-parameter1"));
    assertFalse(basicConfig.getValueAsBoolean("boolean-false-parameter2"));
    assertFalse(basicConfig.getValueAsBoolean("boolean-false-parameter3"));
    assertFalse(basicConfig.getValueAsBoolean("boolean-false-parameter4"));
    assertFalse(basicConfig.getValueAsBoolean("boolean-false-parameter5"));
  }

  @Test()
  void testGetNonBooleanValueAsDouble() {
    assertThrows(
        IllegalArgumentException.class, () -> basicConfig.getValueAsBoolean("string-parameter1"));
  }

  @Test
  void testGetValueAsDuration() {
    assertEquals(Duration.ofSeconds(90), basicConfig.getValueAsDuration("duration-parameter"));
  }

  @Test
  void testGetUrlOfComponent() {
    final SystemConfig config = createConfig("component-2", componentConfigFilename);
    final URL url = config.getUrlOfComponent("component3");
    assertNotNull(url);
    assertEquals(config.getValue(
        SystemConfig.createKey("component3", SystemConfig.HOST)), url.getHost());
    assertEquals(config.getValueAsInt(
        SystemConfig.createKey("component3", SystemConfig.PORT)), url.getPort());
    assertEquals("", url.getPath());
  }

  @Test
  void testGetUrl() {
    final String componentName = "component3";
    final SystemConfig config = createConfig(componentName, componentConfigFilename);
    final URL url = config.getUrl();
    assertNotNull(url);
    assertEquals(config.getValue(
        SystemConfig.createKey(componentName, SystemConfig.HOST)), url.getHost());
    assertEquals(config.getValueAsInt(
        SystemConfig.createKey(componentName, SystemConfig.PORT)), url.getPort());
    assertEquals("", url.getPath());
  }

  @Test
  void testGetServerConfig() {
    final String componentName = "component1";
    final SystemConfig config = createConfig(componentName, componentConfigFilename);
    final ServerConfig serverConfig = config.getServerConfig();
    assertNotNull(serverConfig);
    final BiConsumer<String, Integer> checker = (key, expectedVal) ->
    assertEquals(config.getValueAsInt(SystemConfig.createKey(componentName, key)),
        (int) expectedVal);
    checker.accept(SystemConfig.PORT, serverConfig.getPort());
    checker.accept(SystemConfig.MIN_THREADS, serverConfig.getMinThreadPoolSize());
    checker.accept(SystemConfig.MAX_THREADS, serverConfig.getMaxThreadPoolSize());
    assertEquals(config.getValueAsDuration(
        SystemConfig.createKey(componentName, SystemConfig.IDLE_TIMEOUT)),
        serverConfig.getThreadIdleTimeout());
  }

  @Test()
  void testComponentOverride() {
    final SystemConfig config1 = createConfig("component1", componentConfigFilename);
    final SystemConfig config2 = createConfig("component2", componentConfigFilename);

    assertEquals("component1", config1.getValue("host"));
    assertEquals(8080, config1.getValueAsInt("port"));
    assertEquals("component2", config1.getValue("component2.host"));
    assertEquals(80, config1.getValueAsInt("component2.port"));

    assertEquals("component2", config2.getValue("host"));
    assertEquals(80, config2.getValueAsInt("port"));
    assertEquals("component1", config2.getValue("component1.host"));
    assertEquals(8080, config2.getValueAsInt("component1.port"));
  }

  @Test()
  void testFileOverride() {
    final SystemConfig config1 = createConfig(
        "component1", overrideConfigFilename, componentConfigFilename);
    final SystemConfig config2 = createConfig(
        "component2", overrideConfigFilename, componentConfigFilename);

    assertEquals("component1", config1.getValue("host"));
    assertEquals(9000, config1.getValueAsInt("port"));
    assertEquals("test-component2", config1.getValue("component2.host"));
    assertEquals(9000, config1.getValueAsInt("component2.port"));

    assertEquals("test-component2", config2.getValue("host"));
    assertEquals(9000, config2.getValueAsInt("port"));
    assertEquals("component1", config2.getValue("component1.host"));
    assertEquals(9000, config2.getValueAsInt("component1.port"));
  }

  private static SystemConfig createConfig(String componentName, String... fileNames) {
    final List<SystemConfigRepository> repos = Arrays.stream(fileNames)
        .map(SystemConfigTests::configRepository)
        .collect(Collectors.toList());
    return SystemConfig.create(componentName, repos);
  }

  private static FileSystemConfigRepository configRepository(String fileName) {
    return FileSystemConfigRepository.builder().setFilename(fileName).build();
  }
}
