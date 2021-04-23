package gms.dataacquisition.data.preloader;

import static gms.dataacquisition.data.preloader.GenerationType.CAPABILITY_SOH_ROLLUP;
import static gms.dataacquisition.data.preloader.GenerationType.RAW_STATION_DATA_FRAME;
import static gms.dataacquisition.data.preloader.GenerationType.STATION_SOH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gms.dataacquisition.data.preloader.GenerationSpec.Builder;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerationSpecTests {

  private Builder builder;

  @BeforeEach
  public void testSetup() {
    builder = GenerationSpec.builder();
  }

  @Test
  void testSerializationDeserialization() {
    final var generationType = STATION_SOH;
    final var startTime = Instant.EPOCH.minus(1, ChronoUnit.DAYS);
    final var sampleDuration = Duration.ofSeconds(20);
    final var duration = Duration.ofDays(1);
    GenerationSpec generationSpec = builder
        .setType(generationType)
        .setBatchSize(10)
        .setStartTime(startTime)
        .setSampleDuration(sampleDuration)
        .setDuration(duration)
        .addInitialCondition(InitialCondition.STATION_GROUPS,
            UtilsTestFixtures.STATION_GROUP.getName())
        .build();
    assertEquals(generationType, generationSpec.getType());
    assertEquals(startTime, generationSpec.getStartTime());
    assertEquals(duration, generationSpec.getDuration());
    assertEquals(sampleDuration, generationSpec.getSampleDuration());
    assertEquals(UtilsTestFixtures.STATION_GROUP.getName(),
        generationSpec.getInitialConditions().get(InitialCondition.STATION_GROUPS));
  }

  @Test
  void testSerializationDeserialization_initialConditionBuilder() {
    final var duration = Duration.ofDays(1);
    final var startTime = Instant.now().minus(duration);
    final var stationGroups = List.of(UtilsTestFixtures.STATION_GROUP);
    final var sampleDuration = Duration.ofSeconds(20);
    final var generationType = STATION_SOH;
    GenerationSpec generationSpec = builder
        .setType(generationType)
        .setBatchSize(10)
        .setDuration(duration)
        .setSampleDuration(sampleDuration)
        .setStartTime(startTime)
        .addInitialCondition(InitialCondition.STATION_GROUPS,
            stationGroups.stream().map(StationGroup::getName).collect(Collectors.joining(",")))
        .build();
    assertEquals(generationType, generationSpec.getType());
    assertEquals(duration, generationSpec.getDuration());
    assertEquals(sampleDuration, generationSpec.getSampleDuration());
    assertEquals(stationGroups.stream().map(StationGroup::getName).collect(Collectors.joining(",")),
        generationSpec.getInitialConditions().get(InitialCondition.STATION_GROUPS));
  }

  @Test
  void testUsingBuilderMultipleTimes() {
    final var dataTypes = List.of(CAPABILITY_SOH_ROLLUP, RAW_STATION_DATA_FRAME);

    final Function<GenerationType, GenerationSpec> stringGenerationSpecFunction = dt -> {
      final var startTime = Instant.now();
      return builder
          .setDuration(Duration.ofMinutes(1))
          .setSampleDuration(Duration.ofSeconds(5))
          .setBatchSize(2)
          .setInitialConditions(new HashMap<>())
          .setType(dt)
          .setStartTime(startTime)
          .build();
    };

    final var results = dataTypes.stream()
        .map(GenerationType::toString)
        .map(GenerationType::parseType)
        .map(stringGenerationSpecFunction)
        .collect(Collectors.toList());

    final var actual = results.stream()
        .map(GenerationSpec::getType)
        .collect(Collectors.toList());
    assertEquals(dataTypes, actual);
  }

}