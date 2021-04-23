package gms.shared.frameworks.osd.api.waveforms;

import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.coi.waveforms.FkSpectra;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A repository interface for storing and retrieving FK Spectrum data.
 */
public interface FkSpectraRepositoryInterface {

  /**
   * Stores newly generated FK Spectra Channel Segment.
   *
   * @param newFkChannelSegment newly generated FK Segment
   */
  void storeFkSpectra(ChannelSegment<FkSpectra> newFkChannelSegment);

  /**
   * Returns true if the ChannelSegment already exists
   *
   * @return true if record exists, false otherwise
   */
  boolean fkChannelSegmentRecordExists(ChannelSegment<FkSpectra> fkChannelSegment);

  /**
   * Returns true if ALL channel segments passed in the fkChannelSegments list parameter exist in
   * the channel segment table
   */
  boolean fkChannelSegmentRecordsExist(List<ChannelSegment<FkSpectra>> fkChannelSegments);

  /**
   * Retrieves a single Fk Spectra Channel Segment, with the given Channel Segment ID.
   *
   * @param channelSegmentId channel segment ID
   * @return optional of FK Spectra Channel Segment; may be empty
   */
  Optional<ChannelSegment<FkSpectra>> retrieveFkChannelSegment(UUID channelSegmentId);

  /**
   * Retrieves a list of Fk Spectra Channel Segments that fall within the given time range.
   *
   * @param request {@link ChannelTimeRangeRequest} object re
   * @return list of FK Spectra Channel Segments; may be empty
   */
  List<ChannelSegment<FkSpectra>> segmentsForProcessingChannel(ChannelTimeRangeRequest request);

  /**
   * Retrieves a list of Fk Spectra Channel Segments that fall within the given time range. Merges
   * where needed
   *
   * @param request {@link ChannelTimeRangeRequest} consisting of name of {@link
   * Channel} instance and time range to
   * retrieve {@link ChannelSegment} of {@link FkSpectra}s object instance for.
   * @return list of FK Spectra Channel Segments; may be empty
   */
  Optional<ChannelSegment<FkSpectra>> retrieveFkSpectraByTime(ChannelTimeRangeRequest request);

  /**
   * Retrieves a {@link List} of {@link FkSpectra} objects associated with the provided
   * {@link UUID}s, which relate to timeseries ids.  Optionally includes the underlying
   * {@link gms.shared.frameworks.osd.coi.waveforms.FkSpectrum}
   * values.
   *
   * @param ids for the related Timeseries
   * {@link UUID}s for which to retrieve the associated {@link FkSpectra}s
   * @return {@link List} of {@link FkSpectra}. Optionally includes the underlying {@link
   * gms.shared.frameworks.osd.coi.waveforms.FkSpectrum}
   * values.
   */
  List<FkSpectra> retrieveFkSpectrasByTimeseriesIds(Collection<UUID> ids);
}
