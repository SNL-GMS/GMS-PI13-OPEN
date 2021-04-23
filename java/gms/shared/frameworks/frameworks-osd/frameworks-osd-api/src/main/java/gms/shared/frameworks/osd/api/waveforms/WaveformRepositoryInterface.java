package gms.shared.frameworks.osd.api.waveforms;

import gms.shared.frameworks.osd.api.waveforms.util.ChannelSegmentStorageResponse;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


/**
 * The interface for storing and retrieving waveform COI objects.
 */
public interface WaveformRepositoryInterface {

  /**
   * Store a collection of {@link ChannelSegment} of {@link Waveform} to the underlying
   * persistence.
   *
   * @param segments The channel segments
   * @return A {@link ChannelSegmentStorageResponse} detailing which channel segments stored
   * successfully, and which failed to store.
   */
  ChannelSegmentStorageResponse store(Collection<ChannelSegment<Waveform>> segments);

  /**
   * Helper method for storing a single {@link ChannelSegment} of {@link Waveform}
   *
   * @param segment The channel segment
   * @see WaveformRepositoryInterface#store(Collection)
   */
  default ChannelSegmentStorageResponse store(ChannelSegment<Waveform> segment) {
    return store(List.of(segment));
  }

  /**
   * Get a collection from Waveform objects from the database.
   *
   * @param channelId the id of the processing channel the waveform is for.
   * @param startTime Starting time for the time-series.
   * @param endTime Ending time for the time-series.
   * @return A list of Waveform objects.  The list may be empty.
   */
  List<Waveform> retrieveWaveformsByTime(
      UUID channelId,
      Instant startTime,
      Instant endTime);

  /**
   * Get a Map from UUID to Waveform objects from the database. Makes a single call to Postgres and
   * Cassandra for faster performance
   *
   * @param channelIds the ids of the processing channel the waveform is for.
   * @param startTime Starting time for the time-series.
   * @param endTime Ending time for the time-series.
   * @return A list of Waveform objects.  The list may be empty.
   */
  Map<UUID, List<Waveform>> retrieveWaveformsByTime(
      Collection<UUID> channelIds,
      Instant startTime,
      Instant endTime);

  /**
   * Retrieves channel segments for a collection of channels over a time range.
   *
   * @param channelIds ids of the processing channel to retrieve segments for
   * @param rangeStart - the start of the range to query for - inclusive
   * @param rangeEnd - the end of the range to query for - inclusive
   * @return Map from channelId to a List of ChannelSegment's, ordered by start time. If no data was
   * found for the channel, that id won't be present in the returned map.  The query will return all
   * ChannelSegment's that contain data within the time range specified, which means there may be
   * segment's returned with data outside of [rangeStart, rangeEnd].
   */
  Map<UUID, List<ChannelSegment<Waveform>>> segmentsForProcessingChannel(
      Collection<UUID> channelIds,
      Instant rangeStart,
      Instant rangeEnd);

  /**
   * Retrieves channel segments for a channels over a time range.
   *
   * @param channelId id of the processing channel to retrieve segments for
   * @param rangeStart - the start of the range to query for - inclusive
   * @param rangeEnd - the end of the range to query for - inclusive
   * @return A List of ChannelSegment's, ordered by start time.  The list may be empty.  The query
   * will return all ChannelSegment's that contain data within the time range specified, which means
   * there may be segment's returned with data outside of [rangeStart, rangeEnd].
   */
  List<ChannelSegment<Waveform>> segmentsForProcessingChannel(
      UUID channelId,
      Instant rangeStart,
      Instant rangeEnd);

  /**
   * Retrieve a map from channel Id to new ChannelSegment from querying for the given channel Ids
   * and time range. Channel id's for which no data is found will not be present in the result map.
   *
   * @param channelIds ids of the processing channels to retrieve segments for
   * @param rangeStart - the start of the range to query for - inclusive
   * @param rangeEnd - the end of the range to query for - inclusive
   * @return A Map from the id of the channel to the segment retrieved for it.
   */
  Map<UUID, ChannelSegment<Waveform>> retrieveChannelSegments(
      Collection<UUID> channelIds,
      Instant rangeStart,
      Instant rangeEnd);

  /**
   * Retrieve a new ChannelSegment id of the ProcessingChannel and a time range.  Returns an empty
   * {@link Optional} if no data matches the query parameters.
   *
   * @param channelId id of the processing channel to retrieve segments for
   * @param rangeStart - the start of the range to query for - inclusive
   * @param rangeEnd - the end of the range to query for - inclusive
   * @return An {@link Optional} ChannelSegment.
   */
  Optional<ChannelSegment<Waveform>> retrieveChannelSegment(
      UUID channelId,
      Instant rangeStart,
      Instant rangeEnd);

  /**
   * Retrieve ChannelSegments by their id's.
   *
   * @param segmentIds the ids of the segment
   * @return An {@link Optional} ChannelSegment.
   */
  Map<UUID, ChannelSegment<Waveform>> retrieveChannelSegments(
      Collection<UUID> segmentIds);

  /**
   * Retrieve a ChannelSegment by its id.
   *
   * @param segmentId the id of the segment
   * @return An {@link Optional} ChannelSegment.
   */
  Optional<ChannelSegment> retrieveChannelSegment(UUID segmentId);

  /**
   * @param channelIdSet ids of the processing channels to retrieve segments for
   * @param rangeStart - the start of the range to query for - inclusive
   * @param rangeEnd - the end of the range to query for - inclusive
   * @return A map of the channel id to its percentage of data available.
   */
  Map<UUID, Double> calculateChannelAvailability(
      Collection<UUID> channelIdSet,
      Instant rangeStart,
      Instant rangeEnd);
}
