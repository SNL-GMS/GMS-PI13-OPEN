package gms.shared.frameworks.soh.repository;

import com.google.auto.value.AutoValue;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
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
import gms.shared.frameworks.osd.api.stationreference.util.NetworkMembershipRequest;
import gms.shared.frameworks.osd.api.stationreference.util.ReferenceSiteMembershipRequest;
import gms.shared.frameworks.osd.api.stationreference.util.ReferenceStationMembershipRequest;
import gms.shared.frameworks.osd.api.statuschange.SohStatusChangeRepositoryInterface;
import gms.shared.frameworks.osd.api.systemmessage.SystemMessageRepositoryInterface;
import gms.shared.frameworks.osd.api.transferredfile.TransferredFileRepositoryInterface;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeSohTypeRequest;
import gms.shared.frameworks.osd.api.util.HistoricalStationSohRequest;
import gms.shared.frameworks.osd.api.util.ReferenceChannelRequest;
import gms.shared.frameworks.osd.api.util.StationTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.StationTimeRangeSohTypeRequest;
import gms.shared.frameworks.osd.api.util.StationsTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.api.waveforms.RawStationDataFrameRepositoryInterface;
import gms.shared.frameworks.osd.api.waveforms.RawStationDataFrameRepositoryQueryInterface;
import gms.shared.frameworks.osd.api.waveforms.StationSohRepositoryInterface;
import gms.shared.frameworks.osd.api.waveforms.StationSohRepositoryQueryViewInterface;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ReferenceChannel;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFile;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.soh.quieting.QuietedSohStatusChange;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import gms.shared.frameworks.osd.dto.soh.HistoricalAcquiredChannelEnvironmentalIssues;
import gms.shared.frameworks.osd.coi.preferences.UserPreferences;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroupDefinition;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.soh.quieting.UnacknowledgedSohStatusChange;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceNetwork;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceNetworkMembership;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceResponse;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSensor;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSite;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSiteMembership;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceStation;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceStationMembership;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.osd.dto.soh.HistoricalStationSoh;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@AutoValue
public abstract class SohRepository implements SohRepositoryInterface {
  public abstract CapabilitySohRollupRepositoryInterface getCapabilitySohRollupRepository();

  public abstract ChannelRepositoryInterface getChannelRepository();

  public abstract PerformanceMonitoringRepositoryInterface getPerformanceMonitoringRepository();

  public abstract RawStationDataFrameRepositoryInterface getRawStationDataFrameRepository();

  public abstract RawStationDataFrameRepositoryQueryInterface getRawStationDataFrameQueryRepository();

  public abstract ReferenceChannelRepositoryInterface getReferenceChannelRepository();

  public abstract ReferenceNetworkRepositoryInterface getReferenceNetworkRepository();

  public abstract ReferenceResponseRepositoryInterface getReferenceResponseRepository();

  public abstract ReferenceSensorRepositoryInterface getReferenceSensorRepository();

  public abstract ReferenceSiteRepositoryInterface getReferenceSiteRepository();

  public abstract ReferenceStationRepositoryInterface getReferenceStationRepository();

  public abstract SohStatusChangeRepositoryInterface getSohStatusChangeRepository();

  public abstract StationGroupRepositoryInterface getStationGroupRepository();

  public abstract StationRepositoryInterface getStationRepository();

  public abstract StationSohRepositoryInterface getStationSohRepository();

  public abstract StationSohRepositoryQueryViewInterface getStationSohQueryViewRepository();

  public abstract SystemMessageRepositoryInterface getSystemMessageRepository();

  public abstract TransferredFileRepositoryInterface getTransferredFileRepository();

  public abstract UserPreferencesRepositoryInterface getUserPreferencesRepository();

  public static SohRepository from(
      CapabilitySohRollupRepositoryInterface capabilitySohRollupRepository,
      ChannelRepositoryInterface channelRepository,
      PerformanceMonitoringRepositoryInterface performanceMonitoringRepository,
      RawStationDataFrameRepositoryInterface rawStationDataFrameRepository,
      RawStationDataFrameRepositoryQueryInterface rawStationDataFrameQueryRepository,
      ReferenceChannelRepositoryInterface referenceChannelRepository,
      ReferenceNetworkRepositoryInterface referenceNetworkRepository,
      ReferenceResponseRepositoryInterface referenceResponseRepository,
      ReferenceSensorRepositoryInterface referenceSensorRepository,
      ReferenceSiteRepositoryInterface referenceSiteRepository,
      ReferenceStationRepositoryInterface referenceStationRepository,
      SohStatusChangeRepositoryInterface sohStatusChangeRepository,
      StationGroupRepositoryInterface stationGroupRepository,
      StationRepositoryInterface stationRepository,
      StationSohRepositoryInterface stationSohRepository,
      StationSohRepositoryQueryViewInterface stationSohQueryViewRepository,
      SystemMessageRepositoryInterface systemMessageRepository,
      TransferredFileRepositoryInterface transferredFileRepository,
      UserPreferencesRepositoryInterface userPreferencesRepository) {
    return new AutoValue_SohRepository.Builder()
        .setCapabilitySohRollupRepository(capabilitySohRollupRepository)
        .setChannelRepository(channelRepository)
        .setPerformanceMonitoringRepository(performanceMonitoringRepository)
        .setRawStationDataFrameRepository(rawStationDataFrameRepository)
        .setRawStationDataFrameQueryRepository(rawStationDataFrameQueryRepository)
        .setReferenceChannelRepository(referenceChannelRepository)
        .setReferenceNetworkRepository(referenceNetworkRepository)
        .setReferenceResponseRepository(referenceResponseRepository)
        .setReferenceSensorRepository(referenceSensorRepository)
        .setReferenceSiteRepository(referenceSiteRepository)
        .setReferenceStationRepository(referenceStationRepository)
        .setSohStatusChangeRepository(sohStatusChangeRepository)
        .setStationGroupRepository(stationGroupRepository)
        .setStationSohRepository(stationSohRepository)
        .setStationSohQueryViewRepository(stationSohQueryViewRepository)
        .setStationRepository(stationRepository)
        .setSystemMessageRepository(systemMessageRepository)
        .setTransferredFileRepository(transferredFileRepository)
        .setRawStationDataFrameRepository(rawStationDataFrameRepository)
        .setUserPreferencesRepository(userPreferencesRepository)
        .build();
  }

  public abstract Builder builder();

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder setCapabilitySohRollupRepository(CapabilitySohRollupRepositoryInterface i);

    public abstract Builder setChannelRepository(ChannelRepositoryInterface i);

    public abstract Builder setPerformanceMonitoringRepository(
        PerformanceMonitoringRepositoryInterface i);

    public abstract Builder setRawStationDataFrameRepository(
        RawStationDataFrameRepositoryInterface i);

    public abstract Builder setRawStationDataFrameQueryRepository(
        RawStationDataFrameRepositoryQueryInterface i);

    public abstract Builder setSohStatusChangeRepository(SohStatusChangeRepositoryInterface i);

    public abstract Builder setStationGroupRepository(StationGroupRepositoryInterface i);

    public abstract Builder setStationRepository(StationRepositoryInterface i);

    public abstract Builder setStationSohRepository(StationSohRepositoryInterface i);

    public abstract Builder setStationSohQueryViewRepository(StationSohRepositoryQueryViewInterface i);

    public abstract Builder setSystemMessageRepository(SystemMessageRepositoryInterface i);

    public abstract Builder setTransferredFileRepository(TransferredFileRepositoryInterface i);

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

    public abstract SohRepository build();
  }

  @Override
  public List<CapabilitySohRollup> retrieveCapabilitySohRollupByStationGroup(
      Collection<String> stationGroups) {
    return getCapabilitySohRollupRepository()
        .retrieveCapabilitySohRollupByStationGroup(stationGroups);
  }

  @Override
  public void storeCapabilitySohRollup(Collection<CapabilitySohRollup> capabilitySohRollups) {
    getCapabilitySohRollupRepository()
        .storeCapabilitySohRollup((capabilitySohRollups));
  }

  @Override
  public List<CapabilitySohRollup> retrieveLatestCapabilitySohRollupByStationGroup(
      Collection<String> stationGroupNames) {
    return getCapabilitySohRollupRepository()
        .retrieveLatestCapabilitySohRollupByStationGroup(stationGroupNames);
  }

  @Override
  public List<Channel> retrieveChannels(Collection<String> channelIds) {
    return getChannelRepository().retrieveChannels(channelIds);
  }

  @Override
  public Set<String> storeChannels(Collection<Channel> channels) {
    return getChannelRepository().storeChannels(channels);
  }

  @Override
  public List<StationSoh> retrieveByStationId(List<String> stationNames) {
    return getPerformanceMonitoringRepository().retrieveByStationId(stationNames);
  }

  @Override
  public List<StationSoh> retrieveByStationsAndTimeRange(StationsTimeRangeRequest stationsTimeRangeRequest) {
    return getPerformanceMonitoringRepository().retrieveByStationsAndTimeRange(stationsTimeRangeRequest);
  }

  @Override
  public List<UUID> storeStationSoh(Collection<StationSoh> stationSohs) {
    return getPerformanceMonitoringRepository().storeStationSoh(stationSohs);
  }

  @Override
  public HistoricalStationSoh retrieveHistoricalStationSoh(HistoricalStationSohRequest request) {
    return getPerformanceMonitoringRepository().retrieveHistoricalStationSoh(request);
  }

  @Override
  public Optional<UserPreferences> getUserPreferencesByUserId(String userId) {
    return getUserPreferencesRepository().getUserPreferencesByUserId(userId);
  }

  @Override
  public void setUserPreferences(UserPreferences userPreferences) {
    getUserPreferencesRepository().setUserPreferences(userPreferences);
  }

  @Override
  public List<StationGroup> retrieveStationGroups(Collection<String> stationGroupNames) {
    return getStationGroupRepository().retrieveStationGroups(stationGroupNames);
  }

  @Override
  public void storeStationGroups(Collection<StationGroup> stationGroups) {
    getStationGroupRepository().storeStationGroups(stationGroups);
  }

  @Override
  public void updateStationGroups(Collection<StationGroupDefinition> stationGroupDefinitions) {
    getStationGroupRepository().updateStationGroups(stationGroupDefinitions);
  }

  @Override
  public List<Station> retrieveAllStations(Collection<String> stationNames) {
    return getStationRepository().retrieveAllStations(stationNames);
  }

  @Override
  public void storeStations(Collection<Station> stations) {
    getStationRepository().storeStations(stations);
  }

  @Override
  public List<ReferenceChannel> retrieveReferenceChannels(
      ReferenceChannelRequest referenceChannelRequest) {
    return getReferenceChannelRepository().retrieveReferenceChannels(referenceChannelRequest);
  }

  @Override
  public void storeReferenceChannels(Collection<ReferenceChannel> channels) {
    getReferenceChannelRepository().storeReferenceChannels(channels);
  }

  @Override
  public List<ReferenceNetwork> retrieveNetworks(Collection<UUID> networkIds) {
    return getReferenceNetworkRepository().retrieveNetworks(networkIds);
  }

  @Override
  public List<ReferenceNetwork> retrieveNetworksByName(List<String> names) {
    return getReferenceNetworkRepository().retrieveNetworksByName(names);
  }

  @Override
  public void storeReferenceNetwork(Collection<ReferenceNetwork> network) {
    getReferenceNetworkRepository().storeReferenceNetwork(network);
  }

  @Override
  public Map<UUID, List<ReferenceNetworkMembership>> retrieveNetworkMembershipsByNetworkId(
      Collection<UUID> networkIds) {
    return getReferenceNetworkRepository().retrieveNetworkMembershipsByNetworkId(networkIds);
  }

  @Override
  public Map<UUID, List<ReferenceNetworkMembership>> retrieveNetworkMembershipsByStationId(
      Collection<UUID> referenceStationIds) {
    return getReferenceNetworkRepository().retrieveNetworkMembershipsByStationId(referenceStationIds);
  }

  @Override
  public List<ReferenceNetworkMembership> retrieveNetworkMembershipsByNetworkAndStationId(
      NetworkMembershipRequest request) {
    return getReferenceNetworkRepository().retrieveNetworkMembershipsByNetworkAndStationId(request);
  }

  @Override
  public void storeNetworkMemberships(Collection<ReferenceNetworkMembership> memberships) {
    getReferenceNetworkRepository().storeNetworkMemberships(memberships);
  }

  @Override
  public List<ReferenceResponse> retrieveReferenceResponses(Collection<String> channelNames) {
    return getReferenceResponseRepository().retrieveReferenceResponses(channelNames);
  }

  @Override
  public void storeReferenceResponses(Collection<ReferenceResponse> referenceResponses) {
    getReferenceResponseRepository().storeReferenceResponses(referenceResponses);
  }

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
    return getReferenceSiteRepository().retrieveSiteMembershipsBySiteIdAndChannelName(request);
  }

  @Override
  public void storeSiteMemberships(Collection<ReferenceSiteMembership> memberships) {
    getReferenceSiteRepository().storeSiteMemberships(memberships);
  }

  @Override
  public List<ReferenceStation> retrieveStations(List<UUID> entityIds) {
    return getReferenceStationRepository().retrieveStations(entityIds);
  }

  @Override
  public List<ReferenceStation> retrieveStationsByVersionIds(Collection<UUID> stationVersionIds) {
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
  public void storeStationMemberships(Collection<ReferenceStationMembership> memberships) {
    getReferenceStationRepository().storeStationMemberships(memberships);
  }

  @Override
  public List<String> storeTransferredFiles(Collection<TransferredFile<?>> transferredFiles) {
    return getTransferredFileRepository().storeTransferredFiles(transferredFiles);
  }

  @Override
  public void removeSentAndReceived(Duration olderThan) {
    getTransferredFileRepository().removeSentAndReceived(olderThan);
  }

  @Override
  public List<TransferredFile> retrieveAllTransferredFiles(Collection<String> filenames) {
    return getTransferredFileRepository().retrieveAllTransferredFiles(filenames);
  }

  @Override
  public List<TransferredFile> retrieveByTransferTime(TimeRangeRequest request) {
    return getTransferredFileRepository().retrieveByTransferTime(request);
  }

  @Override
  public <T extends TransferredFile> Optional<TransferredFile> find(T file) {
    return getTransferredFileRepository().find(file);
  }

  @Override
  public List<RawStationDataFrame> retrieveRawStationDataFramesByStationAndTime(
      StationTimeRangeRequest request) {
    return getRawStationDataFrameRepository().retrieveRawStationDataFramesByStationAndTime(request);
  }

  @Override
  public List<RawStationDataFrame> retrieveRawStationDataFramesByTime(TimeRangeRequest request) {
    return getRawStationDataFrameRepository().retrieveRawStationDataFramesByTime(request);
  }

  @Override
  public void storeRawStationDataFrames(Collection<RawStationDataFrame> frames) {
    getRawStationDataFrameRepository().storeRawStationDataFrames(frames);
  }

  /**
   * Retrieves all {@link RawStationDataFrameMetadata}that have data in the specified time range.
   *
   * @param request The {@link TimeRangeRequest}
   * @return
   */
  @Override
  public List<RawStationDataFrameMetadata> retrieveRawStationDataFrameMetadataByTime(TimeRangeRequest request) {
    return getRawStationDataFrameQueryRepository().retrieveRawStationDataFrameMetadataByTime(request);
  }

  /**
   * Retrieves the latest sample times for the provided channels.
   *
   * @param channelNames The {@link List} of channel names to get latest sample times
   * @return A {@link Map} of the channel time to it's latest sample time
   */
  @Override
  public Map<String, Instant> retrieveLatestSampleTimeByChannel(List<String> channelNames) {
    return getRawStationDataFrameQueryRepository().retrieveLatestSampleTimeByChannel(channelNames);
  }

  @Override
  public void storeAcquiredChannelSohAnalog(
      Collection<AcquiredChannelEnvironmentIssueAnalog> acquiredChannelSohAnalogs) {
    getStationSohRepository().storeAcquiredChannelSohAnalog(acquiredChannelSohAnalogs);
  }

  @Override
  public void storeAcquiredChannelEnvironmentIssueBoolean(
      Collection<AcquiredChannelEnvironmentIssueBoolean> acquiredChannelSohBooleans) {
    getStationSohRepository().storeAcquiredChannelEnvironmentIssueBoolean(acquiredChannelSohBooleans);
  }

  @Override
  public void removeAcquiredChannelEnvironmentIssueBooleans(
      Collection<AcquiredChannelEnvironmentIssueBoolean> aceiBooleans) {
    getStationSohRepository().removeAcquiredChannelEnvironmentIssueBooleans(aceiBooleans);
  }

  @Override
  public List<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogByChannelAndTimeRange(
      ChannelTimeRangeRequest request) {
    return getStationSohRepository().retrieveAcquiredChannelEnvironmentIssueAnalogByChannelAndTimeRange(request);
  }

  @Override
  public List<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelEnvironmentIssueBooleanByChannelAndTimeRange(
      ChannelTimeRangeRequest request) {
    return getStationSohRepository().retrieveAcquiredChannelEnvironmentIssueBooleanByChannelAndTimeRange(request);
  }

  @Override
  public Optional<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogById(
      UUID acquiredChannelEnvironmentIssueId) {
    return getStationSohRepository().retrieveAcquiredChannelEnvironmentIssueAnalogById(
        acquiredChannelEnvironmentIssueId);
  }

  @Override
  public Optional<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelEnvironmentIssueBooleanById(
      UUID acquiredChannelEnvironmentIssueId) {
    return getStationSohRepository().retrieveAcquiredChannelEnvironmentIssueBooleanById(
        acquiredChannelEnvironmentIssueId);
  }

  @Override
  public List<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogByChannelTimeRangeAndType(
      ChannelTimeRangeSohTypeRequest request) {
    return getStationSohRepository().retrieveAcquiredChannelEnvironmentIssueAnalogByChannelTimeRangeAndType(request);
  }

  @Override
  public List<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelSohBooleanByChannelTimeRangeAndType(
      ChannelTimeRangeSohTypeRequest request) {
    return getStationSohRepository().retrieveAcquiredChannelSohBooleanByChannelTimeRangeAndType(request);
  }

  @Override
  public List<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogByTime(
      TimeRangeRequest request) {
    return getStationSohRepository().retrieveAcquiredChannelEnvironmentIssueAnalogByTime(request);
  }

  @Override
  public List<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelEnvironmentIssueBooleanByTime(
      TimeRangeRequest request) {
    return getStationSohRepository().retrieveAcquiredChannelEnvironmentIssueBooleanByTime(request);
  }

  @Override
  public List<AcquiredChannelEnvironmentIssueAnalog> retrieveLatestAcquiredChannelEnvironmentIssueAnalog(
      List<String> channelNames) {
    return getStationSohRepository().retrieveLatestAcquiredChannelEnvironmentIssueAnalog(channelNames);
  }

  @Override
  public List<AcquiredChannelEnvironmentIssueBoolean> retrieveLatestAcquiredChannelEnvironmentIssueBoolean(
      List<String> channelNames) {
    return getStationSohRepository().retrieveLatestAcquiredChannelEnvironmentIssueBoolean(channelNames);
  }

  @Override
  public List<UnacknowledgedSohStatusChange> retrieveUnacknowledgedSohStatusChanges(Collection<String> stationNames) {
    return getSohStatusChangeRepository().retrieveUnacknowledgedSohStatusChanges(stationNames);
  }

  @Override
  public void storeUnacknowledgedSohStatusChange(Collection<UnacknowledgedSohStatusChange> unackStatusChanges) {
    getSohStatusChangeRepository().storeUnacknowledgedSohStatusChange(unackStatusChanges);
  }

  @Override
  public void storeQuietedSohStatusChangeList(Collection<QuietedSohStatusChange> quietedSohStatusChangeList) {
    getSohStatusChangeRepository().storeQuietedSohStatusChangeList(quietedSohStatusChangeList);
  }

  @Override
  public List<HistoricalAcquiredChannelEnvironmentalIssues> retrieveAcquiredChannelEnvironmentIssuesByStationTimeRangeAndType(
      StationTimeRangeSohTypeRequest request) {
    return getStationSohQueryViewRepository()
        .retrieveAcquiredChannelEnvironmentIssuesByStationTimeRangeAndType(request);
  }

  @Override
  public Collection<QuietedSohStatusChange> retrieveQuietedSohStatusChangesByTime(Instant currentTime) {
    return getSohStatusChangeRepository().retrieveQuietedSohStatusChangesByTime(currentTime);
  }

  @Override
  public void storeSystemMessages(Collection<SystemMessage> systemMessages) {
    getSystemMessageRepository().storeSystemMessages(systemMessages);
  }
}
