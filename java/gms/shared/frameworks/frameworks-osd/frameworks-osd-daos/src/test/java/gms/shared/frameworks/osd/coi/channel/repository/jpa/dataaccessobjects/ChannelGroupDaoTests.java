package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelGroup;
import gms.shared.frameworks.osd.coi.channel.ChannelGroup.Type;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.stationreference.RelativePosition;
import gms.shared.frameworks.osd.coi.stationreference.StationType;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import static gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.TestFixtures.channel;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelGroupDaoTests {

  @Test
  void testEquality() {
    TestUtilities.checkClassEqualsAndHashcode(ChannelGroupDao.class);
  }

  @Test
  void testConstructionFactoryDoesNotThrowError() throws IOException {
    final Location location = Location.from(50.0, 50.0, 50.0, 100.0);
    final String channelGroupDescription = "This is a test group";
    final String channelGroupName = "Test Group";

    final List<Channel> channels = List.of(channel);
    final ChannelGroup expected = ChannelGroup
        .from(channelGroupName, channelGroupDescription, location, Type.SITE_GROUP, channels);
    final Station station = Station.from(
        "Test Station",
        StationType.SEISMIC_ARRAY,
        "This is a test station",
        Map.of(
            channel.getName(), RelativePosition.from(100.0, 10.0, 10.0)
        ),
        Location.from(135.5, 47.5, 100.0, 55.0),
        List.of(expected),
        List.of(channel)
    );
    assertDoesNotThrow(() -> ChannelGroupDao.from(expected, station));
  }

  @Test
  void testConstructionWithEmptyLocation() throws IOException {
    final String channelGroupDescription = "This is a test group";
    final String channelGroupName = "Test Group";
    final List<ChannelDao> channelDaos = List.of(ChannelDao.from(channel));
    final List<Channel> channels = List.of(channel);
    final ChannelGroup expected = ChannelGroup
        .from(channelGroupName, channelGroupDescription, null, Type.SITE_GROUP, channels);
    final Station station = Station.from(
        "Test Station",
        StationType.SEISMIC_ARRAY,
        "This is a test station",
        Map.of(
            channel.getName(), RelativePosition.from(100.0, 10.0, 10.0)
        ),
        Location.from(135.5, 47.5, 100.0, 55.0),
        List.of(expected),
        List.of(channel)
    );
    final ChannelGroupDao channelGroupDao = ChannelGroupDao.from(expected, station);
    assertAll(
        () -> assertEquals(channelGroupName, channelGroupDao.getName()),
        () -> assertEquals(channelGroupDescription, channelGroupDao.getDescription()),
        () -> assertTrue(!channelGroupDao.getLocation().isPresent()),
        () -> assertEquals(Type.SITE_GROUP, channelGroupDao.getType()),
        () -> assertEquals(channelDaos, channelGroupDao.getChannels())
    );
  }

  @Test
  void testGettersAndSetters() {
    final String channelGroupDescription = "This is a test group";
    final String channelGroupName = "Test Group";
    final ChannelDao channelDao = ChannelDao.from(channel);
    final LocationDao locationDao = new LocationDao(Location.from(50.0, 50.0, 50.0, 100.0));
    final List<ChannelDao> channels = List.of(channelDao);
    final ChannelGroupDao channelGroupDao = new ChannelGroupDao();
    channelGroupDao.setName(channelGroupName);
    channelGroupDao.setDescription(channelGroupDescription);
    channelGroupDao.setChannels(channels);
    channelGroupDao.setLocation(locationDao);
    channelGroupDao.setType(Type.PROCESSING_GROUP);

    assertAll(
        () -> assertEquals(channelGroupName, channelGroupDao.getName()),
        () -> assertEquals(channelGroupDescription, channelGroupDao.getDescription()),
        () -> assertEquals(locationDao, channelGroupDao.getLocation().get()),
        () -> assertEquals(Type.PROCESSING_GROUP, channelGroupDao.getType()),
        () -> assertEquals(channels, channelGroupDao.getChannels())
    );
  }
}
