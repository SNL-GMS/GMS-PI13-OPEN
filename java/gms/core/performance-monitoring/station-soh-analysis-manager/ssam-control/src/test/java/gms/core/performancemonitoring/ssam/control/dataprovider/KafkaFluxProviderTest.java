package gms.core.performancemonitoring.ssam.control.dataprovider;

import gms.shared.frameworks.systemconfig.SystemConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

class KafkaFluxProviderTest {

  @ParameterizedTest
  @MethodSource("getCreateArguments")
  void testCreateValidation(Class<? extends Exception> expectedException,
      Class<String> fluxType,
      String topicName,
      SystemConfig systemConfig) {
    assertThrows(expectedException, () -> KafkaFluxProvider.create(fluxType, topicName, systemConfig));
  }

  static Stream<Arguments> getCreateArguments() {
    return Stream.of(arguments(NullPointerException.class, null, "testTopic", mock(SystemConfig.class)),
        arguments(NullPointerException.class, String.class, null, mock(SystemConfig.class)),
        arguments(IllegalStateException.class, String.class, "", mock(SystemConfig.class)),
        arguments(IllegalStateException.class, String.class, "      " , mock(SystemConfig.class)),
        arguments(NullPointerException.class, String.class, "testTopic", null));
  }

  @Test
  void testCreate() {
    assertNotNull(KafkaFluxProvider.create(String.class, "TestTopic", mock(SystemConfig.class)));
  }

}