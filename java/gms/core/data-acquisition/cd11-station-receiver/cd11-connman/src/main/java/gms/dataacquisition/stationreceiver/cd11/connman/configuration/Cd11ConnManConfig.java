package gms.dataacquisition.stationreceiver.cd11.connman.configuration;

import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11DataConsumerParameters;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11DataConsumerParametersTemplatesFile;
import gms.shared.frameworks.configuration.ConfigurationRepository;
import gms.shared.frameworks.configuration.repository.client.ConfigurationConsumerUtility;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class Cd11ConnManConfig {

  public static final String DEFAULT_RESPONDER_NAME = "TEST";
  public static final String DEFAULT_RESPONDER_TYPE = "IDC";
  public static final String DEFAULT_SERVICE_TYPE = "TCP";
  public static final String DEFAULT_FRAME_CREATOR = "TEST";
  public static final String DEFAULT_FRAME_DESTINATION = "0";

  ConfigurationConsumerUtility configurationConsumerUtility;
  private static final String SEPARATOR = ".";
  private static final String CONFIGURATION_NAME_PREFIX = "connman" + SEPARATOR;
  private static final UnaryOperator<String> KEY_BUILDER = s -> CONFIGURATION_NAME_PREFIX + s;
  private static final String CONFIGURATION_NAME =
      KEY_BUILDER.apply("station-parameters");

  List<Cd11DataConsumerParameters> configuredStations;

  private String responderName;
  private String responderType;
  private String serviceType;
  private String frameCreator;
  private String frameDestination;
  private final int cd11DataConsumerBasePort;

  private Cd11ConnManConfig(ConfigurationConsumerUtility configurationConsumerUtility,
      int datConsumerBasePort) {

    this.configurationConsumerUtility = configurationConsumerUtility;

    setResponderName(DEFAULT_RESPONDER_NAME);
    setResponderType(DEFAULT_RESPONDER_TYPE);
    setServiceType(DEFAULT_SERVICE_TYPE);
    setFrameCreator(DEFAULT_FRAME_CREATOR);
    setFrameDestination(DEFAULT_FRAME_DESTINATION);

    this.cd11DataConsumerBasePort = datConsumerBasePort;

    //use the configurationConsumerUtility to load the station port map, currently in cd11Station
    configuredStations = getCd11StationParameters();
  }

  /**
   * Obtain a new {@link Cd11ConnManConfig} using the provided {@link ConfigurationConsumerUtility}
   * to provide QC configuration.
   *
   * @param configurationConsumerUtility {@link ConfigurationConsumerUtility}, not null
   * @return {@link Cd11ConnManConfig}, not null
   * @throws NullPointerException if configurationConsumerUtility is null
   */
  public static Cd11ConnManConfig create(
      ConfigurationConsumerUtility configurationConsumerUtility, int dataConsumerBasePort) {
    return new Cd11ConnManConfig(configurationConsumerUtility, dataConsumerBasePort);
  }

  public static Cd11ConnManConfig create(ConfigurationRepository configurationRepository,
                                         int dataConsumerBasePort) {

    Objects.requireNonNull(configurationRepository,
            "Cd11ConnManConfig cannot be created with null "
                    + "ConfigurationRepository");

    // Construct a ConfigurationConsumerUtility with the provided configurationRepository and
    // the necessary ConfigurationTransforms
    final ConfigurationConsumerUtility configurationConsumerUtility = ConfigurationConsumerUtility
            .builder(configurationRepository)
            .configurationNamePrefixes(List.of(CONFIGURATION_NAME_PREFIX))
            .build();

    return new Cd11ConnManConfig(configurationConsumerUtility, dataConsumerBasePort);
  }

  public List<Cd11DataConsumerParameters> getCd11StationParameters() {

    Cd11DataConsumerParametersTemplatesFile templatesFile = configurationConsumerUtility
        .resolve(CONFIGURATION_NAME, Collections.emptyList(),
            Cd11DataConsumerParametersTemplatesFile.class);

    return templatesFile.getCd11DataConsumerParametersTemplates().stream()
        .map(template -> Cd11DataConsumerParameters.create(template, cd11DataConsumerBasePort))
        .collect(Collectors.toList());
  }

  public String getResponderName() {
    return responderName;
  }

  public String getResponderType() {
    return responderType;
  }

  public String getServiceType() {
    return serviceType;
  }

  public String getFrameCreator() {
    return frameCreator;
  }

  public String getFrameDestination() {
    return frameDestination;
  }

  public void setResponderName(String responderName) {
    this.responderName = responderName;
  }

  public void setResponderType(String responderType) {
    this.responderType = responderType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public void setFrameCreator(String frameCreator) {
    this.frameCreator = frameCreator;
  }

  public void setFrameDestination(String frameDestination) {
    this.frameDestination = frameDestination;
  }

}


