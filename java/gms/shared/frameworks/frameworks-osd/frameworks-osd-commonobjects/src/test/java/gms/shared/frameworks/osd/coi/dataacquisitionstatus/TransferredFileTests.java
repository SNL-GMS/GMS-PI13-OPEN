package gms.shared.frameworks.osd.coi.dataacquisitionstatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFile;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileInvoiceMetadata;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileMetadataType;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileRawStationDataFrameMetadata;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileStatus;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TransferredFileTests {

  private static final String TEST_FILENAME = "testFile";
  private static final String TEST_PRIORITY = "testPriority";

  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  @Test
  void testSerializationInvoice() throws IOException {
    final JavaType type = objectMapper.getTypeFactory()
            .constructParametricType(TransferredFile.class, TransferredFileInvoiceMetadata.class);
    TestUtilities.testSerialization(DataAcquisitionStatusTestFixtures.transferredInvoice,
            type, objectMapper);
  }

  @Test
  void testSerializationRawStationDataFrameMetadata() throws IOException {
    final JavaType type = objectMapper.getTypeFactory()
            .constructParametricType(TransferredFile.class,
                    TransferredFileRawStationDataFrameMetadata.class);
    TestUtilities
            .testSerialization(DataAcquisitionStatusTestFixtures.transferredRawStationDataFrame,
                    type, objectMapper);
  }

  /**
   * Test "from" with fields that must be present, and not based on transferred file status
   */
  @Test
  void testFromTransferredFileInvoiceMetadataSent() {
    TransferredFile<TransferredFileInvoiceMetadata> tf = TransferredFile
            .from(TEST_FILENAME, TEST_PRIORITY, DataAcquisitionStatusTestFixtures.NOW,
                    DataAcquisitionStatusTestFixtures.receptionTime,
                    TransferredFileStatus.SENT,
                    TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE,
                    DataAcquisitionStatusTestFixtures.transferredFileInvoiceMetadata);

    assertEquals(TEST_FILENAME, tf.getFileName());
    assertEquals(Optional.of(TEST_PRIORITY), tf.getPriority());
    assertEquals(Optional.of(DataAcquisitionStatusTestFixtures.NOW),
            tf.getTransferTime());
    assertEquals(Optional.of(DataAcquisitionStatusTestFixtures.receptionTime),
            tf.getReceptionTime());
    assertEquals(TransferredFileStatus.SENT, tf.getStatus());
    assertEquals(TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE,
            tf.getMetadataType());
    assertEquals(DataAcquisitionStatusTestFixtures.transferredFileInvoiceMetadata,
            tf.getMetadata());
  }

  /**
   * Test "from" with fields that must be present, and not based on transferred file status
   */
  @Test
  void testFromTransferredRawStationDataFrameMetadataReceived() {
    TransferredFile<TransferredFileRawStationDataFrameMetadata> tf = TransferredFile
            .from(TEST_FILENAME, TEST_PRIORITY, DataAcquisitionStatusTestFixtures.NOW,
                    DataAcquisitionStatusTestFixtures.receptionTime,
                    TransferredFileStatus.RECEIVED,
                    TransferredFileMetadataType.RAW_STATION_DATA_FRAME,
                    DataAcquisitionStatusTestFixtures.transferredFileRawStationDataFrameMetadata);

    assertEquals(TEST_FILENAME, tf.getFileName());
    assertEquals(Optional.of(TEST_PRIORITY), tf.getPriority());
    assertEquals(Optional.of(DataAcquisitionStatusTestFixtures.NOW),
            tf.getTransferTime());
    assertEquals(Optional.of(DataAcquisitionStatusTestFixtures.receptionTime),
            tf.getReceptionTime());
    assertEquals(TransferredFileStatus.RECEIVED, tf.getStatus());
    assertEquals(TransferredFileMetadataType.RAW_STATION_DATA_FRAME,
            tf.getMetadataType());
    assertEquals(DataAcquisitionStatusTestFixtures.transferredFileRawStationDataFrameMetadata,
            tf.getMetadata());
  }

  @Test
  void testCreateSent() {
    TransferredFile<TransferredFileInvoiceMetadata> tf = TransferredFile
            .createSent(TEST_FILENAME, TEST_PRIORITY,
                    DataAcquisitionStatusTestFixtures.NOW,
                    DataAcquisitionStatusTestFixtures.transferredFileInvoiceMetadata);

    assertEquals(TEST_FILENAME, tf.getFileName());
    assertEquals(Optional.of(TEST_PRIORITY), tf.getPriority());
    assertEquals(Optional.of(DataAcquisitionStatusTestFixtures.NOW),
            tf.getTransferTime());
    assertEquals(TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE,
            tf.getMetadataType());
    assertEquals(DataAcquisitionStatusTestFixtures.transferredFileInvoiceMetadata,
            tf.getMetadata());
  }

  @Test
  void testCreateReceived() {
    TransferredFile<TransferredFileRawStationDataFrameMetadata> tf = TransferredFile
            .createReceived(TEST_FILENAME,
                    DataAcquisitionStatusTestFixtures.receptionTime,
                    DataAcquisitionStatusTestFixtures.transferredFileRawStationDataFrameMetadata);

    assertEquals(TEST_FILENAME, tf.getFileName());
    assertEquals(Optional.of(DataAcquisitionStatusTestFixtures.receptionTime),
            tf.getReceptionTime());
    assertEquals(TransferredFileMetadataType.RAW_STATION_DATA_FRAME,
            tf.getMetadataType());
    assertEquals(DataAcquisitionStatusTestFixtures.transferredFileRawStationDataFrameMetadata,
            tf.getMetadata());
  }

  @Test
  void testCreateSentAndReceived() {
    TransferredFile<TransferredFileInvoiceMetadata> tf = TransferredFile
            .createSentAndReceived(TEST_FILENAME, TEST_PRIORITY,
                    DataAcquisitionStatusTestFixtures.NOW,
                    DataAcquisitionStatusTestFixtures.receptionTime,
                    DataAcquisitionStatusTestFixtures.transferredFileInvoiceMetadata);

    assertEquals(TEST_FILENAME, tf.getFileName());
    assertEquals(Optional.of(TEST_PRIORITY), tf.getPriority());
    assertEquals(Optional.of(DataAcquisitionStatusTestFixtures.NOW),
            tf.getTransferTime());
    assertEquals(Optional.of(DataAcquisitionStatusTestFixtures.receptionTime),
            tf.getReceptionTime());
    assertEquals(TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE,
            tf.getMetadataType());
    assertEquals(DataAcquisitionStatusTestFixtures.transferredFileInvoiceMetadata,
            tf.getMetadata());
  }

  @Test
  void testBuilder() {
    final TransferredFile<TransferredFileInvoiceMetadata> invoice
        = DataAcquisitionStatusTestFixtures.transferredInvoice;
    assertEquals(TransferredFileStatus.SENT_AND_RECEIVED, invoice.getStatus());
    final TransferredFileStatus newStatus = TransferredFileStatus.SENT;
    final TransferredFile<TransferredFileInvoiceMetadata> modifiedInvoice
        = invoice.toBuilder().setStatus(newStatus).build();
    assertNotEquals(modifiedInvoice.getStatus(), invoice.getStatus());
    assertEquals(newStatus, modifiedInvoice.getStatus());
  }

  /**
   * Test validation of MetadataType (transferred file invoice) matching actual metadata class
   * instance (raw station data frame). Expect an exception to be thrown
   */
  @Test
  void testMetadataTypeMatchesMetadataClassException() {
    assertThrows(IllegalArgumentException.class, () -> TransferredFile.from(TEST_FILENAME, TEST_PRIORITY,
            DataAcquisitionStatusTestFixtures.NOW, DataAcquisitionStatusTestFixtures.NOW,
            TransferredFileStatus.SENT,
            TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE,
            DataAcquisitionStatusTestFixtures.transferredFileRawStationDataFrameMetadata));
  }

}
