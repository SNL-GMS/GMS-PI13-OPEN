package gms.dataacquisition.data.preloader.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import gms.dataacquisition.data.preloader.GenerationSpec;
import gms.dataacquisition.data.preloader.GenerationType;
import gms.shared.frameworks.injector.CapabilitySohRollupIdModifier;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CapabilitySohRollupDataGeneratorTest extends
    CoiDataGeneratorTest<CapabilitySohRollupDataGenerator, CapabilitySohRollup, CapabilitySohRollupIdModifier> {

  @Override
  protected CapabilitySohRollupDataGenerator getDataGenerator(GenerationSpec generationSpec,
      SohRepositoryInterface sohRepository) {
    return new CapabilitySohRollupDataGenerator(generationSpec, sohRepository);
  }

  @Override
  protected GenerationType getGeneratorDataType() {
    return GenerationType.CAPABILITY_SOH_ROLLUP;
  }

  @Override
  protected List<String> getSeedNames() {
    return stationGroups.stream()
        .map(StationGroup::getName)
        .collect(Collectors.toList());
  }

  @Test
  @Override
  void testGenerateSeed() {
    final var stationGroupName = stationGroups.stream().findFirst().orElseThrow().getName();

    final var result = dataGenerator.generateSeed(stationGroupName);

    assertNotNull(result);
    assertNotNull(result.getId());
    assertEquals(stationGroupName, result.getForStationGroup());
    assertEquals(seedTime, result.getTime());
    assertEquals(SohStatus.GOOD, result.getGroupRollupSohStatus());
    assertEmpty(result.getBasedOnStationSohs());
    assertNotNullOrEmpty(result.getRollupSohStatusByStation());
  }

  @Override
  protected List<CapabilitySohRollup> getRecordsToSend() {
    return new ArrayList<>();
  }

  @Override
  protected void verifyTimes(Collection<CapabilitySohRollup> capturedRecords) {
    final var timeOfRun = generationSpec.getStartTime();

    final var lowestStartTime = capturedRecords.stream()
        .map(CapabilitySohRollup::getTime).min(Instant::compareTo)
        .orElse(Instant.MAX);
    assertEquals(timeOfRun, lowestStartTime);

    final var highestStartTime = capturedRecords.stream()
        .map(CapabilitySohRollup::getTime).max(Instant::compareTo)
        .orElse(Instant.MAX);
    assertTrue(highestStartTime.isAfter(timeOfRun));
    assertTrue(highestStartTime.isAfter(lowestStartTime));
  }

  @Override
  protected void mockIntermittentSohRepositoryFailure(int failsOnNthCall) {
    doAnswer(i -> throwErrorOnNthCall(failsOnNthCall)).when(sohRepository)
        .storeCapabilitySohRollup(any());
  }

  @Override
  protected void verifyRepositoryInteraction(int wantedNumberOfInvocations) {
    verify(sohRepository, atLeast(0))
        .storeCapabilitySohRollup(recordCaptor.capture());

    final var ids = recordCaptor.getAllValues().stream()
        .flatMap(Collection::stream)
        .map(CapabilitySohRollup::getId)
        .map(UUID::toString)
        .collect(Collectors.toList());

    assertEquals(wantedNumberOfInvocations, recordCaptor.getAllValues().size());
    validateIds(ids);
  }

  @Override
  protected int getWantedNumberOfItemsGenerated() {
    return (int) Math.ceil(
        ((double) generationDuration.toNanos() / generationFrequency.toNanos()) * stationGroups
            .size());
  }
}