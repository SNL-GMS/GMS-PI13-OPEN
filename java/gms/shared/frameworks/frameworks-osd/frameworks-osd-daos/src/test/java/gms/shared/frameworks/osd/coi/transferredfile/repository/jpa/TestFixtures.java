package gms.shared.frameworks.osd.coi.transferredfile.repository.jpa;

import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFile;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileInvoiceMetadata;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileMetadataType;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileRawStationDataFrameMetadata;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileStatus;
import java.time.Instant;
import java.util.List;

public class TestFixtures {

  private TestFixtures() {

  }

  private static final Instant NOW = Instant.now();
  private static final Instant TRANSFER_TIME = NOW;
  private static final Instant RECEPTION_TIME = NOW.plusSeconds(5);  // now + 5 secs
  // now plus 5, minus 10 = now minus 5 secs
  private static final Instant PAYLOAD_START_TIME = RECEPTION_TIME.minusSeconds(10); // now - 5 secs
  // now minus 5, plus 5 = now
  private static final Instant PAYLOAD_END_TIME = PAYLOAD_START_TIME.plusSeconds(5);  // now

  private static final String STATION_NAME = "PDAR";
  private static final String CHANNEL_NAME = "PDAR.PD01.BHZ";

  //---------------------------------
  // TransferredFileInvoiceMetadata
  static final TransferredFileInvoiceMetadata TRANSFERRED_FILE_INVOICE_METADATA =
      TransferredFileInvoiceMetadata
          .from(12345L);


  static final TransferredFileInvoiceMetadata TRANSFERRED_FILE_INVOICE_METADATA_3 =
      TransferredFileInvoiceMetadata
          .from(11111L);

  //---------------------------------
  // TransferredFileRawStationDataFrameMetadata
  static final TransferredFileRawStationDataFrameMetadata TRANSFERRED_FILE_RAW_STATION_DATA_FRAME_METADATA =
      TransferredFileRawStationDataFrameMetadata
          .from(PAYLOAD_START_TIME, PAYLOAD_END_TIME, STATION_NAME, List.of(CHANNEL_NAME));


  //---------------------------------
  // TransferredFile <InvoiceMetadata>
  static final TransferredFile<TransferredFileInvoiceMetadata> TRANSFERRED_INVOICE = TransferredFile
      .from("Invoice", "PriorityInvoice", TRANSFER_TIME, RECEPTION_TIME,
          TransferredFileStatus.SENT_AND_RECEIVED,
          TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE, TRANSFERRED_FILE_INVOICE_METADATA);

  //---------------------------------
  // TransferredFile <RawStationDataFrameMetadata>
  static final TransferredFile<TransferredFileRawStationDataFrameMetadata> TRANSFERRED_RAW_STATION_DATA_FRAME
      = TransferredFile.from("RSDF", "PriorityRSDF", TRANSFER_TIME, RECEPTION_TIME,
      TransferredFileStatus.SENT_AND_RECEIVED,
      TransferredFileMetadataType.RAW_STATION_DATA_FRAME,
      TRANSFERRED_FILE_RAW_STATION_DATA_FRAME_METADATA);

}
