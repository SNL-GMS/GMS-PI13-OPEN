package gms.shared.frameworks.osd.coi.stationreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ReferenceSiteTest {

  final UUID versionId = UUID.nameUUIDFromBytes(
      (StationReferenceTestFixtures.SITE_NAME + StationReferenceTestFixtures.LATITUDE + StationReferenceTestFixtures.LONGITUDE +
          StationReferenceTestFixtures.ELEVATION + StationReferenceTestFixtures.ACTUAL_TIME)
          .getBytes(StandardCharsets.UTF_16LE));

  @Test
  public void testSerialization() throws Exception {
    TestUtilities.testSerialization(StationReferenceTestFixtures.site, ReferenceSite.class);
  }

  @Test
  public void testAddAlias() {
    ReferenceSite site = ReferenceSite.builder()
        .setName(StationReferenceTestFixtures.SITE_NAME)
        .setDescription(StationReferenceTestFixtures.DESCRIPTION)
        .setSource(StationReferenceTestFixtures.INFORMATION_SOURCE)
        .setComment(StationReferenceTestFixtures.COMMENT)
        .setLatitude(StationReferenceTestFixtures.LATITUDE)
        .setLongitude(StationReferenceTestFixtures.LONGITUDE)
        .setElevation(StationReferenceTestFixtures.ELEVATION)
        .setActualChangeTime(StationReferenceTestFixtures.ACTUAL_TIME)
        .setSystemChangeTime(StationReferenceTestFixtures.SYSTEM_TIME)
        .setActive(true)
        .setPosition(StationReferenceTestFixtures.POSITION)
        .setAliases(new ArrayList<>())
        .build();
    site.getAliases().add(StationReferenceTestFixtures.SITE_ALIAS);
    assertTrue(site.getAliases().size() == 1);
    assertEquals(site.getAliases().get(0), StationReferenceTestFixtures.SITE_ALIAS);
  }
}
