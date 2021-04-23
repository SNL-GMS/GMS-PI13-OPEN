package gms.dataacquisition.stationreceiver.cd11.injector.configuration;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Validator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import java.net.InetAddress;


@AutoValue
@JsonSerialize(as = StationConfig.class)
@JsonDeserialize(builder = AutoValue_StationConfig.Builder.class)
public abstract class StationConfig {

  public static Builder builder() {
    return new AutoValue_StationConfig.Builder();
  }

  public abstract String getId();

  public abstract String getDataPath();

  @Nullable
  public abstract InetAddress getConsumerAddress();

  @Nullable
  public abstract Integer getConsumerPort();

  public abstract boolean isEnabled();

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  @SuppressWarnings("squid:S1610") // No this class shouldn't be an interface
  public abstract static class Builder {

    /**
     * @param id station id/name per CD.1. spec
     */
    public abstract Builder setId(String id);


    /**
     * @param datapath location of station data to be injected
     */
    public abstract Builder setDataPath(String datapath);

    /**
     * @param consumerAddress ip address of the data consumer for this station
     */
    public abstract Builder setConsumerAddress(InetAddress consumerAddress);


    /**
     * @param consumerPort network port of the data consumer for this station
     */
    public abstract Builder setConsumerPort(Integer consumerPort);

    /**
     * @param enabled whether this station is enabled for the injection
     */
    public abstract Builder setEnabled(boolean enabled);

    abstract StationConfig autoBuild();

    public StationConfig build() {
      StationConfig config = autoBuild();

      // station
      checkState(!config.getId().isEmpty(), "Invalid config: a station id is missing or empty");
      Cd11Validator.validStationOrResponderName(config.getId());

      // station-specific items if enabled
      if (config.isEnabled()) { // NOSONAR : this was just validated as not null
        checkState(!config.getDataPath().isEmpty(),
                "Invalid config: station %s's 'datapath' does not exist or is blank",
                config.getId());
        Path path = Paths.get(config.getDataPath());
        checkState(Files.exists(path),
                "Path %s specified in 'datapth' for station %s does not exist",
                config.getDataPath(), config.getId());
        checkState(Files.isDirectory(path),
                "Path %s specified in 'datapath' for station %s is not a directory",
                config.getDataPath(),
                config.getId());

        if (config.getConsumerAddress() != null && config.getConsumerPort() != null) {
          checkNotNull(config.getConsumerAddress(), "Consumer address cannot be empty");
          Cd11Validator.validNonZeroPortNumber(config.getConsumerPort());
        }
      }
      return config;
    }
  }

}
