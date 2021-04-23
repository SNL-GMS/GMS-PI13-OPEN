package gms.core.dataacquisition;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import gms.shared.frameworks.systemconfig.SystemConfig;

@AutoValue
@JsonSerialize(as = AceiMergeProcessorConfiguration.class)
@JsonDeserialize(builder = AutoValue_AceiMergeProcessorConfiguration.Builder.class)
public abstract class AceiMergeProcessorConfiguration {

  public abstract String getApplicationId();

  public abstract String getBootstrapServers();

  public abstract String getInputAceiTopic();

  public abstract String getKeySerializer();

  public abstract String getValueSerializer();

  public abstract int getConnectionRetryCount();

  public abstract int getNumberOfVerificationAttempts();

  public abstract int getStreamsCloseTimeoutMs();

  public abstract long getRetryBackoffMs();

  public abstract long getMergeToleranceMs();

  public abstract int getBenchmarkLoggingPeriodSeconds();

  public abstract int getCacheExpirationPeriodSeconds();

  public abstract long getStoragePeriodMilliseconds();

  public abstract int getProcessorThreadCount();

  public abstract int getMaxItemsPerDbInteraction();

  public abstract int getMaxParallelDbOperations();

  public abstract int getMinItemsToPerformDbOperations();

  public static Builder builder() {
    return new AutoValue_AceiMergeProcessorConfiguration.Builder();
  }

  public static AceiMergeProcessorConfiguration create(SystemConfig systemConfig) {
    return AceiMergeProcessorConfiguration.builder()
        .setApplicationId(systemConfig.getValue("application-id"))
        .setBootstrapServers(systemConfig.getValue("kafka-bootstrap-servers"))
        .setInputAceiTopic(systemConfig.getValue("input-acei-topic"))
        .setKeySerializer(systemConfig.getValue("kafka-key-serializer"))
        .setValueSerializer(systemConfig.getValue("kafka-value-serializer"))
        .setNumberOfVerificationAttempts(systemConfig.getValueAsInt("verification-attempts"))
        .setStreamsCloseTimeoutMs(systemConfig.getValueAsInt("streams-close-timeout-ms"))
        .setConnectionRetryCount(systemConfig.getValueAsInt("connection-retry-count"))
        .setRetryBackoffMs(systemConfig.getValueAsLong("retry-backoff-ms"))
        .setMergeToleranceMs(systemConfig.getValueAsLong("merge-tolerance-ms"))
        .setBenchmarkLoggingPeriodSeconds(systemConfig.getValueAsInt("benchmark-logging-period-seconds"))
        .setCacheExpirationPeriodSeconds(systemConfig.getValueAsInt("cache-expiration-period-seconds"))
        .setStoragePeriodMilliseconds(systemConfig.getValueAsLong("storage-period-milliseconds"))
        .setProcessorThreadCount(systemConfig.getValueAsInt("processor-thread-count"))
        .setMaxItemsPerDbInteraction(systemConfig.getValueAsInt("max-items-per-db-interaction"))
        .setMaxParallelDbOperations(systemConfig.getValueAsInt("max-parallel-db-operations"))
        .setMinItemsToPerformDbOperations(systemConfig.getValueAsInt("min-items-to-perform-db-operations"))
        .build();
  }

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  public abstract static class Builder {

    public abstract Builder setApplicationId(String applicationId);

    public abstract Builder setBootstrapServers(String bootstrapServers);

    public abstract Builder setInputAceiTopic(String inputAceiTopic);

    public abstract Builder setKeySerializer(String keySerializer);

    public abstract Builder setValueSerializer(String valueSerializer);

    public abstract Builder setConnectionRetryCount(int retryCount);

    public abstract Builder setNumberOfVerificationAttempts(int numberOfVerificationAttempts);

    public abstract Builder setStreamsCloseTimeoutMs(int streamsCloseTimeoutMs);

    public abstract Builder setRetryBackoffMs(long retryBackoffMs);

    public abstract Builder setMergeToleranceMs(long mergeToleranceMs);

    public abstract Builder setBenchmarkLoggingPeriodSeconds(int benchmarkLoggingPeriodSeconds);

    public abstract Builder setCacheExpirationPeriodSeconds(int cacheExpirationPeriodSeconds);

    public abstract Builder setStoragePeriodMilliseconds(long storagePeriodMilliseconds);

    public abstract Builder setProcessorThreadCount(int processorThreadCount);

    public abstract Builder setMaxItemsPerDbInteraction(int maxItemsPerDbInteration);

    public abstract Builder setMaxParallelDbOperations(int maxParallelDbOperations);

    public abstract Builder setMinItemsToPerformDbOperations(int minItemsToPerformDbOperations);

    public abstract AceiMergeProcessorConfiguration autoBuild();

    public AceiMergeProcessorConfiguration build() {
      AceiMergeProcessorConfiguration aceiMergeProcessorConfiguration = autoBuild();

      checkArgument(isNotEmpty(aceiMergeProcessorConfiguration.getApplicationId()),
          "AceiMergeProcessorConfiguration requires non-null, non-empty applicationId");
      checkArgument(isNotEmpty(aceiMergeProcessorConfiguration.getBootstrapServers()),
          "AceiMergeProcessorConfiguration requires non-null, non-empty bootstrapServers");
      checkArgument(isNotEmpty(aceiMergeProcessorConfiguration.getInputAceiTopic()),
          "AceiMergeProcessorConfiguration requires non-null, non-empty inputRsdfTopic");
      
      checkArgument(isNotEmpty(aceiMergeProcessorConfiguration.getKeySerializer()),
          "AceiMergeProcessorConfiguration requires non-null, non-empty keySerializer");
      checkArgument(isNotEmpty(aceiMergeProcessorConfiguration.getValueSerializer()),
          "AceiMergeProcessorConfiguration requires non-null, non-empty valueSerializer");

      return aceiMergeProcessorConfiguration;
    }
  }

}
