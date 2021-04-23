package gms.shared.frameworks.processing.configuration.service;

import gms.shared.frameworks.configuration.repository.JpaConfigurationRepository;
import gms.shared.frameworks.service.ServiceGenerator;
import gms.shared.frameworks.systemconfig.SystemConfig;


public class ProcessingConfigurationServiceApplication {

  public static void main(String[] args) {
    final SystemConfig config = SystemConfig.create("processing-cfg");
    ServiceGenerator.runService(
        new JpaConfigurationRepository(config),
        config);
  }
}
