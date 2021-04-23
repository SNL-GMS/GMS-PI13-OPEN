package gms.dataacquisition.stationreceiver.cd11.injector.configuration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;

import java.util.Optional;

@AutoValue
@JsonSerialize(as = RepeaterConfig.class)
@JsonDeserialize(builder = AutoValue_RepeaterConfig.Builder.class)
public abstract class RepeaterConfig {

  private static final boolean DEFAULT_RUN_DEBUG_KAFKA_PUBLISHER = false;

  public static Builder builder() {
    return new AutoValue_RepeaterConfig.Builder().setRunDebugKafkaPublisher(DEFAULT_RUN_DEBUG_KAFKA_PUBLISHER);
  }

  @Nullable
  @JsonAlias({"GMS_CONFIG_KAFKA_CONSUMER_ID", "kafkaConsumerID"})
  public abstract String getKafkaConsumerID();

  @JsonAlias({"GMS_CONFIG_REPEATER_TIMEOUT_SECONDS", "repeaterTimeoutSeconds"})
  public abstract Integer getRepeaterTimeoutSeconds();

  @JsonAlias({"GMS_CONFIG_KAFKA_CONSUMER_TOPIC", "kafkaConsumerTopic"})
  public abstract String getKafkaConsumerTopic();

  @JsonAlias({"GMS_CONFIG_KAFKA_REPEATER_SERVERS", "kafkaRepeaterServers"})
  public abstract String getKafkaRepeaterServers();

  @JsonAlias({"GMS_CONFIG_CONNMAN_ADDRESS", "connManAddress"})
  public abstract String getConnManAddress();

  @JsonAlias({"GMS_CONFIG_CONNMAN_PORT", "connManPort"})
  public abstract Integer getConnManPort();

  @JsonAlias({"GMS_CONFIG_RUN_DEBUG_KAFKA_PUBLISHER", "runDebugKafkaPublisher"})
  public abstract Boolean getRunDebugKafkaPublisher();

  @JsonAlias({"GMS_CONFIG_FRAME_DESTINATION", "frameDestination"})
  public abstract String getFrameDestination();

  @JsonAlias({"GMS_CONFIG_FRAME_CREATOR", "frameCreator"})
  public abstract String getFrameCreator();

  @JsonIgnoreProperties(ignoreUnknown = true)
  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  @SuppressWarnings("squid:S1610") // No this class shouldn't be an interface
  public abstract static class Builder {
    /**
     * @param kafkaConsumerID unique identifier so kafka consumers don't overlap offsets/messages
     */
    @JsonAlias({"GMS_CONFIG_KAFKA_CONSUMER_ID", "kafkaConsumerID"})
    public abstract Builder setKafkaConsumerID(String kafkaConsumerID);

    /**
     * @param timeoutSeconds how long between received messages before the repeater will terminate
     */
    @JsonAlias({"GMS_CONFIG_REPEATER_TIMEOUT_SECONDS", "repeaterTimeoutSeconds"})
    public abstract Builder setRepeaterTimeoutSeconds(Integer timeoutSeconds);

    /**
     * @param kafkaConsumerTopic kafka topic for this repeater to consume
     */
    @JsonAlias({"GMS_CONFIG_KAFKA_CONSUMER_TOPIC", "kafkaConsumerTopic"})
    public abstract Builder setKafkaConsumerTopic(String kafkaConsumerTopic);

    /**
     * @param kafkaBrokerAddress comma-separated host:port specifying the kafka broker to consume from
     */
    @JsonAlias({"GMS_CONFIG_KAFKA_REPEATER_SERVERS", "kafkaRepeaterServers"})
    public abstract Builder setKafkaRepeaterServers(String kafkaBrokerAddress);

    /**
     * @param connManAddress ip address of ConnMan
     */
    @JsonAlias({"GMS_CONFIG_CONN_MAN_ADDRESS", "connManAddress"})
    public abstract Builder setConnManAddress(String connManAddress);

    /**
     * @param connManPort ConnMan's network port
     */
    @JsonAlias({"GMS_CONFIG_CONN_MAN_PORT", "connManPort"})
    public abstract Builder setConnManPort(Integer connManPort);

    /**
     * @param setRunDebugKafkaPublisher toggle for running the debug kafka publisher (to populate the broker)
     */
    @JsonAlias({"GMS_CONFIG_RUN_DEBUG_KAFKA_PUBLISHER", "runDebugKafkaPublisher"})
    public abstract Builder setRunDebugKafkaPublisher(Boolean setRunDebugKafkaPublisher);

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

    abstract RepeaterConfig autoBuild();

    abstract Optional<Boolean> getRunDebugKafkaPublisher();

    public RepeaterConfig build() {

      RepeaterConfig config = autoBuild();

      Validate.inclusiveBetween(0, Integer.MAX_VALUE, config.getRepeaterTimeoutSeconds(),
              "Invalid config: repeaterTimeoutSeconds cannot be negative");

      if (getRunDebugKafkaPublisher().isEmpty()) {
        setRunDebugKafkaPublisher(DEFAULT_RUN_DEBUG_KAFKA_PUBLISHER);
      }

      return config;
    }
  }
}
