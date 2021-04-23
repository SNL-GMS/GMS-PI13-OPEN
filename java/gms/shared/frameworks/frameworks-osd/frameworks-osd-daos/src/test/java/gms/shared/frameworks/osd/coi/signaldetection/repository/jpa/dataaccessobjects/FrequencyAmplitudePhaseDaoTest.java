package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import gms.shared.frameworks.osd.coi.signaldetection.FrequencyAmplitudePhase;
import org.junit.jupiter.api.Test;

class FrequencyAmplitudePhaseDaoTest {

  @Test
  void testFromCoi() {
    FrequencyAmplitudePhaseDao dao = FrequencyAmplitudePhaseDao.from(TestFixtures.fapResponse);
    assertNotNull(dao);
    assertEquals(TestFixtures.fapResponse, dao.toCoi());
  }

  @Test
  void testToCoi() {
    assertEquals(TestFixtures.fapResponse, TestFixtures.fapResponseDao.toCoi());
  }
}
