package gms.dataacquisition.stationreceiver.cd11.injector.configuration;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Validator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;

@AutoValue
@JsonSerialize(as = InjectionConfig.class)
@JsonDeserialize(builder = AutoValue_InjectionConfig.Builder.class)
public abstract class InjectionConfig {

  public static Builder builder() {
    return new AutoValue_InjectionConfig.Builder().setStationConfigs(new ArrayList<>());
  }

  @JsonAlias({"GMS_CONFIG_USE_CONNMAN", "useConnman"})
  public abstract Boolean getUseConnman();

  @Nullable
  @JsonAlias({"GMS_CONFIG_CONNMAN_ADDRESS", "connManAddress"})
  public abstract String getConnManAddress();

  @Nullable
  @JsonAlias({"GMS_CONFIG_CONNMAN_PORT", "connManPort"})
  public abstract Integer getConnManPort();

  @JsonAlias({"GMS_CONFIG_USE_BASE_PATH_DISCOVERY", "useBasePathDiscovery"})
  public abstract Boolean getUseBasePathDiscovery();

  @JsonAlias({"GMS_CONFIG_RSDF_DISCOVERY_BASE_PATH", "rsdfDiscoveryBasePath"})
  public abstract String getRsdfDiscoveryBasePath();

  @JsonAlias({"GMS_CONFIG_LOOP_DATA_STREAM", "loopDataStream"})
  public abstract Boolean getLoopDataStream();

  @JsonAlias({"GMS_CONFIG_PRE_START_DELAY_SECONDS", "preStartDelaySeconds"})
  public abstract Integer getPreStartDelaySeconds();

  @JsonAlias({"GMS_CONFIG_MAX_CONCURRENT_OPEN_FILES", "maxConcurrentOpenFiles"})
  public abstract Integer getMaxConcurrentOpenFiles();

  @JsonAlias({"GMS_CONFIG_REFERENCE_TIME", "referenceTime"})
  public abstract Instant getReferenceTime();

  @JsonAlias({"GMS_CONFIG_FRAME_DESTINATION", "frameDestination"})
  public abstract String getFrameDestination();

  @JsonAlias({"GMS_CONFIG_FRAME_CREATOR", "frameCreator"})
  public abstract String getFrameCreator();

  public abstract Collection<StationConfig> getStationConfigs();

  @JsonIgnoreProperties(ignoreUnknown = true)
  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  @SuppressWarnings("squid:S1610") // No this class shouldn't be an interface
  public abstract static class Builder {

    /**
     * @param useConnman Whether the injector should use ConnMan to determine consumer's
     *                   address/port
     */
    @JsonAlias({"GMS_CONFIG_USE_CONNMAN", "useConnman"})
    public abstract Builder setUseConnman(Boolean useConnman);

    /**
     * @param connManAddress ip address of ConnMan
     */
    @JsonAlias({"GMS_CONFIG_CONNMAN_ADDRESS", "connManAddress"})
    public abstract Builder setConnManAddress(String connManAddress);

    /**
     * @param connManPort ConnMan's network port
     */
    @JsonAlias({"GMS_CONFIG_CONNMAN_PORT", "connManPort"})
    public abstract Builder setConnManPort(Integer connManPort);


    /**
     * @param useBasePathDiscovery whether the injector should search the filesystem for stations
     */
    @JsonAlias({"GMS_CONFIG_USE_BASE_PATH_DISCOVERY", "useBasePathDiscovery"})
    public abstract Builder setUseBasePathDiscovery(Boolean useBasePathDiscovery);

    /**
     * @param rsdfDiscoveryBasePath where injector should start looking for stations
     */
    @JsonAlias({"GMS_CONFIG_RSDF_DISCOVERY_BASE_PATH", "rsdfDiscoveryBasePath"})
    public abstract Builder setRsdfDiscoveryBasePath(String rsdfDiscoveryBasePath);

    /**
     * @param loopDataStream whether to loop the data stream when it completes
     */
    @JsonAlias({"GMS_CONFIG_LOOP_DATA_STREAM", "loopDataStream"})
    public abstract Builder setLoopDataStream(Boolean loopDataStream);

    /**
     * @param preStartDelaySeconds represents the startup time of the injector
     */
    @JsonAlias({"GMS_CONFIG_PRE_START_DELAY_SECONDS", "preStartDelaySeconds"})
    public abstract Builder setPreStartDelaySeconds(Integer preStartDelaySeconds);

    /**
     * @param maxConcurrentOpenFiles max number of concurrently open files
     */
    @JsonAlias({"GMS_CONFIG_MAX_CONCURRENT_OPEN_FILES", "maxConcurrentOpenFiles"})
    public abstract Builder setMaxConcurrentOpenFiles(Integer maxConcurrentOpenFiles);

    /**
     * @param referenceTime reference time of the data
     */
    @JsonAlias({"GMS_CONFIG_REFERENCE_TIME", "referenceTime"})
    public abstract Builder setReferenceTime(Instant referenceTime);

    /**
     * @param frameDestination frame destination per CD1.1 spec
     */
    @JsonAlias({"GMS_CONFIG_FRAME_DESTINATION", "frameDestination"})
    public abstract Builder setFrameDestination(String frameDestination);


    /**
     * @param frameCreator frame creator per CD1.1 spec
     */
    @JsonAlias({"GMS_CONFIG_FRAME_CREATOR", "frameCreator"})
    public abstract Builder setFrameCreator(String frameCreator);

    abstract Optional<Collection<StationConfig>> getStationConfigs();

    /**
     * @param stationConfigs station-specific configurations
     */
    @JsonProperty(value = "stations")
    public abstract Builder setStationConfigs(Collection<StationConfig> stationConfigs);

    abstract InjectionConfig autoBuild();

    public InjectionConfig build() {
      // Provide an empty list of station configs if the value isn't there
      if (getStationConfigs().isEmpty()) {
        setStationConfigs(new ArrayList<>());
      }

      InjectionConfig config = autoBuild();

      // prestart delay
      checkState(0 <= config.getPreStartDelaySeconds(),
          "Invalid config: preStartDelay cannot be negative");

      // station discovery by filesystem traversal
      if (config.getUseBasePathDiscovery()) { // NOSONAR: this was just validated as not null
        checkState(!config.getRsdfDiscoveryBasePath().isBlank(),
            "Invalid config: setting 'rsdfDiscoveryBasePath' not present or blank");

        Path path = Paths.get(config.getRsdfDiscoveryBasePath());
        checkState(Files.exists(path),
            "Path %s specified in injection setting 'rsdfDiscoveryBasePath' does not exist",
            config.getRsdfDiscoveryBasePath());
        checkState(Files.isDirectory(path),
            "Path %s specified in injection setting 'rsdfDiscoveryBasePath' is not a directory",
            config.getRsdfDiscoveryBasePath());


      } else {
        checkState(!config.getStationConfigs().isEmpty(),
            "Invalid config: Not using basepath discovery, but there are no stations individually specified");
      }

      // connman use
      if (config.getUseConnman()) { // NOSONAR : this was just validated as not null
        // address can't be further validated: could be ip or hostname
        checkState(isNotEmpty(config.getConnManAddress()),
            "Invalid config: using connman but connManAddress does not exist or is empty");

        checkNotNull(config.getConnManPort(),
            "Invalid config: using connman but connManPort does not exist");
        Cd11Validator.validNonZeroPortNumber(config.getConnManPort());
      }

      // Stations are consistent with global settings
      for (StationConfig stationConfig : config.getStationConfigs()) {
        if (!stationConfig.isEnabled()) {
          continue;
        }
        if (!config.getUseConnman()) { // NOSONAR : this was just validated as not null
          checkNotNull(stationConfig.getConsumerAddress(),
              "Invalid config: not using connman but station %s's 'consumerAddress' does not exist or is empty",
              stationConfig.getId());
          checkNotNull(stationConfig.getConsumerPort(),
              "Invalid config: nut using connman but station %s's 'consumerPort' does not exist",
              stationConfig.getConsumerPort());
        }
      }

      return config;
    }

  }
}
