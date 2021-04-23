package gms.shared.frameworks.osd.control.waveforms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gms.shared.frameworks.osd.api.util.StationTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.api.waveforms.RawStationDataFrameRepositoryInterface;
import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.osd.control.utils.TestFixtures;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RawStationDataFrameRepositoryJpaTests {

  private static RawStationDataFrameRepositoryInterface dataFramePersistence;

  @BeforeAll
  public static void setUp()  {
    EntityManagerFactory entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    dataFramePersistence = new RawStationDataFrameRepositoryJpa(entityManagerFactory);
    // store 2 frames for further testing
    dataFramePersistence.storeRawStationDataFrames(List.of(TestFixtures.frame1, TestFixtures.frame2));
  }

  @Test
  public void testStoreFrame1Again() {
    Assertions.assertThrows(Exception.class, () -> {
      dataFramePersistence.storeRawStationDataFrames(List.of(TestFixtures.frame1));
    });
  }

  @Test
  public void retrieveByStationIdTest() {
    // Query for frame1's id with time range [frame1.start, frame2.end], should only find frame1
    // since the id of frame2 is different.
    List<RawStationDataFrame> results = dataFramePersistence.retrieveRawStationDataFramesByStationAndTime(
        StationTimeRangeRequest.create(TestFixtures.frame1.getMetadata().getStationName(),
            TestFixtures.SEGMENT_START, TestFixtures.SEGMENT_END2));
    assertEquals(List.of(TestFixtures.frame1), results);

    // Query for frame2's id with time range [frame1.start, frame2.end], should only find frame2
    // since the id of frame1 is different.
    results = dataFramePersistence.retrieveRawStationDataFramesByStationAndTime(
        StationTimeRangeRequest.create(TestFixtures.frame2.getMetadata().getStationName(),
            TestFixtures.SEGMENT_START, TestFixtures.SEGMENT_END2));
    assertEquals(List.of(TestFixtures.frame2), results);

    // query for frame1's id with time range [frame1.start - 1, frame2.end],
    // finds frame 1 since it has some data for the first second of the range
    // (and frame2's id differs)
    results = dataFramePersistence.retrieveRawStationDataFramesByStationAndTime(
        StationTimeRangeRequest.create(TestFixtures.frame1.getMetadata().getStationName(),
            TestFixtures.SEGMENT1_END.minusSeconds(1), TestFixtures.SEGMENT_END2));
    assertEquals(List.of(TestFixtures.frame1), results);

    // query for a fake name, with time range [frame1.start, frame2.end],
    // finds nothing because the id is no good.
    results = dataFramePersistence.retrieveRawStationDataFramesByStationAndTime(
        StationTimeRangeRequest.create(UUID.randomUUID().toString(),
            TestFixtures.SEGMENT_START, TestFixtures.SEGMENT_END2));
    assertEquals(List.of(), results);

    // query for frame1's id in [EPOCH, frame1.start], only find frame 1.
    results = dataFramePersistence.retrieveRawStationDataFramesByStationAndTime(
        StationTimeRangeRequest.create(TestFixtures.frame1.getMetadata().getStationName(),
            Instant.EPOCH, TestFixtures.SEGMENT_START));
    assertEquals(List.of(TestFixtures.frame1), results);

    // query for frame1's name in [EPOCH, frame1.start - 1], find nothing because time range
    // is before frame1.start.
    results = dataFramePersistence.retrieveRawStationDataFramesByStationAndTime(
        StationTimeRangeRequest.create(TestFixtures.frame1.getMetadata().getStationName(),
            Instant.EPOCH, TestFixtures.SEGMENT_START.minusSeconds(1)));
    assertEquals(List.of(), results);

    // query for frame2's id in [frame2.start, frame2.end], only finds frame2.
    results = dataFramePersistence.retrieveRawStationDataFramesByStationAndTime(
        StationTimeRangeRequest.create(TestFixtures.frame2.getMetadata().getStationName(),
            TestFixtures.SEGMENT_START2, TestFixtures.SEGMENT_END2));
    assertEquals(List.of(TestFixtures.frame2), results);

    // query for frame2's id in [frame1.start, frame2.start - 1], finds nothing.
    results = dataFramePersistence.retrieveRawStationDataFramesByStationAndTime(
        StationTimeRangeRequest.create(TestFixtures.frame2.getMetadata().getStationName(),
            TestFixtures.SEGMENT_START, TestFixtures.SEGMENT_START2.minusSeconds(1)));
    assertEquals(List.of(), results);
  }

  @Test
  public void retrieveByTimeTest()  {
    // Retrieve frames, giving exact start/end times of the two known frames.
    // Ensure they were retrieved.
    List<RawStationDataFrame> results = dataFramePersistence.retrieveRawStationDataFramesByTime(
        TimeRangeRequest.create(TestFixtures.SEGMENT_START, TestFixtures.SEGMENT_END2));
    assertEquals(TestFixtures.allFrames, results);
    // query in [frame1.start - 1, frame2.start + 1],
    // since the time range (barely) touches both frames they should both be returned.
    results = dataFramePersistence.retrieveRawStationDataFramesByTime(
        TimeRangeRequest.create(TestFixtures.SEGMENT1_END.minusSeconds(1),
            TestFixtures.SEGMENT_START2.plusSeconds(1)));
    assertEquals(TestFixtures.allFrames, results);
    // query in [frame1.start - 1, frame2.end + 1], should find both frames.
    results = dataFramePersistence.retrieveRawStationDataFramesByTime(
        TimeRangeRequest.create(TestFixtures.SEGMENT_START.minusSeconds(1),
            TestFixtures.SEGMENT_END2.plusSeconds(1)));
    assertEquals(TestFixtures.allFrames, results);
    // query in [EPOCH, frame1.start], finds the first frame only.
    results = dataFramePersistence.retrieveRawStationDataFramesByTime(
        TimeRangeRequest.create(Instant.EPOCH, TestFixtures.SEGMENT_START));
    assertEquals(List.of(TestFixtures.frame1), results);
    // query in [EPOCH, frame1.start - 1], finds nothing.
    results = dataFramePersistence.retrieveRawStationDataFramesByTime(
        TimeRangeRequest.create(Instant.EPOCH, TestFixtures.SEGMENT_START.minusSeconds(1)));
    assertEquals(List.of(), results);
    // query in [frame2.start, frame2.end], only finds the 2nd frame.
    results = dataFramePersistence.retrieveRawStationDataFramesByTime(
        TimeRangeRequest.create(TestFixtures.SEGMENT_START2, TestFixtures.SEGMENT_END2));
    assertEquals(List.of(TestFixtures.frame2), results);
    // query in [frame2.end + 1, frame2.end + 61], finds nothing.
    results = dataFramePersistence.retrieveRawStationDataFramesByTime(
        TimeRangeRequest.create(TestFixtures.SEGMENT_END2.plusSeconds(1),
            TestFixtures.SEGMENT_END2.plusSeconds(61)));
    assertEquals(List.of(), results);
  }

  @Test
  public void storeNullFrameTest()  {
    Assertions.assertThrows(Exception.class, () -> {
      dataFramePersistence.storeRawStationDataFrames(null);
    });
  }

}
