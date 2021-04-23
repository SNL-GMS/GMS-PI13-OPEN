package gms.shared.frameworks.osd.service;

import gms.shared.frameworks.service.ServiceGenerator;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import gms.shared.frameworks.systemconfig.SystemConfig;

public class SohServiceApplication {

    public static void main(String[] args) {
      final SystemConfig config = SystemConfig.create("osd");
      ServiceGenerator.runService(
          SohRepositoryFactory.createSohRepository(config),
          config);
    }
}
