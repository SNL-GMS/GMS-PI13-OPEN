package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import org.junit.jupiter.api.Test;

class LocationDaoTests {
  @Test
  void testEquality() {
    TestUtilities.checkClassEqualsAndHashcode(LocationDao.class);
  }

  @Test
  void testConstruction() {
    Location expected = Location.from(265.0, 100.0, 60.0, 75.5);
    LocationDao locationDao = new LocationDao(expected);
    assertEquals(expected, locationDao.toCoi());
  }
}
