package gms.shared.frameworks.osd.coi.event;


import static org.junit.jupiter.api.Assertions.assertEquals;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import org.junit.jupiter.api.Test;

public class EventLocationTests {

  @Test
  public void testFrom() {
      EventLocation loc = EventLocation.from(EventTestFixtures.LAT, EventTestFixtures.LON,
        EventTestFixtures.DEPTH, EventTestFixtures.TIME);
    assertEquals(EventTestFixtures.LAT, loc.getLatitudeDegrees(), 0.001);
    assertEquals(EventTestFixtures.LON, loc.getLongitudeDegrees(), 0.001);
    assertEquals(EventTestFixtures.DEPTH, loc.getDepthKm(), 0.001);
    assertEquals(EventTestFixtures.TIME, loc.getTime());
  }

  @Test
  public void testSerialization() throws Exception {
    TestUtilities.testSerialization(EventTestFixtures.EVENT_LOCATION, EventLocation.class);
  }

}
