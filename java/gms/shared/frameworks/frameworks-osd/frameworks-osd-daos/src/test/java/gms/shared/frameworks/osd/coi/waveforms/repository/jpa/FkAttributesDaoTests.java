package gms.shared.frameworks.osd.coi.waveforms.repository.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class FkAttributesDaoTests {

  @Test
  public void testEquality() {
    FkAttributesDao fk1 = createFkAttributesDao();
    FkAttributesDao fk2 = createFkAttributesDao();
    FkSpectrumDao spectrumDao = Mockito.mock(FkSpectrumDao.class);

    fk1.setFkSpectrum(spectrumDao);
    fk2.setFkSpectrum(spectrumDao);
    assertEquals(fk1, fk2);
    assertEquals(fk1.hashCode(), fk2.hashCode());

    // not equal daoId's
    long daoId = fk1.getPrimaryKey();
    fk1.setPrimaryKey(daoId + 1);
    assertNotEquals(fk1, fk2);
    fk1.setPrimaryKey(daoId);
    assertEquals(fk1, fk2);

    fk1.setPeakFStat(12345.6789);
    assertNotEquals(fk1, fk2);
  }

  public static FkAttributesDao createFkAttributesDao() {

    double azimuth = 1.1;
    double slowness = 2.2;
    double azimuthUncertainty = 3.3;
    double slownessUncertainty = 4.4;
    double peakFStat = 5.5;

    FkAttributesDao fk = new FkAttributesDao();
    fk.setAzimuth(azimuth);
    fk.setSlowness(slowness);
    fk.setAzimuthUncertainty(azimuthUncertainty);
    fk.setSlownessUncertainty(slownessUncertainty);
    fk.setPeakFStat(peakFStat);

    return fk;
  }
}
