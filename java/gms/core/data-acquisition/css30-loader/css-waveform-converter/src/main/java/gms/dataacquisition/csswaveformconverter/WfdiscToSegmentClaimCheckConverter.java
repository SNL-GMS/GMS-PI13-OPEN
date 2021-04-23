package gms.dataacquisition.csswaveformconverter;

import gms.dataacquisition.cssreader.data.WfdiscRecord;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment.Type;
import gms.shared.frameworks.osd.coi.dataacquisition.SegmentClaimCheck;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WfdiscToSegmentClaimCheckConverter {

  private static final Logger logger = LoggerFactory
      .getLogger(WfdiscToSegmentClaimCheckConverter.class);

  private static final Map<String, Type> cssSegtypeToChannelSegmentType = Map.of(
      "A", ChannelSegment.Type.ACQUIRED,
      "R", ChannelSegment.Type.RAW,
      "D", ChannelSegment.Type.DETECTION_BEAM,
      "K", ChannelSegment.Type.FK_BEAM,
      "P", ChannelSegment.Type.FK_SPECTRA,
      "F", ChannelSegment.Type.FILTER);

  public List<SegmentClaimCheck> convert(List<WfdiscRecord> wfdiscRecords,
      Map<String, Channel> acquiredChannelMap,
      Map<Long, Channel> derivedChannelMap) {

    Objects.requireNonNull(wfdiscRecords);
    final List<SegmentClaimCheck> results = new ArrayList<>();
    for (WfdiscRecord wdr : wfdiscRecords) {
      final Optional<ChannelSegment.Type> segmentType = getCoiObjectChanSegType(wdr.getSegtype());
      if (!segmentType.isPresent()) {
        logger.error("Unknown segtype {} on record {}", wdr.getSegtype(), wdr);
      } else {

        Channel channelRefernce = acquiredChannelMap.get(wdr.getSta() + "." + wdr.getChan());

        if (channelRefernce == null) {
          channelRefernce = derivedChannelMap.get(wdr.getWfid());

          if (channelRefernce == null) {
            logger.warn("No channel found for {}.{} or wfid {}", 
                wdr.getSta(), wdr.getChan(), wdr.getWfid());
            continue;
          }
        }

        results.add(convert(wdr, segmentType.get(), channelRefernce));
      }
    }
    logger.warn("Found {} channels from {} wfdisc records", 
        results.size(), wfdiscRecords.size());
    return results;
  }

  private SegmentClaimCheck convert(WfdiscRecord wdr, ChannelSegment.Type segType,
      Channel channelObj) {
    return SegmentClaimCheck.from(
        UUID.nameUUIDFromBytes(String.valueOf(wdr.getWfid()).getBytes()),
        channelObj,
        segmentName(wdr),
        wdr.getTime(),
        wdr.getSamprate(),
        wdr.getDfile(),
        wdr.getNsamp(),
        wdr.getFoff(),
        wdr.getDatatype(),
        segType,
        wdr.getClip());
  }

  private static String segmentName(WfdiscRecord wdr) {
    final Optional<ChannelSegment.Type> segmentType = getCoiObjectChanSegType(wdr.getSegtype());
    final String segTypeString = segmentType.isPresent() ? segmentType.get().toString() : "UNKNOWN";
    return wdr.getSta() + "." + wdr.getChan() + " " + segTypeString;
  }

  /**
   * Get the channel segment type enumerated in the COI object, instead of the enumeration used in
   * CSS
   *
   * @return Type The channel segment type enumerated in the COI object
   */
  private static Optional<ChannelSegment.Type> getCoiObjectChanSegType(String cssSegType) {
    if (cssSegType == null) {
      return Optional.empty();
    }
    if (cssSegType.contains("-") || cssSegType.contains("o")) {
      return Optional.of(ChannelSegment.Type.ACQUIRED); // return default for N/A value.
    }
    return Optional.ofNullable(
        cssSegtypeToChannelSegmentType.get(cssSegType.toUpperCase(Locale.ENGLISH)));
  }
}
