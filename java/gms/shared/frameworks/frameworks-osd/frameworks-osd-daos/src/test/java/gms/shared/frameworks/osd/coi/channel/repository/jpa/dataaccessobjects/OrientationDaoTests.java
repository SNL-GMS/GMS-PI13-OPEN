package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.channel.Orientation;
import org.junit.jupiter.api.Test;

class OrientationDaoTests {

  @Test
  void testEquality() {
    TestUtilities.checkClassEqualsAndHashcode(OrientationDao.class);
  }

  @Test
  void testConstruction() {
    Orientation expected = Orientation.from(100.0, 50.0);
    OrientationDao orientationDao = new OrientationDao(expected);
    assertEquals(expected, orientationDao.toCoi());
  }
}
