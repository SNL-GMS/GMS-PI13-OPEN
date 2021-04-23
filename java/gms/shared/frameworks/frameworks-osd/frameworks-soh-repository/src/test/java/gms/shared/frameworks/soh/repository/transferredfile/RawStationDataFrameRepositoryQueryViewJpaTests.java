package gms.shared.frameworks.soh.repository.transferredfile;

import com.google.common.base.Functions;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import gms.shared.frameworks.soh.repository.util.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.soh.repository.util.DbTest;
import gms.shared.frameworks.soh.repository.util.TestFixtures;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import javax.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testcontainers.junit.jupiter.Testcontainers;

import static gms.shared.frameworks.soh.repository.util.TestFixtures.SEGMENT1_END;
import static gms.shared.frameworks.soh.repository.util.TestFixtures.SEGMENT_END2;
import static gms.shared.frameworks.soh.repository.util.TestFixtures.channel1;
import static gms.shared.frameworks.soh.repository.util.TestFixtures.frame1;
import static gms.shared.frameworks.soh.repository.util.TestFixtures.frame2;
import static gms.shared.frameworks.soh.repository.util.TestFixtures.waveformSummaries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Testcontainers
@Disabled
class RawStationDataFrameRepositoryQueryViewJpaTests extends DbTest {

  private RawStationDataFrameRepositoryQueryViewJpa rsdfQueryView;

  @BeforeEach
  void testCaseSetUp() {
    rsdfQueryView = new RawStationDataFrameRepositoryQueryViewJpa(entityManagerFactory);

    new RawStationDataFrameRepositoryJpa(entityManagerFactory).storeRawStationDataFrames(List.of(TestFixtures.frame1,
      TestFixtures.frame2));
  }

  @AfterEach
  void tearDown() {
    entityManagerFactory.close();
  }

  @Test
  void testRetrieveMetadataByTimeValidation() {
    assertThrows(NullPointerException.class,
      () -> rsdfQueryView.retrieveRawStationDataFrameMetadataByTime(null));
  }

  @ParameterizedTest
  @MethodSource("getRetrieveMetadataArguments")
  void testRetrieveMetadataByTime(TimeRangeRequest request,
    List<RawStationDataFrameMetadata> expected) {
    List<RawStationDataFrameMetadata> actual = rsdfQueryView
      .retrieveRawStationDataFrameMetadataByTime(request);
    assertEquals(expected.size(), actual.size());

    Map<String, RawStationDataFrameMetadata> actualByStation = actual.stream()
      .collect(Collectors.toMap(RawStationDataFrameMetadata::getStationName, Functions.identity()));
    expected.stream().forEach(expectedMetadata -> {
      assertTrue(actualByStation.containsKey(expectedMetadata.getStationName()));
      RawStationDataFrameMetadata actualMetadata = actualByStation.get(expectedMetadata.getStationName());
      assertTrue(EqualsBuilder.reflectionEquals(expectedMetadata, actualMetadata, "channelNames"));
      assertEquals(expectedMetadata.getChannelNames().size(), actualMetadata.getChannelNames().size());
      assertTrue(expectedMetadata.getChannelNames().containsAll(actualMetadata.getChannelNames()));
    });
  }

  static Stream<Arguments> getRetrieveMetadataArguments() {
    return Stream.of(
      arguments(TimeRangeRequest.create(SEGMENT1_END, SEGMENT_END2),
        List.of(frame1.getMetadata(), frame2.getMetadata())),
      arguments(TimeRangeRequest.create(SEGMENT1_END.plusMillis(1), SEGMENT_END2.plusMillis(1)),
        List.of(frame2.getMetadata())),
      arguments(TimeRangeRequest.create(Instant.now(), Instant.now().plusSeconds(2)),
        List.of()));
  }

  @ParameterizedTest
  @MethodSource("getRetrieveLatestSampleTimeArguments")
  void testRetrieveLatestSampleTimeValidation(Class<? extends Exception> expectedException, List<String> channelNames) {
    assertThrows(expectedException, () -> rsdfQueryView.retrieveLatestSampleTimeByChannel(channelNames));
  }

  static Stream<Arguments> getRetrieveLatestSampleTimeArguments() {
    return Stream.of(arguments(NullPointerException.class, null),
      arguments(IllegalStateException.class, List.of()));
  }

  @Test
  void testRetrieveLatestSampleTime() {
    Map<String, Instant> latestSampleTimes = rsdfQueryView.retrieveLatestSampleTimeByChannel(List.of(channel1.getName()));
    assertEquals(1, latestSampleTimes.size());
    assertTrue(latestSampleTimes.containsKey(channel1.getName()));
    assertEquals(waveformSummaries.get(channel1.getName()).getEndTime(), latestSampleTimes.get(channel1.getName()));
  }

  @Test
  void testRetrieverLatestSampleTimeDuplicates() {
    new RawStationDataFrameRepositoryJpa(entityManagerFactory).storeRawStationDataFrames(List.of(frame1.toBuilder().setId(UUID.randomUUID()).build()));
    Map<String, Instant> latestSampleTimes = rsdfQueryView.retrieveLatestSampleTimeByChannel(List.of(channel1.getName()));
    assertEquals(1, latestSampleTimes.size());
    assertTrue(latestSampleTimes.containsKey(channel1.getName()));
    assertEquals(waveformSummaries.get(channel1.getName()).getEndTime(), latestSampleTimes.get(channel1.getName()));
  }

}