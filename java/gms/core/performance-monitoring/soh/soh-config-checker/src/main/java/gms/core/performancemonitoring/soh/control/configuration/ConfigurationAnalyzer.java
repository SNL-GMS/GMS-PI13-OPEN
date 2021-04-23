package gms.core.performancemonitoring.soh.control.configuration;

/**
 * Analyses a configuration represented in a StationSohMonitoringDefinition class.
 *
 * Aside from the methods already implemented, hopefully there will be more to come, like digging
 * into configuration to find values or sets of values, etc.
 */
public class ConfigurationAnalyzer {

  private StationSohMonitoringDefinition stationSohMonitoringDefinition;

  ConfigurationAnalyzer(
      StationSohMonitoringDefinition stationSohMonitoringDefinition
  ) {

    this.stationSohMonitoringDefinition = stationSohMonitoringDefinition;
  }

  /**
   * Number of StationSohDefinitions in the top-level object
   */
  int countStationSohDefinitions() {

    return stationSohMonitoringDefinition.getStationSohDefinitions().size();
  }

  /**
   * Number of ChannelSohDefinitions across all StationSohDefinitions
   */
  int countChannelSohDefinitions() {

    return stationSohMonitoringDefinition.getStationSohDefinitions().stream()
        .mapToInt(stationSohDefinition -> stationSohDefinition.getChannelSohDefinitions().size())
        .sum();
  }

  /**
   * Number of SohMonitorTypes in the Station rollus across all StationSohDefinitions
   */
  int countStationMonitorTypesForRollup() {

    return stationSohMonitoringDefinition.getStationSohDefinitions().stream()
        .mapToInt(stationSohDefinition -> stationSohDefinition.getSohMonitorTypesForRollup().size())
        .sum();
  }

  /**
   * Number of mappings of SohMonitorType -> channels in rollup
   */
  int countChannelsByMonitorTypeEntries() {

    return stationSohMonitoringDefinition.getStationSohDefinitions().stream()
        .mapToInt(stationSohDefinition -> stationSohDefinition.getChannelsBySohMonitorType().size())
        .sum();
  }

  /**
   * Number of mappings of SohMonitorType -> SohMonitorAndValueDefinition across all ChannelSohDefinitions
   */
  int countSohMonitorValueAndStatusDefinitionBySohMonitorType() {

    return stationSohMonitoringDefinition.getStationSohDefinitions().stream()
        .flatMap(stationSohDefinition -> stationSohDefinition.getChannelSohDefinitions().stream())
        .mapToInt(channelSohDefinition -> channelSohDefinition
            .getSohMonitorStatusThresholdDefinitionsBySohMonitorType().keySet().size())
        .sum();
  }

  /**
   * Total number of monitor typs included for channel rollups across all ChannelSohDefinitions
   */
  int countChannelMonitorTypesForRollup() {

    return stationSohMonitoringDefinition.getStationSohDefinitions().stream()
        .flatMap(stationSohDefinition -> stationSohDefinition.getChannelSohDefinitions().stream())
        .mapToInt(channelSohDefinition -> channelSohDefinition.getSohMonitorTypesForRollup().size())
        .sum();
  }

  /**
   * Number of CapabilityRollupDefinitions in the top-level object
   */
  int countCapabilityRollupDefinitions() {

    return stationSohMonitoringDefinition.getCapabilitySohRollupDefinitions().size();
  }

  /**
   * Number of StationRollupDefinitions across all CapabilityRollupDefinitions
   */
  int countStationCapabilityRollupDefinitions() {

    return stationSohMonitoringDefinition.getCapabilitySohRollupDefinitions().stream()
        .mapToInt(capabilityRollupDefinition -> capabilityRollupDefinition
            .getStationRollupDefinitionsByStation().keySet().size())
        .sum();
  }

  /**
   * Number of ChannelRollupDefintions across all StationRollupDefinitions across all CapabilityRollupDefinitions
   * @return
   */
  int countChannelCapabilityRollupDefinitions() {

    return stationSohMonitoringDefinition.getCapabilitySohRollupDefinitions().stream()
        .flatMap(capabilitySohRollupDefinition -> capabilitySohRollupDefinition
            .getStationRollupDefinitionsByStation().values().stream())
        .mapToInt(
            stationRollupDefinition -> stationRollupDefinition.getChannelRollupDefinitionsByChannel()
                .keySet().size())
        .sum();
  }
}
