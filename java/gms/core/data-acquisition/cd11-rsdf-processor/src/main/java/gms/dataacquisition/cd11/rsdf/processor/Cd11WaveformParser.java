package gms.dataacquisition.cd11.rsdf.processor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Sets;
import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.enums.CompressionFormat;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment.Type;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import gms.utilities.waveformreader.WaveformReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Cd11WaveformParser {

  private final DataFrameReceiverConfiguration configuration;
  private final ChannelRepositoryInterface channelRepository;
  private Map<String, Channel> channelsByName;

  private static Logger logger = LoggerFactory.getLogger(Cd11WaveformParser.class);

  private Cd11WaveformParser(DataFrameReceiverConfiguration configuration,
      ChannelRepositoryInterface channelRepository) {
    this.configuration = configuration;
    this.channelRepository = channelRepository;
    this.channelsByName = Map.of();
  }

  public static Cd11WaveformParser create(DataFrameReceiverConfiguration receiverConfiguration,
      ChannelRepositoryInterface channelRepository) {
    checkNotNull(receiverConfiguration);
    checkNotNull(channelRepository);

    return new Cd11WaveformParser(receiverConfiguration, channelRepository);
  }

  /**
   * Retrieves {@link Channel}s via the {@link ChannelRepositoryInterface} and caches them for use
   * in processing.
   *
   * @throws IllegalStateException If {@link Channel}s were not found for every one found in
   * configuration.
   */
  public void updateChannelCache() {
    List<String> channelNames = configuration.channelNames().collect(toList());
    List<Channel> channels = channelRepository.retrieveChannels(channelNames);

    checkState(channelNames.size() == channels.size(),
        "Not all channels retrieved, missing channels:%s", channelNames.size(),
        findMissingChannels(channelNames, channels));

    channelsByName = channels.stream()
        .collect(toMap(Channel::getName, identity()));
  }

  /**
   * Finds the channel names that were expected to be a part of the result, but were missing.
   *
   * @param expectedChannelNames Channel names expected to be loaded/found
   * @param actualChannels Actual Channels loaded/found
   * @return an unmodifiable view of the difference between the expected channel names and the names
   * of the actual channels
   */
  private Set<String> findMissingChannels(Collection<String> expectedChannelNames,
      Collection<Channel> actualChannels) {

    var expectedChannelNameSet = new HashSet<>(expectedChannelNames);
    var actualChannelNameSet = actualChannels.stream()
        .map(Channel::getName)
        .collect(toSet());

    return Sets.difference(expectedChannelNameSet, actualChannelNameSet);
  }

  /**
   * Parses a {@link RawStationDataFrame} into waveforms and SOH
   *
   * @param rsdf The CD11 RawStationDataFrame to parse
   * @return Pair of ChannelSegment of Waveform and AcquiredChannelSoh Lists
   * @throws IOException When there is some issue reading from the byte stream of the rsdf,
   * including an I/O interrupt
   * @throws IllegalStateException if there are discrepancies in configuration or the channel cache
   */
  public List<ChannelSegment<Waveform>> parseWaveform(RawStationDataFrame rsdf) throws IOException {
    Objects.requireNonNull(rsdf, "Cannot parse null RawStationDataframe");

    List<ChannelSegment<Waveform>> channelSegments = new ArrayList<>();
    try (ByteArrayInputStream input = new ByteArrayInputStream(rsdf.getRawPayload());
        DataInputStream rawPayloadInputStream = new DataInputStream(input)) {

      Cd11ByteFrame bf = new Cd11ByteFrame(rawPayloadInputStream, () -> false);
      Cd11DataFrame df = new Cd11DataFrame(bf);

      //Parse each subframe (1 subframe = 1 channel)
      for (Cd11ChannelSubframe sf : df.channelSubframes) {

        //Resolve channel name from config
        Optional<String> channelName = configuration.getChannelName(String.format("%s.%s.%s",
            rsdf.getMetadata().getStationName(), sf.siteName, sf.channelName));

        if (channelName.isPresent()) {
          //Get the corresponding channel
          Channel channel = Optional.ofNullable(channelsByName.get(channelName.get()))
              .orElseThrow(() -> new IllegalStateException(
                  format("No channel matching name %s found in waveform cache", channelName)));
          ChannelSegment<Waveform> channelSegment = parseWaveform(sf, channel);
          channelSegments.add(channelSegment);
        } else {
          //log warning and move on...
          String subFrameName = format("%s.%s.%s",
              rsdf.getMetadata().getStationName(), sf.siteName, sf.channelName);

          logger.warn(
              "Channel name for subframe name {} not found in configuration. "
                  + "Skipping channel subframe.",
              subFrameName);
        }
      }
      return channelSegments;
    }
  }

  private ChannelSegment<Waveform> parseWaveform(Cd11ChannelSubframe sf, Channel channel)
      throws IOException {
    //Grab channel data, call waveform reader, which returns and int[] so convert it to double[]
    InputStream waveformData = new ByteArrayInputStream(sf.channelData);
    double[] waveformValues;
    //No Compression, use what is in data type field
    if (sf.compressionFormat == CompressionFormat.NONE) {
      waveformValues = WaveformReader
          .readSamples(waveformData, sf.cd11DataFormat.toString(), sf.samples, 0);
    }
    //Canadian Compression, ignore data type field
    else if (sf.compressionFormat == CompressionFormat.CANADIAN_BEFORE_SIGNATURE
        || sf.compressionFormat == CompressionFormat.CANADIAN_AFTER_SIGNATURE) {
      waveformValues = WaveformReader.readSamples(waveformData, "cc", sf.samples, 0);
    } else {
      throw new InvalidParameterException(
          "Unsupported compression format: " + sf.compressionFormat);
    }

    Waveform waveform = Waveform
        .from(sf.timeStamp, sf.sampleRate, waveformValues);

    return ChannelSegment.create(channel, channel.getName(), Type.ACQUIRED, List.of(waveform));
  }
}
