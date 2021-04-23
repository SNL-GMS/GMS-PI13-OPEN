package gms.shared.frameworks.osd.api;

import gms.shared.frameworks.common.annotations.Component;
import gms.shared.frameworks.osd.api.capabilityrollup.CapabilitySohRollupRepositoryInterface;
import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.performancemonitoring.PerformanceMonitoringRepositoryInterface;
import gms.shared.frameworks.osd.api.preferences.UserPreferencesRepositoryInterface;
import gms.shared.frameworks.osd.api.station.StationGroupRepositoryInterface;
import gms.shared.frameworks.osd.api.station.StationRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceNetworkRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceResponseRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceSensorRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceSiteRepositoryInterface;
import gms.shared.frameworks.osd.api.stationreference.ReferenceStationRepositoryInterface;
import gms.shared.frameworks.osd.api.statuschange.SohStatusChangeRepositoryInterface;
import gms.shared.frameworks.osd.api.systemmessage.SystemMessageRepositoryInterface;
import gms.shared.frameworks.osd.api.transferredfile.TransferredFileRepositoryInterface;
import gms.shared.frameworks.osd.api.waveforms.RawStationDataFrameRepositoryInterface;
import gms.shared.frameworks.osd.api.waveforms.RawStationDataFrameRepositoryQueryInterface;
import gms.shared.frameworks.osd.api.waveforms.StationSohRepositoryInterface;
import gms.shared.frameworks.osd.api.waveforms.StationSohRepositoryQueryViewInterface;
import javax.ws.rs.Path;

@Component("osd")
@Path("/osd")
public interface SohRepositoryInterface extends
    CapabilitySohRollupRepositoryInterface,
    ChannelRepositoryInterface,
    PerformanceMonitoringRepositoryInterface,
    RawStationDataFrameRepositoryInterface,
  RawStationDataFrameRepositoryQueryInterface,
    ReferenceChannelRepositoryInterface,
    ReferenceNetworkRepositoryInterface,
    ReferenceResponseRepositoryInterface,
    ReferenceSensorRepositoryInterface,
    ReferenceSiteRepositoryInterface,
    ReferenceStationRepositoryInterface,
    SohStatusChangeRepositoryInterface,
    StationGroupRepositoryInterface,
    StationRepositoryInterface,
    StationSohRepositoryInterface,
    StationSohRepositoryQueryViewInterface,
    SystemMessageRepositoryInterface,
    TransferredFileRepositoryInterface,
    UserPreferencesRepositoryInterface {

}
