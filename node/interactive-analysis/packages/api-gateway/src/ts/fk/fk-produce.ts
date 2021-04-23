import { produce } from 'immer';
import { ChannelSegment } from '../channel-segment/model';
import { FeatureMeasurement } from '../signal-detection/model';
import { getChannelSegmentDescriptor } from '../util/signal-detection-utils';
import { FkPowerSpectra } from './model';

/**
 * Updates the event hypothesis
 * @param measurement the feature measurement
 * @param fkChannelSegment fk channel segment
 * @returns updated feature measurement
 */
export function updateFMChannelSegmentDescriptor(
  measurement: FeatureMeasurement,
  fkChannelSegment: ChannelSegment<FkPowerSpectra>
) {
  return produce<FeatureMeasurement>(measurement, draftState => {
    draftState.measuredChannelSegmentDescriptor = getChannelSegmentDescriptor(fkChannelSegment);
  });
}
