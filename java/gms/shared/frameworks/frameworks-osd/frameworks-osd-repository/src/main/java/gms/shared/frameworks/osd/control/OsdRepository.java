package gms.shared.frameworks.osd.control;

import com.google.auto.value.AutoValue;
import gms.shared.frameworks.osd.api.OsdRepositoryInterface;
import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.channel.ChannelSegmentsRepositoryInterface;
import gms.shared.frameworks.osd.api.channel.util.ChannelSegmentsIdRequest;
import gms.shared.frameworks.osd.api.event.EventRepositoryInterface;
import gms.shared.frameworks.osd.api.event.util.FindEventByTimeAndLocationRequest;
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
import gms.shared.frameworks.osd.api.stationreference.util.NetworkMembershipRequest;
import gms.shared.frameworks.osd.api.stationreference.util.ReferenceSiteMembershipRequest;
import gms.shared.frameworks.osd.api.stationreference.util.ReferenceStationMembershipRequest;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ChannelsTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ReferenceChannelRequest;
import gms.shared.frameworks.osd.api.util.StationTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.StationsTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.api.waveforms.RawStationDataFrameRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.channel.ReferenceChannel;
import gms.shared.frameworks.osd.coi.event.Event;
import gms.shared.frameworks.osd.coi.preferences.UserPreferences;
import gms.shared.frameworks.osd.coi.signaldetection.QcMask;
import gms.shared.frameworks.osd.coi.signaldetection.Response;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetection;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroupDefinition;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceNetwork;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceNetworkMembership;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceResponse;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSensor;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSite;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSiteMembership;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceStation;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceStationMembership;
import gms.shared.frameworks.osd.coi.waveforms.FkSpectra;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import gms.shared.frameworks.osd.coi.waveforms.Timeseries;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@AutoValue
public abstract class OsdRepository implements OsdRepositoryInterface {

  public abstract ChannelRepositoryInterface getChannelRepository();

  public abstract ChannelSegmentsRepositoryInterface getChannelSegmentRepository();

  public abstract EventRepositoryInterface getEventRepository();

  public abstract QcMaskRepositoryInterface getQcMaskRepository();

  public abstract RawStationDataFrameRepositoryInterface getRawStationDataFrameRepository();

  public abstract ReferenceChannelRepositoryInterface getReferenceChannelRepository();

  public abstract ReferenceNetworkRepositoryInterface getReferenceNetworkRepository();

  public abstract ReferenceResponseRepositoryInterface getReferenceResponseRepository();

  public abstract ReferenceSensorRepositoryInterface getReferenceSensorRepository();

  public abstract ReferenceSiteRepositoryInterface getReferenceSiteRepository();

  public abstract ReferenceStationRepositoryInterface getReferenceStationRepository();

  public abstract ResponseRepositoryInterface getResponseRepository();

  public abstract SignalDetectionRepositoryInterface getSignalDetectionRepository();

  public abstract StationGroupRepositoryInterface getStationGroupRepository();

  public abstract StationRepositoryInterface getStationRepository();

  public abstract UserPreferencesRepositoryInterface getUserPreferencesRepository();

  public static OsdRepository from(
      ChannelRepositoryInterface channelRepository,
      ChannelSegmentsRepositoryInterface channelSegmentRepository,
      EventRepositoryInterface eventRepository,
      QcMaskRepositoryInterface qcMaskRepository,
      RawStationDataFrameRepositoryInterface rawStationDataFrameRepository,
      ReferenceChannelRepositoryInterface referenceChannelRepository,
      ReferenceNetworkRepositoryInterface referenceNetworkRepository,
      ReferenceResponseRepositoryInterface referenceResponseRepository,
      ReferenceSensorRepositoryInterface referenceSensorRepository,
      ReferenceSiteRepositoryInterface referenceSiteRepository,
      ReferenceStationRepositoryInterface referenceStationRepository,
      ResponseRepositoryInterface responseRepository,
      SignalDetectionRepositoryInterface signalDetectionRepository,
      StationGroupRepositoryInterface stationGroupRepository,
      StationRepositoryInterface stationRepository,
      UserPreferencesRepositoryInterface userPreferencesRepository) {
    return new AutoValue_OsdRepository.Builder()
        .setChannelRepository(channelRepository)
        .setChannelSegmentRepository(channelSegmentRepository)
        .setEventRepository(eventRepository)
        .setQcMaskRepository(qcMaskRepository)
        .setRawStationDataFrameRepository(rawStationDataFrameRepository)
        .setReferenceChannelRepository(referenceChannelRepository)
        .setReferenceNetworkRepository(referenceNetworkRepository)
        .setReferenceResponseRepository(referenceResponseRepository)
        .setReferenceSensorRepository(referenceSensorRepository)
        .setReferenceSiteRepository(referenceSiteRepository)
        .setReferenceStationRepository(referenceStationRepository)
        .setResponseRepository(responseRepository)
        .setSignalDetectionRepository(signalDetectionRepository)
        .setStationGroupRepository(stationGroupRepository)
        .setStationRepository(stationRepository)
        .setUserPreferencesRepository(userPreferencesRepository)
        .build();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setChannelRepository(ChannelRepositoryInterface i);

    public abstract Builder setChannelSegmentRepository(ChannelSegmentsRepositoryInterface i);

    public abstract Builder setEventRepository(EventRepositoryInterface i);

    public abstract Builder setQcMaskRepository(QcMaskRepositoryInterface i);

    public abstract Builder setRawStationDataFrameRepository(
        RawStationDataFrameRepositoryInterface i);

    public abstract Builder setSignalDetectionRepository(SignalDetectionRepositoryInterface i);

    public abstract Builder setStationGroupRepository(StationGroupRepositoryInterface i);

    public abstract Builder setStationRepository(StationRepositoryInterface i);

    public abstract Builder setUserPreferencesRepository(UserPreferencesRepositoryInterface i);

    public abstract Builder setReferenceChannelRepository(
        ReferenceChannelRepositoryInterface i);

    public abstract Builder setReferenceNetworkRepository(
        ReferenceNetworkRepositoryInterface i);

    public abstract Builder setReferenceResponseRepository(ReferenceResponseRepositoryInterface i);

    public abstract Builder setReferenceSensorRepository(ReferenceSensorRepositoryInterface i);

    public abstract Builder setReferenceSiteRepository(ReferenceSiteRepositoryInterface i);

    public abstract Builder setReferenceStationRepository(
        ReferenceStationRepositoryInterface i);

    public abstract Builder setResponseRepository(ResponseRepositoryInterface i);

    public abstract OsdRepository build();
  }

  // ChannelRepositoryInterface
  @Override
  public List<Channel> retrieveChannels(Collection<String> channelIds) {
    return getChannelRepository().retrieveChannels(channelIds);
  }

  @Override
  public Set<String> storeChannels(Collection<Channel> channels) {
    return getChannelRepository().storeChannels(channels);
  }

  // ChannelSegmentsRepositoryInterface
  @Override
  public Collection<ChannelSegment<? extends Timeseries>> retrieveChannelSegmentsByIds(
      ChannelSegmentsIdRequest request) {
    return getChannelSegmentRepository().retrieveChannelSegmentsByIds(request);
  }

  public Collection<ChannelSegment<Waveform>> retrieveChannelSegmentsByChannelNames(
      ChannelsTimeRangeRequest request) {
    return getChannelSegmentRepository().retrieveChannelSegmentsByChannelNames(request);
  }

  @Override
  public Collection<ChannelSegment<Waveform>> retrieveChannelSegmentsByChannelsAndTimeRanges(
      Collection<ChannelTimeRangeRequest> channelTimeRangeRequests) {
    return getChannelSegmentRepository()
        .retrieveChannelSegmentsByChannelsAndTimeRanges(channelTimeRangeRequests);
  }

  @Override
  public void storeChannelSegments(
      Collection<ChannelSegment<Waveform>> segments) {
    getChannelSegmentRepository().storeChannelSegments(segments);
  }

  @Override
  public List<ChannelSegment<FkSpectra>> retrieveFkChannelSegmentsByChannelsAndTime(
      Collection<ChannelTimeRangeRequest> channelTimeRangeRequests) {
    return getChannelSegmentRepository().retrieveFkChannelSegmentsByChannelsAndTime(channelTimeRangeRequests);
  }

  @Override
  public void storeFkChannelSegments(Collection<ChannelSegment<FkSpectra>> segments) {
    getChannelSegmentRepository().storeFkChannelSegments(segments);
  }

  // EventRepositoryInterface
  @Override
  public void storeEvents(Collection<Event> events) {
    getEventRepository().storeEvents(events);
  }

  @Override
  public Collection<Event> findEventsByIds(Collection<UUID> eventIds) {
    return getEventRepository().findEventsByIds(eventIds);
  }

  @Override
  public Collection<Event> findEventsByTimeAndLocation(FindEventByTimeAndLocationRequest request) {
    return getEventRepository().findEventsByTimeAndLocation(request);
  }

  // QcMaskRepositoryInterface
  @Override
  public void storeQcMasks(Collection<QcMask> qcMasks) {
    getQcMaskRepository().storeQcMasks(qcMasks);
  }

  @Override
  public Collection<QcMask> findCurrentQcMasksByChannelIdAndTimeRange(
      ChannelTimeRangeRequest request) {
    return getQcMaskRepository().findCurrentQcMasksByChannelIdAndTimeRange(request);
  }

  @Override
  public Map<String, List<QcMask>> findCurrentQcMasksByChannelNamesAndTimeRange(
      ChannelsTimeRangeRequest request) {
    return getQcMaskRepository().findCurrentQcMasksByChannelNamesAndTimeRange(request);
  }

  // RawStationDataFrameRepositoryInterface
  public List<RawStationDataFrame> retrieveRawStationDataFramesByStationAndTime(
      StationTimeRangeRequest request) {
    return getRawStationDataFrameRepository()
        .retrieveRawStationDataFramesByStationAndTime(request);
  }

  @Override
  public List<RawStationDataFrame> retrieveRawStationDataFramesByTime(
      TimeRangeRequest timeRangeRequest) {
    return getRawStationDataFrameRepository()
        .retrieveRawStationDataFramesByTime(timeRangeRequest);
  }

  @Override
  public void storeRawStationDataFrames(Collection<RawStationDataFrame> frames) {
    getRawStationDataFrameRepository().storeRawStationDataFrames(frames);
  }

  // SignalDetectionRepositoryInterface
  @Override
  public void storeSignalDetections(Collection<SignalDetection> signalDetections) {
    getSignalDetectionRepository().storeSignalDetections(signalDetections);
  }

  @Override
  public List<SignalDetection> findSignalDetectionsByIds(Collection<UUID> ids) {
    return getSignalDetectionRepository().findSignalDetectionsByIds(ids);
  }

  @Override
  public List<SignalDetection> findSignalDetectionsByStationAndTime(StationsTimeRangeRequest request) {
    return getSignalDetectionRepository().findSignalDetectionsByStationAndTime(request);
  }

  @Override
  public void storeSignalDetectionHypotheses(Collection<SignalDetectionHypothesis> signalDetectionHypotheses) {
    getSignalDetectionRepository().storeSignalDetectionHypotheses(signalDetectionHypotheses);
  }

  // Reference Channel
  @Override
  public List<ReferenceChannel> retrieveReferenceChannels(
      ReferenceChannelRequest referenceChannelRequest) {
    return getReferenceChannelRepository().retrieveReferenceChannels(referenceChannelRequest);
  }

  @Override
  public void storeReferenceChannels(Collection<ReferenceChannel> channels) {
    getReferenceChannelRepository().storeReferenceChannels(channels);
  }

  // ReferenceSensor
  @Override
  public List<ReferenceSensor> retrieveReferenceSensorsById(Collection<UUID> sensorIds) {
    return getReferenceSensorRepository().retrieveReferenceSensorsById(sensorIds);
  }

  @Override
  public Map<String, List<ReferenceSensor>> retrieveSensorsByChannelName(
      Collection<String> channelNames) {
    return getReferenceSensorRepository().retrieveSensorsByChannelName(channelNames);
  }

  @Override
  public void storeReferenceSensors(Collection<ReferenceSensor> sensors) {
    getReferenceSensorRepository().storeReferenceSensors(sensors);
  }

  // ReferenceNetwork
  @Override
  public List<ReferenceNetwork> retrieveNetworks(Collection<UUID> networkIds) {
    return getReferenceNetworkRepository().retrieveNetworks(networkIds);
  }

  @Override
  public List<ReferenceNetwork> retrieveNetworksByName(List<String> names) {
    return getReferenceNetworkRepository().retrieveNetworksByName(names);
  }

  @Override
  public void storeReferenceNetwork(Collection<ReferenceNetwork> networks) {
    getReferenceNetworkRepository().storeReferenceNetwork(networks);
  }

  @Override
  public Map<UUID, List<ReferenceNetworkMembership>> retrieveNetworkMembershipsByNetworkId(
      Collection<UUID> networkIds) {
    return getReferenceNetworkRepository().retrieveNetworkMembershipsByNetworkId(networkIds);
  }

  @Override
  public Map<UUID, List<ReferenceNetworkMembership>> retrieveNetworkMembershipsByStationId(
      Collection<UUID> stationIds) {
    return getReferenceNetworkRepository().retrieveNetworkMembershipsByStationId(stationIds);
  }

  @Override
  public List<ReferenceNetworkMembership> retrieveNetworkMembershipsByNetworkAndStationId(
      NetworkMembershipRequest request) {
    return getReferenceNetworkRepository()
        .retrieveNetworkMembershipsByNetworkAndStationId(request);
  }

  @Override
  public void storeNetworkMemberships(
      Collection<ReferenceNetworkMembership> networkMemberships) {
    getReferenceNetworkRepository().storeNetworkMemberships(networkMemberships);
  }

  // ReferenceResponse
  @Override
  public List<ReferenceResponse> retrieveReferenceResponses(Collection<String> channelNames) {
    return getReferenceResponseRepository().retrieveReferenceResponses(channelNames);
  }

  @Override
  public void storeReferenceResponses(Collection<ReferenceResponse> referenceResponses) {
    getReferenceResponseRepository().storeReferenceResponses(referenceResponses);
  }

  @Override
  public List<ReferenceSite> retrieveSites(List<UUID> entityIds) {
    return getReferenceSiteRepository().retrieveSites(entityIds);
  }

  @Override
  public List<ReferenceSite> retrieveSitesByName(List<String> names) {
    return getReferenceSiteRepository().retrieveSitesByName(names);
  }

  @Override
  public void storeReferenceSites(Collection<ReferenceSite> sites) {
    getReferenceSiteRepository().storeReferenceSites(sites);
  }

  @Override
  public Map<UUID, List<ReferenceSiteMembership>> retrieveSiteMembershipsBySiteId(
      List<UUID> siteIds) {
    return getReferenceSiteRepository().retrieveSiteMembershipsBySiteId(siteIds);
  }

  @Override
  public Map<String, List<ReferenceSiteMembership>> retrieveSiteMembershipsByChannelNames(
      List<String> channelNames) {
    return getReferenceSiteRepository().retrieveSiteMembershipsByChannelNames(channelNames);
  }

  @Override
  public List<ReferenceSiteMembership> retrieveSiteMembershipsBySiteIdAndChannelName(
      ReferenceSiteMembershipRequest request) {
    return getReferenceSiteRepository().
        retrieveSiteMembershipsBySiteIdAndChannelName(request);
  }

  @Override
  public void storeSiteMemberships(Collection<ReferenceSiteMembership> siteMemberships) {
    getReferenceSiteRepository().storeSiteMemberships(siteMemberships);
  }

  // ReferenceStation
  @Override
  public List<ReferenceStation> retrieveStations(List<UUID> entityIds) {
    return getReferenceStationRepository().retrieveStations(entityIds);
  }

  @Override
  public List<ReferenceStation> retrieveStationsByVersionIds(
      Collection<UUID> stationVersionIds) {
    return getReferenceStationRepository().retrieveStationsByVersionIds(stationVersionIds);
  }

  @Override
  public List<ReferenceStation> retrieveStationsByName(List<String> names) {
    return getReferenceStationRepository().retrieveStationsByName(names);
  }

  @Override
  public void storeReferenceStation(Collection<ReferenceStation> stations) {
    getReferenceStationRepository().storeReferenceStation(stations);
  }

  @Override
  public Map<UUID, List<ReferenceStationMembership>> retrieveStationMemberships(List<UUID> ids) {
    return getReferenceStationRepository().retrieveStationMemberships(ids);
  }

  @Override
  public Map<UUID, List<ReferenceStationMembership>> retrieveStationMembershipsByStationId(
      List<UUID> stationIds) {
    return getReferenceStationRepository().retrieveStationMembershipsByStationId(stationIds);
  }

  @Override
  public Map<UUID, List<ReferenceStationMembership>> retrieveStationMembershipsBySiteId(
      List<UUID> siteIds) {
    return getReferenceStationRepository().retrieveStationMembershipsBySiteId(siteIds);
  }

  @Override
  public List<ReferenceStationMembership> retrieveStationMembershipsByStationAndSiteId(
      ReferenceStationMembershipRequest request) {
    return getReferenceStationRepository().retrieveStationMembershipsByStationAndSiteId(request);
  }

  @Override
  public void storeStationMemberships(
      Collection<ReferenceStationMembership> stationMemberships) {
    getReferenceStationRepository().storeStationMemberships(stationMemberships);
  }

  // ResponseRepositoryInterface


  @Override
  public Map<String, Response> retrieveResponsesByChannels(Set<String> channelNames) {
    return getResponseRepository().retrieveResponsesByChannels(channelNames);
  }

  @Override
  public void storeResponses(Collection<Response> responses) {
    getResponseRepository().storeResponses(responses);
  }

  // StationGroupRepositoryInterface
  @Override
  public List<StationGroup> retrieveStationGroups(
      Collection<String> stationGroupNames) {
    return getStationGroupRepository().retrieveStationGroups(stationGroupNames);
  }

  @Override
  public void storeStationGroups(
      Collection<StationGroup> stationGroups) {
    getStationGroupRepository().storeStationGroups(stationGroups);
  }

  @Override
  public void updateStationGroups(Collection<StationGroupDefinition> stationGroupDefinitions) {
    getStationGroupRepository().updateStationGroups(stationGroupDefinitions);
  }

  // StationRepositoryInterface
  @Override
  public List<Station> retrieveAllStations(Collection<String> stationNames) {
    return getStationRepository().retrieveAllStations(stationNames);
  }

  @Override
  public void storeStations(Collection<Station> stations) {
    getStationRepository().storeStations(stations);
  }

  // UserRepositoryInterface
  public Optional<UserPreferences> getUserPreferencesByUserId(String userId) {
    return getUserPreferencesRepository().getUserPreferencesByUserId(userId);
  }

  @Override
  public void setUserPreferences(UserPreferences userPreferences) {
    getUserPreferencesRepository().setUserPreferences(userPreferences);
  }
}
