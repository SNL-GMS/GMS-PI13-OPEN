import { EventTypes, SignalDetectionTypes } from '@gms/common-graphql';
import { random, seed } from 'faker';
import { calculateOffsets } from '../../../../../src/ts/components/analyst-ui/components/waveform-display/utils';

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

const numberOfFeaturePredictions = random.number({ min: 10, max: 12 });
const fakerWaveformDisplaySeed = 123;
seed(fakerWaveformDisplaySeed);

function* MockFeaturePrediction() {
  while (true) {
    const predictedValue = {
      value: random.number({ min: 1, max: 20 }),
      standardDeviation: 0
    };
    const predictionType = SignalDetectionTypes.FeatureMeasurementTypeName.ARRIVAL_TIME;
    const phase = 'P';
    const featurePrediction: EventTypes.FeaturePrediction = {
      predictedValue,
      predictionType,
      phase
    };
    yield featurePrediction;
  }
}

const mockFeaturePrediction = MockFeaturePrediction();

const mockFeaturePredictionGenerator: () => EventTypes.FeaturePrediction = (() => () =>
  mockFeaturePrediction.next().value as EventTypes.FeaturePrediction)();

const mockFeaturePredictions: EventTypes.FeaturePrediction[] = [];

for (let i = 0; i < numberOfFeaturePredictions; i++) {
  mockFeaturePredictions.push(mockFeaturePredictionGenerator());
}

describe('Waveform Display Utility Test', () => {
  describe('Calculate Offsets', () => {
    test('calculateOffsets should return a list of offsets', () => {
      const offsets = calculateOffsets(mockFeaturePredictions, '11111111111111111', 'P');
      expect(offsets).toBeDefined();
    });
  });
});
