package gms.shared.utilities.kafka;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import gms.shared.frameworks.systemconfig.SystemConfig;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@AutoValue
@JsonSerialize(as = KafkaConfiguration.class)
@JsonDeserialize(builder = AutoValue_KafkaConfiguration.Builder.class)
public abstract class KafkaConfiguration {

    public abstract String getApplicationId();

    public abstract String getBootstrapServers();

    public abstract String getInputRsdfTopic();

    public abstract String getMalformedFrameTopic();

    public abstract String getOutputAcquiredChannelSohTopic();

    public abstract String getOutputStationSohInputTopic();

    public abstract String getKeySerializer();

    public abstract String getValueSerializer();

    public abstract int getConnectionRetryCount();

    public abstract int getNumberOfVerificationAttempts();

    public abstract int getStreamsCloseTimeoutMs();

    public abstract long getRetryBackoffMs();

    public abstract int getTransactionTimeout();

    public abstract String getAcks();

    public abstract int getDeliveryTimeout();

    public abstract int getSessionTimeout();

    public abstract int getMaxPollInterval();

    public abstract int getMaxPollRecords();

    public abstract boolean getAutoCommit();

    public abstract int getHeartbeatInterval();

    public static Builder builder() {
        return new AutoValue_KafkaConfiguration.Builder();
    }

    public static KafkaConfiguration create(SystemConfig systemConfig) {
        return KafkaConfiguration.builder()
                .setApplicationId(systemConfig.getValue("application-id"))
                .setBootstrapServers(systemConfig.getValue("kafka-bootstrap-servers"))
                .setInputRsdfTopic(systemConfig.getValue("kafka-rsdf-topic"))
                .setMalformedFrameTopic(systemConfig.getValue("kafka-malformed-topic"))
                .setOutputAcquiredChannelSohTopic(systemConfig.getValue("kafka-acquiredchannelsoh-topic"))
                .setOutputStationSohInputTopic(systemConfig.getValue("kafka-stationsohinput-topic"))
                .setKeySerializer(systemConfig.getValue("reactor-kafka-key-serializer"))
                .setValueSerializer(systemConfig.getValue("reactor-kafka-value-serializer"))
                .setNumberOfVerificationAttempts(systemConfig.getValueAsInt("verification-attempts"))
                .setStreamsCloseTimeoutMs(systemConfig.getValueAsInt("streams-close-timeout-ms"))
                .setConnectionRetryCount(systemConfig.getValueAsInt("connection-retry-count"))
                .setRetryBackoffMs(systemConfig.getValueAsLong("retry-backoff-ms"))
                .setTransactionTimeout(systemConfig.getValueAsInt("reactor-kafka-sender-transaction-timeout"))
                .setAcks(systemConfig.getValue("reactor-kafka-sender-acks"))
                .setDeliveryTimeout(systemConfig.getValueAsInt("reactor-kafka-sender-delivery-timeout"))
                .setSessionTimeout(systemConfig.getValueAsInt("reactor-kafka-consumer-session-timeout"))
                .setMaxPollInterval(systemConfig.getValueAsInt("reactor-kafka-consumer-max-poll-interval"))
                .setMaxPollRecords(systemConfig.getValueAsInt("reactor-kafka-consumer-max-poll-records"))
                .setAutoCommit(systemConfig.getValueAsBoolean("reactor-kafka-auto-commit"))
                .setHeartbeatInterval(systemConfig.getValueAsInt("reactor-kafka-consumer-heartbeat-interval"))
                .build();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "set")
    public interface Builder {

        Builder setApplicationId(String applicationId);

        Builder setBootstrapServers(String bootstrapServers);

        Builder setInputRsdfTopic(String inputRsdfTopic);

        Builder setMalformedFrameTopic(String malformedFrameTopic);

        Builder setOutputAcquiredChannelSohTopic(String outputAcquiredChannelSohTopic);

        Builder setOutputStationSohInputTopic(String stationSohInputTopic);

        Builder setKeySerializer(String keySerializer);

        Builder setValueSerializer(String valueSerializer);

        Builder setConnectionRetryCount(int retryCount);

        Builder setNumberOfVerificationAttempts(int numberOfVerificationAttempts);

        Builder setStreamsCloseTimeoutMs(int streamsCloseTimeoutMs);

        Builder setRetryBackoffMs(long retryBackoffMs);

        Builder setTransactionTimeout(int transactionTimeout);

        Builder setAcks(String acks);

        Builder setDeliveryTimeout(int deliveryTimeout);

        Builder setSessionTimeout(int sessionTimeout);

        Builder setMaxPollInterval(int maxPollInterval);

        Builder setMaxPollRecords(int maxPollRecords);

        Builder setAutoCommit(boolean autoCommit);

        Builder setHeartbeatInterval(int heartbeatInterval);

        KafkaConfiguration autoBuild();

        default KafkaConfiguration build() {
            KafkaConfiguration cd11RsdfProcessorConfiguration = autoBuild();
            Preconditions.checkArgument(isNotEmpty(cd11RsdfProcessorConfiguration.getApplicationId()),
                    "ReactorKafkaConfiguration requires non-null, non-empty applicationId");
            Preconditions.checkArgument(isNotEmpty(cd11RsdfProcessorConfiguration.getBootstrapServers()),
                    "ReactorKafkaConfiguration requires non-null, non-empty bootstrapServers");
            Preconditions.checkArgument(isNotEmpty(cd11RsdfProcessorConfiguration.getInputRsdfTopic()),
                    "ReactorKafkaConfiguration requires non-null, non-empty inputRsdfTopic");
            Preconditions.checkArgument(isNotEmpty(cd11RsdfProcessorConfiguration.getMalformedFrameTopic()),
                    "ReactorKafkaConfiguration requires non-null, non-empty malformedFrameTopic");
            Preconditions.checkArgument(isNotEmpty(cd11RsdfProcessorConfiguration.getOutputAcquiredChannelSohTopic()),
                    "ReactorKafkaConfiguration requires non-null, non-empty outputAcquiredChannelSohTopic");
            Preconditions.checkArgument(isNotEmpty(cd11RsdfProcessorConfiguration.getOutputStationSohInputTopic()),
                    "ReactorKafkaConfiguration requires non-null, non-empty outputStationSohInputTopic");

            Preconditions.checkArgument(isNotEmpty(cd11RsdfProcessorConfiguration.getKeySerializer()),
                    "ReactorKafkaConfiguration requires non-null, non-empty keySerializer");
            Preconditions.checkArgument(isNotEmpty(cd11RsdfProcessorConfiguration.getValueSerializer()),
                    "ReactorKafkaConfiguration requires non-null, non-empty valueSerializer");

            Preconditions.checkArgument(cd11RsdfProcessorConfiguration.getNumberOfVerificationAttempts() >= 0,
                    "ReactorKafkaConfiguration requires non-null, non-empty verificationAttempts");
            Preconditions.checkArgument(cd11RsdfProcessorConfiguration.getStreamsCloseTimeoutMs() >= 0,
                    "ReactorKafkaConfiguration requires non-null, non-empty streamsCloseTimeoutMs");
            Preconditions.checkArgument(cd11RsdfProcessorConfiguration.getConnectionRetryCount() >= 0,
                    "ReactorKafkaConfiguration requires non-null, non-empty connectionRetryCount");
            Preconditions.checkArgument(cd11RsdfProcessorConfiguration.getRetryBackoffMs() >= 0,
                    "ReactorKafkaConfiguration requires non-null, non-empty retryBackoffMs");
            Preconditions.checkArgument(cd11RsdfProcessorConfiguration.getTransactionTimeout() >= 0,
                    "ReactorKafkaConfiguration requires non-null, non-empty transactionTimeout");
            Preconditions.checkArgument(isNotEmpty(cd11RsdfProcessorConfiguration.getAcks()),
                    "ReactorKafkaConfiguration requires non-null, non-empty acks");

            Preconditions.checkArgument(cd11RsdfProcessorConfiguration.getDeliveryTimeout() >= 0,
                    "ReactorKafkaConfiguration requires non-null, non-empty deliveryTimeout");
            Preconditions.checkArgument(cd11RsdfProcessorConfiguration.getSessionTimeout() >= 0,
                    "ReactorKafkaConfiguration requires non-null, non-empty sessionTimeout");
            Preconditions.checkArgument(cd11RsdfProcessorConfiguration.getMaxPollInterval() >= 0,
                    "ReactorKafkaConfiguration requires non-null, non-empty maxPollInterval");
            Preconditions.checkArgument(cd11RsdfProcessorConfiguration.getMaxPollRecords() >= 0,
                    "ReactorKafkaConfiguration requires non-null, non-empty maxPollRecords");
            Preconditions.checkArgument(cd11RsdfProcessorConfiguration.getHeartbeatInterval() >= 0,
                    "ReactorKafkaConfiguration requires non-null, non-empty heartbeatInterval");

            return cd11RsdfProcessorConfiguration;
        }
    }
}
