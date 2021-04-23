package gms.dataacquisition.data.preloader;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

@AutoValue
@JsonSerialize(as = GenerationSpec.class)
@JsonDeserialize(builder = AutoValue_GenerationSpec.Builder.class)
public abstract class GenerationSpec {

  public abstract GenerationType getType();

  public abstract Instant getStartTime();

  public Instant getReceptionTime() {
    final var receptionCondition = getInitialConditions().get(InitialCondition.RECEPTION_DELAY);
    final var receptionDelay = Duration.parse(receptionCondition);
    return getStartTime().plus(receptionDelay);
  }

  public abstract Duration getSampleDuration();

  public abstract Duration getDuration();

  public abstract ImmutableMap<InitialCondition, String> getInitialConditions();

  public Stream<Entry<InitialCondition, String>> initialConditions() {
    return getInitialConditions().entrySet().stream();
  }

  public Optional<String> getInitialCondition(InitialCondition condition) {
    return Optional.ofNullable(getInitialConditions().get(condition));
  }

  public Duration getBatchDuration() {
    return Duration.ofNanos((long) getBatchSize() * getSampleDuration().toNanos());
  }

  public abstract int getBatchSize();

  public static Builder builder() {
    return new AutoValue_GenerationSpec.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  public abstract static class Builder {

    public abstract Builder setBatchSize(int batchSize);

    public abstract Builder setType(GenerationType type);

    public abstract Builder setStartTime(Instant startTime);

    public abstract Builder setSampleDuration(Duration sampleDuration);

    public abstract Builder setDuration(Duration duration);

    abstract Builder setInitialConditions(ImmutableMap<InitialCondition, String> initialConditions);

    public Builder setInitialConditions(Map<InitialCondition, String> initialConditions) {
      return setInitialConditions(ImmutableMap.copyOf(initialConditions));
    }

    abstract ImmutableMap.Builder<InitialCondition, String> initialConditionsBuilder();

    public Builder addInitialCondition(InitialCondition condition, String value) {
      initialConditionsBuilder().put(condition, value);
      return this;
    }

    abstract GenerationSpec autoBuild();

    public GenerationSpec build() {
      return autoBuild();
    }
  }
}
