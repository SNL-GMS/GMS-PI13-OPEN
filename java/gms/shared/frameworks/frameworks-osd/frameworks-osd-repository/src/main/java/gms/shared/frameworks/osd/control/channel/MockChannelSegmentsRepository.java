package gms.shared.frameworks.osd.control.channel;

import gms.shared.frameworks.osd.api.channel.ChannelSegmentsRepositoryInterface;
import gms.shared.frameworks.osd.api.channel.util.ChannelSegmentsIdRequest;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ChannelsTimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.waveforms.FkSpectra;
import gms.shared.frameworks.osd.coi.waveforms.Timeseries;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import java.util.Collection;
import java.util.List;

public class MockChannelSegmentsRepository implements ChannelSegmentsRepositoryInterface {
  @Override
  public Collection<ChannelSegment<? extends Timeseries>> retrieveChannelSegmentsByIds(ChannelSegmentsIdRequest request) {
    return List.of();
  }

  @Override
  public Collection<ChannelSegment<Waveform>> retrieveChannelSegmentsByChannelNames(ChannelsTimeRangeRequest request) {
    return List.of();
  }

  @Override
  public Collection<ChannelSegment<Waveform>> retrieveChannelSegmentsByChannelsAndTimeRanges(Collection<ChannelTimeRangeRequest> channelTimeRangeRequests) {
    return List.of();
  }

  @Override
  public void storeChannelSegments(Collection<ChannelSegment<Waveform>> segments) {
    // Mock repo does not store
  }

  @Override
  public List<ChannelSegment<FkSpectra>> retrieveFkChannelSegmentsByChannelsAndTime(Collection<ChannelTimeRangeRequest> channelTimeRangeRequests) {
    return List.of();
  }

  @Override
  public void storeFkChannelSegments(Collection<ChannelSegment<FkSpectra>> segments) {
    // Mock repo does not store
  }
}
