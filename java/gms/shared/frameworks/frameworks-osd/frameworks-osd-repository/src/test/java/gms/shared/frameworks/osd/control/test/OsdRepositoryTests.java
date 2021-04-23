package gms.shared.frameworks.osd.control.test;

import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.channel.ChannelSegmentsRepositoryInterface;
import gms.shared.frameworks.osd.api.event.EventRepositoryInterface;
import gms.shared.frameworks.osd.api.instrumentresponse.ResponseRepositoryInterface;
import gms.shared.frameworks.osd.api.performancemonitoring.PerformanceMonitoringRepositoryInterface;
import gms.shared.frameworks.osd.api.preferences.UserPreferencesRepositoryInterface;
import gms.shared.frameworks.osd.api.signaldetection.QcMaskRepositoryInterface;
import gms.shared.frameworks.osd.api.signaldetection.SignalDetectionRepositoryInterface;
import gms.shared.frameworks.osd.api.station.StationGroupRepositoryInterface;
import gms.shared.frameworks.osd.api.station.StationRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceNetworkRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceResponseRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceSensorRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceSiteRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceStationRepositoryInterface;
import gms.shared.frameworks.osd.api.transferredfile.TransferredFileRepositoryInterface;
import gms.shared.frameworks.osd.api.waveforms.RawStationDataFrameRepositoryInterface;
import gms.shared.frameworks.osd.api.waveforms.StationSohRepositoryInterface;
import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.control.OsdRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OsdRepositoryTests {

  @Mock
  private ChannelRepositoryInterface mockChannelRepo;
  @Mock
  private ChannelSegmentsRepositoryInterface mockChannelSegmentsRepo;
  @Mock
  private EventRepositoryInterface mockEventRepo;
  @Mock
  private PerformanceMonitoringRepositoryInterface mockPMRepo;
  @Mock
  private QcMaskRepositoryInterface mockQcRepo;
  @Mock
  private ReferenceChannelRepositoryInterface mockReferenceChannelRepo;
  @Mock
  private ReferenceNetworkRepositoryInterface mockReferenceNetworkRepo;
  @Mock
  private ReferenceResponseRepositoryInterface mockReferenceResponseRepo;
  @Mock
  private ReferenceSensorRepositoryInterface mockReferenceSensorRepo;
  @Mock
  private ReferenceSiteRepositoryInterface mockReferenceSiteRepo;
  @Mock
  private ReferenceStationRepositoryInterface mockReferenceStationRepo;
  @Mock
  private ResponseRepositoryInterface mockResponseRepo;
  @Mock
  private RawStationDataFrameRepositoryInterface mockRsdfRepo;
  @Mock
  private SignalDetectionRepositoryInterface mockSdRepo;
  @Mock
  private StationGroupRepositoryInterface mockStationGroupRepo;
  @Mock
  private StationRepositoryInterface mockStationRepo;
  @Mock
  private TransferredFileRepositoryInterface mockXferFileRepo;
  @Mock
  private UserPreferencesRepositoryInterface mockUserPreferencesRepositoryInterface;
  
  @Test
  void testFromNullValidation() throws Exception {
    TestUtilities.checkStaticMethodValidatesNullArguments(OsdRepository.class,
        "from", mockChannelRepo,
        mockChannelSegmentsRepo,
        mockEventRepo,
        mockPMRepo,
        mockQcRepo,
        mockRsdfRepo,
        mockReferenceChannelRepo,
        mockReferenceNetworkRepo,
        mockReferenceResponseRepo,
        mockReferenceSensorRepo,
        mockReferenceSiteRepo,
        mockReferenceStationRepo,
        mockResponseRepo,
        mockSdRepo,
        mockStationGroupRepo,
        mockStationRepo,
        mockXferFileRepo,
        mockUserPreferencesRepositoryInterface);
  }
}