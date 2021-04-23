package gms.dataacquisition.stationreceiver.cd11.injector.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Validator;

import org.apache.commons.lang3.Validate;

import java.net.InetAddress;

@AutoValue
@JsonSerialize(as = StationRepeaterConfig.class)
@JsonDeserialize(builder = AutoValue_StationRepeaterConfig.Builder.class)
public abstract class StationRepeaterConfig {

  public static Builder builder() {
    return new AutoValue_StationRepeaterConfig.Builder();
  }

  public abstract Integer getRepeaterTimeoutSeconds();

  public abstract String getKafkaConsumerTopic();

  public abstract String getKafkaConsumerID();

  public abstract String getKafkaRepeaterServers();

  public abstract String getStationName();

  public abstract InetAddress getConsumerAddress();

  public abstract Integer getConsumerPort();

  public abstract String getFrameDestination();

  public abstract String getFrameCreator();

  // Ignoring unknown because we map to this from a vert.x config which has properties from the env
  @JsonIgnoreProperties(ignoreUnknown = true)
  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  @SuppressWarnings("squid:S1610") // No this class shouldn't be an interface
  public abstract static class Builder {
    /**
     * @param timeoutSeconds how long between received messages before the repeater will terminate
     */
    public abstract Builder setRepeaterTimeoutSeconds(Integer timeoutSeconds);

    /**
     * @param kafkaConsumerTopic kafka topic for this repeater to consume
     */
    public abstract Builder setKafkaConsumerTopic(String kafkaConsumerTopic);

    /**
     * @param kafkaConsumerID unique identifier so kafka consumers don't overlap offsets/messages
     */
    public abstract Builder setKafkaConsumerID(String kafkaConsumerID);

    /**
     * @param stationName name of the station per the CD1.1 spec
     */
    public abstract Builder setStationName(String stationName);

    /**
     * @param kafkaRepeaterServers comma-separated host:port specifying the kafka broker to consume from
     */
    public abstract Builder setKafkaRepeaterServers(String kafkaRepeaterServers);

    /**
     * @param consumerAddress ipv4 or v6 address of the data consumer for this station
     */
    public abstract Builder setConsumerAddress(InetAddress consumerAddress);

    /**
     * @param consumerPort Port of the data consumer for this station
     */
    public abstract Builder setConsumerPort(Integer consumerPort);

    /**
     * @param frameDestination frame destination per the CD1.1 spec
     */
    public abstract Builder setFrameDestination(String frameDestination);

    /**
     * @param frameCreator frame creator per the CD1.1 spec
     */
    public abstract Builder setFrameCreator(String frameCreator);

    public abstract StationRepeaterConfig autoBuild();

    public StationRepeaterConfig build() {

      StationRepeaterConfig config = autoBuild();

      // station
      Cd11Validator.validStationOrResponderName(config.getStationName());

      // net location
      Validate.notBlank(config.getConsumerAddress().toString(),
              "station injector setting 'consumerAddress' not present or blank");
      Cd11Validator.validIpAddress(config.getConsumerAddress());
      Cd11Validator.validNonZeroPortNumber(config.getConsumerPort());


      // Frame fields
      Validate.notBlank(config.getFrameDestination(),
              "Station injector setting 'frameDestination' not present or blank");
      Cd11Validator.validFrameDestination(config.getFrameDestination());
      Validate
              .notBlank(config.getFrameCreator(),
                      "Station injector setting 'frameCreator' not present or blank");
      Cd11Validator.validFrameCreator(config.getFrameCreator());

      return config;
    }

  }
}

