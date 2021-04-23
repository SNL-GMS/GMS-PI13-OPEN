package gms.shared.frameworks.osd.control.utils;

import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelGroup;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelConfiguredInputsDao;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelGroupDao;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.LocationDao;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.StationChannelInfoDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.StationChannelInfoKey;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.StationDao;
import gms.shared.frameworks.osd.coi.stationreference.RelativePosition;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Utility class for common querying operations for storing/retrieving stations. All operations in
 * this class are low-level, as they assume implicit transactions, but were written in a way so that
 * one can use them as part of an explicit transaction instantiated by the entityManager.
 */
@Deprecated(since = "12.4")
public class StationUtils {

  private EntityManager entityManager;
  private ChannelUtils channelUtils;

  public StationUtils(EntityManager entityManager) {
    this.entityManager = entityManager;
    this.channelUtils = new ChannelUtils(entityManager);
  }

  public StationDao storeStation(Station station) {
    StationDao stationDao = entityManager.find(StationDao.class, station.getName());
    if (stationDao == null) {
      stationDao = StationDao.from(station);
      entityManager.persist(stationDao);
    }

    // storeChannels all the channel groups and channels
    storeChannelGroups(station.getChannelGroups(), stationDao,
        station.getRelativePositionsByChannel());

    return stationDao;
  }

  public Station generateStation(StationDao stationDao, boolean retrieveDerivedChannels)
      throws IOException {
    final LocationDao location = stationDao.getLocation();
    // retrieve the channel group and channels from the given station. In some cases,
    // the channel groups will return all the channels of the given station. However, derived
    // channels may NOT need to be part of a channel group but still be associated with
    // a station. This is why we are performing two different queries to grab all channel
    // groups and all channels associated with a station.
    var channelGroupsAndRelativePositionsByChannel = retrieveChannelGroups(stationDao,
        retrieveDerivedChannels);
    var channels = channelGroupsAndRelativePositionsByChannel.getLeft().stream()
        .map(ChannelGroup::getChannels)
        .flatMap(Collection::stream)
        .distinct()
        .collect(Collectors.toList());
    return Station.from(stationDao.getName(),
        stationDao.getType(),
        stationDao.getDescription(),
        channelGroupsAndRelativePositionsByChannel.getRight(),
        Location.from(location.getLatitude(), location.getLongitude(), location.getDepth(),
            location.getElevation()),
        channelGroupsAndRelativePositionsByChannel.getLeft(),
        channels
    );
  }

  private void storeChannelGroups(Set<ChannelGroup> channelGroups, StationDao station,
      Map<String, RelativePosition> relativePositionByChannelMap) {
    for (ChannelGroup channelGroup : channelGroups) {
      List<ChannelDao> channels = storeChannels(channelGroup, station,
          relativePositionByChannelMap);
      ChannelGroupDao channelGroupDao = entityManager
          .find(ChannelGroupDao.class, channelGroup.getName());
      if (channelGroupDao == null) {
        channelGroupDao = new ChannelGroupDao(
            channelGroup.getName(),
            channelGroup.getDescription(),
            new LocationDao(channelGroup.getLocation().orElse(null)),
            channelGroup.getType(),
            channels,
            station);
        entityManager.persist(channelGroupDao);
      }
    }
  }

  private List<ChannelDao> storeChannels(ChannelGroup channelGroup, StationDao stationDao,
      Map<String, RelativePosition> relativePositionByChannelMap) {
    List<ChannelDao> channels = new ArrayList<>();
    for (Channel channel : channelGroup.getChannels()) {
      ChannelDao channelDao = entityManager.find(ChannelDao.class, channel.getName());
      if (channelDao == null) {
        channelDao = ChannelDao.from(channel);
      }

      // Stores the channel displacment info from the given StationDao.
      StationChannelInfoDao stationChannelInfoDao = entityManager
          .find(StationChannelInfoDao.class, new StationChannelInfoKey(stationDao, channelDao));
      if (stationChannelInfoDao == null) {
        stationChannelInfoDao = new StationChannelInfoDao();
        stationChannelInfoDao.setId(new StationChannelInfoKey(stationDao, channelDao));
        RelativePosition channelRelativePosition = relativePositionByChannelMap
            .getOrDefault(channel.getName(), null);
        if (channelRelativePosition != null) {
          stationChannelInfoDao
              .setNorthDisplacementKm(channelRelativePosition.getNorthDisplacementKm());
          stationChannelInfoDao
              .setEastDisplacementKm(channelRelativePosition.getEastDisplacementKm());
          stationChannelInfoDao
              .setVerticalDisplacementKm(channelRelativePosition.getVerticalDisplacementKm());
        }

        // all related daos for the given channel are constructed so now we're ready to persist
        // everything.
        entityManager.persist(channelDao);
        entityManager.persist(stationChannelInfoDao);
        channelUtils.storeChannelConfiguredInputs(channelDao, channel.getConfiguredInputs());
      }
      channels.add(channelDao);
    }
    return channels;
  }

  /*
   * retrieve channels for the given StationDao along with their relative positions (which are stored in the database).
   *
   * @param stationDao StationDao we are retrieving ChannelDaos for
   */
  private Map<String, RelativePosition> retrieveChannelRelativePositions(StationDao stationDao,
      boolean retrieveDerivedChannels) {
    Map<String, RelativePosition> relativePositionsByChannel = new HashMap<>();
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<StationChannelInfoDao> stationChannelInfoCriteria = cb
        .createQuery(StationChannelInfoDao.class);
    Root<StationChannelInfoDao> fromStationChannelInfo = stationChannelInfoCriteria
        .from(StationChannelInfoDao.class);
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(fromStationChannelInfo.get("id").get("station"), stationDao));
    if (!retrieveDerivedChannels) {
      Subquery<ChannelConfiguredInputsDao> sub = stationChannelInfoCriteria
          .subquery(ChannelConfiguredInputsDao.class);
      Root<ChannelConfiguredInputsDao> subFrom = sub.from(ChannelConfiguredInputsDao.class);
      sub.select(subFrom.get("channelName"));
      predicates.add(cb.not(fromStationChannelInfo.get("id").get("channel").in(sub)));
    }
    stationChannelInfoCriteria.select(fromStationChannelInfo)
        .where(cb.and(predicates.stream().toArray(Predicate[]::new)));
    List<StationChannelInfoDao> queryResultList = entityManager
        .createQuery(stationChannelInfoCriteria)
        .getResultList();

    queryResultList.stream().forEach(stationChannelInfoDao -> relativePositionsByChannel
        .put(stationChannelInfoDao.getId().getChannel().getName(),
            RelativePosition.from(stationChannelInfoDao.getNorthDisplacementKm(),
                stationChannelInfoDao.getEastDisplacementKm(),
                stationChannelInfoDao.getVerticalDisplacementKm())));

    return relativePositionsByChannel;
  }

  private Pair<List<ChannelGroup>, Map<String, RelativePosition>> retrieveChannelGroups(
      StationDao stationDao, boolean retrieveDerivedChannels) throws IOException {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<ChannelGroupDao> channelGroupQuery = cb.createQuery(ChannelGroupDao.class);
    Root<ChannelGroupDao> fromChannelGroup = channelGroupQuery.from(ChannelGroupDao.class);
    channelGroupQuery.select(fromChannelGroup)
        .where(cb.equal(fromChannelGroup.get("station"), stationDao));
    List<ChannelGroupDao> groupDaos = entityManager.createQuery(channelGroupQuery)
        .getResultList();
    List<ChannelGroup> channelGroups = new ArrayList<>();
    for (ChannelGroupDao groupDao : groupDaos) {
      List<Channel> channels = channelUtils
          .constructChannels(groupDao.getChannels(), stationDao.getName());
      if (groupDao.getLocation().isPresent()) {
        final LocationDao locationDao = groupDao.getLocation().get();
        channelGroups.add(ChannelGroup.from(
            groupDao.getName(),
            groupDao.getDescription(),
            Location
                .from(locationDao.getLatitude(), locationDao.getLongitude(), locationDao.getDepth(),
                    locationDao.getElevation()),
            groupDao.getType(),
            channels));
      } else {
        channelGroups.add(ChannelGroup.from(
            groupDao.getName(),
            groupDao.getDescription(),
            null,
            groupDao.getType(),
            channels));
      }
    }
    return Pair
        .of(channelGroups, retrieveChannelRelativePositions(stationDao, retrieveDerivedChannels));
  }
}
