package gms.shared.frameworks.soh.repository;

import gms.shared.frameworks.osd.dao.util.CoiEntityManagerFactory;
import gms.shared.frameworks.soh.repository.capabilityrollup.CapabilitySohRollupRepositoryJpa;
import gms.shared.frameworks.soh.repository.channel.ChannelRepositoryJpa;
import gms.shared.frameworks.soh.repository.performancemonitoring.PerformanceMonitoringRepositoryJpa;
import gms.shared.frameworks.soh.repository.performancemonitoring.StationSohRepositoryJpa;
import gms.shared.frameworks.soh.repository.performancemonitoring.StationSohRepositoryQueryViewJpa;
import gms.shared.frameworks.soh.repository.preferences.UserPreferencesRepositoryJpa;
import gms.shared.frameworks.soh.repository.sohstatuschange.SohStatusChangeRepositoryJpa;
import gms.shared.frameworks.soh.repository.station.StationGroupRepositoryJpa;
import gms.shared.frameworks.soh.repository.station.StationRepositoryJpa;
import gms.shared.frameworks.soh.repository.stationreference.ReferenceChannelRepositoryJpa;
import gms.shared.frameworks.soh.repository.stationreference.ReferenceNetworkRepositoryJpa;
import gms.shared.frameworks.soh.repository.stationreference.ReferenceResponseRepositoryJpa;
import gms.shared.frameworks.soh.repository.stationreference.ReferenceSensorRepositoryJpa;
import gms.shared.frameworks.soh.repository.stationreference.ReferenceSiteRepositoryJpa;
import gms.shared.frameworks.soh.repository.stationreference.ReferenceStationRepositoryJpa;
import gms.shared.frameworks.soh.repository.systemmessage.SystemMessageRepositoryJpa;
import gms.shared.frameworks.soh.repository.transferredfile.RawStationDataFrameRepositoryJpa;
import gms.shared.frameworks.soh.repository.transferredfile.RawStationDataFrameRepositoryQueryViewJpa;
import gms.shared.frameworks.soh.repository.transferredfile.TransferredFileRepositoryJpa;
import gms.shared.frameworks.systemconfig.SystemConfig;
import javax.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SohRepositoryFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(SohRepositoryFactory.class);

  private SohRepositoryFactory() {
  }

  public static SohRepository createSohRepository(SystemConfig config) {
    EntityManagerFactory emf = CoiEntityManagerFactory.create(config);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOGGER.info("Shutting down EntityManagerFactory");
      emf.close();
    }));

    return SohRepository.from(
        new CapabilitySohRollupRepositoryJpa(emf),
        new ChannelRepositoryJpa(emf),
        new PerformanceMonitoringRepositoryJpa(emf),
        new RawStationDataFrameRepositoryJpa(emf),
        new RawStationDataFrameRepositoryQueryViewJpa(emf),
        new ReferenceChannelRepositoryJpa(emf),
        new ReferenceNetworkRepositoryJpa(emf),
        new ReferenceResponseRepositoryJpa(emf),
        new ReferenceSensorRepositoryJpa(emf),
        new ReferenceSiteRepositoryJpa(emf),
        new ReferenceStationRepositoryJpa(emf),
        new SohStatusChangeRepositoryJpa(emf),
        new StationGroupRepositoryJpa(emf),
        new StationRepositoryJpa(emf),
        new StationSohRepositoryJpa(emf),
        new StationSohRepositoryQueryViewJpa(emf, new StationRepositoryJpa(emf)),
        new SystemMessageRepositoryJpa(emf),
        new TransferredFileRepositoryJpa(emf),
        new UserPreferencesRepositoryJpa(emf));
  }

}
