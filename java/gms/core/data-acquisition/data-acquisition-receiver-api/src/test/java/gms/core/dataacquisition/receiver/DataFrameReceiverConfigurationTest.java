package gms.core.dataacquisition.receiver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import gms.shared.frameworks.configuration.ConfigurationRepository;
import gms.shared.frameworks.configuration.repository.FileConfigurationRepository;
import gms.shared.frameworks.osd.coi.waveforms.AcquisitionProtocol;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataFrameReceiverConfigurationTest {

  private static URL configurationBase;

  @Mock
  SystemConfig mockSystemConfig;

  @BeforeAll
  static void setUp() {
    configurationBase = Thread.currentThread().getContextClassLoader()
        .getResource("gms/core/dataacquisition/receiver/configuration-base/");
  }

  @Test
  void testGetChannelId() {
    AcquisitionProtocol protocol = AcquisitionProtocol.CD11;
    Assertions.assertNotNull(configurationBase);

    ConfigurationRepository configurationRepository = FileConfigurationRepository
        .create(new File(configurationBase.getPath()).toPath());

    given(mockSystemConfig.getValueAsDuration(any())).willReturn(Duration.ofDays(1));

    DataFrameReceiverConfiguration configuration = DataFrameReceiverConfiguration
        .create(protocol, configurationRepository, mockSystemConfig);

    Optional<String> actualOptional = configuration.getChannelName("foo");
    actualOptional.ifPresentOrElse(
        actual -> assertEquals("bar", actual),
        () -> fail("No test value found in configuration")
    );
  }
}