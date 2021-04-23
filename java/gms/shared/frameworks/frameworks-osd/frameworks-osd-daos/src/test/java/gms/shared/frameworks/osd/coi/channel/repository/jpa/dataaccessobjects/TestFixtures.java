package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects;

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

/**
 * Contains commonly used test data classes
 */
public final class TestFixtures {

  public static final Channel channel = Channel.from(
      "Test Channel",
      "Test Canonical Name",
      "This is a test channel",
      "ASAR",
      ChannelDataType.DIAGNOSTIC_SOH,
      ChannelBandType.BROADBAND,
      ChannelInstrumentType.HIGH_GAIN_SEISMOMETER,
      ChannelOrientationType.VERTICAL,
      ChannelOrientationType.VERTICAL.getCode(),
      Units.COUNTS_PER_NANOMETER,
      50.0,
      Location.from(265.0, 47.65, 50.0, 100.0),
      Orientation.from(50.0, 95.0),
      List.of("Some parent Channel"),
      Map.of(),
      Map.of(ChannelProcessingMetadataType.CHANNEL_GROUP, "Test Channel Group"));
}
