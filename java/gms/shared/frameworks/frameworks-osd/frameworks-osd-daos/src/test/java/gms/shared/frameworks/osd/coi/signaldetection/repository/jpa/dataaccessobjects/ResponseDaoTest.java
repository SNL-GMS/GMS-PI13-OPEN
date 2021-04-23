package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.Response;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResponseDaoTest {

  @Test
  void testFromToCoi() {
    final Response response = TestFixtures.response;
    final ChannelDao channelDao = mock(ChannelDao.class);
    when(channelDao.getName()).thenReturn(response.getChannelName());
    ResponseDao dao = new ResponseDao(response, channelDao);
    assertNotNull(dao);

    assertEquals(response.getCalibration(), dao.getCalibration().toCoi());
    assertEquals(response.getFapResponse(), dao.getFrequencyAmplitudePhase().map(
        FrequencyAmplitudePhaseDao::toCoi));
    assertEquals(response, dao.toCoi());
  }
}
