package gms.dataacquisition.css.stationrefconverter;

import static java.util.stream.Collectors.toList;

import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelFactory;
import gms.shared.frameworks.osd.coi.channel.ChannelGroup;
import gms.shared.frameworks.osd.coi.channel.ChannelGroup.Type;
import gms.shared.frameworks.osd.coi.channel.ReferenceChannel;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import gms.shared.frameworks.osd.coi.signaldetection.Response;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceNetwork;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceNetworkMembership;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceResponse;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSite;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSiteMembership;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceStation;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceStationMembership;
import gms.shared.frameworks.osd.coi.stationreference.RelativePosition;
import gms.shared.frameworks.osd.coi.stationreference.StatusType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StationGroupBuilder {

  private static final Logger logger = LoggerFactory.getLogger(StationGroupBuilder.class);
  private final Set<ReferenceNetworkMembership> referenceNetworkMemberships;
  private final Set<ReferenceStationMembership> referenceStationMemberships;
  private final Set<ReferenceSiteMembership> referenceSiteMemberships;
  private final Collection<ReferenceNetwork> referenceNetworks;
  private final Collection<ReferenceStation> referenceStations;
  private final Collection<ReferenceSite> referenceSites;
  private final Collection<ReferenceChannel> referenceChannels;
  private final Collection<ReferenceResponse> referenceResponses;

  public StationGroupBuilder(
      Set<ReferenceNetworkMembership> referenceNetworkMemberships,
      Set<ReferenceStationMembership> referenceStationMemberships,
      Set<ReferenceSiteMembership> referenceSiteMemberships,
      Collection<ReferenceNetwork> referenceNetworks,
      Collection<ReferenceStation> referenceStations,
      Collection<ReferenceSite> referenceSites,
      Collection<ReferenceChannel> referenceChannels,
      Collection<ReferenceResponse> referenceResponses) {
    this.referenceNetworkMemberships = Objects.requireNonNull(referenceNetworkMemberships);
    this.referenceStationMemberships = Objects.requireNonNull(referenceStationMemberships);
    this.referenceSiteMemberships = Objects.requireNonNull(referenceSiteMemberships);
    this.referenceNetworks = Objects.requireNonNull(referenceNetworks);
    this.referenceStations = Objects.requireNonNull(referenceStations);
    this.referenceSites = Objects.requireNonNull(referenceSites);
    this.referenceChannels = Objects.requireNonNull(referenceChannels);
    this.referenceResponses = Objects.requireNonNull(referenceResponses);
  }

  Pair<Set<StationGroup>, Set<Response>> createStationGroupsAndResponses() {
    final Set<Response> responses = new HashSet<>();
    final Map<String, ReferenceResponse> refResponseByChan = latestRefResponseByChannel(
        this.referenceResponses);
    // Now build the StationGroup on down using the existing
    // reference entries (i.e. network, station, site and channels)
    Set<StationGroup> stationGroups = new HashSet<>();

    /*Build up the reference objects (network, station, site and channels) to then convert to
      each entry into the corresponding "processing" object
      (station group, station, channel group and channel)
     */

    // For each reference network lookup station references
    for (ReferenceNetwork network : this.referenceNetworks) {
      // Find all the reference stations associated to the network
      Collection<ReferenceNetworkMembership> networkMemberships =
          this.getLatestNetworkMemberships(network);
      // But limit it down to the active, most recent stations only
      Collection<ReferenceStation> refStations = getActiveReferenceStations(networkMemberships);

      // Walk the reference stations for each network getting back the site list for each station
      List<Station> stations = new ArrayList<>();
      for (ReferenceStation referenceStation : refStations) {
        Map<String, RelativePosition> relativePositionsByChannel = new HashMap<>();
        List<Channel> stationChannels = new ArrayList<>();
        Collection<ReferenceStationMembership> stationMemberships =
            this.getLatestStationMemberships(referenceStation);
        Collection<ReferenceSite> refSites = this.getActiveReferenceSites(stationMemberships);

        // Walk each site to get back a list of reference channels
        List<ChannelGroup> stationChannelGroups = new ArrayList<>();
        for (ReferenceSite referenceSite : refSites) {
          Collection<ReferenceSiteMembership> siteMemberships =
              this.getLatestSiteMemberships(referenceSite);
          Collection<ReferenceChannel> refChannels = this
              .getActiveReferenceChannels(siteMemberships);

          // If no reference channels are found warn and skip adding channel group
          if (refChannels.isEmpty()) {
            logger.warn("No reference channels found for reference site: {}",
                referenceSite.getName());
            continue;
          }

          // Convert the Reference Channels for each site to "processing channels"
          Pair<List<Channel>, List<Response>> channelsAndResponses
              = this.createChannelsAndResponses(refChannels, refResponseByChan,
              referenceStation.getName(), referenceSite.getName());

          final List<Channel> channelGroupChannels = channelsAndResponses.getLeft();
          responses.addAll(channelsAndResponses.getRight());

          // Walk the channels to add the Reference Site relative position for each channel
          // to the station's relativePositionsByChannel map
          for (Channel channel : channelGroupChannels) {
            relativePositionsByChannel.put(channel.getName(), referenceSite.getPosition());
          }

          // Add each channel groups channels to station channel list
          stationChannels.addAll(channelGroupChannels);

          makeChannelGroup(referenceSite, channelGroupChannels)
              .ifPresent(stationChannelGroups::add);
        }

        makeStation(referenceStation, stationChannelGroups, stationChannels,
            relativePositionsByChannel)
            .ifPresent(stations::add);
      }
      makeStationGroup(network.getName(), network.getDescription(), stations)
          .ifPresent(stationGroups::add);
    }
    return Pair.of(stationGroups, responses);
  }

  private static Optional<ChannelGroup> makeChannelGroup(ReferenceSite refSite,
      final List<Channel> channels) {

    if (channels.isEmpty()) {
      logger.warn("Failed to create station group for site {} (no channels found)",
          refSite.getName());
      return Optional.empty();
    }
    return Optional.of(ChannelGroup.from(
        refSite.getName(),
        refSite.getDescription(),
        Location.from(
            refSite.getLatitude(),
            refSite.getLongitude(),
            0, // Depth
            refSite.getElevation()),
        Type.SITE_GROUP,
        channels));
  }

  private static Optional<Station> makeStation(ReferenceStation refSta,
      List<ChannelGroup> stationChannelGroups,
      List<Channel> stationChannels,
      Map<String, RelativePosition> relativePositionsByChannel) {

    if (stationChannels.isEmpty()) {
      logger.warn("Failed to add Station {} (no channels found)", refSta.getName());
      return Optional.empty();
    }
    return Optional.of(Station.from(
        refSta.getName(),
        refSta.getStationType(),
        refSta.getDescription(),
        relativePositionsByChannel,
        Location.from(
            refSta.getLatitude(),
            refSta.getLongitude(),
            0, // Depth
            refSta.getElevation()),
        stationChannelGroups,
        stationChannels));
  }

  private static Optional<StationGroup> makeStationGroup(String name, String description,
      List<Station> stations) {

    if (stations.isEmpty()) {
      logger.warn("Failed to add StationGroup name={}, description={}... no stations found",
          name, description);
      return Optional.empty();
    }
    return Optional.of(StationGroup.from(name, description, stations));
  }

  Pair<List<Channel>, List<Response>> createChannelsAndResponses(
      Collection<ReferenceChannel> referenceChannels,
      Map<String, ReferenceResponse> referenceResponsesByChan,
      String stationName, String siteName) {

    Map<String, List<ReferenceChannel>> referenceChannelsByName = referenceChannels.stream()
        .collect(Collectors.groupingBy(ReferenceChannel::getName));

    final List<Channel> processingChans = new ArrayList<>();
    final List<Response> processingResponses = new ArrayList<>();

    for (Map.Entry<String, List<ReferenceChannel>> e : referenceChannelsByName.entrySet()) {
      final ReferenceChannel latestChan = getLatestOrThrow(e.getValue(),
          ReferenceChannel::getActualTime);
      final Channel chan = ChannelFactory
          .rawFromReferenceChannel(latestChan, stationName, siteName);
      processingChans.add(chan);
      if (referenceResponsesByChan.containsKey(e.getKey())) {
        final ReferenceResponse refResponse = referenceResponsesByChan.get(e.getKey());
        processingResponses.add(Response.from(chan.getName(),
            refResponse.getReferenceCalibration().getCalibration(),
            refResponse.getFapResponse().orElse(null)));
      } else {
        logger.warn("Could not find response for raw channel {}", e.getKey());
      }
    }
    return Pair.of(processingChans, processingResponses);
  }

  private Collection<ReferenceNetworkMembership> getLatestNetworkMemberships(
      ReferenceNetwork referenceNetwork) {
    // Find the latest active list of network memberships
    return this.referenceNetworkMemberships
        .stream()
        .filter(referenceNetworkMembership -> referenceNetworkMembership.getNetworkId()
            .equals(referenceNetwork.getEntityId()))
        .collect(Collectors.groupingBy(ReferenceNetworkMembership::getStationId))
        .values().stream()
        .map(List::stream)
        .map(membershipStream -> membershipStream
            .max(Comparator.comparing(ReferenceNetworkMembership::getActualChangeTime)))
        .flatMap(Optional::stream)
        //.filter(membership -> membership.getStatus().equals(StatusType.ACTIVE))
        // the filtering of active station/sites was commented out since for loading historical
        // data there are channels
        // that were turned off that may be desired
        .filter(membership -> membership.getStatus() == StatusType.ACTIVE)
        .collect(toList());
  }

  private Collection<ReferenceStationMembership> getLatestStationMemberships(
      ReferenceStation referenceStation) {
    // Find the latest active list of station memberships
    return this.referenceStationMemberships
        .stream()
        .filter(referenceStationMembership -> referenceStationMembership.getStationId()
            .equals(referenceStation.getEntityId()))
        .collect(Collectors.groupingBy(ReferenceStationMembership::getSiteId))
        .values().stream()
        .map(List::stream)
        .map(membershipStream -> membershipStream
            .max(Comparator.comparing(ReferenceStationMembership::getActualChangeTime)
                .thenComparing(ReferenceStationMembership::getStatus)))
        .flatMap(Optional::stream)
        .collect(toList());
  }

  private Collection<ReferenceSiteMembership> getLatestSiteMemberships(
      ReferenceSite referenceSite) {
    return this.referenceSiteMemberships
        .stream()
        .filter(referenceSiteMembership -> referenceSiteMembership.getSiteId()
            .equals(referenceSite.getEntityId()))
        .collect(Collectors.groupingBy(ReferenceSiteMembership::getChannelName))
        .values().stream()
        .map(List::stream)
        .map(membershipStream -> membershipStream
            .max(Comparator.comparing(ReferenceSiteMembership::getActualChangeTime)))
        .flatMap(Optional::stream)
        // .filter(membership -> membership.getStatus().equals(StatusType.ACTIVE))
        // the filtering of active station/sites was commented out since for loading historical
        // data there are channels
        // that were turned off that may be desired
        .filter(membership -> membership.getStatus() == StatusType.ACTIVE)
        .collect(toList());
  }

  private Collection<ReferenceStation> getActiveReferenceStations(
      Collection<ReferenceNetworkMembership> memberships) {
    List<ReferenceStation> activeStations = new ArrayList<>();
    for (ReferenceNetworkMembership membership : memberships) {
      boolean stationFound = false;
      for (Iterator<ReferenceStation> iterator = referenceStations.iterator();
          iterator.hasNext() && !stationFound; ) {
        ReferenceStation station = iterator.next();
        if (station.isActive() && station.getEntityId().equals(membership.getStationId())) {
          activeStations.add(station);
          stationFound = true;
        }
      }
    }

    return activeStations;
  }

  private Collection<ReferenceSite> getActiveReferenceSites(
      Collection<ReferenceStationMembership> memberships) {
    return memberships.stream()
        .map(this::getActiveSiteFor)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private Optional<ReferenceSite> getActiveSiteFor(ReferenceStationMembership membership) {
    return referenceSites.stream()
        .filter(site -> site.getEntityId().equals(membership.getSiteId()))
        .filter(ReferenceSite::isActive)
        .findAny();
  }

  private Collection<ReferenceChannel> getActiveReferenceChannels(
      Collection<ReferenceSiteMembership> memberships) {
    return memberships.stream()
        .map(this::getActiveChannelFor)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  private Optional<ReferenceChannel> getActiveChannelFor(ReferenceSiteMembership membership) {
    return referenceChannels.stream()
        .filter(channel -> channel.getName().equals(membership.getChannelName()))
        .filter(ReferenceChannel::isActive)
        .findAny();
  }

  private Map<String, ReferenceResponse> latestRefResponseByChannel(
      Collection<ReferenceResponse> responses) {
    return responses.stream().collect(Collectors.groupingBy(ReferenceResponse::getChannelName))
        .entrySet().stream().collect(Collectors.toMap(Entry::getKey,
            e -> getLatestOrThrow(e.getValue(), ReferenceResponse::getActualTime)));
  }

  private <T, C extends Comparable<? super C>> T getLatestOrThrow(
      Collection<T> coll, Function<? super T, ? extends C> keyExtractor) {
    return coll.stream().max(Comparator.comparing(keyExtractor))
        .orElseThrow(() -> new IllegalArgumentException("collection is empty"));
  }
}
