package gms.dataacquisition.stationreceiver.cd11.injector.configuration;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Validator;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;

@AutoValue
@JsonSerialize(as = StationInjectorConfig.class)
@JsonDeserialize(builder = AutoValue_StationInjectorConfig.Builder.class)
public abstract class StationInjectorConfig {


  public static final boolean DEFAULT_METER_DATA_STREAM = true;

  public static Builder builder() {
    return new AutoValue_StationInjectorConfig.Builder()
            .setMeterDataStream(DEFAULT_METER_DATA_STREAM);
  }

  public abstract String getStationName();

  public abstract InetAddress getConsumerAddress();

  public abstract int getConsumerPort();

  public abstract String getStationDataPath();

  public abstract Instant getDataReferenceTime();

  public abstract Instant getTargetStartTime();

  public abstract boolean getLoopDataStream();

  public abstract boolean getMeterDataStream();

  public abstract int getMaxConcurrentOpenFiles();

  public abstract String getFrameDestination();

  public abstract String getFrameCreator();

  // Ignoring unknown because we map to this from a vert.x config which has properties from the env
  @JsonIgnoreProperties(ignoreUnknown = true)
  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  @SuppressWarnings("squid:S1610") // No this class shouldn't be an interface
  public abstract static class Builder {

    /**
     * @param stationName name of the station per the CD1.1 spec
     */
    public abstract Builder setStationName(String stationName);

    /**
     * @param consumerAddress ipv4 or v6 address of the data consumer for this station
     */
    public abstract Builder setConsumerAddress(InetAddress consumerAddress);

    /**
     * @param consumerPort Port of the data consumer for this station
     */
    public abstract Builder setConsumerPort(int consumerPort);

    /**
     * @param stationDataPath Directory of the raw station data frames to be injected
     */
    public abstract Builder setStationDataPath(String stationDataPath);

    /**
     * Reference time for the data, i.e. earliest time of  all data across all injectors
     *
     * @param dataReferenceTime reference time for the data
     */
    public abstract Builder setDataReferenceTime(Instant dataReferenceTime);


    /**
     * @param targetStartTime Start time for the injection
     */
    public abstract Builder setTargetStartTime(Instant targetStartTime);


    /**
     * @param loopDataStream whether to loop the data stream
     */
    public abstract Builder setLoopDataStream(boolean loopDataStream);

    /**
     * Max number of concurrently open files; this restricts parallelism when determining the start
     * time of all injection data files
     *
     * @param maxConcurrentOpenFiles Max number of concurrently open files
     */
    public abstract Builder setMaxConcurrentOpenFiles(int maxConcurrentOpenFiles);

    /**
     * @param frameDestination frame destination per the CD1.1 spec
     */
    public abstract Builder setFrameDestination(String frameDestination);

    /**
     * @param frameCreator frame creator per the CD1.1 spec
     */
    public abstract Builder setFrameCreator(String frameCreator);

    public abstract StationInjectorConfig autoBuild();

    abstract Optional<Boolean> getMeterDataStream();

    /**
     * @param meterDataStream whether the data stream should be metered
     */
    public abstract Builder setMeterDataStream(boolean meterDataStream);

    public StationInjectorConfig build() {
      if (getMeterDataStream().isEmpty()) {
        setMeterDataStream(StationInjectorConfig.DEFAULT_METER_DATA_STREAM);
      }

      StationInjectorConfig config = autoBuild();

      // station
      Cd11Validator.validStationOrResponderName(config.getStationName());

      // net location
      checkNotNull(config.getConsumerAddress(),
              "station injector setting 'consumerAddress' not present or blank");
      Cd11Validator.validIpAddress(config.getConsumerAddress());
      Cd11Validator.validNonZeroPortNumber(config.getConsumerPort());

      // station data location
      checkState(!config.getStationDataPath().isBlank(),
              "Station injector setting 'stationDataPath' not present or blank");
      Path path = Paths.get(config.getStationDataPath());

      checkState(Files.exists(path),
              "Path %s specified in station injector setting 'stationDataPath' does not exist",
              config.getStationDataPath());
      checkState(Files.isDirectory(path),
              "Path %s specified in station injector setting 'stationDataPath' is not a directory",
              config.getStationDataPath());

      // Frame fields
      checkState(!config.getFrameDestination().isBlank(),
              "Station injector setting 'frameDestination' not present or blank");
      Cd11Validator.validFrameDestination(config.getFrameDestination());
      checkState(!config.getFrameCreator().isBlank(),
              "Station injector setting 'frameCreator' not present or blank");
      Cd11Validator.validFrameCreator(config.getFrameCreator());

      return config;
    }

  }
}

