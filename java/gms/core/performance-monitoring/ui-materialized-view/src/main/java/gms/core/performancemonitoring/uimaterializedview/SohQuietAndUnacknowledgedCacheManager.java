package gms.core.performancemonitoring.uimaterializedview;

import com.google.common.base.Functions;
import com.google.common.collect.TreeBasedTable;
import gms.core.performancemonitoring.soh.control.configuration.StationSohDefinition;
import gms.core.performancemonitoring.ssam.control.config.StationSohMonitoringUiClientParameters;
import gms.shared.frameworks.osd.coi.soh.ChannelSoh;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.soh.quieting.QuietedSohStatusChange;
import gms.shared.frameworks.osd.coi.soh.quieting.SohStatusChange;
import gms.shared.frameworks.osd.coi.soh.quieting.UnacknowledgedSohStatusChange;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import gms.shared.frameworks.osd.coi.systemmessages.util.ChannelMonitorTypeQuietPeriodCanceledBuilder;
import gms.shared.frameworks.osd.coi.systemmessages.util.ChannelMonitorTypeQuietPeriodExpiredBuilder;
import gms.shared.frameworks.osd.coi.systemmessages.util.ChannelMonitorTypeQuietedBuilder;
import gms.shared.frameworks.osd.coi.systemmessages.util.ChannelMonitorTypeStatusChangeAcknowledgedBuilder;
import gms.shared.frameworks.osd.coi.systemmessages.util.ChannelMonitorTypeStatusChangedBuilder;
import gms.shared.frameworks.osd.coi.systemmessages.util.StationSohStatusChangedBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.FluxSink;

/**
 * Class manages both the Acknowledgement and Quiet SOH lists used in the construction of the UI
 * Station SOH messages. When a Quiet or Acknowledgement message is read from a topic the consumer
 * will call this manager to update the state of the Quieted map. This manager is also called every
 * time a new set Station SOH is generated to update the Acknowledgement map.
 */
public class SohQuietAndUnacknowledgedCacheManager {

  private static final Logger logger = LogManager
      .getLogger(SohQuietAndUnacknowledgedCacheManager.class);
  private static final Level SSAM_DEBUG = Level.valueOf("SSAM_DEBUG");

  /* StationSoh list used to create previous UiStationSoh entries
     used in tracking unacknowledged changes */
  private List<StationSoh> lastStationSohList;
  private final Map<String, UiUnacknowledgedSohStatusChange> unackSohStatusChangesMap;
  private long ackQuietDuration = 300000; //default value
  //systemMessageFluxSink for publishing to Kafka topic.
  private final FluxSink<SystemMessage> systemMessageFluxSink;
  private final FluxSink<UnacknowledgedSohStatusChange> unacknowledgedSohStatusChangeFluxSink;
  private final TreeBasedTable<String, Instant, QuietedSohStatusChangeUpdate>
      quietedChangesByExpirationAndStation = TreeBasedTable.create();
  private Map<String, StationSohDefinition> sohDefinitions = null;

  public SohQuietAndUnacknowledgedCacheManager(
      Set<QuietedSohStatusChange> quietedSohStatusChangeUpdates,
      Set<UnacknowledgedSohStatusChange> unacknowledgedChanges,
      List<StationSoh> lastStationSoh,
      StationSohMonitoringUiClientParameters stationSohParameters
  ) {

    this(
        quietedSohStatusChangeUpdates,
        unacknowledgedChanges,
        lastStationSoh,
        stationSohParameters,
        null,
        null
    );
  }

  /**
   * Constructor for SohQuietAndUnacknowledgedCacheManager
   *
   * @param stationSohParameters the SOH client params.
   * @param systemMessageFluxSink the Map of system messages.
   */
  public SohQuietAndUnacknowledgedCacheManager(
      Set<QuietedSohStatusChange> quietedSohStatusChangeUpdates,
      Set<UnacknowledgedSohStatusChange> unacknowledgedChanges,
      List<StationSoh> lastStationSoh,
      StationSohMonitoringUiClientParameters stationSohParameters,
      FluxSink<SystemMessage> systemMessageFluxSink,
      FluxSink<UnacknowledgedSohStatusChange> unacknowledgedSohStatusChangeFluxSink
  ) {
    this.systemMessageFluxSink = systemMessageFluxSink;

    this.unacknowledgedSohStatusChangeFluxSink = unacknowledgedSohStatusChangeFluxSink;

    // Set the latest Station Soh retrieved on startup
    this.lastStationSohList = new ArrayList<>(lastStationSoh);

    // Iterate through the unacknowledgedChanges adding to the unackSohStatusChangesMap map
    this.unackSohStatusChangesMap = new HashMap<>();
    unacknowledgedChanges.forEach(unackStation -> this.unackSohStatusChangesMap.put(
        unackStation.getStation(),
        new UiUnacknowledgedSohStatusChange(unackStation)));

    // Initialize Quieted Change Map with each entry
    quietedSohStatusChangeUpdates.forEach(
        quietedChange -> this
            .addQuietSohStatusChange(QuietedSohStatusChangeUpdate.create(quietedChange)));

    if (stationSohParameters != null) {
      this.ackQuietDuration = stationSohParameters.
          getStationSohMonitoringDisplayParameters().getAcknowledgementQuietDuration().toMillis();

      this.sohDefinitions = stationSohParameters.getStationSohControlConfiguration()
          .getStationSohDefinitions().stream()
          .collect(Collectors.toMap(StationSohDefinition::getStationName, Functions.identity()));

      // Build the static maps that determine which channel/monitor types are contributors
      StationSohContributingUtility.getInstance().initialize(stationSohParameters);
    }
  }

  /**
   * Get the Unacknowledged changes for each station
   *
   * @return List<UnacknowledgedSohStatusChange>
   */
  public List<UnacknowledgedSohStatusChange> getUnacknowledgedList() {
    return this.unackSohStatusChangesMap.values().stream().
        map(UiUnacknowledgedSohStatusChange::getUnacknowledgedSohStatusChange)
        .collect(Collectors.toList());
  }

  /**
   * Updates the Unacknowledged map to reflect changes between the lastStationSohList and the
   * newStationSohList. The function walks through the new station soh list comparing the channel
   * monitor pairs against the previously sent channel monitor pairs. If there is a difference in
   * their values the entry is added to the Unacknowledged list.
   *
   * @param newStationSohList the current {@link StationSoh}s.
   */
  public void updateUnacknowledgedList(
      List<StationSoh> newStationSohList
  ) {
    // Walk through the new station sohs
    for (StationSoh newStationSoh : newStationSohList) {
      // Find the corresponding entry in the previously sent station soh list
      Optional<StationSoh> oldStationSoh =
          this.lastStationSohList.stream()
              .filter(
                  oldStation -> oldStation.getStationName().equals(newStationSoh.getStationName()))
              .findFirst();

      if (oldStationSoh.isPresent()) {
        // Add status changed system message.
        SohStatus previous = oldStationSoh.get().getSohStatusRollup();
        SohStatus current = newStationSoh.getSohStatusRollup();

        if (!previous.equals(current)) {
          logger.log(SSAM_DEBUG,
              "Adding STATION_SOH_STATUS_CHANGED system message: station: {}, previous: {}, current: {}, time: {}",
              newStationSoh.getStationName(), previous, current, Instant.now());

          addStationStatusChangedSystemMessage(newStationSoh.getStationName(), previous, current);
        }

        // If a station not seen before add it to the map
        if (!this.unackSohStatusChangesMap.containsKey(newStationSoh.getStationName())) {
          this.unackSohStatusChangesMap.put(newStationSoh.getStationName(),
              new UiUnacknowledgedSohStatusChange(newStationSoh.getStationName()));
        }

        // Add any un-acknowledge changes
        this.addStatusChangesForStation(
            newStationSoh,
            oldStationSoh.get(),
            this.unackSohStatusChangesMap.get(newStationSoh.getStationName()));

        if (Objects.nonNull(unacknowledgedSohStatusChangeFluxSink)) {
          this.unacknowledgedSohStatusChangeFluxSink.next(
              this.unackSohStatusChangesMap.get(newStationSoh.getStationName())
                  .getUnacknowledgedSohStatusChange()
          );
        }

      }
    }

    // new stations soh list is now the old after updating the unack list
    this.lastStationSohList = newStationSohList;
  }

  /**
   * Adds the status change system message.
   *
   * @param stationName the station name.
   * @param previous the previous {@link SohStatus} value.
   * @param current the current {@link SohStatus} value.
   */
  private void addStationStatusChangedSystemMessage(
      String stationName, SohStatus previous, SohStatus current) {

    SystemMessage message = new StationSohStatusChangedBuilder(stationName, previous, current)
        .build();

    if (Objects.nonNull(systemMessageFluxSink)) {
      systemMessageFluxSink.next(message);
    }
  }

  /**
   * Compares the Old Station Channels and the New Station Channels and adds a new SohStatusChange
   * if detected. Note the UiUnacknowledgedSohStatusChange will only add one unacknowledged change
   * per channel/monitor type.
   *
   * @param newStationSoh the current {@link StationSoh}.
   * @param oldStationSoh the previous {@link StationSoh}.
   * @param uiUnacknowledgedSohStatusChange tracks channel/monitor changes for a station
   */
  private void addStatusChangesForStation(StationSoh newStationSoh,
      StationSoh oldStationSoh, UiUnacknowledgedSohStatusChange uiUnacknowledgedSohStatusChange) {

    // if station matches, compare channels. Check monitorTypes.
    newStationSoh.getChannelSohs().forEach(newChannelSoh -> {
      Optional<ChannelSoh> oldFoundChannelSoh =
          oldStationSoh.getChannelSohs().stream()
              .filter(
                  oldChan -> oldChan.getChannelName().equals(newChannelSoh.getChannelName()))
              .findFirst();

      oldFoundChannelSoh.ifPresent(
          channelSoh -> newChannelSoh.getAllSohMonitorValueAndStatuses().forEach(newSohMVS -> {
            // Find out if this is a contributing channel/monitor type for this station
            // else there is nothing to do
            if (StationSohContributingUtility.getInstance().isChannelMonitorContributing(
                newStationSoh.getStationName(),
                newChannelSoh.getChannelName(),
                newSohMVS.getMonitorType()
            )) {

              // compare new sohmvs against old sohmvs
              Optional<SohMonitorValueAndStatus<?>> oldSohMVS =
                  getSohMVS(
                      newSohMVS.getMonitorType(),
                      channelSoh.getAllSohMonitorValueAndStatuses()
                  );

              // Compare previous MVS to new MVS enumeration statuses
              if (oldSohMVS.isPresent() && (!newSohMVS.getStatus()
                  .equals(oldSohMVS.get().getStatus()))) {

                StationSohDefinition stationSohDefinition = this.sohDefinitions
                    .get(newStationSoh.getStationName());

                /*
                 * The Channel SohMonitorValueAndStatus' monitorType must appear in the sohMonitorTypesForRollup
                 * list for the Station's StationSohDefinition.
                 *
                 * In the Station's StationSohDefinition, the Channel must appear in the channelsByMonitorTypeRollup
                 * collection for the entry keyed by the Channel SohMonitorValueAndStatus' monitorType.
                 */
                if (Objects.nonNull(stationSohDefinition) && stationSohDefinition
                    .getSohMonitorTypesForRollup().contains(newSohMVS.getMonitorType())
                    && stationSohDefinition.getChannelsBySohMonitorType()
                    .get(newSohMVS.getMonitorType()).contains(newChannelSoh.getChannelName())) {

                  logger.log(SSAM_DEBUG,
                      "SohStatusChange detected for channel {} and monitor {}",
                      newChannelSoh.getChannelName(), newSohMVS.getMonitorType());

                  // Call Ui unacknowledged entry to add soh change status if one doesn't already exist
                  // The channel name and monitor type are the key to the internal map
                  boolean added = uiUnacknowledgedSohStatusChange.addSohStatusChange(
                      SohStatusChange.from(newStationSoh.getTime(),
                          newSohMVS.getMonitorType(),
                          newChannelSoh.getChannelName())
                  );

                  addChannelMonitorTypeStatusChangedSystemMessage(added,
                      newStationSoh.getStationName(),
                      newChannelSoh.getChannelName(), oldSohMVS.get(), newSohMVS,
                      systemMessageFluxSink);
                }
              }
            }
          }));
    });
  }

  /**
   * Add the channel monitor type status changed system message.
   *
   * @param createMessage test for message creation.
   * @param stationName the station name.
   * @param channelName the channel name.
   * @param previousStatus the previous {@link SohStatus}.
   * @param currentStatus the current {@link SohStatus}.
   * @param systemMessageFluxSink the Map of system messages.
   */
  private static void addChannelMonitorTypeStatusChangedSystemMessage(boolean createMessage,
      String stationName, String channelName, SohMonitorValueAndStatus<?> previousStatus,
      SohMonitorValueAndStatus<?> currentStatus,
      FluxSink<SystemMessage> systemMessageFluxSink) {

    if (Objects.nonNull(systemMessageFluxSink) &&
        createMessage && Objects.nonNull(currentStatus) && !currentStatus.equals(previousStatus)) {

      logger.log(SSAM_DEBUG,
          "Adding status changed message for station {}, channel {}, with previous status {} and current status {}",
          stationName, channelName, previousStatus, currentStatus);

      SystemMessage message = new ChannelMonitorTypeStatusChangedBuilder(
          stationName, channelName, currentStatus.getMonitorType(), previousStatus, currentStatus)
          .build();

      systemMessageFluxSink.next(message);
    }
  }

  /**
   * Get the latest StationSoh list sent to the UI
   *
   * @return List<StationSoh>
   */
  public List<StationSoh> getLastStationSohList() {
    return this.lastStationSohList;
  }

  /**
   * Add the Quiet entry to the map
   *
   * @param quietedChange the quieted change instance.
   * @return boolean did we modify the quiet map (send update to UI)
   */
  public boolean addQuietSohStatusChange(QuietedSohStatusChangeUpdate quietedChange) {
    String key = quietedChange.getChannelName() + "." + quietedChange.getSohMonitorType();
    synchronized (quietedChangesByExpirationAndStation) {
      //add the system message in for the quietedChange
      addChannelMonitorTypeQuietedMessages(quietedChange);
      quietedChangesByExpirationAndStation.row(key).clear();
      // Always set the latest quiet status, even if there was one later expiration
      quietedChangesByExpirationAndStation.put(key, quietedChange.getQuietUntil(), quietedChange);
    }

    return true;
  }

  /**
   * Add a new SystemMessage for Quieted channel monitor pair
   * @param quietedChange the quieted change instance.
   */
  private void addChannelMonitorTypeQuietedMessages(QuietedSohStatusChangeUpdate quietedChange) {

    if (Objects.nonNull(systemMessageFluxSink)) {
      SystemMessage channelMonitorTypeQuietedMessage = new ChannelMonitorTypeQuietedBuilder(
          quietedChange.getStationName(),
          quietedChange.getChannelName(),
          quietedChange.getSohMonitorType(),
          quietedChange.getQuietDuration(),
          quietedChange.quietedBy(),
          quietedChange.getComment().orElse("")
      ).build();

      systemMessageFluxSink.next(channelMonitorTypeQuietedMessage);
    }

  }

  /**
   * Takes the acknowledged channel monitor pairs to quiet for a station.
   *
   * @param ackSohStatusChange the acknowledged status change.
   * @return boolean did we modify the quiet map (send update to UI)
   */
  public boolean addAcknowledgedStationToQuietList(AcknowledgedSohStatusChange ackSohStatusChange) {
    boolean quietedListUpdated = false;
    synchronized (quietedChangesByExpirationAndStation) {
      for (SohStatusChange sohStatusChange : ackSohStatusChange.getAcknowledgedChanges()) {
        // Find quiet entry in map
        String key =
            sohStatusChange.getChangedChannel() + "." + sohStatusChange.getSohMonitorType();

        QuietedSohStatusChangeUpdate update = QuietedSohStatusChangeUpdate.create(
            Instant.ofEpochMilli(
                ackSohStatusChange.getAcknowledgedAt().toEpochMilli() + this.ackQuietDuration),
            Duration.ofMillis(this.ackQuietDuration),
            sohStatusChange.getSohMonitorType(),
            sohStatusChange.getChangedChannel(),
            ackSohStatusChange.getComment(),
            ackSohStatusChange.getAcknowledgedStation(),
            ackSohStatusChange.getAcknowledgedBy());

        QuietedSohStatusChangeUpdate previousUpdate = quietedChangesByExpirationAndStation
            .put(key, update.getQuietUntil(), update);
        if (previousUpdate == null || previousUpdate.getQuietUntil().isBefore(
            ackSohStatusChange.getAcknowledgedAt().plus(ackQuietDuration, ChronoUnit.MILLIS))) {
          addChannelMonitorTypeStatusChangedAcknowledgedSystemMessage(ackSohStatusChange,
              sohStatusChange);
          quietedListUpdated = true;
        }

        // Get the latest QuietedSohStatusChangeUpdates for the station.  This size should always be
        // 1 or 2 if we are maintaining the cache correctly, but part of doing that is assuming it
        // could have more than 1 item (i.e., we're not maintaining it correctly)

        SortedMap<Instant, QuietedSohStatusChangeUpdate> existingUpdates =
            quietedChangesByExpirationAndStation.row(key);

        // Get all but the last update and delete - copying into a list to avoid concurrent
        // modification exceptions, since backing maps of TreeBasedTable does allow itself to be
        // modified via its row maps.
        List<Instant> colKeysToRemove = new ArrayList<>(
            existingUpdates.subMap(existingUpdates.firstKey(), existingUpdates.lastKey())
                .keySet());

        colKeysToRemove.forEach(colKey -> quietedChangesByExpirationAndStation.remove(key, colKey));
      }
      // Clear the unacknowledged changes for the station
      this.clearUnacknowledgedStation(ackSohStatusChange.getAcknowledgedStation());

    }
    return quietedListUpdated;
  }

  /**
   * Add the channel monitor type status changed acknowledged system message.
   *
   * @param ackSohStatusChange the acknowledged status change.
   * @param sohStatusChange the SOH status change.
   */
  private void addChannelMonitorTypeStatusChangedAcknowledgedSystemMessage(
      AcknowledgedSohStatusChange ackSohStatusChange, SohStatusChange sohStatusChange) {

    if (Objects.nonNull(systemMessageFluxSink)) {
      SystemMessage message = new ChannelMonitorTypeStatusChangeAcknowledgedBuilder(
          ackSohStatusChange.getAcknowledgedStation(), sohStatusChange.getChangedChannel(),
          sohStatusChange.getSohMonitorType(), ackSohStatusChange.getAcknowledgedBy(),
          ackSohStatusChange.getComment().orElse(null)).build();

      systemMessageFluxSink.next(message);
    }

  }

  /**
   * Clones the current quiet list, pruning and expired quiet entries.
   *
   * @return cloned list minus expired entries
   */
  public List<QuietedSohStatusChangeUpdate> getQuietedSohStatusChanges() {
    synchronized (quietedChangesByExpirationAndStation) {
      Instant now = Instant.now();

      // Clean out the quieted cache and send system messages for anything that expired or was
      // cancelled
      List<Pair<String, Instant>> keysToRemove = quietedChangesByExpirationAndStation.rowKeySet()
          .stream()
          .map(quietedChangesByExpirationAndStation::row)
          .map(updatesByTime -> updatesByTime.headMap(now))
          .map(SortedMap::values)
          .flatMap(Collection::stream)
          .peek(update -> {
            if (update.getQuietDuration().isZero()) {
              addStationQuietCanceledSystemMessage(update, systemMessageFluxSink);
            } else {
              addStationQuietExpiredSystemMessage(update, systemMessageFluxSink);
            }
          })
          .map(update -> Pair.of(update.getChannelName() + "." + update.getSohMonitorType(),
              update.getQuietUntil()))
          .collect(Collectors.toList());

      keysToRemove.forEach(
          keys -> quietedChangesByExpirationAndStation.remove(keys.getKey(), keys.getValue()));

      // return a copy of the values to ensure that future cache updates don't affect downstream
      // processing
      return List.copyOf(quietedChangesByExpirationAndStation.values());
    }
  }

  /**
   * Adds the quiet expired system message.
   *
   * @param quietedSohStatusChange Information about expired station.
   * @param systemMessageFluxSink the Map of system messages.
   */
  private void addStationQuietExpiredSystemMessage(
      QuietedSohStatusChangeUpdate quietedSohStatusChange,
      FluxSink<SystemMessage> systemMessageFluxSink) {

    if (Objects.nonNull(systemMessageFluxSink)) {

      SystemMessage quietExpiredMessage = new ChannelMonitorTypeQuietPeriodExpiredBuilder(
          quietedSohStatusChange.getStationName(),
          quietedSohStatusChange.getChannelName(),
          quietedSohStatusChange.getSohMonitorType()).build();

      systemMessageFluxSink.next(quietExpiredMessage);
    }
  }

  /**
   * Adds the quiet canceled system message.
   *
   * @param quietedSohStatusChange Information about expired station.
   * @param systemMessageFluxSink the Map of system messages.
   */
  private void addStationQuietCanceledSystemMessage(
      QuietedSohStatusChangeUpdate quietedSohStatusChange,
      FluxSink<SystemMessage> systemMessageFluxSink) {

    if (Objects.nonNull(systemMessageFluxSink)) {
      SystemMessage quietCanceledMessage = new ChannelMonitorTypeQuietPeriodCanceledBuilder(
          quietedSohStatusChange.getStationName(),
          quietedSohStatusChange.getChannelName(),
          quietedSohStatusChange.getSohMonitorType(),
          quietedSohStatusChange.quietedBy()).build();

      systemMessageFluxSink.next(quietCanceledMessage);
    }

  }

  /**
   * Clear the unacknowledged SOH Status Changes now that the station has been acknowledged
   *
   * @param stationName the station name.
   */
  private void clearUnacknowledgedStation(String stationName) {
    if (this.unackSohStatusChangesMap.containsKey(stationName)) {
      this.unackSohStatusChangesMap.get(stationName).clearSohStatusChanges();
    }
  }

  /**
   * Helper method to find the MonitorValueAndStatus entry
   *
   * @param monitorType the monitor type.
   * @param setMVS the Set of {@link SohMonitorValueAndStatus}s.
   * @return Optional<SohMonitorValueAndStatus < ?>>
   */
  private Optional<SohMonitorValueAndStatus<?>> getSohMVS(
      SohMonitorType monitorType, Set<SohMonitorValueAndStatus<?>> setMVS
  ) {
    return setMVS.stream().filter(mvs -> mvs.getMonitorType().equals(monitorType)).findFirst();
  }
}