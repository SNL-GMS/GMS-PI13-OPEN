package gms.dataacquisition.csswaveformconverter;

import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelBandType;
import gms.shared.frameworks.osd.coi.channel.ChannelDataType;
import gms.shared.frameworks.osd.coi.channel.ChannelInstrumentType;
import gms.shared.frameworks.osd.coi.channel.ChannelOrientationType;
import gms.shared.frameworks.osd.coi.channel.ChannelProcessingMetadataType;
import gms.shared.frameworks.osd.coi.channel.Orientation;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import java.util.List;
import java.util.Map;


class WaveformConverterTestFixtures {
  private WaveformConverterTestFixtures(){}

  static final String STATION_NAME = "DAVOX";
  static final String FREQUENCY = "HHN";
  static final String FREQUENCY2 = "HHE";
  static final String FREQUENCY3 = "HHN3";
  static final String FREQUENCY4 = "HHN4";
  static final String FULL_CHAN_NAME = String.join(".", STATION_NAME, FREQUENCY);
  static final String FULL_CHAN_NAME_HHE = String.join(".", STATION_NAME, FREQUENCY2);
  static final String FULL_CHAN_NAME_HNN2 = String.join(".", STATION_NAME, FREQUENCY3);
  static final String FULL_CHAN_NAME_HNN3 = String.join(".", STATION_NAME, FREQUENCY4);
  static final String SEG_ID = "7a85adb7-1234-3c8a-bcc3-32b692f2e9ae";
  static final String DATA_PATH = "src/test/resources/css/WFS4";

  static final Channel DAVOX_HHN_CHANNEL = Channel.from(
      FULL_CHAN_NAME,
      FULL_CHAN_NAME,
      "This is a test channel",
      STATION_NAME,
      ChannelDataType.HYDROACOUSTIC,
      ChannelBandType.HIGH_BROADBAND,
      ChannelInstrumentType.HIGH_GAIN_SEISMOMETER,
      ChannelOrientationType.NORTH_SOUTH,
      ChannelOrientationType.NORTH_SOUTH.getCode(),
      Units.COUNTS_PER_NANOMETER,
      50.0,
      Location.from(265.0, 47.65, 50.0, 100.0),
      Orientation.from(50.0, 95.0),
      List.of("Some parent Channel"),
      Map.of(),
      Map.of(ChannelProcessingMetadataType.CHANNEL_GROUP, "Test Channel Group"));

  static final Channel DAVOX_HHE_CHANNEL = Channel.from(
      FULL_CHAN_NAME_HHE,
      FULL_CHAN_NAME_HHE,
      "This is a test channel",
      STATION_NAME,
      ChannelDataType.HYDROACOUSTIC,
      ChannelBandType.HIGH_BROADBAND,
      ChannelInstrumentType.HIGH_GAIN_SEISMOMETER,
      ChannelOrientationType.NORTH_SOUTH,
      ChannelOrientationType.NORTH_SOUTH.getCode(),
      Units.COUNTS_PER_NANOMETER,
      50.0,
      Location.from(265.0, 47.65, 50.0, 100.0),
      Orientation.from(50.0, 95.0),
      List.of("Some parent Channel"),
      Map.of(),
      Map.of(ChannelProcessingMetadataType.CHANNEL_GROUP, "Test Channel Group"));

}
