package gms.shared.frameworks.osd.api;

import gms.shared.frameworks.common.annotations.Component;
import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.channel.ChannelSegmentsRepositoryInterface;
import gms.shared.frameworks.osd.api.event.EventRepositoryInterface;
import gms.shared.frameworks.osd.api.instrumentresponse.ResponseRepositoryInterface;
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
import gms.shared.frameworks.osd.api.waveforms.RawStationDataFrameRepositoryInterface;
import javax.ws.rs.Path;

@Component("osd")
@Path("/osd")
public interface OsdRepositoryInterface extends
    ChannelRepositoryInterface,
    ChannelSegmentsRepositoryInterface,
    EventRepositoryInterface,
    QcMaskRepositoryInterface,
    RawStationDataFrameRepositoryInterface,
    ReferenceChannelRepositoryInterface,
    ReferenceNetworkRepositoryInterface,
    ReferenceResponseRepositoryInterface,
    ReferenceSensorRepositoryInterface,
    ReferenceSiteRepositoryInterface,
    ReferenceStationRepositoryInterface,
    ResponseRepositoryInterface,
    SignalDetectionRepositoryInterface,
    StationGroupRepositoryInterface,
    StationRepositoryInterface,
    UserPreferencesRepositoryInterface {

}
