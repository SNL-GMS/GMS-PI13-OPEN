package gms.core.performancemonitoring.soh.control;

import gms.core.performancemonitoring.soh.control.configuration.ChannelSohDefinition;
import gms.core.performancemonitoring.soh.control.configuration.ChannelsByMonitorTypeConfigurationOption;
import gms.core.performancemonitoring.soh.control.configuration.DurationSohMonitorStatusThresholdDefinition;
import gms.core.performancemonitoring.soh.control.configuration.PercentSohMonitorStatusThresholdDefinition;
import gms.core.performancemonitoring.soh.control.configuration.SohMonitorStatusThresholdDefinition;
import gms.core.performancemonitoring.soh.control.configuration.SohMonitorTypesForRollupConfigurationOption;
import gms.core.performancemonitoring.soh.control.configuration.StationSohDefinition;
import gms.core.performancemonitoring.soh.control.configuration.TimeWindowDefinition;
import gms.shared.frameworks.configuration.ConfigurationRepository;
import gms.shared.frameworks.configuration.Selector;
import gms.shared.frameworks.configuration.repository.client.ConfigurationConsumerUtility;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType.SohValueType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;


public class StationSohControlUtility {

  private static final String STATION_SOH_PREFIX = "soh-control";

  private static final String MONITOR_TYPE_SELECTOR_KEY = "MonitorType";

  private static final String STATION_NAME_SELECTOR_KEY = "StationName";

  private static final String CHANNEL_NAME_SELECTOR_KEY = "ChannelName";

  private static final String NULL_CONFIGURATION_CONSUMER_UTILITY = "Null configurationConsumerUtility";

  private static final String NULL_STATION_NAME = "Null stationName";

  /* Hiding default public constructor */
  private StationSohControlUtility() {
  }

  /**
   * Resolves {@link StationSohDefinition}s for the provided {@link Set} of {@link Station}s against
   * the provided {@link ConfigurationConsumerUtility}.
   *
   * @param configurationConsumerUtility Used to resolve processing configuration.  Not null.
   * @param stations Channels to resolve StationSohDefinitions for.  Not null.
   * @return Set of StationSohDefinitions for the provided Set of Stations loaded from processing
   * configuration.
   */
  public static Set<StationSohDefinition> resolveStationSohDefinitions(
      ConfigurationConsumerUtility configurationConsumerUtility,
      Collection<Station> stations) {

    Objects.requireNonNull(configurationConsumerUtility, NULL_CONFIGURATION_CONSUMER_UTILITY);

    return stations.stream().map(station -> {

      var stationName = station.getName();
      Objects.requireNonNull(stationName, NULL_STATION_NAME);

      var allChannelNameSet = station.getChannels()
          .stream()
          .map(Channel::getName)
          .collect(Collectors.toSet());

      var channelSohDefinitionSet = resolveChannelSohDefinitions(
          configurationConsumerUtility,
          station.getChannels()
      );

      var monitorTypeSetInStationLevelRollup =
          resolveSohMonitorTypesForRollupConfigurationOptionForStationRollup(
              configurationConsumerUtility,
              stationName
          ).getSohMonitorTypesForRollup();

      var channelsBySohMonitorTypeMap =
          resolveChannelsByMonitorTypeConfigurationOptionForStation(
              configurationConsumerUtility,
              stationName,
              allChannelNameSet
          ).getChannelsByMonitorType();

      var timeWindowDefinitionMap = resolveTimeWindowDefinitionsForStation(
          configurationConsumerUtility,
          stationName
      );

      return StationSohDefinition.create(
          stationName,
          monitorTypeSetInStationLevelRollup,
          channelsBySohMonitorTypeMap,
          channelSohDefinitionSet,
          timeWindowDefinitionMap
      );
    }).collect(Collectors.toSet());
  }

  /**
   * Converts the provided {@link Object} to a {@link Set} of {@link String}s.  Intermediately
   * converts the Object into a {@link List} because the Object read in from config is an
   * UnmodifiableRandomAccessList.
   *
   * @param channelsObject Object to be casted to a Set of Strings.
   * @return Set of Strings.
   * @throws IllegalArgumentException If the provided Object is not an instance of List, or if the
   * elements of the List are not Strings.
   */
  private static Set<String> convertObjectToChannelNames(Object channelsObject) {

    var channelNames = new HashSet<String>();

    if (channelsObject instanceof List<?>) {

      var channelNamesWildcardList = (List<?>) channelsObject;

      channelNamesWildcardList.forEach(channelNameWildcard -> {
        if (channelNameWildcard instanceof String) {

          channelNames.add((String) channelNameWildcard);
        } else {

          throw new IllegalArgumentException(
              "Elements of the channelNames List in the channelsByMonitorType configuration map are"
                  + " expected to be Strings, but are not");
        }
      });
    } else {

      throw new IllegalArgumentException(
          "Values in the channelsByMonitorType configuration map are expected to be Lists, but are not.");
    }

    return channelNames;
  }

  /**
   * Converts raw configuration {@link Map} keys and values into {@link SohMonitorType} and {@link
   * Set}s of {@link Channel} names, respectively.  Adds the converted key/value pairs to the
   * provided map from SohMonitorType to Channel names.
   *
   * @param channelsByMonitorTypeToPopulate Configuration values contained in
   * channelsByMonitorTypeConfigurationMap are added to this map once they are converted to the
   * appropriate types.
   * @param channelsByMonitorTypeConfigurationMap Raw configuration map whose keys and values will
   * be converted into SohMonitorTypes and sets of Channel names, respectively.  Those key/value
   * pairs are added to the channelsByMonitorTypeToPopulate map.
   */
  private static void populateChannelsByMonitorTypeFromConfigurationMap(
      Map<SohMonitorType, Set<String>> channelsByMonitorTypeToPopulate,
      Map<String, Object> channelsByMonitorTypeConfigurationMap) {

    channelsByMonitorTypeConfigurationMap.forEach((monitorTypeString, channelsObject) ->
        channelsByMonitorTypeToPopulate.put(
            SohMonitorType.valueOf(monitorTypeString),
            convertObjectToChannelNames(channelsObject)
        )
    );
  }

  /**
   * Resolves channelsByMonitorType configuration values into a {@link Map} from {@link
   * SohMonitorType} to {@link Set} of {@link Channel} names.  Uses the provided {@link Selector}s
   * to resolve the correct configuration.  For each SohMonitorType key that does not exist in the
   * resolved configuration, entries are added mapping the missing SohMonitorType to the provided
   * set of Channel names.  If the requested configuration option does not exist, the returned map
   * contains each SohMonitorType as keys and the provided set of Channel names as values.
   *
   * @param configurationConsumerUtility {@link ConfigurationConsumerUtility} used to resolve
   * configuration against a {@link ConfigurationRepository}. Not null.
   * @param selectors {@link Selector}s to apply when resolving a MonitorTypesInRollupConfigurationOption.
   * @param channelNames Contains set of all Channel names that will serve as the value for any
   * SohMonitorTypes missing from channelsByMonitorType.
   * @return Map from SohMonitorType to set of Channel names.  Any SohMonitorType that does not
   * appear in configuration is added as an entry whose value is the set of provided Channel names.
   */
  private static Map<SohMonitorType, Set<String>> resolveChannelsByMonitorTypeConfiguration(
      ConfigurationConsumerUtility configurationConsumerUtility,
      List<Selector> selectors,
      Set<String> channelNames) {

    Map<SohMonitorType, Set<String>> channelsByMonitorType = new EnumMap<>(SohMonitorType.class);

    var channelsByMonitorTypeConfigurationName = STATION_SOH_PREFIX + ".channels-by-monitor-type";

    try {

      var channelsByMonitorTypeConfigMap = configurationConsumerUtility
          .resolve(
              channelsByMonitorTypeConfigurationName,
              selectors
          );

      Objects.requireNonNull(channelsByMonitorTypeConfigMap,
          "Null channelsByMonitorTypeConfigurationMap");

      populateChannelsByMonitorTypeFromConfigurationMap(
          channelsByMonitorType,
          channelsByMonitorTypeConfigMap);

    } catch (IllegalStateException | IllegalArgumentException e) {

      // IllegalStateException is caught if a configuration option for the specified Selectors
      // does not exist.
      // IllegalArgumentException is caught if no configuration with the specified name exists.
      // Either of these cases are expected and are not errors.
    } finally {

      SohMonitorType.validTypes().forEach(monitorType ->
          channelsByMonitorType.computeIfAbsent(monitorType, key -> channelNames));
    }

    return channelsByMonitorType;
  }

  /**
   * Resolves a {@link ChannelsByMonitorTypeConfigurationOption} using the provided {@link
   * Selector}s.  For any {@link SohMonitorType}s that are missing from configuration, or if the
   * configuration does not exist at all, entries are added mapping the missing SohMonitorTypes to
   * the provided set of {@link Channel} names.
   *
   * @param configurationConsumerUtility {@link ConfigurationConsumerUtility} used to resolve
   * configuration against a {@link ConfigurationRepository}. Not null.
   * @param stationName Name of the Station to use a selector when resolving the
   * ChannelsByMonitorTypeConfigurationOption.
   * @param channelNames Set of all Channel names to be used as values for msising SohMonitortype
   * entries.
   * @return ChannelsByMonitorTypeConfigurationOption matching the provided selectors.  For any
   * {@link SohMonitorType}s that are missing from configuration, or if the configuration does not
   * exist at all, entries are added mapping the missing SohMonitorTypes to the set of provided
   * Channel names.
   */
  private static ChannelsByMonitorTypeConfigurationOption
  resolveChannelsByMonitorTypeConfigurationOptionForStation(
      ConfigurationConsumerUtility configurationConsumerUtility,
      String stationName,
      Set<String> channelNames) {

    return ChannelsByMonitorTypeConfigurationOption.create(
        resolveChannelsByMonitorTypeConfiguration(
            configurationConsumerUtility,
            List.of(
                Selector.from(STATION_NAME_SELECTOR_KEY, stationName)
            ),
            channelNames
        )
    );
  }

  /**
   * Resolves a {@link SohMonitorTypesForRollupConfigurationOption} using the provided {@link
   * Selector}s.  If no monitor types in rollup configuration exists, or if no
   * MonitorTypesInRollupConfigurationOptions exist, returns a MonitorTypesInRollupConfigurationOption
   * containing all {@link SohMonitorType}s.
   *
   * @param configurationConsumerUtility {@link ConfigurationConsumerUtility} used to resolve
   * configuration against a {@link ConfigurationRepository}. Not null.
   * @param selectors Selectors to apply when resolving a MonitorTypesInRollupConfigurationOption.
   * @return MonitorTypesInRollupConfigurationOption matching the provided Selectors.  If no monitor
   * types in rollup configuration exists, or if no MonitorTypesInRollupConfigurationOptions exist,
   * returns a MonitorTypesInRollupConfigurationOption containing all SohMonitorTypes. Not null.
   */
  private static SohMonitorTypesForRollupConfigurationOption
  resolveMonitorTypesInRollupConfigurationOptionForSelectors(
      ConfigurationConsumerUtility configurationConsumerUtility,
      List<Selector> selectors,
      String monitorTypesInRollupConfigurationName) {

    SohMonitorTypesForRollupConfigurationOption sohMonitorTypesForRollupConfigurationOption;

    try {

      sohMonitorTypesForRollupConfigurationOption = configurationConsumerUtility.resolve(
          monitorTypesInRollupConfigurationName,
          selectors,
          SohMonitorTypesForRollupConfigurationOption.class
      );
    } catch (IllegalStateException | IllegalArgumentException e) {

      // IllegalStateException is caught if a configuration option for the specified Selectors
      // does not exist.
      // IllegalArgumentException is caught if no configuration with the specified name exists.
      // This is expected and is not an error.
      sohMonitorTypesForRollupConfigurationOption = SohMonitorTypesForRollupConfigurationOption
          .create(SohMonitorType.validTypes());
    }

    return sohMonitorTypesForRollupConfigurationOption;
  }

  /**
   * Resolves a {@link SohMonitorTypesForRollupConfigurationOption} by providing the necessary
   * selectors. MonitorTypesInRollupConfigurationOption can be overriden by {@link
   * gms.shared.frameworks.osd.coi.signaldetection.Station} or {@link
   * gms.shared.frameworks.osd.coi.channel.Channel}.  This implementation of
   * resolveMonitorTypesInRollupConfigurationOption(...) is only for retrieving configuration
   * overridden by Channel.
   *
   * @param configurationConsumerUtility {@link ConfigurationConsumerUtility} used to resolve
   * configuration against a {@link ConfigurationRepository}. Not null.
   * @param channelName Channel name selector allowing MonitorTypesInRollupConfigurationOption to be
   * overridden by Channel name.  Not null.
   * @return MonitorTypesInRollupConfigurationOption matching the provided selector.  Not null.
   */
  private static SohMonitorTypesForRollupConfigurationOption
  resolveSohMonitorTypesForRollupConfigurationOptionChannelRollup(
      ConfigurationConsumerUtility configurationConsumerUtility,
      String stationName,
      String channelName) {

    return resolveMonitorTypesInRollupConfigurationOptionForSelectors(
        configurationConsumerUtility,
        List.of(
            Selector.from(CHANNEL_NAME_SELECTOR_KEY, channelName),
            Selector.from(STATION_NAME_SELECTOR_KEY, stationName)
        ),
        STATION_SOH_PREFIX + ".soh-monitor-types-for-rollup-channel"
    );
  }


  /**
   * Resolves a {@link SohMonitorStatusThresholdDefinition} by providing the necessary selectors. The
   * {@link SohMonitorStatusThresholdDefinition} can be overridden by {@link SohMonitorType}, {@link
   * gms.shared.frameworks.osd.coi.signaldetection.Station} name, and {@link
   * gms.shared.frameworks.osd.coi.channel.Channel} name.
   *
   * @param configurationConsumerUtility {@link ConfigurationConsumerUtility} used to resolve
   * configuration against a {@link ConfigurationRepository}. Not null.
   * @param monitorType SohMonitorType selector allowing SohMonitorValueAndStatusDefinition to be
   * overridden by SohMonitorType.  Not null.
   * @param stationName Station name selector allowing SohMonitorValueAndStatusDefinition to be
   * overridden by Station name.  Not null.
   * @param channelName Channel name selector allowing SohMonitorValueAndStatusDefinition to be
   * overridden by Channel name.  Not null.
   * @return SohMonitorStatusThresholdDefinition matching the provided selectors.  Either a {@link
   * DurationSohMonitorStatusThresholdDefinition} or {@link PercentSohMonitorStatusThresholdDefinition}.
   * Not null.
   */
  private static SohMonitorStatusThresholdDefinition<?> resolveSohMonitorStatusThresholdDefinition(
      ConfigurationConsumerUtility configurationConsumerUtility,
      SohMonitorType monitorType,
      String stationName,
      String channelName
  ) {

    var sohMonitorThresholdConfigurationName =
        STATION_SOH_PREFIX + ".soh-monitor-thresholds";

    //
    // Force this List be of the non-generic Selector, because
    // that is what resolve takes.
    //
    var selectors = List.<Selector>of(
        Selector.from(MONITOR_TYPE_SELECTOR_KEY, monitorType.toString()),
        Selector.from(STATION_NAME_SELECTOR_KEY, stationName),
        Selector.from(CHANNEL_NAME_SELECTOR_KEY, channelName)
    );

    try {
      if (monitorType.getSohValueType() == SohValueType.DURATION) {
        return configurationConsumerUtility.resolve(
            sohMonitorThresholdConfigurationName,
            selectors,
            DurationSohMonitorStatusThresholdDefinition.class
        );
      } else if (monitorType.getSohValueType() == SohValueType.PERCENT) {
        return configurationConsumerUtility.resolve(
            sohMonitorThresholdConfigurationName,
            selectors,
            PercentSohMonitorStatusThresholdDefinition.class
        );
      } else {
        throw new IllegalArgumentException(
            "Unrecognized SohMonitorType"
        );
      }
    } catch (IllegalStateException e) {
      throw new IllegalStateException(
          "Configuration error for monitor type " + monitorType
          + ", station " + stationName
          + ", channel " + channelName,
          e
      );
    }

  }


  /**
   * Resolves a {@link SohMonitorTypesForRollupConfigurationOption} by providing the necessary
   * selectors. MonitorTypesInRollupConfigurationOption can be overriden by {@link
   * gms.shared.frameworks.osd.coi.signaldetection.Station} or Station and {@link
   * gms.shared.frameworks.osd.coi.channel.Channel}.  This implementation of
   * resolveMonitorTypesInRollupConfigurationOption(...) is only for retrieving configuration
   * overridden by Station.
   *
   * @param configurationConsumerUtility {@link ConfigurationConsumerUtility} used to resolve
   * configuration against a {@link ConfigurationRepository}. Not null.
   * @param stationName Station name selector allowing MonitorTypesInRollupConfigurationOption to be
   * overridden by Station name.  Not null.
   * @return MonitorTypesInRollupConfigurationOption matching the provided selector.  Not null.
   */
  private static SohMonitorTypesForRollupConfigurationOption
  resolveSohMonitorTypesForRollupConfigurationOptionForStationRollup(
      ConfigurationConsumerUtility configurationConsumerUtility,
      String stationName) {

    return resolveMonitorTypesInRollupConfigurationOptionForSelectors(
        configurationConsumerUtility,
        List.of(Selector.from(STATION_NAME_SELECTOR_KEY, stationName)),
        STATION_SOH_PREFIX + ".soh-monitor-types-for-rollup-station"
    );
  }

  /**
   * Resolves {@link ChannelSohDefinition}s for the provided {@link Set} of {@link Channel}s against
   * the provided {@link ConfigurationConsumerUtility}.
   *
   * @param configurationConsumerUtility Used to resolve processing configuration.  Not null.
   * @param channels Channels to resolve ChannelSohDefinitions for.  Not null.
   * @return Set of ChannelSohDefinitions for the provided Set of Channels loaded from processing
   * configuration.
   */
  private static Set<ChannelSohDefinition> resolveChannelSohDefinitions(
      ConfigurationConsumerUtility configurationConsumerUtility,
      Collection<Channel> channels) {

    return channels.stream().map(channel -> {

      var stationName = channel.getStation();
      Validate.isTrue(!stationName.isEmpty(), "Empty stationName");

      var channelName = channel.getName();

      var definitionsByMonitorType =
          new EnumMap<SohMonitorType, SohMonitorStatusThresholdDefinition<?>>(SohMonitorType.class);

      SohMonitorType.validTypes().forEach(
          sohMonitorType ->
              definitionsByMonitorType.put(
                  sohMonitorType,
                  resolveSohMonitorStatusThresholdDefinition(
                      configurationConsumerUtility,
                      sohMonitorType,
                      stationName,
                      channelName
                  )
              )
      );

      var sohMonitorTypesForChannelLevelRollup =
          resolveSohMonitorTypesForRollupConfigurationOptionChannelRollup(
              configurationConsumerUtility,
              stationName,
              channelName
          ).getSohMonitorTypesForRollup();

      return ChannelSohDefinition.create(
          channelName,
          sohMonitorTypesForChannelLevelRollup,
          definitionsByMonitorType
      );
    }).collect(Collectors.toSet());
  }

  /**
   * Resolve a map of SohMonitorType to TimeWindowDefinition, for a particular station.
   *
   * @param configurationConsumerUtility configuration consumer utility to utilize
   * @param stationName station name to resolve time windows for
   * @return Map of SohMonitorType to TimeWindowDefinition
   */
  private static Map<SohMonitorType, TimeWindowDefinition> resolveTimeWindowDefinitionsForStation(
      ConfigurationConsumerUtility configurationConsumerUtility,
      String stationName
  ) {

    return SohMonitorType.validTypes().stream().map(
        sohMonitorType -> Map.entry(
            sohMonitorType,
            configurationConsumerUtility.resolve(
                STATION_SOH_PREFIX + ".soh-monitor-timewindows",
                List.of(
                    Selector.from(STATION_NAME_SELECTOR_KEY, stationName),
                    Selector.from(MONITOR_TYPE_SELECTOR_KEY, sohMonitorType.toString())
                ),
                TimeWindowDefinition.class
            )
        )
    ).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

}
