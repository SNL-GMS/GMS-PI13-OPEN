import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.osd.coi.signaldetection.QcMask;
import gms.shared.frameworks.osd.coi.signaldetection.QcMaskCategory;
import gms.shared.frameworks.osd.coi.signaldetection.QcMaskType;
import gms.shared.frameworks.osd.coi.signaldetection.QcMaskVersion;
import gms.shared.utilities.standardtestdataset.qcmaskconverter.QcMaskConverter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QcMaskConverterTest {

  private static QcMaskConverter qcMaskConverter;

  //Values fo generating UUIDs
  private final Instant startTime = Instant.parse("2010-05-20T22:30:00Z");
  private final Instant endTime = Instant.parse("2010-05-20T22:45:00Z");

  private static final String KDAK_BHE_ID = "KDAK.KDAK.BHE";
  private static final Instant SOME_MASK_START_TIME = Instant.parse("2010-05-20T22:30:00Z");

  /**
   * Reads the Qc Mask file, picks a specific one and asserts it has the values we expect.
   */
  @Test
  void testConversion() throws IOException {
    String resourcesDir = "src/test/resources/input-json-files/";
    String masksFile = resourcesDir + "KDAK.KDAK.BHE.json";
    final List<QcMask> qcMasks = QcMaskConverter.convertJsonToCoi(masksFile);

    assertEquals(4, qcMasks.size());
    final Optional<QcMask> qcMaskOptional = qcMasks.stream()
        .filter(x -> x.getCurrentQcMaskVersion().getStartTime().get().equals(SOME_MASK_START_TIME))
        .findFirst();
    assertTrue(qcMaskOptional.isPresent());
    final QcMask qcMask = qcMaskOptional.get();
    final UUID expectedQcId = UUID.nameUUIDFromBytes(Double.toString(1500).getBytes());

    assertEquals(KDAK_BHE_ID, qcMask.getChannelName());
    assertEquals(1, qcMask.getQcMaskVersions().size());
    final QcMaskVersion qcMaskVersion = qcMask.getQcMaskVersions().get(0);
    assertEquals(0, qcMaskVersion.getVersion());
    assertEquals(1, qcMaskVersion.getChannelSegmentIds().size());
    assertEquals(expectedQcId,
        qcMaskVersion.getChannelSegmentIds().get(0));
    assertEquals(QcMaskCategory.WAVEFORM_QUALITY, qcMaskVersion.getCategory());
    assertTrue(qcMaskVersion.getType().isPresent());
    assertEquals(QcMaskType.LONG_GAP, qcMaskVersion.getType().get());
    assertEquals("Rationale", qcMaskVersion.getRationale());
    assertTrue(qcMaskVersion.getStartTime().isPresent());
    assertTrue(qcMaskVersion.getEndTime().isPresent());
    assertEquals(startTime, qcMaskVersion.getStartTime().get());
    assertEquals(endTime, qcMaskVersion.getEndTime().get());
  }
}
