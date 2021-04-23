package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import gms.shared.frameworks.osd.coi.signaldetection.Calibration;
import org.junit.jupiter.api.Test;

public class CalibrationDaoTest {

  @Test
  public void testFrom() {

    CalibrationDao dao = CalibrationDao.from(TestFixtures.calibration);
    assertNotNull(dao);

    assertEquals(TestFixtures.calibrationDao.getCalibrationPeriodSec(),
        dao.getCalibrationPeriodSec());
    assertEquals(TestFixtures.calibrationDao.getCalibrationTimeShift(),
        dao.getCalibrationTimeShift());
    assertEquals(TestFixtures.calibrationDao.getCalibrationFactor(),
        dao.getCalibrationFactor());
  }

  @Test
  public void testToCoi() {
    Calibration cal = TestFixtures.calibrationDao.toCoi();
    assertEquals(TestFixtures.calibration, cal);
  }

}
