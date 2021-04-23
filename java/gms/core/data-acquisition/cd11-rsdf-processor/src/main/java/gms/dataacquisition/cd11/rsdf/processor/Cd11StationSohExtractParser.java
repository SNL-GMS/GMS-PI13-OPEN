package gms.dataacquisition.cd11.rsdf.processor;

import static com.google.common.base.Preconditions.checkNotNull;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Cd11StationSohExtractParser {

  private static final Logger logger = LoggerFactory.getLogger(Cd11StationSohExtractParser.class);

  private DataFrameReceiverConfiguration dataFrameReceiverConfiguration;

  private Cd11StationSohExtractParser(
      DataFrameReceiverConfiguration dataFrameReceiverConfiguration) {
    this.dataFrameReceiverConfiguration = dataFrameReceiverConfiguration;
  }

  public static Cd11StationSohExtractParser create(
      DataFrameReceiverConfiguration dataFrameReceiverConfiguration) {
    return new Cd11StationSohExtractParser(dataFrameReceiverConfiguration);
  }

  /**
   * Parses a {@link RawStationDataFrame}, building up a collection of data frame metadata and
   * State-of-Health
   *
   * @param rsdf The Data frame to parse
   * @return {@link AcquiredStationSohExtract} representing State-of-Health data and data frame
   * metadata
   * @throws IOException If there were errors in reading the data frame
   * @throws IllegalArgumentException If the input data frame was malformed in any way
   */
  public AcquiredStationSohExtract parseStationSohExtract(RawStationDataFrame rsdf)
      throws IOException {
    checkNotNull(rsdf, "Cannot parse null RawStationDataFrame");
    logger.info("Parsing StationSohExtract for RawStationDataFrame {}:{}",
        rsdf.getMetadata().getStationName(),
        rsdf.getId());

    Cd11DataFrame df;
    try (ByteArrayInputStream input = new ByteArrayInputStream(rsdf.getRawPayload())) {
      DataInputStream rawPayloadInputStream;
      rawPayloadInputStream = new DataInputStream(input);
      Cd11ByteFrame bf = new Cd11ByteFrame(rawPayloadInputStream, () -> true);
      df = new Cd11DataFrame(bf);
    }

    //Parse each subframe (1 subframe = 1 channel)
    //Resolve channel name from config, if not present, skip.
    // Parse the channel status bits and then save to the OSD.
    List<AcquiredChannelEnvironmentIssue> statesOfHealth = new ArrayList<>();
    for (Cd11ChannelSubframe sf : df.channelSubframes) {
      String subFrameName = String
          .format("%s.%s.%s", rsdf.getMetadata().getStationName(), sf.siteName, sf.channelName);
      dataFrameReceiverConfiguration
          .getChannelName(subFrameName)
          .ifPresentOrElse(
              channelName -> statesOfHealth.addAll(Cd11AcquiredChannelEnvironmentIssuesParser
                  .parseAcquiredChannelSoh(sf.channelStatusData, channelName,
                      sf.timeStamp, sf.endTime)),
              () -> logger.warn(
                  "Channel name for subframe with name {} not found in configuration. "
                      + "Skipping channel subframe.",
                  subFrameName));
    }

    return AcquiredStationSohExtract.create(List.of(rsdf.getMetadata()),
        statesOfHealth);
  }


}
