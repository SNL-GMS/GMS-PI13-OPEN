package gms.shared.frameworks.osd.control.test.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
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
import gms.shared.frameworks.osd.control.channel.ChannelRepositoryJpa;
import gms.shared.frameworks.osd.control.station.StationGroupRepositoryJpa;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.station.StationGroupRepositoryInterface;
import gms.shared.frameworks.osd.api.station.StationRepositoryInterface;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class StationAndChannelIntegrationTest {

  private static final EntityManagerFactory emf = CoiTestingEntityManagerFactory.createTesting();
  private static final EntityManager em = emf.createEntityManager();
  private static final ChannelRepositoryInterface channelRepository =
      new ChannelRepositoryJpa(emf);
  private static final StationRepositoryInterface stationRepository = new StationRepositoryJpa(emf);
  private static final StationGroupRepositoryInterface stationGroupRepository =
      new StationGroupRepositoryJpa(emf);
  private static final List<Channel> CHANNELS = List.of(
      TestFixtures.channel1,
      TestFixtures.channel2,
      TestFixtures.channel3,
      TestFixtures.channel4,
      TestFixtures.channel5,
      TestFixtures.channel6);
  private static final List<Station> STATIONS = List.of(TestFixtures.station);

  @BeforeAll
  static void setUpData() {
    stationRepository.storeStations(STATIONS);
    stationGroupRepository.storeStationGroups(List.of(TestFixtures.STATION_GROUP));
  }

  @Test
  void testRetrieveChannels() {
    List<String> channelIds = CHANNELS.stream().map(Channel::getName).collect(Collectors.toList());
    List<Channel> storedChannels = channelRepository.retrieveChannels(channelIds);
    for (Channel channel : CHANNELS) {
      assertTrue(storedChannels.contains(channel));
    }
  }

  @Test
  void testStoreStation() {
    stationRepository.storeStations(List.of(TestFixtures.stationTwo));
  }

  @Test
  void testStoreChannels() {
    channelRepository.storeChannels(List.of(TestFixtures.derivedChannelTwo));
    List<Channel> storedChannels = channelRepository.retrieveChannels(List.of("derivedChannelTwo"));
    assertAll(
        () -> assertTrue(!storedChannels.isEmpty()),
        () -> assertEquals(TestFixtures.derivedChannelTwo, storedChannels.get(0))
    );
  }

  @Test
  void testStoringChannelOfNonexistentStationWillThrowException() {
    final Channel invalidChannel = Channel.from(
        "testChannelEight",
        "Test Channel Eight",
        "This is a description of the channel",
        "stationThree",
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
        Map.of(ChannelProcessingMetadataType.CHANNEL_GROUP, "")
    );
    assertThrows(Exception.class, () -> channelRepository.storeChannels(List.of(invalidChannel)));
  }

  @Test
  void testEmptyListPassedToRetrieveChannelsWillReturnAllChannels() {
    List<Channel> storedChannels = channelRepository.retrieveChannels(List.of());
    for (Channel channel : CHANNELS) {
      assertTrue(storedChannels.contains(channel));
    }
  }

  @Test
  void testStoringChannelWithStationThatDoesNotExistThrowsException() {
    final List<Channel> channels = List.of(
        TestFixtures.channelWithNonExistentStation
    );
    assertThrows(Exception.class, () -> channelRepository.storeChannels(channels));
  }

  @Test
  void testStoringChannelThatExistsAlreadyThrowsException() {
    final List<Channel> channels = List.of(
        TestFixtures.channel1
    );
    assertThrows(Exception.class, () -> channelRepository.storeChannels(channels));
  }

  @Test
  void testRetrieveStations() {
    List<Station> storedStations = stationRepository
        .retrieveAllStations(List.of(TestFixtures.station.getName()));
    Station stored = storedStations.get(0);
    assertAll(
        () -> assertEquals(2, storedStations.size()),
        () -> assertEquals(TestFixtures.station.getName(), stored.getName()),
        () -> assertEquals(TestFixtures.station.getDescription(), stored.getDescription()),
        () -> assertEquals(TestFixtures.station.getLocation(), stored.getLocation()),
        () -> assertEquals(TestFixtures.station.getType(), stored.getType()),
        () -> assertEquals(TestFixtures.station.getChannelGroups(), stored.getChannelGroups()),
        () -> assertEquals(TestFixtures.station.getChannels(), stored.getChannels()),
        () -> assertEquals(TestFixtures.station.getRelativePositionsByChannel(),
            stored.getRelativePositionsByChannel())
    );
  }

  @Test
  void testNoStationsPassedWillRetrieveAllStations() {
    List<Station> storedStations = stationRepository.retrieveAllStations(List.of());
    Station stored = storedStations.get(0);
    assertAll(
        () -> assertEquals(TestFixtures.station.getName(), stored.getName()),
        () -> assertEquals(TestFixtures.station.getDescription(), stored.getDescription()),
        () -> assertEquals(TestFixtures.station.getLocation(), stored.getLocation()),
        () -> assertEquals(TestFixtures.station.getType(), stored.getType()),
        () -> assertEquals(TestFixtures.station.getChannelGroups(), stored.getChannelGroups()),
        () -> assertEquals(TestFixtures.station.getChannels(), stored.getChannels()),
        () -> assertEquals(TestFixtures.station.getRelativePositionsByChannel(),
            stored.getRelativePositionsByChannel())
    );
  }

  @Test
  void testNullStationPassedWillThrowException() {
    assertThrows(NullPointerException.class, () -> stationRepository.storeStations(null));
  }

  @Test
  void testNullChannelIdListPassedWillThrowException() {
    assertThrows(NullPointerException.class, () -> channelRepository.storeChannels(null));
  }

  @Test
  void testNullStationIdCollectionToRetrieveAllStationsWillThrowException() {
    assertThrows(NullPointerException.class, () -> stationRepository.storeStations(null));
  }

  @Test
  void testPassingNullToRetrieveStationGroupsThrowsException() {
    assertThrows(NullPointerException.class, () -> stationGroupRepository.retrieveStationGroups(null));
  }

  @Test
  void testPassingEmptyCollectionToRetrieveStationGroupThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> stationGroupRepository.retrieveStationGroups(List.of()));
  }

  @Test
  void testPassingNullToStoreStationGroupsThrowsException() {
    assertThrows(NullPointerException.class, () -> stationGroupRepository.storeStationGroups(null));
  }

  @Test
  void testPassingEmptyCollectionToStoreStationGroupsThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> stationGroupRepository.storeStationGroups(List.of()));
  }

  @Test
  void testRetrieveStationGroups() {
    List<StationGroup> storedPsgs = stationGroupRepository.retrieveStationGroups(List.of("testStationGroup"));
    assertAll(
        () -> assertEquals(TestFixtures.STATION_GROUP.getName(), storedPsgs.get(0).getName()),
        () -> assertEquals(TestFixtures.STATION_GROUP.getDescription(), storedPsgs.get(0).getDescription())
    );
    for(Station station : TestFixtures.STATION_GROUP.getStations()) {
      assertTrue(storedPsgs.get(0).getStations().contains(station));
    }
  }

  @Test
  void testStoreStationGroupWithStationNotCurrentlyInDatabase() {
    final Station station = Station.from(
        "testStationTwo",
        StationType.SEISMIC_3_COMPONENT,
        "Sample 3-component station",
        Map.of(
            "testChannelEight", RelativePosition.from(25, 35, 35)
        ),
        Location.from(65.75, 135.50, 100.0, 50),
        List.of(ChannelGroup.from(
            "testChannelGroupThree",
            "Another Channel Group",
            Location.from(136.76, 65.75, 105.0, 55.0),
            Type.SITE_GROUP,
            List.of(TestFixtures.channel8)
        )),
        List.of(TestFixtures.channel8)
    );
    final StationGroup stationGroup = StationGroup.from(
        "AnotherPSG",
        "This is a PSG with a station that has not yet been stored",
        List.of(station)
    );
    stationGroupRepository.storeStationGroups(List.of(stationGroup));
  }

  @Test
  void testStoreStationWithChannelGroupThatHasNullLocation() {
    final Channel channel = Channel.from(
        "newChannel",
        "Test Channel One",
        "This is a description of the channel",
        TestFixtures.station.getName(),
        ChannelDataType.DIAGNOSTIC_SOH,
        ChannelBandType.BROADBAND,
        ChannelInstrumentType.HIGH_GAIN_SEISMOMETER,
        ChannelOrientationType.EAST_WEST,
        'E',
        Units.HERTZ,
        50.0,
        Location.from(100.0, 150.0, 30, 20),
        Orientation.from(10.0, 35.0),
        List.of(),
        Map.of(),
        Map.of(ChannelProcessingMetadataType.CHANNEL_GROUP, "channelGroupOne")
    );

    final ChannelGroup newChannelGroup = ChannelGroup.from(
        "channelGroupWithNullLocation",
        "Sample channel group containing all test suite channels",
        null,
        Type.SITE_GROUP,
        List.of(channel));

    final Station station = Station.from(
        "stationWithChannelWithUnknownLocation",
        StationType.SEISMIC_ARRAY,
        "Station that does has a channel with unknown location",
        Map.of(
            "newChannel", RelativePosition.from(30, 55, 120)
        ),
        Location.from(135.75, 65.75, 50.0, 0.0),
        List.of(newChannelGroup),
        List.of(channel));
    stationRepository.storeStations(List.of(station));
  }

  @Test
  void testStoringStationGroupsWithTheSameStations() {
    final Channel channel = Channel.from(
        "newChannel",
        "Test Channel One",
        "This is a description of the channel",
        TestFixtures.station.getName(),
        ChannelDataType.DIAGNOSTIC_SOH,
        ChannelBandType.BROADBAND,
        ChannelInstrumentType.HIGH_GAIN_SEISMOMETER,
        ChannelOrientationType.EAST_WEST,
        'E',
        Units.HERTZ,
        50.0,
        Location.from(100.0, 150.0, 30, 20),
        Orientation.from(10.0, 35.0),
        List.of(),
        Map.of(),
        Map.of(ChannelProcessingMetadataType.CHANNEL_GROUP, "channelGroupOne")
    );

    final ChannelGroup newChannelGroup = ChannelGroup.from(
        "channelGroupWithNullLocation",
        "Sample channel group containing all test suite channels",
        null,
        Type.SITE_GROUP,
        List.of(channel));

    final Station station = Station.from(
        "stationWithChannelWithUnknownLocation",
        StationType.SEISMIC_ARRAY,
        "Station that does has a channel with unknown location",
        Map.of(
            "newChannel", RelativePosition.from(30, 55, 120)
        ),
        Location.from(135.75, 65.75, 50.0, 0.0),
        List.of(newChannelGroup),
        List.of(channel));

    final StationGroup stationGroup = StationGroup.from(
        "Yet Another PSG",
        "This is a PSG with a station that has not yet been stored",
        List.of(station)
    );

    final StationGroup stationGroupTwo = StationGroup.from(
        "Different PSG",
        "This is a PSG with a station that has been stored",
        List.of(station)
    );
    stationGroupRepository.storeStationGroups(List.of(stationGroup, stationGroupTwo));
  }
}
