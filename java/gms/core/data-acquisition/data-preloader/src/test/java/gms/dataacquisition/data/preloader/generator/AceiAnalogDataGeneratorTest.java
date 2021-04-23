package gms.dataacquisition.data.preloader.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import gms.dataacquisition.data.preloader.GenerationSpec;
import gms.dataacquisition.data.preloader.GenerationType;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AceiAnalogDataGeneratorTest extends
    AceiDataGeneratorTest<AceiAnalogDataGenerator, AcquiredChannelEnvironmentIssueAnalog> {

  @Override
  protected AceiAnalogDataGenerator getDataGenerator(GenerationSpec generationSpec,
      SohRepositoryInterface sohRepository) {
    return new AceiAnalogDataGenerator(generationSpec, sohRepository);
  }

  @Override
  protected GenerationType getGeneratorDataType() {
    return GenerationType.ACQUIRED_CHANNEL_ENV_ISSUE_ANALOG;
  }

  @Test
  @Override
  void testGenerateSeed() {
    final var stationName = stationGroups.stream().flatMap(g -> g.getStations().stream())
        .findFirst().orElseThrow().getName();

    final var result = dataGenerator.generateSeed(stationName);

    assertNotNull(result);
    assertNotNull(result.getId());
    assertEquals(stationName, result.getChannelName());
    assertEquals(AcquiredChannelEnvironmentIssueType.CLOCK_DIFFERENTIAL_IN_MICROSECONDS,
        result.getType());
    assertEquals(seedTime, result.getStartTime());
    assertEquals(seedTime.plus(generationFrequency), result.getEndTime());
    assertEquals(0.0, result.getStatus());
  }

  @Override
  protected void verifyTimes(Collection<AcquiredChannelEnvironmentIssueAnalog> capturedRecords) {
    final var timeOfRun = generationSpec.getStartTime();

    final var lowestStartTime = capturedRecords.stream()
        .map(AcquiredChannelEnvironmentIssue::getStartTime).min(Instant::compareTo)
        .orElse(Instant.MAX);
    assertEquals(timeOfRun, lowestStartTime);

    final var highestStartTime = capturedRecords.stream()
        .map(AcquiredChannelEnvironmentIssue::getStartTime).max(Instant::compareTo)
        .orElse(Instant.MAX);
    assertTrue(highestStartTime.isAfter(timeOfRun));
    assertTrue(highestStartTime.isAfter(lowestStartTime));

    final var lowestEndTime = capturedRecords.stream()
        .map(AcquiredChannelEnvironmentIssue::getEndTime).min(Instant::compareTo)
        .orElse(Instant.MAX);
    assertFalse(lowestEndTime.isBefore(timeOfRun));
    assertTrue(lowestEndTime.isAfter(lowestStartTime));

    final var highestEndTime = capturedRecords.stream()
        .map(AcquiredChannelEnvironmentIssue::getEndTime).max(Instant::compareTo)
        .orElse(Instant.MAX);
    assertTrue(highestEndTime.isAfter(timeOfRun));
    assertTrue(highestEndTime.isAfter(lowestStartTime));
    assertTrue(highestEndTime.isAfter(lowestEndTime));

  }

  @Override
  protected void mockIntermittentSohRepositoryFailure(int failsOnNthCall) {
    doAnswer(i -> throwErrorOnNthCall(failsOnNthCall)).when(sohRepository)
        .storeAcquiredChannelSohAnalog(any());
  }

  @Override
  protected void verifyRepositoryInteraction(int wantedNumberOfInvocations) {
    verify(sohRepository, atLeast(0))
        .storeAcquiredChannelSohAnalog(recordCaptor.capture());

    final var ids = recordCaptor.getAllValues().stream()
        .flatMap(Collection::stream)
        .map(AcquiredChannelEnvironmentIssueAnalog::getId)
        .map(UUID::toString)
        .collect(Collectors.toList());

    assertEquals(wantedNumberOfInvocations, recordCaptor.getAllValues().size());
    validateIds(ids);
  }
}