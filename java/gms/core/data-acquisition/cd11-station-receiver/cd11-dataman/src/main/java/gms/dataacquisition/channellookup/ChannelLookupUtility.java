package gms.dataacquisition.channellookup;

import static java.lang.String.format;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import java.util.Optional;

public class ChannelLookupUtility {

  private DataFrameReceiverConfiguration dataFrameReceiverConfiguration;

  public ChannelLookupUtility(
      DataFrameReceiverConfiguration dataFrameReceiverConfiguration) {
    this.dataFrameReceiverConfiguration = dataFrameReceiverConfiguration;
  }

  /**
   * Formats a packet name for the subframe, and retrieves a matching channel name from
   * configuration
   *
   * @param stationName Name of the station this subframe originates from
   * @param subframe The channel subframe holding channel metadata
   * @return The Channel name the System uses for processing
   * @throws IllegalStateException if configuration did not resolve a channel name for the packet
   * name
   */
  public Optional<String> getChannelName(String stationName, Cd11ChannelSubframe subframe) {
    // Channel names are formatted like:
    // Station.ChannelGroup.ChannelCode
    String packetName = format("%s.%s.%s",
        stationName, subframe.siteName, subframe.channelName);
    return dataFrameReceiverConfiguration.getChannelName(packetName);
  }

}
