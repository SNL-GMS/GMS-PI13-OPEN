package gms.shared.frameworks.osd.control;

import gms.shared.frameworks.osd.coi.util.CoiEntityManagerFactory;
import gms.shared.frameworks.osd.control.channel.ChannelRepositoryJpa;
import gms.shared.frameworks.osd.control.channel.MockChannelSegmentsRepository;
import gms.shared.frameworks.osd.control.event.EventRepositoryJpa;
import gms.shared.frameworks.osd.control.instrumentresponse.ResponseRepositoryJpa;
import gms.shared.frameworks.osd.control.preferences.UserPreferencesRepositoryJpa;
import gms.shared.frameworks.osd.control.signaldetection.QcMaskRepositoryJpa;
import gms.shared.frameworks.osd.control.signaldetection.SignalDetectionRepositoryJpa;
import gms.shared.frameworks.osd.control.station.StationGroupRepositoryJpa;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import gms.shared.frameworks.osd.control.stationreference.ReferenceChannelRepositoryJpa;
import gms.shared.frameworks.osd.control.stationreference.ReferenceNetworkRepositoryJpa;
import gms.shared.frameworks.osd.control.stationreference.ReferenceResponseRepositoryJpa;
import gms.shared.frameworks.osd.control.stationreference.ReferenceSensorRepositoryJpa;
import gms.shared.frameworks.osd.control.stationreference.ReferenceSiteRepositoryJpa;
import gms.shared.frameworks.osd.control.stationreference.ReferenceStationRepositoryJpa;
import gms.shared.frameworks.osd.control.utils.CassandraConfig;
import gms.shared.frameworks.osd.control.utils.CassandraConfigFactory;
import gms.shared.frameworks.osd.control.waveforms.RawStationDataFrameRepositoryJpa;
import gms.shared.frameworks.systemconfig.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class OsdRepositoryFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(OsdRepositoryFactory.class);

  private OsdRepositoryFactory() {
  }

  public static OsdRepository createOsdRepository(SystemConfig config) {
    final EntityManagerFactory emf = CoiEntityManagerFactory.create(config);
    final EntityManager em = emf.createEntityManager();
    Runtime.getRuntime().addShutdownHook(new Thread(
        () -> {
          LOGGER.info("Entered shutdown hook");
          LOGGER.info("Shutting down EntityManager");
          em.close();
          LOGGER.info("Shutting down EntityManagerFactory");
          emf.close();
        }
    ));
    final CassandraConfig cassandraConfig = CassandraConfigFactory.create(config);
    final ChannelRepositoryJpa channelRepositoryJpa = new ChannelRepositoryJpa(emf);
    return OsdRepository.from(
        channelRepositoryJpa,
        new MockChannelSegmentsRepository(),
        new EventRepositoryJpa(emf),
        new QcMaskRepositoryJpa(emf),
        new RawStationDataFrameRepositoryJpa(emf),
        new ReferenceChannelRepositoryJpa(emf),
        new ReferenceNetworkRepositoryJpa(emf),
        new ReferenceResponseRepositoryJpa(emf),
        new ReferenceSensorRepositoryJpa(emf),
        new ReferenceSiteRepositoryJpa(emf),
        new ReferenceStationRepositoryJpa(emf),
        new ResponseRepositoryJpa(emf),
        new SignalDetectionRepositoryJpa(emf),
        new StationGroupRepositoryJpa(emf),
        new StationRepositoryJpa(emf),
        new UserPreferencesRepositoryJpa(emf));
  }

}