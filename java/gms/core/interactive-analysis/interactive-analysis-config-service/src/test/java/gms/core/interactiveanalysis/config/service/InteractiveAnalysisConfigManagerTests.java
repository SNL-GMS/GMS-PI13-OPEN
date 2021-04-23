package gms.core.interactiveanalysis.config.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import gms.shared.frameworks.configuration.Selector;
import gms.shared.frameworks.configuration.repository.client.ConfigurationConsumerUtility;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InteractiveAnalysisConfigManagerTests {

  private static final ConfigQuery query = ConfigQuery.from(
      "a name", List.of(Selector.from("criteria", 5)));
  private static final Map<String, Object> result
      = Map.of("a", 1, "b", Instant.now());

  @Mock
  private ConfigurationConsumerUtility configConsumer;

  private InteractiveAnalysisConfigManager manager;

  @BeforeEach
  void setup() {
    manager = InteractiveAnalysisConfigManager.create(configConsumer);
    assertNotNull(manager);
  }

  @Test
  void testResolve() {
    when(configConsumer.resolve(query.getConfigurationName(), query.getSelectors()))
        .thenReturn(result);
    assertEquals(result, manager.resolve(query));
  }
}
