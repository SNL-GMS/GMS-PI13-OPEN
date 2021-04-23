package gms.dataacquisition.csswaveformconverter;

import gms.dataacquisition.cssreader.waveformreaders.FlatFileWaveformReader;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.dataacquisition.SegmentClaimCheck;
import gms.shared.frameworks.osd.coi.dataacquisition.WaveformAcquiredChannelSohPair;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.Validate;

/**
 * Converts {@link SegmentClaimCheck}'s into waveform segments and state of health.
 * Reads segment claim checks in batches for efficiency (and not running out of memory).
 */
public class SegmentClaimCheckConverter {

  private final int batchSize;
  private final String waveformsDir;
  private final FlatFileWaveformReader waveformReader = new FlatFileWaveformReader();
  private Iterator<SegmentClaimCheck> segmentClaimCheckIterator;

  /**
   * Creates a SegmentClaimCheckConverter
   *
   * @param segmentClaimChecks the segment claim check info
   * errors, etc.
   */
  public SegmentClaimCheckConverter(Collection<SegmentClaimCheck> segmentClaimChecks, 
      String waveformsDir, int batchSize) {
    Validate.isTrue(batchSize > 0, "The batchSize value must be greater than zero.");
    this.segmentClaimCheckIterator = Objects.requireNonNull(segmentClaimChecks).iterator();
    this.waveformsDir = Objects.requireNonNull(waveformsDir);
    this.batchSize = batchSize;
  }

  /**
   * Indicates whether there is more CSS data to retrieve from the WF Disc file.
   *
   * @return true if more data exists, otherwise false
   */
  boolean nextBatchExists() {
    return segmentClaimCheckIterator.hasNext();
  }

  /**
   * Retrieves the next batch of CSS data.
   *
   * @return list of css data records.
   */
  WaveformAcquiredChannelSohPair readNextBatch() throws IOException {
    List<ChannelSegment<Waveform>> channelSegmentBatch = new ArrayList<>();
    List<AcquiredChannelEnvironmentIssue> sohBatch = new ArrayList<>();

    int i = 0;
    while (i++ < this.batchSize && this.segmentClaimCheckIterator.hasNext()) {
      final SegmentClaimCheck claimCheck = this.segmentClaimCheckIterator.next();

      SortedSet<Waveform> wfs = new TreeSet<>();
      final double[] vals = this.waveformReader.readWaveform(
          prefixIfNotNull(waveformsDir, claimCheck.getWaveformFile()),
          claimCheck.getfOff(),
          claimCheck.getSampleCount(),
          claimCheck.getDataType());
      final Waveform wf = Waveform
          .from(claimCheck.getStartTime(), claimCheck.getSampleRate(), vals);
      wfs.add(wf);

      Channel channel = claimCheck.getChannel();
      String chanName = channel.getName();
      channelSegmentBatch.add(ChannelSegment.from(
          claimCheck.getSegmentId(),
          channel,
          "segment for channel " + chanName,
          claimCheck.getSegmentType(),
          wfs));

      // Add boolean SOH if 'clipped' is true.
      if (claimCheck.isClipped()) {
        AcquiredChannelEnvironmentIssueBoolean soh = AcquiredChannelEnvironmentIssueBoolean.create(
            chanName,
            AcquiredChannelEnvironmentIssueType.CLIPPED,
            wf.getStartTime(),
            wf.getEndTime(),
            true);   // data is clipped, so status is true.
        sohBatch.add(soh);
      }

    }
    return WaveformAcquiredChannelSohPair.from(channelSegmentBatch, sohBatch);
  }

  /**
   * Reads all of the batches of data at once and returns them.
   * @return all of the data this reader can get
   */
  public List<WaveformAcquiredChannelSohPair> readAllBatches() throws IOException {
    final List<WaveformAcquiredChannelSohPair> batches = new ArrayList<>();
    while (nextBatchExists()) {
      batches.add(readNextBatch());
    }
    return Collections.unmodifiableList(batches);
  }

  private static String prefixIfNotNull(String dir, String file) {
    if (dir == null) {
      return file;
    } else {
      if (dir.endsWith(File.separator)) {
        return dir + file;
      }
      return dir + File.separator + file;
    }
  }
}
