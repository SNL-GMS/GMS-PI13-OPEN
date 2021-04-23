package gms.core.dataacquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeRangeMap;
import gms.core.dataacquisition.AceiBooleanRangeMap.Update;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AceiBooleanRangeMapTest {

  private static final String CHANNEL_NAME = "test";
  private static final AcquiredChannelEnvironmentIssueType TYPE = AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED;
  private static final Duration MERGE_TOLERANCE = Duration.ofSeconds(1);

  private AceiBooleanRangeMap aceiMap;

  @BeforeEach
  void setUp() {
    aceiMap = new AceiBooleanRangeMap(CHANNEL_NAME, TYPE, MERGE_TOLERANCE, true);
  }

  @ParameterizedTest
  @MethodSource("putInvalidArguments")
  void testPutInvalidArguments(AcquiredChannelEnvironmentIssueBoolean acei,
      String expectedMessage) {
    IllegalArgumentException actualException = assertThrows(IllegalArgumentException.class,
        () -> aceiMap.put(acei));

    assertEquals(expectedMessage, actualException.getMessage());
  }

  private static Stream<Arguments> putInvalidArguments() {
    return Stream.of(
        arguments(AcquiredChannelEnvironmentIssueBoolean
                .create("Not Test", TYPE,
                    Instant.EPOCH, Instant.EPOCH.plusSeconds(1), true),
            "Cannot insert ACEI, channel name Not Test does not match expected test"),
        arguments(AcquiredChannelEnvironmentIssueBoolean
                .create(CHANNEL_NAME, AcquiredChannelEnvironmentIssueType.AMPLIFIER_SATURATION_DETECTED,
                    Instant.EPOCH, Instant.EPOCH.plusSeconds(1), true),
            "Cannot insert ACEI, Type amplifier saturation detected does not match expected vault door opened")
    );
  }

  @Test
  void testRangeMapBehavior() {
    RangeMap<Instant, String> rangeMap = TreeRangeMap.create();

    Instant now = Instant.now();

    Range<Instant> fiveSecondsAgoTilNow = Range.closedOpen(now.minusSeconds(5), now);
    Range<Instant> nowTilFiveSecondsFromNow = Range.closedOpen(now, now.plusSeconds(5));
    Range<Instant> fiveSecondsFromNowTilTenSecondFromNow = Range.closedOpen(
        now.plusSeconds(5), now.plusSeconds(10)
    );

    rangeMap.put(fiveSecondsAgoTilNow, "a");
    rangeMap.put(fiveSecondsFromNowTilTenSecondFromNow, "c");

    // Should not be anything in the middle range.
    assertTrue(rangeMap.subRangeMap(nowTilFiveSecondsFromNow).asMapOfRanges().isEmpty());

    // But there should be two in the subrange map if the middle range is expanded on each end
    // by a second.
    RangeMap<Instant, String> subrangeMap = rangeMap.subRangeMap(
        Range.closedOpen(now.minusSeconds(1), now.plusSeconds(6))
    );

    assertEquals("a", subrangeMap.get(now.minusMillis(500)));
    assertEquals("c", subrangeMap.get(now.plusMillis(5500)));

    Map<Range<Instant>, String> map = subrangeMap.asMapOfRanges();
    assertEquals(2, map.size());

    assertEquals(Arrays.asList("a", "c"),
        new ArrayList<>(subrangeMap.asMapOfRanges().values()));

    Iterator<Range<Instant>> it = map.keySet().iterator();
    // The ranges will be truncated to fit within the subrange range.
    assertEquals(Range.closedOpen(now.minusSeconds(1), now), it.next());
    assertEquals(Range.closedOpen(now.plusSeconds(5), now.plusSeconds(6)), it.next());
  }

  /**
   * Test putting an acei with an opposing status between 2 previously inserted aceis that it
   * overlaps by a small amount.
   */
  @Test
  void testPutWithConflicts1() {

    Instant startTime = Instant.now();
    Instant endTime = startTime.plusSeconds(5);

    AcquiredChannelEnvironmentIssueBoolean aceiBefore = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, startTime.minusSeconds(6), startTime.minusMillis(500), true);

    AceiBooleanRangeMap.Update update = aceiMap.put(aceiBefore);

    assertTrue(update.getRemovedAceis().isEmpty());
    assertEquals(1, update.getInsertedAceis().size());
    assertEquals(aceiBefore,
        update.getInsertedAceis().toArray(new AcquiredChannelEnvironmentIssueBoolean[1])[0]);

    AcquiredChannelEnvironmentIssueBoolean aceiAfter = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, endTime.plusMillis(500), endTime.plusSeconds(6), true);

    update = aceiMap.put(aceiAfter);
    assertTrue(update.getRemovedAceis().isEmpty());
    assertEquals(1, update.getInsertedAceis().size());
    assertEquals(aceiAfter,
        update.getInsertedAceis().toArray(new AcquiredChannelEnvironmentIssueBoolean[1])[0]);

    AcquiredChannelEnvironmentIssueBoolean aceiMiddle = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, startTime.minusSeconds(1), endTime.plusSeconds(1), false);

    update = aceiMap.put(aceiMiddle);

    assertTrue(update.getRemovedAceis().isEmpty());
    assertEquals(1, update.getInsertedAceis().size());

    AcquiredChannelEnvironmentIssueBoolean insertedAcei = update.getInsertedAceis().toArray(
        new AcquiredChannelEnvironmentIssueBoolean[1])[0];

    // Should equal aceiMiddle except for the UUID and the endpoints. An overlapping acei does
    // not override the values already in the map, it just fills in the gaps.
    assertEquals(AcquiredChannelEnvironmentIssueBoolean.from(
        insertedAcei.getId(),
        aceiMiddle.getChannelName(),
        aceiMiddle.getType(),
        aceiBefore.getEndTime(),
        aceiAfter.getStartTime(),
        aceiMiddle.getStatus()
        ), insertedAcei);
  }

  /**
   * Test putting an acei with the same status between 2 previously inserted aceis that it
   * overlaps by a small amount.
   */
  @Test
  void testPutWithConflicts2() {

    Instant startTime = Instant.now();
    Instant endTime = startTime.plusSeconds(5);

    AcquiredChannelEnvironmentIssueBoolean aceiBefore = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, startTime.minusSeconds(6), startTime.minusMillis(500), true);

    AceiBooleanRangeMap.Update update = aceiMap.put(aceiBefore);

    AcquiredChannelEnvironmentIssueBoolean aceiAfter = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, endTime.plusMillis(500), endTime.plusSeconds(6), true);

    update = aceiMap.put(aceiAfter);

    AcquiredChannelEnvironmentIssueBoolean aceiMiddle = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, startTime.minusSeconds(1), endTime.plusSeconds(1), true);

    update = aceiMap.put(aceiMiddle);

    assertEquals(2, update.getRemovedAceis().size());
    assertTrue(update.getRemovedAceis().contains(aceiBefore));
    assertTrue(update.getRemovedAceis().contains(aceiAfter));

    assertEquals(1, update.getInsertedAceis().size());

    AcquiredChannelEnvironmentIssueBoolean insertedAcei = update.getInsertedAceis().toArray(
        new AcquiredChannelEnvironmentIssueBoolean[1])[0];

    // Should equal aceiMiddle except for the UUID and the endpoints. An overlapping acei does
    // not override the values already in the map, it just fills in the gaps.
    assertEquals(AcquiredChannelEnvironmentIssueBoolean.from(
        insertedAcei.getId(),
        aceiMiddle.getChannelName(),
        aceiMiddle.getType(),
        aceiBefore.getStartTime(),
        aceiAfter.getEndTime(),
        aceiMiddle.getStatus()
    ), insertedAcei);
  }

  @Test
  void testPutEmptyMap() {
    Instant startTime = Instant.EPOCH;
    Instant endTime = startTime.plusSeconds(1);
    AcquiredChannelEnvironmentIssueBoolean expectedAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, startTime, endTime, true);
    AceiBooleanRangeMap.Update actualUpdate = aceiMap.put(expectedAcei);

    assertTrue(actualUpdate.getRemovedAceis().isEmpty());
    assertEquals(1, actualUpdate.getInsertedAceis().size());
    assertTrue(actualUpdate.getInsertedAceis().contains(expectedAcei));

    assertInMap(startTime, expectedAcei);
    assertInMap(startTime.plusMillis(500), expectedAcei);
    assertInMap(endTime, expectedAcei);

    assertEquals(1, aceiMap.size());

    aceiMap.clear();

    assertEquals(0, aceiMap.size());
  }

  @Test
  void testPutWithGapSameStatusNoMerge() {
    Instant firstStartTime = Instant.EPOCH;
    Instant firstEndTime = firstStartTime.plusSeconds(1);
    AcquiredChannelEnvironmentIssueBoolean firstAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, firstStartTime, firstEndTime, true);

    Instant secondStartTime = firstEndTime.plus(MERGE_TOLERANCE).plusSeconds(1);
    Instant secondEndTime = secondStartTime.plusSeconds(1);
    AcquiredChannelEnvironmentIssueBoolean secondAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, secondStartTime, secondEndTime, true);

    aceiMap.put(firstAcei);
    Update secondUpdate = aceiMap.put(secondAcei);

    assertTrue(secondUpdate.getRemovedAceis().isEmpty());
    assertEquals(1, secondUpdate.getInsertedAceis().size());
    assertTrue(secondUpdate.getInsertedAceis().contains(secondAcei));

    assertEquals(2, aceiMap.size());

    assertInMap(firstStartTime, firstAcei);
    assertInMap(firstStartTime.plusMillis(500), firstAcei);
    assertInMap(firstEndTime, firstAcei);
    assertInMap(secondStartTime, secondAcei);
    assertInMap(secondStartTime.plusMillis(500), secondAcei);
    assertInMap(secondEndTime, secondAcei);
  }

  @Test
  void testPutNoGapSameStatusMerge() {

    Instant firstStartTime = Instant.EPOCH;
    Instant firstEndTime = firstStartTime.plusSeconds(1);
    AcquiredChannelEnvironmentIssueBoolean firstAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, firstStartTime, firstEndTime, true);

    Instant secondStartTime = firstEndTime.plus(MERGE_TOLERANCE);
    Instant secondEndTime = secondStartTime.plusSeconds(1);
    AcquiredChannelEnvironmentIssueBoolean secondAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, secondStartTime, secondEndTime, true);

    aceiMap.put(firstAcei);

    assertEquals(1, aceiMap.size());

    Update secondUpdate = aceiMap.put(secondAcei);

    assertEquals(1, secondUpdate.getRemovedAceis().size());
    assertTrue(secondUpdate.getRemovedAceis().contains(firstAcei));
    assertEquals(1, secondUpdate.getInsertedAceis().size());

    assertEquals(1, aceiMap.size());

    AcquiredChannelEnvironmentIssueBoolean insertedAcei = secondUpdate.getInsertedAceis().iterator()
        .next();
    AcquiredChannelEnvironmentIssueBoolean expectedAcei = AcquiredChannelEnvironmentIssueBoolean
        .from(insertedAcei.getId(), CHANNEL_NAME, TYPE, firstStartTime, secondEndTime, true);
    assertEquals(expectedAcei, insertedAcei);

    assertInMap(firstStartTime, expectedAcei);
    assertInMap(firstEndTime, expectedAcei);
    assertInMap(firstStartTime.plusMillis(500), expectedAcei);
    assertInMap(secondStartTime, expectedAcei);
    assertInMap(secondStartTime.plusMillis(500), expectedAcei);
    assertInMap(secondEndTime, expectedAcei);

  }

  @Test
  void testPutNoGapDifferentStatusNoMerge() {
    Instant firstStartTime = Instant.EPOCH;
    Instant firstEndTime = firstStartTime.plusSeconds(1);
    AcquiredChannelEnvironmentIssueBoolean firstAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, firstStartTime, firstEndTime, true);

    Instant secondStartTime = firstEndTime.plus(MERGE_TOLERANCE);
    Instant secondEndTime = secondStartTime.plusSeconds(1);
    AcquiredChannelEnvironmentIssueBoolean secondAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, secondStartTime, secondEndTime, false);

    aceiMap.put(firstAcei);
    Update secondUpdate = aceiMap.put(secondAcei);

    assertTrue(secondUpdate.getRemovedAceis().isEmpty());
    assertEquals(1, secondUpdate.getInsertedAceis().size());
    assertTrue(secondUpdate.getInsertedAceis().contains(secondAcei));

    assertInMap(firstStartTime, firstAcei);
    assertInMap(firstStartTime.plusMillis(500), firstAcei);
    assertInMap(firstEndTime, firstAcei);
    assertInMap(secondStartTime, secondAcei);
    assertInMap(secondStartTime.plusMillis(500), secondAcei);
    assertInMap(secondEndTime, secondAcei);
  }

  @Test
  void testFillGapSameStatusMerge() {
    Instant firstStartTime = Instant.EPOCH;
    Instant firstEndTime = firstStartTime.plusSeconds(1);
    AcquiredChannelEnvironmentIssueBoolean firstAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, firstStartTime, firstEndTime, true);

    Instant secondStartTime = firstEndTime.plus(MERGE_TOLERANCE);
    Instant secondEndTime = secondStartTime.plusSeconds(1);
    AcquiredChannelEnvironmentIssueBoolean secondAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, secondStartTime, secondEndTime, true);

    Instant thirdStartTime = secondStartTime.plus(MERGE_TOLERANCE);
    Instant thirdEndTime = thirdStartTime.plusSeconds(1);
    AcquiredChannelEnvironmentIssueBoolean thirdAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, TYPE, thirdStartTime, thirdEndTime, true);

    Update update = aceiMap.put(firstAcei);
    assertEquals(1, update.getInsertedAceis().size());
    assertEquals(0, update.getRemovedAceis().size());
    assertTrue(update.getInsertedAceis().contains(firstAcei));

    update = aceiMap.put(thirdAcei);
    assertEquals(1, update.getInsertedAceis().size());
    assertEquals(0, update.getRemovedAceis().size());
    assertTrue(update.getInsertedAceis().contains(thirdAcei));

    update = aceiMap.put(secondAcei);
    checkUpdate(update);

    assertEquals(2, update.getRemovedAceis().size());
    assertTrue(update.getRemovedAceis().contains(firstAcei));
    assertTrue(update.getRemovedAceis().contains(thirdAcei));

    assertEquals(1, update.getInsertedAceis().size());
    AcquiredChannelEnvironmentIssueBoolean insertedAcei = update.getInsertedAceis().iterator()
        .next();
    AcquiredChannelEnvironmentIssueBoolean expectedAcei = AcquiredChannelEnvironmentIssueBoolean
        .from(insertedAcei.getId(), CHANNEL_NAME, TYPE, firstStartTime, thirdEndTime, true);
    assertEquals(expectedAcei, insertedAcei);

    assertInMap(firstStartTime, expectedAcei);
    assertInMap(firstEndTime, expectedAcei);
    assertInMap(firstStartTime.plusMillis(500), expectedAcei);
    assertInMap(secondStartTime, expectedAcei);
    assertInMap(secondStartTime.plusMillis(500), expectedAcei);
    assertInMap(secondEndTime, expectedAcei);
    assertInMap(thirdStartTime, expectedAcei);
    assertInMap(thirdStartTime.plusMillis(500), expectedAcei);
    assertInMap(thirdEndTime, expectedAcei);

  }

  @Test
  void testRandomPuts() {
    Instant epoch = Instant.EPOCH;
    Random random = new SecureRandom();

    final int numPuts = 100;
    int expectedSize = 0;

    Set<UUID> insertedUUIDs = new HashSet<>();
    Set<UUID> removedUUIDs = new HashSet<>();

    for (int i=0; i<numPuts; i++) {

      int startMin = random.nextInt(100);
      int endMin = startMin + 1 + random.nextInt(20);
      Instant start = epoch.plusSeconds(startMin * 60);
      Instant end = epoch.plusSeconds(endMin * 60);

      Optional<AcquiredChannelEnvironmentIssueBoolean> afterOpt = aceiMap.get(end);

      AcquiredChannelEnvironmentIssueBoolean acei = AcquiredChannelEnvironmentIssueBoolean
          .create(CHANNEL_NAME, TYPE, start, end, random.nextBoolean());

      AceiBooleanRangeMap.Update update = aceiMap.put(acei);
      checkUpdate(update);

      expectedSize += update.getInsertedAceis().size();
      expectedSize -= update.getRemovedAceis().size();

      update.getInsertedAceis().stream()
          .map(AcquiredChannelEnvironmentIssue::getId)
          .forEach(uuid -> {
            assertFalse(insertedUUIDs.contains(uuid));
            insertedUUIDs.add(uuid);
          });

      update.getRemovedAceis().stream()
          .map(AcquiredChannelEnvironmentIssue::getId)
          .forEach(uuid -> {
            assertFalse(removedUUIDs.contains(uuid));
            removedUUIDs.add(uuid);
          });

      assertNoOverlap();
    }

    assertEquals(expectedSize, aceiMap.size());
  }

  @Test
  void testOverlappingPutsWithNoGaps() {

    Instant epoch = Instant.EPOCH;
    int maxMinute = 50;
    int curMinute = 1;

    boolean status = false;
    while(curMinute < maxMinute) {
      Instant startInstant = epoch.plus(Duration.ofMinutes(curMinute));
      Instant endInstant = startInstant.plus(Duration.ofMinutes(5));
      AcquiredChannelEnvironmentIssueBoolean acei = AcquiredChannelEnvironmentIssueBoolean.create(
          CHANNEL_NAME, TYPE, startInstant, endInstant, status
      );
      status = !status;
      AceiBooleanRangeMap.Update update = aceiMap.put(acei);
      assertEquals(1, update.getInsertedAceis().size());
      assertEquals(0, update.getRemovedAceis().size());
      curMinute += 5;
    }

    Range<Instant> span = aceiMap.internalMap().span();

    AcquiredChannelEnvironmentIssueBoolean acei = AcquiredChannelEnvironmentIssueBoolean.create(
        CHANNEL_NAME, TYPE, span.lowerEndpoint().plus(Duration.ofMinutes(1)),
        span.upperEndpoint().minus(Duration.ofMinutes(1)),
        status
    );

    AceiBooleanRangeMap.Update update = aceiMap.put(acei);
    assertEquals(0, update.getInsertedAceis().size());
    assertEquals(0, update.getRemovedAceis().size());

    acei = AcquiredChannelEnvironmentIssueBoolean.create(
        CHANNEL_NAME, TYPE, span.lowerEndpoint().minus(Duration.ofMinutes(1)),
        span.upperEndpoint().minus(Duration.ofMinutes(1)),
        true
    );

    update = aceiMap.put(acei);
    assertEquals(1, update.getInsertedAceis().size());
    assertEquals(0, update.getRemovedAceis().size());

    AcquiredChannelEnvironmentIssueBoolean insertedAcei =
        update.getInsertedAceis().iterator().next();
    assertEquals(acei.getStartTime(), insertedAcei.getStartTime());
    assertEquals(insertedAcei.getEndTime(), span.lowerEndpoint());

    acei = AcquiredChannelEnvironmentIssueBoolean.create(
        CHANNEL_NAME, TYPE, span.lowerEndpoint().plus(Duration.ofMinutes(1)),
        span.upperEndpoint().plus(Duration.ofMinutes(1)),
        status
    );

    update = aceiMap.put(acei);
    assertEquals(1, update.getInsertedAceis().size());
    assertEquals(0, update.getRemovedAceis().size());

    insertedAcei = update.getInsertedAceis().iterator().next();
    assertEquals(acei.getEndTime(), insertedAcei.getEndTime());
    assertEquals(insertedAcei.getStartTime(), span.upperEndpoint());
  }

  @Test
  void testOverlappingPutsWithGaps() {

    Instant epoch = Instant.EPOCH;
    int maxMinute = 50;
    int curMinute = 1;

    while(curMinute < maxMinute) {
      Instant startInstant = epoch.plus(Duration.ofMinutes(curMinute));
      Instant endInstant = startInstant.plus(Duration.ofMinutes(3));
      AcquiredChannelEnvironmentIssueBoolean acei = AcquiredChannelEnvironmentIssueBoolean.create(
          CHANNEL_NAME, TYPE, startInstant, endInstant, false
      );
      AceiBooleanRangeMap.Update update = aceiMap.put(acei);
      assertEquals(1, update.getInsertedAceis().size());
      assertEquals(0, update.getRemovedAceis().size(),
          String.format("curMinute = %d", curMinute));
      curMinute += 5;
    }

    int numAceis = aceiMap.size();

    Range<Instant> span = aceiMap.internalMap().span();

    AcquiredChannelEnvironmentIssueBoolean acei = AcquiredChannelEnvironmentIssueBoolean.create(
        CHANNEL_NAME, TYPE, span.lowerEndpoint(),
        span.upperEndpoint(),
        true
    );

    AceiBooleanRangeMap.Update update = aceiMap.put(acei);
    assertEquals(numAceis - 1, update.getInsertedAceis().size());
    assertEquals(0, update.getRemovedAceis().size());

    acei = AcquiredChannelEnvironmentIssueBoolean.create(
        CHANNEL_NAME, TYPE, span.lowerEndpoint().minus(Duration.ofMinutes(1)),
        span.upperEndpoint().plus(Duration.ofMinutes(1)),
        true
    );

    update = aceiMap.put(acei);
    assertEquals(2, update.getInsertedAceis().size());
    assertEquals(0, update.getRemovedAceis().size());

    Optional<AcquiredChannelEnvironmentIssueBoolean> opt = aceiMap.get(acei.getStartTime());
    assertTrue(opt.isPresent());
    AcquiredChannelEnvironmentIssueBoolean aceiFront = opt.get();

    assertTrue(update.getInsertedAceis().contains(aceiFront));
    opt = aceiMap.get(span.upperEndpoint());

    assertTrue(opt.isPresent());
    assertTrue(update.getInsertedAceis().contains(opt.get()));
  }

  @Test
  void testClearBefore() {
    Instant epoch = Instant.EPOCH;

    // Add 11 5 second aceis with 2 seconds between each so they are not merged.
    int aceiCount = 11;
    int aceiLen = 5;
    int aceiSpacing = 2;

    Instant start = epoch;
    for (int i=0; i<aceiCount; i++) {
      Instant end = start.plusSeconds(5);

      AcquiredChannelEnvironmentIssueBoolean acei = AcquiredChannelEnvironmentIssueBoolean.create(
          CHANNEL_NAME, TYPE, start, end, true
      );
      AceiBooleanRangeMap.Update update = aceiMap.put(acei);
      checkUpdate(update);

      assertEquals(1, update.getInsertedAceis().size());
      assertEquals(0, update.getRemovedAceis().size());
      assertEquals(i+1, aceiMap.size());

      start = end.plusSeconds(2);
    }

    Instant center = epoch.plusSeconds((aceiCount*aceiLen + (aceiCount-1)*aceiSpacing)/2);

    aceiMap.clearBefore(center);

    assertEquals(1 + aceiCount/2, aceiMap.size());
  }

  /**
   * Tests what happens when an acei of a given status is added when another acei of that
   * same status is already present with an interval that completely contains the new acei.
   *
   * This probably never actually occurs. But if somehow it did,
   * the behavior should at least make sense.
   */
  @Test
  void testAceiWithinExistingAcei() {
    Instant start1 = Instant.EPOCH;
    Instant end1 = start1.plusSeconds(10);

    Instant start2 = start1.plusSeconds(1);
    Instant end2 = end1.minusSeconds(1);

    AcquiredChannelEnvironmentIssueBoolean acei1 = AcquiredChannelEnvironmentIssueBoolean.create(
        CHANNEL_NAME, TYPE, start1, end1, true
    );

    AcquiredChannelEnvironmentIssueBoolean acei2 = AcquiredChannelEnvironmentIssueBoolean.create(
        CHANNEL_NAME, TYPE, start2, end2, true
    );

    AceiBooleanRangeMap.Update update = aceiMap.put(acei1);
    assertEquals(1, update.getInsertedAceis().size());
    assertEquals(0, update.getRemovedAceis().size());

    update = aceiMap.put(acei2);
    checkUpdate(update);

    // Since acei2 is completely contained within acei1 and they both have the same status,
    // either of 2 results would make sense.
    //
    if (update.getInsertedAceis().isEmpty() && update.getRemovedAceis().isEmpty()) {

      // 1. No updates at all with acei1 still in aceiMap.
      Optional<AcquiredChannelEnvironmentIssueBoolean> opt = aceiMap.get(start1);
      assertTrue(opt.isPresent());
      assertEquals(acei1, opt.get());

      opt = aceiMap.get(end1);
      assertTrue(opt.isPresent());
      assertEquals(acei1, opt.get());

    } else {

      // 2. acei1 removed, but replaced by an acei identical in all but the id.

      // The inserted acei should be identical to acei1, except for the id.
      assertEquals(1, update.getInsertedAceis().size());
      // The removed acei should be acei1.
      assertEquals(1, update.getRemovedAceis().size());

      assertTrue(update.getRemovedAceis().contains(acei1));

      Optional<AcquiredChannelEnvironmentIssueBoolean> opt = update.getInsertedAceis().stream()
          .findAny();

      assertTrue(opt.isPresent());

      AcquiredChannelEnvironmentIssueBoolean inserted = opt.get();

      assertEquals(acei1, AcquiredChannelEnvironmentIssueBoolean.from(
          acei1.getId(),
          inserted.getChannelName(),
          inserted.getType(),
          inserted.getStartTime(),
          inserted.getEndTime(),
          inserted.getStatus()));
    }
  }

  @Test
  void confirmContiguousOppositesAreNotMerged() {
    int numAceis = 5;
    int aceiLen = 5;
    Instant start = Instant.EPOCH;
    boolean status = true;

    for (int i=0; i<numAceis; i++) {
      Instant end = start.plusSeconds(aceiLen);
      AcquiredChannelEnvironmentIssueBoolean acei = AcquiredChannelEnvironmentIssueBoolean.create(
          CHANNEL_NAME, TYPE, start, end, status
      );
      Update update = aceiMap.put(acei);
      checkUpdate(update);
      assertEquals(1, update.getInsertedAceis().size());
      assertEquals(0, update.getRemovedAceis().size());
      assertTrue(update.getInsertedAceis().contains(acei));
      start = end;
      status = !status;
    }

    assertEquals(numAceis, aceiMap.size());
  }

  @Test
  void confirmContiguousSamesAreMerged() {
    int numAceis = 5;
    int aceiLen = 5;
    Instant start = Instant.EPOCH;

    for (int i=0; i<numAceis; i++) {
      Instant end = start.plusSeconds(aceiLen);
      AcquiredChannelEnvironmentIssueBoolean acei = AcquiredChannelEnvironmentIssueBoolean.create(
          CHANNEL_NAME, TYPE, start, end, true
      );
      Update update = aceiMap.put(acei);
      checkUpdate(update);
      assertEquals(1, update.getInsertedAceis().size());
      assertEquals(i > 0 ? 1 : 0, update.getRemovedAceis().size());
      start = end;
    }

    assertEquals(1, aceiMap.size());
  }

  private void assertInMap(Instant timeKey, AcquiredChannelEnvironmentIssueBoolean secondAcei) {
    aceiMap.get(timeKey).ifPresentOrElse(
        actualAcei -> assertEquals(secondAcei, actualAcei),
        () -> fail("Expected ACEI not found")
    );
  }

  private void assertNoOverlap() {
    RangeMap<Instant, AcquiredChannelEnvironmentIssueBoolean> rangeMap = aceiMap.internalMap();
    Range<Instant> lastRange = null;
    for (Range<Instant> range: rangeMap.asMapOfRanges().keySet()) {
      if (lastRange != null) {
        assertFalse(lastRange.upperEndpoint().isAfter(range.lowerEndpoint()));
      }
      lastRange = range;
    }
  }

  private void assertStatusEquals(Instant start, Instant end, boolean status, long msecInc) {
    Instant instant = start;
    while(instant.isBefore(end)) {
      Optional<AcquiredChannelEnvironmentIssueBoolean> opt = aceiMap.get(instant);
      assertTrue(opt.isPresent());
      assertEquals(status, opt.get().getStatus());
      instant = instant.plusMillis(msecInc);
    }
  }

  private static void checkUpdate(AceiBooleanRangeMap.Update update) {
    assertTrue(Sets.intersection(update.getInsertedAceis(), update.getRemovedAceis()).isEmpty());
  }
}