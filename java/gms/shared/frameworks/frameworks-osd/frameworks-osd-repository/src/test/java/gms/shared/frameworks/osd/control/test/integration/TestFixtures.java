package gms.shared.frameworks.osd.control.test.integration;

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

class TestFixtures {

  public static final Channel channelWithNonExistentStation = Channel.from(
      "testChannelOne",
      "Test Channel One",
      "This is a description of the channel",
      "stationDoesNotExist",
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
  public static final Channel channel1 = Channel.from(
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
  public static final Channel channel2 = Channel.from(
      "testChannelTwo",
      "Test Channel Two",
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
  public static final Channel channel3 = Channel.from(
      "testChannelThree",
      "Test Channel Three",
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
  public static final Channel channel4 = Channel.from(
      "testChannelFour",
      "Test Channel Four",
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
  public static final Channel channel5 = Channel.from(
      "testChannelFive",
      "Test Channel Five",
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
  public static final Channel channel6 = Channel.from(
      "testChannelSix",
      "Test Channel Six",
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
  public static final Channel derivedChannelOne = Channel.from(
      "derivedChannelOne",
      "Derived Channel One",
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
      List.of(channel1.getName(), channel6.getName()),
      Map.of(),
      Map.of(ChannelProcessingMetadataType.CHANNEL_GROUP, "channelGroupOne")
  );

  public static final Channel channel7 = Channel.from(
      "testChannelSeven",
      "Test Channel Seven",
      "This is a description of the channel",
      "stationTwo",
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

  public static final Channel channel8 = Channel.from(
      "testChannelEight",
      "Test Channel Eight",
      "This is a description of the channel",
      "stationTwo",
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

  public static final Channel derivedChannelTwo = Channel.from(
      "derivedChannelTwo",
      "Derived from Test Channel Seven",
      "This is a description of the channel",
      "stationTwo",
      ChannelDataType.DIAGNOSTIC_SOH,
      ChannelBandType.BROADBAND,
      ChannelInstrumentType.HIGH_GAIN_SEISMOMETER,
      ChannelOrientationType.EAST_WEST,
      'E',
      Units.HERTZ,
      50.0,
      Location.from(100.0, 10.0, 50.0, 100),
      Orientation.from(10.0, 35.0),
      List.of(TestFixtures.channel7.getName()),
      Map.of(),
      Map.of(ChannelProcessingMetadataType.CHANNEL_GROUP, "")
  );


  public static final ChannelGroup channelGroupOne = ChannelGroup.from(
      "channelGroupOne",
      "Sample channel group containing all test suite channels",
      Location.from(100.0, 10.0, 50.0, 100.0),
      Type.SITE_GROUP,
      List.of(channel1, channel2, channel3, channel4, channel5, channel6));

  public static final ChannelGroup channelGroupTwo = ChannelGroup.from(
      "channelGroupTwo",
      "Sample channel group containing all test suite channels",
      Location.from(100.0, 10.0, 50.0, 100.0),
      Type.SITE_GROUP,
      List.of(channel7));

  public static final Station station = Station.from(
      "stationOne",
      StationType.SEISMIC_ARRAY,
      "Test station",
      Map.of(
          "testChannelOne", RelativePosition.from(50.0, 55.0, 60.0),
          "testChannelTwo", RelativePosition.from(40.0, 35.0, 60.0),
          "testChannelThree", RelativePosition.from(30.0, 15.0, 60.0),
          "testChannelFour", RelativePosition.from(20.0, 40.0, 60.0),
          "testChannelFive", RelativePosition.from(32.5, 16.0, 60.0),
          "testChannelSix", RelativePosition.from(22.5, 27.0, 60.0)),
      Location.from(135.75, 65.75, 50.0, 0.0),
      List.of(channelGroupOne),
      List.of(channel1, channel2, channel3, channel4, channel5, channel6));

  public static final Station stationTwo = Station.from(
      "stationTwo",
      StationType.SEISMIC_ARRAY,
      "Test station",
      Map.of(
          "testChannelSeven", RelativePosition.from(50.0, 55.0, 64.0)
      ),
      Location.from(135.75, 65.75, 50.0, 0.0),
      List.of(channelGroupTwo),
      List.of(channel7));

  public static final StationGroup STATION_GROUP = StationGroup.from(
      "testStationGroup",
      "This is an example of a station group",
      List.of(station)
  );

}
