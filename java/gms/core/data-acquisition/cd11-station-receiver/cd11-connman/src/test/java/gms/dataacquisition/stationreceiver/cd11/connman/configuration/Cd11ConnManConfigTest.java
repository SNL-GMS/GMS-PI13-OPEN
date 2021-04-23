package gms.dataacquisition.stationreceiver.cd11.connman.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11DataConsumerParameters;
import gms.shared.frameworks.configuration.repository.FileConfigurationRepository;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Cd11ConnManConfigTest {

  private static String configurationBase;

  private static final int DATA_CONSUMER_BASE_PORT = 8100;

  @BeforeAll
  static void setUp() {
    configurationBase = Thread.currentThread().getContextClassLoader()
        .getResource("gms/shared/frameworks/processing/configuration/service/configuration-base")
        .getPath();
  }

  @Test
  void testCreate() {
    assertDoesNotThrow(() -> Cd11ConnManConfig
        .create(FileConfigurationRepository.create(new File(configurationBase).toPath()),
            DATA_CONSUMER_BASE_PORT));
  }

  @Test
  public void testConfig() {

    Cd11ConnManConfig config = Cd11ConnManConfig
        .create(FileConfigurationRepository.create(new File(configurationBase).toPath()),
            DATA_CONSUMER_BASE_PORT);

    List<Cd11DataConsumerParameters> stationList = config
        .getCd11StationParameters();

    assertNotNull(stationList);

    Cd11DataConsumerParameters parameters = stationList.get(0);

    assertEquals(8155, parameters.getPort());
    assertTrue(parameters.isAcquired());
    assertFalse(parameters.isFrameProcessingDisabled());
  }
}
