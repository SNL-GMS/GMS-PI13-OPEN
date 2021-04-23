package gms.shared.frameworks.osd.coi.waveforms.repository.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class FkSpectrumDaoTests {

  //TODO: Implement testEquality() method following osd-repository/.../signaldetection.repository.jpa/dataaccessobjects/*Test.java as examples

  @Test
  public void testEquality() {
    FkSpectrumDao fk1 = createFkSpectrumDao();
    FkSpectrumDao fk2 = createFkSpectrumDao();
    FkSpectraDao spectraDao = Mockito.mock(FkSpectraDao.class);
    final FkAttributesDao fkAttributesDao = FkAttributesDaoTests.createFkAttributesDao();
    fkAttributesDao.setFkSpectrum(Mockito.mock(FkSpectrumDao.class));
    List<FkAttributesDao> fkAttributesList = List.of(fkAttributesDao);
    
    fk1.setAttributes(fkAttributesList);
    fk2.setAttributes(fkAttributesList);

    assertEquals(fk1, fk2);
    assertEquals(fk1.hashCode(), fk2.hashCode());

    // not equal daoId's
    long daoId = fk1.getId();
    fk1.setId(daoId + 1);
    assertNotEquals(fk1, fk2);
    fk1.setId(daoId);
    assertEquals(fk1, fk2);
  }

  public static FkSpectrumDao createFkSpectrumDao() {
    int fkQual = 4;
    double[][] values = TestFixtures.FK_SPECTRUM_POWER;
    FkSpectrumDao fk = new FkSpectrumDao();
    fk.setQuality(fkQual);
    fk.setPower(values);

    return fk;
  }
}
