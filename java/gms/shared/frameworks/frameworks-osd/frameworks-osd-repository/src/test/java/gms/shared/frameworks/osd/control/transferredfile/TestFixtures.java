package gms.shared.frameworks.osd.control.transferredfile;

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

  private static final Instant now = Instant.now();
  private static final Instant transferTime = now;
  private static final Instant receptionTime = now.plusSeconds(5);  // now + 5 secs
  // now plus 5, minus 10 = now minus 5 secs
  private static final Instant payloadStartTime = receptionTime.minusSeconds(10); // now - 5 secs
  // now minus 5, plus 5 = now
  private static final Instant payloadEndTime = payloadStartTime.plusSeconds(5);  // now

  // 1 hour ago (3600 secs)
  static final Instant transferTime2 = now.minusSeconds(60 * (long) 60);
  // 50 mins ago (3000 secs)
  static final Instant receptionTime2 = now.minusSeconds(60 * (long) 50);
  // 70 mins ago (4200 secs)
  private static final Instant payloadStartTime2 = now.minusSeconds(60 * (long) 70);
  // 1 hour ago (3600 secs)
  private static final Instant payloadEndTime2 = now.minusSeconds(60 * (long) 60);

  private static final String STATION_ID = "PDAR";
  private static final String CHANNEL_ID = "PDAR.PD01.BHZ";

  //---------------------------------
  // TransferredFileInvoiceMetadata
  private static final TransferredFileInvoiceMetadata transferredFileInvoiceMetadata =
      TransferredFileInvoiceMetadata
          .from(12345L);

  private static final TransferredFileInvoiceMetadata transferredFileInvoiceMetadata2 =
      TransferredFileInvoiceMetadata
          .from(6789L);

  //---------------------------------
  // TransferredFileRawStationDataFrameMetadata
  private static final TransferredFileRawStationDataFrameMetadata
      transferredFileRawStationDataFrameMetadata = TransferredFileRawStationDataFrameMetadata
      .from(payloadStartTime, payloadEndTime, STATION_ID, List.of(CHANNEL_ID));

  private static final TransferredFileRawStationDataFrameMetadata
      transferredFileRawStationDataFrameMetadata2 = TransferredFileRawStationDataFrameMetadata
      .from(payloadStartTime2, payloadEndTime2, STATION_ID, List.of(CHANNEL_ID));

  //---------------------------------
  // TransferredFile <InvoiceMetadata>
  static final TransferredFile<TransferredFileInvoiceMetadata>
      transferredInvoice = TransferredFile.from("Invoice", "PriorityInvoice",
      transferTime, receptionTime, TransferredFileStatus.SENT_AND_RECEIVED,
      TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE, transferredFileInvoiceMetadata);

  static final TransferredFile<TransferredFileInvoiceMetadata> transferredInvoice2 = TransferredFile
      .from("Invoice2", "PriorityInvoice2", transferTime2, receptionTime2,
          TransferredFileStatus.SENT,
          TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE, transferredFileInvoiceMetadata2);

  //---------------------------------
  // TransferredFile <RawStationDataFrameMetadata>
  static final TransferredFile<TransferredFileRawStationDataFrameMetadata>
      transferredRawStationDataFrame
      = TransferredFile.from("RSDF", "PriorityRSDF", transferTime, receptionTime,
      TransferredFileStatus.SENT_AND_RECEIVED,
      TransferredFileMetadataType.RAW_STATION_DATA_FRAME,
      transferredFileRawStationDataFrameMetadata);

  static final TransferredFile<TransferredFileRawStationDataFrameMetadata>
      transferredRawStationDataFrame2
      = TransferredFile
      .from("RSDF2", "PriorityRSDF2", transferTime2, receptionTime2,
          TransferredFileStatus.SENT_AND_RECEIVED,
          TransferredFileMetadataType.RAW_STATION_DATA_FRAME,
          transferredFileRawStationDataFrameMetadata2);

}
