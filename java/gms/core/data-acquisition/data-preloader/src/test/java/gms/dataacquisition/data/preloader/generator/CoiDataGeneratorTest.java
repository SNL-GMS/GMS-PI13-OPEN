package gms.dataacquisition.data.preloader.generator;

import static gms.shared.frameworks.osd.coi.station.StationTestFixtures.asar;
import static gms.shared.frameworks.osd.coi.station.StationTestFixtures.pdar;
import static gms.shared.frameworks.osd.coi.station.StationTestFixtures.txar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import gms.dataacquisition.data.preloader.DatasetGeneratorOptions;
import gms.dataacquisition.data.preloader.GenerationSpec;
import gms.dataacquisition.data.preloader.GenerationType;
import gms.shared.frameworks.injector.Modifier;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

abstract class CoiDataGeneratorTest<G extends CoiDataGenerator<T, M>, T, M extends Modifier<?>> {


  @Captor
  protected ArgumentCaptor<Collection<T>> recordCaptor;
  @Mock
  protected SohRepositoryInterface sohRepository;

  protected GenerationSpec generationSpec;
  G dataGenerator;

  private final StationGroup test_station_group_1 = StationGroup.from("Test_Station_Group_1",
      "Test StationGroup 1", List.of(asar()));
  private final StationGroup test_station_group_2 = StationGroup.from("Test_Station_Group_2",
      "Test StationGroup 2", List.of(pdar(), txar()));
  private final StationGroup test_station_group_3 = StationGroup.from("Test_Station_Group_3",
      "Test StationGroup 3", List.of(asar(), pdar(), txar()));

  protected final List<StationGroup> stationGroups = List
      .of(test_station_group_1, test_station_group_2, test_station_group_3);
  private final List<String> stationGroupNames = stationGroups.stream()
      .map(StationGroup::getName)
      .distinct()
      .collect(Collectors.toList());

  protected boolean validateUniqueRecords;
  protected Instant seedTime;
  protected Instant receptionTime;

  public static final int BATCH_SIZE = 10;
  // TODO need to handle case where our total number of items generated is less than BATCH_SIZE
  // to match frequency in modifiers for now
  protected final Duration generationFrequency = Duration.ofSeconds(20);
  protected final Duration generationDuration = Duration.ofSeconds(200);
  protected final Duration receptionDelay = Duration.ofSeconds(20);
  protected final Instant startTime = Instant.now();

  private final AtomicInteger repositoryCallCounter = new AtomicInteger(0);

  @BeforeEach
  public void testSetup() {
    // We want to validate that we aren't trying to store the same records
    // under normal circumstances, but we won't check for uniqueness if we
    // re-try the storage of a collection after a failure because the Captor
    // retains all collections we attempt to store. We could also capture the
    // the exception is thrown on and remove it.
    validateUniqueRecords = true;
    repositoryCallCounter.set(0);
    MockitoAnnotations.initMocks(this);
    buildGenerationSpec();

    when(sohRepository.retrieveStationGroups(stationGroupNames)).thenReturn(stationGroups);

    dataGenerator = getDataGenerator(generationSpec, sohRepository);
  }

  private void buildGenerationSpec() {
    final var stationGroups = this.stationGroups.stream()
        .map(StationGroup::getName)
        .distinct()
        .reduce((s, s2) -> s + "," + s2)
        .orElseThrow();
    final var generatorDataType = getGeneratorDataType();
    final String[] args = String.format(
        "--duration=%s --dataType=%s --stationGroups=%s --startTime=%s --sampleDuration=%s --receptionDelay=%s",
        generationDuration, generatorDataType, stationGroups, startTime, generationFrequency,
        receptionDelay).split(" ");

    generationSpec = DatasetGeneratorOptions.parse(args).toBuilder().setBatchSize(BATCH_SIZE)
        .build();
    seedTime = generationSpec.getStartTime();
    receptionTime = generationSpec.getReceptionTime();
  }

  protected abstract G getDataGenerator(GenerationSpec generationSpec,
      SohRepositoryInterface sohRepository);

  protected abstract GenerationType getGeneratorDataType();

  @Test
  void testGetSeedNames() {
    final var result = dataGenerator.getSeedNames();

    assertNotNull(result);
    final List<String> seedNames = getSeedNames();
    assertEquals(seedNames, result);
    verify(sohRepository, times(1)).retrieveStationGroups(stationGroupNames);
    verifyNoMoreInteractions(sohRepository);
  }


  protected abstract List<String> getSeedNames();

  @Test
  void testGenerateSeed_dependencyInteraction() {
    final var stationName = stationGroups.stream().flatMap(g -> g.getStations().stream())
        .findFirst().orElseThrow().getName();

    dataGenerator.generateSeed(stationName);

    verify(sohRepository, atLeast(0)).retrieveStationGroups(stationGroupNames);
    verifyNoMoreInteractions(sohRepository);
  }

  abstract void testGenerateSeed();

  @Test
  void testConsumeRecord_dependencyInteraction() {
    final var records = getRecordsToSend();

    dataGenerator.consumeRecords(records);

    verifyRepositoryInteraction(1);
    assertEquals(1, recordCaptor.getAllValues().size());
    verifyNoMoreInteractions(sohRepository);
  }

  protected abstract List<T> getRecordsToSend();

  @Test
  void testRun() {
    dataGenerator.run();

    verifyRepositoryInteraction(getWantedNumberOfConsumeInvocations());
    assertEquals(getWantedNumberOfConsumeInvocations(), recordCaptor.getAllValues().size(),
        "The expected number of calls to consume records was not met");
    assertEquals(getWantedNumberOfItemsGenerated(),
        recordCaptor.getAllValues().stream().mapToLong(Collection::size).sum(),
        "The expected number of items generated was not met");
    verifyTimes(recordCaptor.getAllValues().stream().flatMap(Collection::parallelStream).collect(
        Collectors.<T>toList()));
    verify(sohRepository, times(1)).retrieveStationGroups(stationGroupNames);
    verifyNoMoreInteractions(sohRepository);
  }

  protected abstract void verifyTimes(Collection<T> capturedRecords);

  @Test
  void testRunWithFailures() {
    final int wantedNumberOfConsumeInvocationsPlusRetry = getWantedNumberOfConsumeInvocations() + 1;
    validateUniqueRecords = false;
    mockIntermittentSohRepositoryFailure(1);

    dataGenerator.run();

    verifyRepositoryInteraction(wantedNumberOfConsumeInvocationsPlusRetry);
    assertEquals(wantedNumberOfConsumeInvocationsPlusRetry, recordCaptor.getAllValues().size(),
        "The expected number of calls to consume records was not met");
    verify(sohRepository, times(1)).retrieveStationGroups(stationGroupNames);
    verifyNoMoreInteractions(sohRepository);
  }

  private int getWantedNumberOfConsumeInvocations() {
    return (int) Math.ceil((double) getWantedNumberOfItemsGenerated() / BATCH_SIZE);
  }

  protected abstract void mockIntermittentSohRepositoryFailure(int failsOnNthCall);

  protected abstract int getWantedNumberOfItemsGenerated();

  protected abstract void verifyRepositoryInteraction(int wantedNumberOfInvocations);


  protected <I> void validateIds(List<I> ids) {
    if (validateUniqueRecords) {
      assertEquals(ids.stream().distinct().count(), ids.size());
    } else {
      Map<I, Long> idCount = ids.stream()
          .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
      final var duplicates = idCount.values().stream()
          .filter(integer -> integer > 1)
          .collect(Collectors.toList());
      assertEquals(BATCH_SIZE, duplicates.size());
      assertTrue(duplicates.stream().allMatch(count -> count == 2));
    }
  }

  protected <A> void assertEmpty(Collection<A> actual) {
    assertNotNull(actual);
    assertTrue(actual.isEmpty());
  }

  protected <A> void assertNotNullOrEmpty(Collection<A> actual) {
    assertNotNull(actual);
    assertFalse(actual.isEmpty());
  }

  protected <A, B> void assertNotNullOrEmpty(Map<A, B> actual) {
    assertNotNull(actual);
    assertFalse(actual.isEmpty());
  }

  protected synchronized Object throwErrorOnNthCall(int failsOnNthCall) {
    final var timesCalled = repositoryCallCounter.getAndIncrement();
    if (failsOnNthCall == timesCalled) {
      throw new IllegalStateException();
    } else {
      return null;
    }
  }
}
