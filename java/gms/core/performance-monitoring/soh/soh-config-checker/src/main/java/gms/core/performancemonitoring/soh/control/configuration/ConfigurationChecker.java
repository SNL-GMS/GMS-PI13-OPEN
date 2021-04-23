package gms.core.performancemonitoring.soh.control.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import gms.core.performancemonitoring.soh.control.StationSohControlConfiguration;
import gms.shared.frameworks.configuration.repository.FileConfigurationRepository;
import gms.shared.frameworks.osd.api.station.StationGroupRepositoryInterface;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroupDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * Main class for "configuration checker". This loads config from the provided directory, using
 * the OSD service at the provided host name. It then prints out statistics about the configuration.
 *
 * Hopefully there will be more to come, like digging into configuration to find values or sets
 * of values, etc.
 */
public class ConfigurationChecker {

  public static void main(String... args) {

    getCommandLineArguments(args).ifPresentOrElse(
        commandLineArguments -> {

          if (!commandLineArguments.suppressInfo()) {
            System.out.println("Loading configuration...");
          }

          StationSohControlConfiguration stationSohControlConfiguration;

          try {
            var startMs = System.currentTimeMillis();

            stationSohControlConfiguration = getStationSohControlConfiguration(
                FileConfigurationRepository.create(
                    Path.of(commandLineArguments.getConfigurationDirectory())
                ),
                getSohRepositoryInterface(
                    commandLineArguments.getOsdHostName(),
                    commandLineArguments.getStationGroups(),
                    commandLineArguments.getStations(),
                    commandLineArguments.printTimingInfo()
                )
            );

            if (commandLineArguments.printTimingInfo()) {
              System.out.println("Overall config resolution (including OSD operation) took "
              + (System.currentTimeMillis() - startMs) + " ms");
            }
          } catch (IOException e) {
            e.printStackTrace();
            return;
          }

          var stationSohMonitoringDefinition = stationSohControlConfiguration
              .getInitialConfigurationPair()
              .getStationSohMonitoringDefinition();

          if (!commandLineArguments.suppressInfo()) {

            System.out.println("Configuration successfully loaded! \n");

            printStatistics(stationSohMonitoringDefinition);
          }

          if (commandLineArguments.printJson()) {
            try {
              System.out.println(
                  CoiObjectMapperFactory.getJsonObjectMapper().writeValueAsString(
                      stationSohMonitoringDefinition
                  )
              );
            } catch (JsonProcessingException e) {
              e.printStackTrace();
            }
          }
        },
        () -> {
          // No arguments, do nothing. The parser will have printed a no-args message.
        }
    );

  }

  /**
   * Retrieve the command line arguments from the command line.
   * @param args list of raw arguments
   * @return CommandLineArguments object with parsed out parameter values.
   */
  private static Optional<CommandLineArguments> getCommandLineArguments(String[] args) {

    CommandLineArguments commandLineArguments = new CommandLineArguments();

    CmdLineParser parser = new CmdLineParser(commandLineArguments);

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      return Optional.empty();
    }

    return Optional.of(commandLineArguments);
  }

  private static StationSohControlConfiguration getStationSohControlConfiguration(
      FileConfigurationRepository fileConfigurationRepository,
      StationGroupRepositoryInterface sohRepositoryInterface
  ) {

    return StationSohControlConfiguration
        .create(
            fileConfigurationRepository, sohRepositoryInterface
        );
  }

  /**
   * Return an OsdRepositoryInterface object with an implementation of retrieveStationGroups.
   * When called, retrieveStationGroups connects to the provided OSD service host to retrieve
   * station groups.
   *
   * @param host OSD service host name
   */
  private static StationGroupRepositoryInterface getSohRepositoryInterface(String host,
      List<String> onlyStationGroups, List<String> onlyStations, boolean printTimingInfo)
      throws IOException {
    return new StationGroupRepositoryInterface() {

      private URLConnection connection = new URL(
          "http://" + host + "/osd/station-groups")
          .openConnection();

      @Override
      public List<StationGroup> retrieveStationGroups(Collection<String> stationGroupNames) {

        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;

        try {
          httpURLConnection.setRequestMethod("POST"); // PUT is another valid option
        } catch (ProtocolException e) {
          e.printStackTrace();

          return List.of();
        }

        httpURLConnection.setDoOutput(true);

        try {
          String requestBody = CoiObjectMapperFactory.getJsonObjectMapper()
              .writeValueAsString(stationGroupNames);
          httpURLConnection.setFixedLengthStreamingMode(requestBody.getBytes().length);
          httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
          httpURLConnection.connect();
          try (OutputStream os = httpURLConnection.getOutputStream()) {
            os.write(requestBody.getBytes());
          }

          var startMs = System.currentTimeMillis();

          String response;
          try (InputStream is = httpURLConnection.getInputStream()) {
            response = new String(is.readAllBytes());
          }

          if (printTimingInfo) {
            System.out.println("OSD Station group operation took "
                + (System.currentTimeMillis() - startMs) + " ms");
          }

          return trimStationGroupTree(
              onlyStations,

              ((List<StationGroup>) CoiObjectMapperFactory.getJsonObjectMapper()
                  .readValue(response, new TypeReference<List<StationGroup>>() {
                  })).stream()
                  .filter(
                      stationGroup -> onlyStationGroups.isEmpty() || onlyStationGroups
                          .contains(stationGroup.getName())
                  )
                  .collect(Collectors.toList())
          );

        } catch (IOException e) {
          e.printStackTrace();
        }

        return List.of();
      }

      @Override
      public void storeStationGroups(Collection<StationGroup> stationGroups) {

      }

      @Override
      public void updateStationGroups(Collection<StationGroupDefinition> stationGroupDefinitions) {

      }
    };
  }

  /**
   * "Trims" the "station group tree" that represents the list of station groups. What this means is
   * that given a set of stations the following occurs:
   *
   * <ul>
   *   <li>
   *     All station groups that contain none of the provided stations are filtered out
   *   </li>
   *   <li>
   *     The station groups that remain each contain ONLY a subset of the passed in stations.
   *   </li>
   * </ul>
   *
   *
   * @param onlyStations
   * @param stationGroups
   * @return
   */
  private static List<StationGroup> trimStationGroupTree(
      Collection<String> onlyStations,
      List<StationGroup> stationGroups
  ) {

    if (onlyStations.isEmpty()) {
      return stationGroups;
    }

    return stationGroups.stream()
        //
        // Filter out station groups that contain none of the stations
        //
        .filter(stationGroup -> !Collections.disjoint(
            stationGroup.getStations().stream()
            .map(Station::getName)
            .collect(Collectors.toList())
            , onlyStations)
        )
        //
        // All station groups that make it here have a set of stations that intersects
        // onlyStations. For each of those station groups, we now create a new station group that
        // ONLY has the intersection.
        //
        .map(stationGroup -> StationGroup.from(
            stationGroup.getName(),
            stationGroup.getDescription(),
            stationGroup.getStations().stream()
                .filter(station -> onlyStations.contains(station.getName()))
                .collect(Collectors.toList())
        )).collect(Collectors.toList());
  }

  private static void printStatistics(StationSohMonitoringDefinition definition) {

    ConfigurationAnalyzer configurationAnalyzer = new ConfigurationAnalyzer(
        definition
    );

    System.out.println(
        String.format(
            "Station SOH definitions: %d \n"
                + "Channel SOH definitions: %d \n"
                + "Channels by monitor type entries: %d \n"
                + "Station monitor types for rollup: %d \n"
                + "Channel monitor types for rollup: %d \n"
                + "Entries of monitor type -> soh status: %d \n"
                + "Capability rollup definitions: %d \n"
                + "Station rollup definitions: %d \n"
                + "Channel rollup definitions: %d \n",
            configurationAnalyzer.countStationSohDefinitions(),
            configurationAnalyzer.countChannelSohDefinitions(),
            configurationAnalyzer.countChannelsByMonitorTypeEntries(),
            configurationAnalyzer.countStationMonitorTypesForRollup(),
            configurationAnalyzer.countChannelMonitorTypesForRollup(),
            configurationAnalyzer.countSohMonitorValueAndStatusDefinitionBySohMonitorType(),
            configurationAnalyzer.countCapabilityRollupDefinitions(),
            configurationAnalyzer.countStationCapabilityRollupDefinitions(),
            configurationAnalyzer.countChannelCapabilityRollupDefinitions()
        )
    );
  }
}
