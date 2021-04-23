package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelBandType;
import gms.shared.frameworks.osd.coi.channel.ChannelDataType;
import gms.shared.frameworks.osd.coi.channel.ChannelGroup;
import gms.shared.frameworks.osd.coi.channel.ChannelGroup.Type;
import gms.shared.frameworks.osd.coi.channel.ChannelInstrumentType;
import gms.shared.frameworks.osd.coi.channel.ChannelOrientationType;
import gms.shared.frameworks.osd.coi.channel.ChannelProcessingMetadataType;
import gms.shared.frameworks.osd.coi.channel.Orientation;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.stationreference.RelativePosition;
import gms.shared.frameworks.osd.coi.stationreference.StationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class StationGroupDaoTests {

  @Test
  void testEqualityAndHashCode() {
    TestUtilities.checkClassEqualsAndHashcode(StationGroupDao.class);
  }

  @Test
  void testConstructor() {
    StationDao stationDao = Mockito.mock(StationDao.class);
    List<StationDao> stations = List.of(stationDao);
    final String testStationGroupName = "Test Station Group";
    final String testDescription = "Test Description";
    StationGroupDao dao = StationGroupDao.from(testStationGroupName,
        testDescription, stations);

    assertAll(
        () -> assertEquals(testStationGroupName, dao.getName()),
        () -> assertEquals(testDescription, dao.getDescription()),
        () -> assertEquals(stations, dao.getStations())
    );
  }

  @Test
  void testGettersAndSetters() {
    StationDao stationDao = Mockito.mock(StationDao.class);
    List<StationDao> stations = List.of(stationDao);
    final String testStationGroupName = "Test Station Group";
    final String testDescription = "Test Description";
    StationGroupDao dao = new StationGroupDao();
    dao.setName(testStationGroupName);
    dao.setDescription(testDescription);
    dao.setStations(stations);

    assertAll(
        () -> assertEquals(testStationGroupName, dao.getName()),
        () -> assertEquals(testDescription, dao.getDescription()),
        () -> assertEquals(stations, dao.getStations())
    );
  }

  @Test
  void testFactoryMethodThrowsException() {
    assertThrows(NullPointerException.class, () -> StationGroupDao.from(null));
  }

  @Test
  void testFactoryMethodPassingNullNameThrowsException() {
    StationDao stationDao = Mockito.mock(StationDao.class);
    assertThrows(NullPointerException.class,
        () -> StationGroupDao.from(null, "Test Description", List.of(stationDao)));
  }

  @Test
  void testFactoryMethodPassingNullDescriptionThrowsException() {
    StationDao stationDao = Mockito.mock(StationDao.class);
    assertThrows(NullPointerException.class,
        () -> StationGroupDao.from("testStation", null, List.of(stationDao)));
  }

  @Test
  void testFactoryMethodPassingNullListOfStationsThrowsException() {
    assertThrows(NullPointerException.class,
        () -> StationGroupDao.from("testStation", "Test Description", null));
  }

  @Test
  void testFactoryMethodPassingNullStationGroupThrowsException() {
    assertThrows(NullPointerException.class, () -> StationGroupDao.from(null));
  }

  @Test
  void testFactoryMethodPassingNonNullStationGroupDoesNotThrowException() {
    assertDoesNotThrow(
        () -> StationGroupDao.from(UtilsTestFixtures.STATION_GROUP));
  }

  @Test
  void testFactoryMethodNonNullStationGroup() {
    final Channel channel1 = Channel.from(
        "testChannelOne",
        "Test Channel One",
        "This is a description of the channel",
        "stationOne",
        ChannelDataType.DIAGNOSTIC_SOH,
        ChannelBandType.BROADBAND,
        ChannelInstrumentType.HIGH_GAIN_SEISMOMETER,
        ChannelOrientationType.EAST_WEST,
        'E',
        Units.HERTZ,
        50.0,
        Location.from(100.0, 10.0, 50.0, 100),
        Orientation.from(10.0, 35.0),
        List.of(),
        Map.of(),
        Map.of(ChannelProcessingMetadataType.CHANNEL_GROUP, "channelGroupOne")
    );
    final ChannelGroup channelGroupOne = ChannelGroup.from(
        "channelGroupOne",
        "Sample channel group containing all test suite channels",
        Location.from(100.0, 10.0, 50.0, 100.0),
        Type.SITE_GROUP,
        List.of(channel1));
    final Station station = Station.from(
        "stationOne",
        StationType.SEISMIC_ARRAY,
        "Test station",
        Map.of(
            "testChannelOne", RelativePosition.from(50.0, 55.0, 60.0)),
        Location.from(135.75, 65.75, 50.0, 0.0),
        List.of(channelGroupOne),
        List.of(channel1));
    List<Station> stations = List.of(station);
    final String testStationGroupName = "Test Station Group";
    final String testDescription = "Test Description";
    StationGroup psg = StationGroup
        .from(testStationGroupName, testDescription, stations);
    StationGroupDao dao = StationGroupDao.from(psg);
    assertAll(
        () -> assertEquals(psg.getName(), dao.getName()),
        () -> assertEquals(psg.getDescription(), dao.getDescription())
    );

    for (StationDao stationDao : dao.getStations()) {
      assertEquals(StationDao.from(station), stationDao);
    }
  }

}
