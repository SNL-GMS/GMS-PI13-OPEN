import { EventTypes } from '@gms/common-graphql';
import { isSdInstantMeasurementValue } from '~analyst-ui/common/utils/instance-of-util';

export interface Offset {
  channelId: string;
  offset: number;
}

/**
 * Calculate offsets based on station with earliest arrival.
 */
export const calculateOffsets = (
  featurePredictions: EventTypes.FeaturePrediction[],
  baseChannelId: string,
  phaseToOffset: string
): Offset[] => {
  if (!featurePredictions || featurePredictions.length <= 0) {
    return [];
  }
  const filteredPredictions: EventTypes.FeaturePrediction[] = featurePredictions.filter(
    fp => fp.phase === phaseToOffset
  );

  // If no FPs were found for the phase selected, return nothing
  if (filteredPredictions.length <= 0) {
    return [];
  }

  filteredPredictions.sort((a, b) => {
    if (
      isSdInstantMeasurementValue(a.predictedValue) &&
      isSdInstantMeasurementValue(b.predictedValue)
    ) {
      const aValue = a.predictedValue.value;
      const bValue = b.predictedValue.value;
      return aValue - bValue;
    }
  });

  const offsets: Offset[] = [];
  const baseChannelFeaturePrediction = filteredPredictions.find(
    fp => fp.channelName === baseChannelId
  );

  if (
    baseChannelFeaturePrediction &&
    isSdInstantMeasurementValue(baseChannelFeaturePrediction.predictedValue)
  ) {
    const baseTime: number = baseChannelFeaturePrediction.predictedValue.value;

    filteredPredictions.forEach(sortedFeaturePrediction => {
      if (isSdInstantMeasurementValue(sortedFeaturePrediction.predictedValue)) {
        const offset: Offset = {
          channelId: sortedFeaturePrediction.channelName,
          offset: baseTime - sortedFeaturePrediction.predictedValue.value
        };
        offsets.push(offset);
      }
    });
  }
  return offsets;
};
