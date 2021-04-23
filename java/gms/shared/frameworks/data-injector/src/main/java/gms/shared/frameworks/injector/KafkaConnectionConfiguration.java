package gms.shared.frameworks.injector;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import gms.shared.frameworks.systemconfig.SystemConfig;
import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@AutoValue
@JsonSerialize(as = KafkaConnectionConfiguration.class)
@JsonDeserialize(builder = AutoValue_KafkaConnectionConfiguration.Builder.class)
public abstract class KafkaConnectionConfiguration {

  public abstract String getKeySerializer();

  public abstract String getValueSerializer();

  public abstract String getCompressionType();

  public static Builder builder() {
    return new AutoValue_KafkaConnectionConfiguration.Builder();
  }

  public static KafkaConnectionConfiguration create(SystemConfig systemConfig) {
    return KafkaConnectionConfiguration.builder()
        .setKeySerializer(systemConfig.getValue("kafka-key-serializer"))
        .setValueSerializer(systemConfig.getValue("kafka-value-serializer"))
        .setCompressionType(systemConfig.getValue("kafka-compression-type"))
        .build();
  }

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  public abstract static class Builder {

    public abstract Builder setKeySerializer(String keySerializer);

    public abstract Builder setValueSerializer(String valueSerializer);

    public abstract Builder setCompressionType(String compressionType);

    public abstract KafkaConnectionConfiguration autoBuild();

    public KafkaConnectionConfiguration build() {
      KafkaConnectionConfiguration kafkaConnectionConfiguration = autoBuild();

      checkArgument(isNotEmpty(kafkaConnectionConfiguration.getKeySerializer()),
          "SystemKafkaConnectionConfiguration requires non-null, non-empty keySerializer");
      checkArgument(isNotEmpty(kafkaConnectionConfiguration.getValueSerializer()),
          "SystemKafkaConnectionConfiguration requires non-null, non-empty valueSerializer");
      checkArgument(isNotEmpty(kafkaConnectionConfiguration.getCompressionType()),
      "SystemKafkaConnectionConfiguration requires non-null, non-empty compressionType");

      return kafkaConnectionConfiguration;
    }
  }
}
