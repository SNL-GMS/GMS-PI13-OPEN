package gms.dataacquisition.stationreceiver.cd11.dataman.configuration;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import gms.shared.frameworks.systemconfig.SystemConfig;

@AutoValue
@JsonSerialize(as = KafkaConnectionConfiguration.class)
@JsonDeserialize(builder = AutoValue_KafkaConnectionConfiguration.Builder.class)
public abstract class KafkaConnectionConfiguration {

  public abstract String getBootstrapServers();

  public abstract String getOutputRsdfTopic();

  public abstract int getRetries();

  public abstract long getRetryBackoffMs();

  public abstract String getKeySerializer();

  public abstract String getValueSerializer();

  public static Builder builder() {
    return new AutoValue_KafkaConnectionConfiguration.Builder();
  }

  public static KafkaConnectionConfiguration create(SystemConfig systemConfig) {
    return KafkaConnectionConfiguration.builder()
        .setBootstrapServers(systemConfig.getValue("kafka-bootstrap-servers"))
        .setOutputRsdfTopic(systemConfig.getValue("kafka-rsdf-topic"))
        .setRetries(systemConfig.getValueAsInt("connection-retry-count"))
        .setRetryBackoffMs(systemConfig.getValueAsLong("retry-backoff-ms"))
        .setKeySerializer(systemConfig.getValue("kafka-key-serializer"))
        .setValueSerializer(systemConfig.getValue("kafka-value-serializer"))
        .build();
  }

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  public abstract static class Builder {

    public abstract Builder setBootstrapServers(String bootstrapServers);

    public abstract Builder setOutputRsdfTopic(String outputRsdfTopic);

    public abstract Builder setRetries(int retries);

    public abstract Builder setRetryBackoffMs(long retryBackoffMs);

    public abstract Builder setKeySerializer(String keySerializer);

    public abstract Builder setValueSerializer(String valueSerializer);

    public abstract KafkaConnectionConfiguration autoBuild();

    public KafkaConnectionConfiguration build() {
      KafkaConnectionConfiguration kafkaConnectionConfiguration = autoBuild();

      checkArgument(isNotEmpty(kafkaConnectionConfiguration.getBootstrapServers()),
          "SystemKafkaConnectionConfiguration requires non-null, non-empty bootstrapServers");
      checkArgument(isNotEmpty(kafkaConnectionConfiguration.getOutputRsdfTopic()),
          "SystemKafkaConnectionConfiguration requires non-null, non-empty outputRsdfTopic");

      checkArgument(isNotEmpty(kafkaConnectionConfiguration.getKeySerializer()),
          "SystemKafkaConnectionConfiguration requires non-null, non-empty keySerializer");
      checkArgument(isNotEmpty(kafkaConnectionConfiguration.getValueSerializer()),
          "SystemKafkaConnectionConfiguration requires non-null, non-empty valueSerializer");

      return kafkaConnectionConfiguration;
    }
  }
}
