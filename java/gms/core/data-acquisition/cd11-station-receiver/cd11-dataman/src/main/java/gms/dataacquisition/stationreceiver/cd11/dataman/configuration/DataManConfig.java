package gms.dataacquisition.stationreceiver.cd11.dataman.configuration;

import static com.google.common.base.Preconditions.checkNotNull;

import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11DataConsumerParameters;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11DataConsumerParametersTemplatesFile;
import gms.shared.frameworks.configuration.ConfigurationRepository;
import gms.shared.frameworks.configuration.repository.client.ConfigurationConsumerUtility;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class DataManConfig {

  private static final String SEPARATOR = ".";
  private static final String CONFIGURATION_NAME_PREFIX = "dataman" + SEPARATOR;
  private static final UnaryOperator<String> KEY_BUILDER = s -> CONFIGURATION_NAME_PREFIX + s;

  private final ConfigurationConsumerUtility configurationConsumerUtility;
  private static final String CONFIGURATION_NAME =
      KEY_BUILDER.apply("station-parameters");

  private final int cd11DataConsumerBasePort;

  private DataManConfig(ConfigurationConsumerUtility configurationConsumerUtility,
      int dataConsumerBasePort) {
    this.configurationConsumerUtility = configurationConsumerUtility;
    this.cd11DataConsumerBasePort = dataConsumerBasePort;
  }

  public static DataManConfig create(
      ConfigurationConsumerUtility processingConfigurationConsumerUtility, int dataConsumerBasePort) {
    return new DataManConfig(processingConfigurationConsumerUtility, dataConsumerBasePort);
  }

  public static DataManConfig create(ConfigurationRepository configurationRepository,
      int dataConsumerBasePort) {

    checkNotNull(configurationRepository,
        "Cd11StationConfigurationControl cannot be created with null "
            + "ConfigurationRepository");

    // Construct a ConfigurationConsumerUtility with the provided configurationRepository and
    // the necessary ConfigurationTransforms
    final ConfigurationConsumerUtility configurationConsumerUtility = ConfigurationConsumerUtility
        .builder(configurationRepository)
        .configurationNamePrefixes(List.of(CONFIGURATION_NAME_PREFIX))
        .build();

    return new DataManConfig(configurationConsumerUtility, dataConsumerBasePort);
  }


  public List<Cd11DataConsumerParameters> getCd11StationParameters() {
    Cd11DataConsumerParametersTemplatesFile stationConfig = configurationConsumerUtility
        .resolve(CONFIGURATION_NAME, Collections.emptyList(), Cd11DataConsumerParametersTemplatesFile.class);

    return stationConfig.getCd11DataConsumerParametersTemplates().stream()
        .map(template -> Cd11DataConsumerParameters.create(template, cd11DataConsumerBasePort))
        .collect(Collectors.toList());
  }
}
