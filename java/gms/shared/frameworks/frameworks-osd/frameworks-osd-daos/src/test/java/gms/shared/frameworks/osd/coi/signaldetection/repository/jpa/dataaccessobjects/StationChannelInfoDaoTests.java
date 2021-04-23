package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.stationreference.RelativePosition;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StationChannelInfoDaoTests {

  @Test
  void testEqualAndHashcode() {
    TestUtilities.checkClassEqualsAndHashcode(StationChannelInfoDao.class);
  }

  @Test
  void testConstruction() {
    StationChannelInfoDao dao = new StationChannelInfoDao();
    StationDao stationDao = Mockito.mock(StationDao.class);
    ChannelDao channelDao = Mockito.mock(ChannelDao.class);
    dao.setId(new StationChannelInfoKey(stationDao, channelDao));
    final RelativePosition position = RelativePosition.from(0, 0, 0);
    dao.setNorthDisplacementKm(position.getNorthDisplacementKm());
    dao.setEastDisplacementKm(position.getEastDisplacementKm());
    dao.setVerticalDisplacementKm(position.getVerticalDisplacementKm());
    assertAll(
        () -> assertEquals(dao.getNorthDisplacementKm(), position.getNorthDisplacementKm()),
        () -> assertEquals(dao.getEastDisplacementKm(), position.getEastDisplacementKm()),
        () -> assertEquals(dao.getVerticalDisplacementKm(), position.getVerticalDisplacementKm()),
        () -> assertEquals(stationDao, dao.getId().getStation()),
        () -> assertEquals(channelDao, dao.getId().getChannel())
    );
  }

  @Test
  void testSetters() {
    StationChannelInfoDao dao = new StationChannelInfoDao();
    RelativePosition position = RelativePosition.from(0, 6, 12);
    dao.setNorthDisplacementKm(position.getNorthDisplacementKm());
    dao.setEastDisplacementKm(position.getEastDisplacementKm());
    dao.setVerticalDisplacementKm(position.getVerticalDisplacementKm());
    assertAll(
        () -> assertEquals(dao.getNorthDisplacementKm(), position.getNorthDisplacementKm()),
        () -> assertEquals(dao.getEastDisplacementKm(), position.getEastDisplacementKm()),
        () -> assertEquals(dao.getVerticalDisplacementKm(), position.getVerticalDisplacementKm())
    );
  }

  @Test
  void testToCoi() {
    RelativePosition position = RelativePosition.from(0, 6, 12);
    StationChannelInfoDao dao = new StationChannelInfoDao();
    dao.setNorthDisplacementKm(position.getNorthDisplacementKm());
    dao.setEastDisplacementKm(position.getEastDisplacementKm());
    dao.setVerticalDisplacementKm(position.getVerticalDisplacementKm());
    assertEquals(position, dao.grabRelativePosition());
  }
}
