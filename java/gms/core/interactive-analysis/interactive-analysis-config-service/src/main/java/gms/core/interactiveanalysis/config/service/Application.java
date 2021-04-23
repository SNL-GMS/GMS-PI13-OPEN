package gms.core.interactiveanalysis.config.service;

import static gms.core.interactiveanalysis.config.service.InteractiveAnalysisConfigManager.COMPONENT_NAME;

import gms.shared.frameworks.client.generation.ClientGenerator;
import gms.shared.frameworks.configuration.ConfigurationRepository;
import gms.shared.frameworks.configuration.repository.client.ConfigurationConsumerUtility;
import gms.shared.frameworks.service.ServiceGenerator;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.util.List;

/**
 * Application for running the interactive analysis config service.
 */
public class Application {

  private Application() {
  }

  public static void main(String[] args) {
    final SystemConfig sysConfig = SystemConfig.create(COMPONENT_NAME);
    final ConfigurationRepository configRepo
        = ClientGenerator.createClient(ConfigurationRepository.class);
    final ConfigurationConsumerUtility configConsumer = ConfigurationConsumerUtility
        .builder(configRepo)
        .configurationNamePrefixes(List.of(
            "ui.analyst-settings", "ui.common-settings"))
        .build();
    ServiceGenerator.runService(
        InteractiveAnalysisConfigManager.create(configConsumer),
        sysConfig);
  }
}
