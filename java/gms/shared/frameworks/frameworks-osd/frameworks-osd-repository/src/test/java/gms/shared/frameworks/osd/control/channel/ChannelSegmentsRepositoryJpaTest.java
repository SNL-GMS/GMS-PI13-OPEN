package gms.shared.frameworks.osd.control.channel;

import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.channel.util.ChannelSegmentsIdRequest;
import gms.shared.frameworks.osd.api.station.StationRepositoryInterface;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ChannelsTimeRangeRequest;
import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelSegmentDao;
import gms.shared.frameworks.osd.coi.waveforms.FkSpectra;
import gms.shared.frameworks.osd.coi.waveforms.Timeseries;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import gms.shared.frameworks.osd.control.utils.TestFixtures;
import gms.shared.frameworks.osd.control.waveforms.TimeseriesRepositoryCassandra;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChannelSegmentsRepositoryJpaTest {

  private static EntityManagerFactory entityManagerFactory;

  @Mock
  private TimeseriesRepositoryCassandra mockTimeseriesRepository;

  private ChannelRepositoryInterface channelRepository;

  private ChannelSegmentsRepositoryJpa channelSegmentsRepository;
  private Map<UUID, ChannelSegment<Waveform>> expectedChannelSegmentsById;
  private Map<String, ChannelSegment<Waveform>> expectedChannelSegmentsByName;
  private Map<String, List<Waveform>> expectedTimeseriesByCanonicalChannelName;

  @BeforeEach
  void setUp() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    channelRepository = new ChannelRepositoryJpa(entityManagerFactory);
    channelSegmentsRepository = ChannelSegmentsRepositoryJpa.create(entityManagerFactory,
        channelRepository,
        mockTimeseriesRepository);

    StationRepositoryInterface stationRepository = new StationRepositoryJpa(entityManagerFactory);
    stationRepository.storeStations(List.of(TestFixtures.station));

    expectedChannelSegmentsByName = Map.of(
        TestFixtures.channelSegment1.getChannel().getName(), TestFixtures.channelSegment1,
        TestFixtures.channelSegment2.getChannel().getName(), TestFixtures.channelSegment2);
    expectedChannelSegmentsById = Map.of(
        TestFixtures.channelSegment1.getId(), TestFixtures.channelSegment1,
        TestFixtures.channelSegment2.getId(), TestFixtures.channelSegment2);
    expectedTimeseriesByCanonicalChannelName = Map.of(
        TestFixtures.channelSegment1.getChannel().getCanonicalName(),
        TestFixtures.channelSegment1.getTimeseries(),
        TestFixtures.channelSegment2.getChannel().getCanonicalName(),
        TestFixtures.channelSegment2.getTimeseries());
  }

  @AfterEach
  void tearDown() {
    entityManagerFactory.close();
  }

  @ParameterizedTest
  @MethodSource("getCreateArguments")
  void testCreateArgumentValidation(EntityManagerFactory entityManagerFactory,
      ChannelRepositoryInterface channelRepository,
      TimeseriesRepositoryCassandra timeseriesRepository) {

    assertThrows(NullPointerException.class,
        () -> ChannelSegmentsRepositoryJpa.create(entityManagerFactory,
            channelRepository,
            timeseriesRepository));
  }

  static Stream<Arguments> getCreateArguments() {
    return Stream.of(
        arguments(null, mock(ChannelRepositoryInterface.class),
            mock(TimeseriesRepositoryCassandra.class)),
        arguments(mock(EntityManagerFactory.class), null,
            mock(TimeseriesRepositoryCassandra.class)),
        arguments(mock(EntityManagerFactory.class), mock(ChannelRepositoryInterface.class), null)
    );
  }

  @Test
  void testCreate() {
    assertDoesNotThrow(() -> ChannelSegmentsRepositoryJpa.create(entityManagerFactory,
        channelRepository,
        mockTimeseriesRepository));
  }

  @ParameterizedTest
  @MethodSource("getStoreChannelSegmentsArguments")
  public void testStoreChannelSegmentsValidation(Class<? extends Exception> exceptionType,
      Collection<ChannelSegment<Waveform>> channelSegments) {
    assertThrows(exceptionType,
        () -> channelSegmentsRepository.storeChannelSegments(channelSegments));
  }

  static Stream<Arguments> getStoreChannelSegmentsArguments() {
    return Stream.of(
        arguments(NullPointerException.class, null),
        arguments(IllegalStateException.class, List.of())
    );
  }

  @Test
  void testStoreChannelSegments() {
    channelSegmentsRepository.storeChannelSegments(expectedChannelSegmentsByName.values());

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<ChannelSegmentDao> channelSegmentQuery =
        builder.createQuery(ChannelSegmentDao.class);
    Root<ChannelSegmentDao> fromChannelSegment = channelSegmentQuery.from(ChannelSegmentDao.class);
    channelSegmentQuery.select(fromChannelSegment);
    entityManager.createQuery(channelSegmentQuery)
        .getResultStream()
        .forEach(actual -> {
          ChannelSegment<? extends Timeseries> expected =
              expectedChannelSegmentsByName.get(actual.getChannel().getName());
          assertNotNull(expected);

          assertEquals(expected.getId(), actual.getId());
          assertEquals(expected.getName(), actual.getName());
          assertEquals(expected.getType(), actual.getType());
          assertEquals(expected.getTimeseriesType(), actual.getTimeseriesType());
          assertEquals(expected.getStartTime(), actual.getStartTime());
          assertEquals(expected.getEndTime(), actual.getEndTime());
        });

    entityManager.close();
  }

  @Test
  void testStoreChannelSegmentsWithNewChannel() {
    ChannelSegment<Waveform> expected = TestFixtures.channelSegment3;

    channelSegmentsRepository.storeChannelSegments(List.of(expected));

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    ChannelSegmentDao stored = entityManager.find(ChannelSegmentDao.class, expected.getId());
    assertNotNull(stored);
    assertEquals(expected.getId(), stored.getId());

    Channel actualChannel =
        channelRepository.retrieveChannels(List.of(expected.getChannel().getName())).get(0);
    assertEquals(expected.getChannel(), actualChannel);
    assertEquals(expected.getName(), stored.getName());
    assertEquals(expected.getType(), stored.getType());
    assertEquals(expected.getTimeseriesType(), stored.getTimeseriesType());
    assertEquals(expected.getStartTime(), stored.getStartTime());
    assertEquals(expected.getEndTime(), stored.getEndTime());
    entityManager.close();
  }

  @Test
  void testRetrieveChannelSegmentsByIdsValidation() {
    assertThrows(NullPointerException.class,
        () -> channelSegmentsRepository.retrieveChannelSegmentsByIds(null));
  }

  @Test
  void testRetrieveChannelSegmentsByIds() {
    channelSegmentsRepository.storeChannelSegments(expectedChannelSegmentsByName.values());

    setUpTimeseriesMock();

    ChannelSegmentsIdRequest request = ChannelSegmentsIdRequest.create(
        expectedChannelSegmentsById.keySet(),
        true);
    Collection<ChannelSegment<? extends Timeseries>> actualChannelSegments =
        channelSegmentsRepository.retrieveChannelSegmentsByIds(request);

    assertTrue(!actualChannelSegments.isEmpty());

    actualChannelSegments.stream()
        .forEach(actual -> {
          ChannelSegment<? extends Timeseries> expected =
              expectedChannelSegmentsById.get(actual.getId());
          assertEquals(expected, actual);
        });
  }

  @Test
  public void testRetrieveChannelSegmentsByChannelIdsValidation() {
    assertThrows(NullPointerException.class,
        () -> channelSegmentsRepository.retrieveChannelSegmentsByChannelNames(null));
  }

  // TODO: Expand these tests when resolving time range queries
  @Test
  void retrieveChannelSegmentsByChannelIds() {
    channelSegmentsRepository.storeChannelSegments(expectedChannelSegmentsByName.values());

    List<String> channelNames = expectedChannelSegmentsByName.values().stream()
        .map(ChannelSegment::getChannel)
        .map(Channel::getName)
        .collect(Collectors.toList());

    Instant minStart = expectedChannelSegmentsByName.values()
        .stream()
        .map(ChannelSegment::getStartTime)
        .min(Instant::compareTo)
        .orElseThrow(IllegalStateException::new);

    Instant maxEnd = expectedChannelSegmentsByName.values()
        .stream()
        .map(ChannelSegment::getEndTime)
        .max(Instant::compareTo)
        .orElseThrow(IllegalStateException::new);

    ChannelsTimeRangeRequest request = ChannelsTimeRangeRequest.create(channelNames,
        minStart,
        maxEnd);

    willReturn(expectedTimeseriesByCanonicalChannelName)
        .given(mockTimeseriesRepository)
        .retrieveWaveformsByChannelAndTime(expectedTimeseriesByCanonicalChannelName.keySet(),
            minStart, maxEnd);

    Collection<ChannelSegment<Waveform>> actualChannelSegments =
        channelSegmentsRepository.retrieveChannelSegmentsByChannelNames(request);

    assertTrue(!actualChannelSegments.isEmpty());

    actualChannelSegments.stream()
        .forEach(actual -> {
          ChannelSegment<? extends Timeseries> expected =
              expectedChannelSegmentsByName.get(actual.getChannel().getName());
          assertEquals(expected.getStartTime(), actual.getStartTime());
          assertEquals(expected.getEndTime(), actual.getEndTime());
          assertEquals(expected.getTimeseriesType(), actual.getTimeseriesType());
          assertEquals(expected.getTimeseries(), actual.getTimeseries());
          assertEquals(expected.getType(), actual.getType());
          assertEquals(expected.getChannel(), actual.getChannel());
          assertEquals(expected.getName(), actual.getName());
          assertEquals(expected.getName(), actual.getName());
        });
  }

  @ParameterizedTest
  @MethodSource("getBatchChannelsTimeArguments")
  void testRetrieveChannelSegmentsByChannelsAndTimeRangesValidation(
      Class<? extends Exception> expectedException,
      Collection<ChannelTimeRangeRequest> requests) {
    assertThrows(expectedException,
        () -> channelSegmentsRepository.retrieveChannelSegmentsByChannelsAndTimeRanges(requests));
  }

  static Stream<Arguments> getBatchChannelsTimeArguments() {
    return Stream.of(arguments(NullPointerException.class, null),
        arguments(IllegalStateException.class, List.of()));
  }

  @Test
  void testRetrieveChannelSegmentsByChannelsAndTimeRangesExactMatch() {
    channelSegmentsRepository.storeChannelSegments(expectedChannelSegmentsByName.values());

    List<ChannelTimeRangeRequest> requests = new ArrayList<>();
    for (ChannelSegment<Waveform> channelSegment : expectedChannelSegmentsByName.values()) {
      ChannelTimeRangeRequest request = ChannelTimeRangeRequest
          .create(channelSegment.getChannel().getName(),
              channelSegment.getStartTime(),
              channelSegment.getEndTime());

      willReturn(channelSegment.getTimeseries())
          .given(mockTimeseriesRepository)
          .retrieveWaveformsByTime(channelSegment.getChannel().getCanonicalName(),
              channelSegment.getStartTime(), channelSegment.getEndTime());

      requests.add(request);
    }

    Collection<ChannelSegment<Waveform>> actualChannelSegments = channelSegmentsRepository
        .retrieveChannelSegmentsByChannelsAndTimeRanges(requests);
    assertEquals(expectedChannelSegmentsByName.values().size(), actualChannelSegments.size());

    for (ChannelSegment<Waveform> actual : actualChannelSegments) {
      ChannelSegment<Waveform> expected = expectedChannelSegmentsByName
          .get(actual.getChannel().getName());
      assertEquals(expected.getStartTime(), actual.getStartTime());
      assertEquals(expected.getEndTime(), actual.getEndTime());
      assertEquals(expected.getTimeseriesType(), actual.getTimeseriesType());
      assertEquals(expected.getTimeseries(), actual.getTimeseries());
      assertEquals(expected.getType(), actual.getType());
      assertEquals(expected.getChannel(), actual.getChannel());
      assertEquals(expected.getName(), actual.getName());
      assertEquals(expected.getName(), actual.getName());
    }
  }

  @Test
  void testRetrieveChannelSegmentsByChannelsAndTimeRangesInexactMatch() {
    Map<String, ChannelSegment<Waveform>> expectedChannelSegments = new HashMap<>();
    Map<String, ChannelTimeRangeRequest> requestsByChannel = new HashMap<>();
    Waveform waveform1 = TestFixtures.buildLongWaveform(Instant.EPOCH.plusSeconds(5), 100, 20);
    ChannelSegment<Waveform> channelSegment1Stored = ChannelSegment.from(UUID.randomUUID(),
        TestFixtures.channel1,
        "Test Channel Segment 1",
        ChannelSegment.Type.RAW,
        List.of(waveform1));

    ChannelTimeRangeRequest request1 =
        ChannelTimeRangeRequest.create(TestFixtures.channel1.getName(),
            channelSegment1Stored.getStartTime().plusSeconds(5),
            channelSegment1Stored.getEndTime().minusSeconds(2));

    Waveform waveform1Trimmed = waveform1.trim(request1.getTimeRange().getStartTime(),
        request1.getTimeRange().getEndTime());
    ChannelSegment<Waveform> channelSegment1Expected = ChannelSegment.from(UUID.randomUUID(),
        TestFixtures.channel1,
        "Test Channel Segment 1",
        ChannelSegment.Type.RAW,
        List.of(waveform1Trimmed));

    expectedChannelSegments.put(channelSegment1Stored.getChannel().getName(),
        channelSegment1Expected);
    requestsByChannel.put(request1.getChannelName(), request1);

    willReturn(List.of(waveform1Trimmed))
        .given(mockTimeseriesRepository)
        .retrieveWaveformsByTime(TestFixtures.channel1.getCanonicalName(),
            request1.getTimeRange().getStartTime(),
            request1.getTimeRange().getEndTime());

    Waveform waveform2 = TestFixtures.buildLongWaveform(Instant.EPOCH.plusSeconds(30), 90, 20);
    ChannelSegment<Waveform> channelSegment2Stored =
        ChannelSegment.from(UUID.randomUUID(),
            TestFixtures.channel2,
            "Test Channel Segment 2",
            ChannelSegment.Type.RAW,
            List.of(waveform2));

    ChannelTimeRangeRequest request2 =
        ChannelTimeRangeRequest.create(TestFixtures.channel2.getName(),
            channelSegment2Stored.getStartTime().plusSeconds(30),
            channelSegment2Stored.getEndTime().minusSeconds(10));

    Waveform waveform2Trimmed = waveform2.trim(request2.getTimeRange().getStartTime(),
        request2.getTimeRange().getEndTime());
    ChannelSegment<Waveform> channelSegment2Expected =
        ChannelSegment.from(channelSegment2Stored.getId(),
            TestFixtures.channel2,
            "Test Channel Segment 2",
            ChannelSegment.Type.RAW,
            List.of(waveform2Trimmed));

    expectedChannelSegments.put(channelSegment2Stored.getChannel().getName(),
        channelSegment2Expected);
    requestsByChannel.put(request2.getChannelName(), request2);

    willReturn(List.of(waveform2Trimmed))
        .given(mockTimeseriesRepository)
        .retrieveWaveformsByTime(TestFixtures.channel2.getCanonicalName(),
            request2.getTimeRange().getStartTime(),
            request2.getTimeRange().getEndTime());

    Waveform waveform3 = TestFixtures.buildLongWaveform(Instant.EPOCH.plusSeconds(20), 80, 20);
    ChannelSegment<Waveform> channelSegment3Stored = ChannelSegment.from(UUID.randomUUID(),
        TestFixtures.channel3,
        "Test Channel Segment 3",
        ChannelSegment.Type.RAW,
        List.of(waveform3));

    channelSegmentsRepository.storeChannelSegments(List.of(channelSegment1Stored,
        channelSegment2Stored,
        channelSegment3Stored));

    Collection<ChannelSegment<Waveform>> actualChannelSegments = channelSegmentsRepository
        .retrieveChannelSegmentsByChannelsAndTimeRanges(List.of(request1, request2));
    assertEquals(expectedChannelSegmentsByName.values().size(), actualChannelSegments.size());

    for (ChannelSegment<Waveform> actual : actualChannelSegments) {
      ChannelSegment<Waveform> expected = expectedChannelSegments
          .get(actual.getChannel().getName());

      assertEquals(expected.getStartTime(), actual.getStartTime());
      assertEquals(expected.getEndTime(), actual.getEndTime());
      assertEquals(expected.getTimeseriesType(), actual.getTimeseriesType());
      assertEquals(expected.getTimeseries(), actual.getTimeseries());
      assertEquals(expected.getType(), actual.getType());
      assertEquals(expected.getChannel(), actual.getChannel());
      assertEquals(expected.getName(), actual.getName());
      assertEquals(expected.getName(), actual.getName());
    }
  }

  @Test
  void testStoringFkChannelSegmentsTwiceWillThrowException() {
    List<ChannelSegment<FkSpectra>> testFkSpectraData = List
        .of(TestFixtures.buildFkSpectraChannelSegment());
    channelSegmentsRepository.storeFkChannelSegments(testFkSpectraData);
    assertThrows(RuntimeException.class,
        () -> channelSegmentsRepository.storeFkChannelSegments(testFkSpectraData));
  }

  @Test
  void testStoringEmptyFkChannelSegmentsWilLThrowException() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        channelSegmentsRepository.storeFkChannelSegments(List.of()));
    assertEquals("Must pass in a non-empty collection of FkSpectra channel segments",
        ex.getMessage());
  }

  @Test
  void testRetrieveFkChannelSegmentsByChannelsAndTime() {
    ChannelSegment<FkSpectra> testFkSpectraData = TestFixtures.buildFkSpectraChannelSegment();
    willReturn(testFkSpectraData.getTimeseries())
        .given(mockTimeseriesRepository)
        .populateFkSpectra(any());

    channelSegmentsRepository.storeFkChannelSegments(List.of(testFkSpectraData));

    // retrieve our collection of fkspectra
    Collection<ChannelTimeRangeRequest> requests = List.of(ChannelTimeRangeRequest
        .create(TestFixtures.channel1.getName(), testFkSpectraData.getStartTime(),
            testFkSpectraData.getEndTime()));
    assertEquals(List.of(testFkSpectraData), channelSegmentsRepository
        .retrieveFkChannelSegmentsByChannelsAndTime(requests));
  }

  @Test
  void testRetrieveFkChannelSegmentThrowsOnNull() {
    assertThrows(NullPointerException.class,
        () ->
            channelSegmentsRepository
                .retrieveFkChannelSegmentsByChannelsAndTime(null));
  }

  private void setUpTimeseriesMock() {
    for (ChannelSegment<Waveform> channelSegment :
        expectedChannelSegmentsByName.values()) {
      willReturn(channelSegment.getTimeseries())
          .given(mockTimeseriesRepository).retrieveWaveformsByTime(
          channelSegment.getChannel().getCanonicalName(),
          channelSegment.getStartTime(),
          channelSegment.getEndTime());
    }
  }

}