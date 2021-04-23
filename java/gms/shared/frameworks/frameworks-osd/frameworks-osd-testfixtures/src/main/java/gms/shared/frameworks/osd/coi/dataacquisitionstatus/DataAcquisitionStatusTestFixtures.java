package gms.shared.frameworks.osd.coi.dataacquisitionstatus;

import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.StationSohIssue;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFile;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileInvoiceMetadata;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileMetadataType;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileRawStationDataFrameMetadata;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileStatus;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.station.StationTestFixtures;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame.AuthenticationStatus;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFramePayloadFormat;
import gms.shared.frameworks.osd.coi.waveforms.WaveformSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataAcquisitionStatusTestFixtures {

  private DataAcquisitionStatusTestFixtures() {

  }

  public static final Instant NOW = Instant.now();  // used as transferTime
  static final Instant receptionTime = NOW.plusSeconds(5);
  private static final Instant payloadStartTime = receptionTime.minusSeconds(10); // now - 5
  private static final Instant payloadEndTime = payloadStartTime
      .plusSeconds(5);  // now - 5 + 5 = now
  public static final Instant NOW_MINUS_FIVE_MINUTES = NOW.minusSeconds(300);
  public static final Instant NOW_MINUS_TEN_MINUTES = NOW_MINUS_FIVE_MINUTES.minusSeconds(300);
  public static final Instant NOW_MINUS_FIFTEEN_MINUTES = NOW_MINUS_TEN_MINUTES.minusSeconds(300);
  private static final String STATION_ID = "PDAR";
  private static final String CHANNEL_ID = "PDAR.PD01.BHZ";
  private static final String NET02 = "net02";

  static final TransferredFileInvoiceMetadata transferredFileInvoiceMetadata =
      TransferredFileInvoiceMetadata
          .from(12345L);

  static final TransferredFileRawStationDataFrameMetadata transferredFileRawStationDataFrameMetadata = TransferredFileRawStationDataFrameMetadata
      .from(payloadStartTime, payloadEndTime, STATION_ID, List.of(CHANNEL_ID));

  static final TransferredFile<TransferredFileInvoiceMetadata> transferredInvoice = TransferredFile
      .from("testFile", "testPriority", NOW, receptionTime, TransferredFileStatus.SENT_AND_RECEIVED,
          TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE, transferredFileInvoiceMetadata);

  static final TransferredFile<TransferredFileRawStationDataFrameMetadata> transferredRawStationDataFrame
      = TransferredFile.from("testFile", "testPriority", NOW, receptionTime,
      TransferredFileStatus.SENT_AND_RECEIVED, TransferredFileMetadataType.RAW_STATION_DATA_FRAME,
      transferredFileRawStationDataFrameMetadata);

  private static final WaveformSummary waveformSummary = WaveformSummary
      .from(UtilsTestFixtures.CHANNEL.getName(), Instant.EPOCH, Instant.EPOCH.plusSeconds(10));

// StationGroupSohStatus test fixtures

  // latency
  private static final Duration badLatency = Duration.ofSeconds(300);
  private static final Duration marginalLatency = Duration.ofSeconds(60);
  private static final Duration goodLatency = Duration.ofSeconds(5);

  // completeness
  private static final double BAD_COMPLETENESS = 0.1;
  private static final double MARGINAL_COMPLETENESS = 0.7;
  private static final double GOOD_COMPLETENESS = 0.9;

  // acknowledged status
  static final StationSohIssue acknowledged = StationSohIssue.from(false, Instant.EPOCH);
  static final StationSohIssue notAcknowledged = StationSohIssue.from(true, NOW);

  public static final AcquiredChannelEnvironmentIssueAnalog ACQUIRED_CHANNEL_SOH_ANALOG =
      AcquiredChannelEnvironmentIssueAnalog.from(UUID.randomUUID(),
          UtilsTestFixtures.CHANNEL.getName(),
          AcquiredChannelEnvironmentIssueType.CLIPPED,
          NOW_MINUS_TEN_MINUTES,
          NOW_MINUS_FIVE_MINUTES,
          1.0);

  public static final AcquiredChannelEnvironmentIssueAnalog ACQUIRED_CHANNEL_SOH_ANALOG_TWO =
      AcquiredChannelEnvironmentIssueAnalog.from(UUID.randomUUID(),
          UtilsTestFixtures.CHANNEL_TWO.getName(),
          AcquiredChannelEnvironmentIssueType.CLIPPED,
          NOW_MINUS_TEN_MINUTES,
          NOW_MINUS_FIVE_MINUTES,
          1.0);

  public static final AcquiredChannelEnvironmentIssueBoolean ACQUIRED_CHANNEL_SOH_BOOLEAN =
      AcquiredChannelEnvironmentIssueBoolean.from(UUID.randomUUID(),
          UtilsTestFixtures.CHANNEL.getName(),
          AcquiredChannelEnvironmentIssueType.CLOCK_LOCKED,
          NOW_MINUS_TEN_MINUTES,
          NOW_MINUS_FIVE_MINUTES,
          true);

  private static RawStationDataFrameMetadata metadata = RawStationDataFrameMetadata.builder()
      .setStationName(UtilsTestFixtures.CHANNEL.getStation())
      .setChannelNames(List.of(UtilsTestFixtures.CHANNEL.getName()))
      .setPayloadFormat(RawStationDataFramePayloadFormat.CD11)
      .setReceptionTime(Instant.EPOCH)
      .setWaveformSummaries(Map.of("ac1", waveformSummary))
      .setPayloadStartTime(Instant.EPOCH)
      .setPayloadEndTime(Instant.EPOCH)
      .setAuthenticationStatus(AuthenticationStatus.NOT_YET_AUTHENTICATED)
      .build();
  static AcquiredStationSohExtract acquiredStationSohExtract = AcquiredStationSohExtract
      .create(List.of(metadata), List.of(ACQUIRED_CHANNEL_SOH_ANALOG, ACQUIRED_CHANNEL_SOH_BOOLEAN));

}