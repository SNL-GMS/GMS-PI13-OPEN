package gms.dataacquisition.stationreceiver.cd11.parser;

import static com.google.common.base.Preconditions.checkNotNull;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframeHeader;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.shared.frameworks.osd.coi.dataacquisition.ReceivedStationDataPacket;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame.AuthenticationStatus;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFramePayloadFormat;
import gms.shared.frameworks.osd.coi.waveforms.WaveformSummary;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cd11RawStationDataFrameUtility {

  private static final Logger logger = LoggerFactory.getLogger(Cd11RawStationDataFrameUtility.class);

  private Cd11RawStationDataFrameUtility() {
  }

  /**
   * Parses a {@link ReceivedStationDataPacket} containing CD-1.1 data into a {@link
   * RawStationDataFrame}
   *
   * @param dataFrameReceiverConfiguration configuration to load the
   * @param packet the received packet
   * @return a {@link RawStationDataFrame}, not null
   * @throws IllegalArgumentException if the packet cannot be read as a CD-1.1 frame
   */
  public static RawStationDataFrame parseAcquiredStationDataPacket(
      DataFrameReceiverConfiguration dataFrameReceiverConfiguration,
      ReceivedStationDataPacket packet) {
    checkNotNull(packet, "Cannot parse null packet");
    final Cd11DataFrame df;
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getPacket()))) {
      final Instant start = Instant.now();
      df = new Cd11DataFrame(
          new Cd11ByteFrame(in, () -> Duration.between(start, Instant.now()).toMillis() < 1000));
    } catch (Exception ex) {
      throw new IllegalArgumentException("Could not read frame from cd11 packet", ex);
    }

    final Cd11ChannelSubframeHeader header = df.chanSubframeHeader;
    final Instant startTime = header.nominalTime;
    final Instant endTime = startTime.plusMillis(header.frameTimeLength);
    final List<String> channelNames = new ArrayList<>();
    final Map<String, WaveformSummary> waveformSummaries = new HashMap<>();
    for (Cd11ChannelSubframe s : df.channelSubframes) {
      String subFrameName = String
          .format("%s.%s.%s", packet.getStationIdentifier(), s.siteName, s.channelName);
      dataFrameReceiverConfiguration
          .getChannelName(subFrameName)
          .ifPresentOrElse(channelName -> {
                channelNames.add(channelName);
                waveformSummaries
                    .put(channelName, WaveformSummary.from(channelName, s.timeStamp, s.endTime));
              },
              () -> logger.warn(
                  "Channel name for subframe name {} not found in configuration. Skipping channel subframe.",
                  subFrameName));
    }
    return RawStationDataFrame.builder()
        .setId(UUID.randomUUID())
        .setMetadata(RawStationDataFrameMetadata.builder()
            .setPayloadFormat(RawStationDataFramePayloadFormat.CD11)
            .setStationName(packet.getStationIdentifier())
            .setChannelNames(channelNames)
            .setAuthenticationStatus(AuthenticationStatus.NOT_YET_AUTHENTICATED)
            .setReceptionTime(packet.getReceptionTime())
            .setWaveformSummaries(waveformSummaries)
            .setPayloadStartTime(startTime)
            .setPayloadEndTime(endTime)
            .build())
        .setRawPayload(packet.getPacket())
        .build();
  }

  public static RawStationDataFrame parseAcquiredStationDataPacket(
      DataFrameReceiverConfiguration dataFrameReceiverConfiguration,
      Cd11DataFrame df, Instant currentTime, String stationIndentifier) {
    checkNotNull(df, "Cannot parse null dataframe");

    final Cd11ChannelSubframeHeader header = df.chanSubframeHeader;
    final Instant startTime = header.nominalTime;
    final Instant endTime = startTime.plusMillis(header.frameTimeLength);
    final List<String> channelNames = new ArrayList<>();
    final Map<String, WaveformSummary> waveformSummaries = new HashMap<>();
    for (Cd11ChannelSubframe s : df.channelSubframes) {
      String subFrameName = String
          .format("%s.%s.%s", stationIndentifier, s.siteName, s.channelName);
      dataFrameReceiverConfiguration
          .getChannelName(subFrameName)
          .ifPresentOrElse(channelName -> {
                channelNames.add(channelName);
                waveformSummaries
                    .put(channelName, WaveformSummary.from(channelName, s.timeStamp, s.endTime));
              },
              () -> logger.warn(
                  "Channel name for subframe name {} not found in configuration. Skipping channel subframe.",
                  subFrameName));
    }

    return RawStationDataFrame.builder()
        .setId(UUID.randomUUID())
        .setMetadata(RawStationDataFrameMetadata.builder()
            .setPayloadFormat(RawStationDataFramePayloadFormat.CD11)
            .setStationName(stationIndentifier)
            .setChannelNames(channelNames)
            .setAuthenticationStatus(AuthenticationStatus.NOT_YET_AUTHENTICATED)
            .setReceptionTime(currentTime)
            .setWaveformSummaries(waveformSummaries)
            .setPayloadStartTime(startTime)
            .setPayloadEndTime(endTime)
            .build())
        .setRawPayload(df.getRawNetworkBytes())
        .build();
  }
}
