package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.channel.ChannelBandType;
import gms.shared.frameworks.osd.coi.channel.ChannelDataType;
import gms.shared.frameworks.osd.coi.channel.ChannelInstrumentType;
import gms.shared.frameworks.osd.coi.channel.ChannelOrientationType;
import gms.shared.frameworks.osd.coi.channel.Orientation;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelDaoTests {

  // TODO: test ChannelDao.from(Channel)

  @Test
  void testEquality() {
    TestUtilities.checkClassEqualsAndHashcode(ChannelDao.class);
  }

  @Test
  void testGettersAndSetters() throws IOException {
    final ChannelDao channelDao = new ChannelDao();
    final String name = "Test Channel";
    final String canonicalName = "Test Canonical Name";
    final String description = "This is a test channel";
    final String station = "ASAR";
    final ChannelBandType channelBandType = ChannelBandType.EXTREMELY_LONG_PERIOD;
    final ChannelInstrumentType channelInstrumentType = ChannelInstrumentType.HIGH_GAIN_SEISMOMETER;
    final ChannelOrientationType channelOrientationType = ChannelOrientationType.VERTICAL;
    final char channelOrientationCode = channelOrientationType.getCode();
    final LocationDao location = new LocationDao(Location.from(265.0, 47.65, 50.0, 100.0));
    final OrientationDao orientation = new OrientationDao(Orientation.from(50.0, 95.0));
    final ObjectMapper jsonObjectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    final String blankDefinition = jsonObjectMapper
        .readTree(jsonObjectMapper.writeValueAsString(Map.of())).toString();
    final String blankMetadata = jsonObjectMapper
        .readTree(jsonObjectMapper.writeValueAsString(Map.of())).toString();

    channelDao.setName(name);
    channelDao.setCanonicalName(canonicalName);
    channelDao.setDescription(description);
    channelDao.setChannelDataType(ChannelDataType.DIAGNOSTIC_SOH);
    channelDao.setChannelBandType(channelBandType);
    channelDao.setChannelInstrumentType(channelInstrumentType);
    channelDao.setChannelOrientationType(channelOrientationType);
    channelDao.setChannelOrientationCode(channelOrientationCode);
    channelDao.setNominalSampleRateHz(50.0);
    channelDao.setLocation(location);
    channelDao.setOrientationAngles(orientation);
    channelDao.setProcessingDefinition(blankDefinition);
    channelDao.setProcessingMetadata(blankMetadata);

    assertAll(
        () -> assertEquals(name, channelDao.getName()),
        () -> assertEquals(canonicalName, channelDao.getCanonicalName()),
        () -> assertEquals(description, channelDao.getDescription()),
        () -> assertEquals(channelBandType, channelDao.getChannelBandType()),
        () -> assertEquals(channelInstrumentType, channelDao.getChannelInstrumentType()),
        () -> assertEquals(channelOrientationType, channelDao.getChannelOrientationType()),
        () -> assertEquals(channelOrientationCode, channelDao.getChannelOrientationCode()),
        () -> assertEquals(location, channelDao.getLocation()),
        () -> assertEquals(orientation, channelDao.getOrientationAngles()),
        () -> assertEquals(50.0, channelDao.getNominalSampleRateHz()),
        () -> assertEquals(blankDefinition, channelDao.getProcessingDefinition()),
        () -> assertEquals(blankMetadata, channelDao.getProcessingMetadata())
    );
  }
}
