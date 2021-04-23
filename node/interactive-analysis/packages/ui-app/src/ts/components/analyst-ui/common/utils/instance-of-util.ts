import { FkPowerSpectra } from '@gms/common-graphql/lib/graphql/fk/types';
import {
  AmplitudeMeasurementValue,
  InstantMeasurementValue,
  NumericMeasurementValue
} from '@gms/common-graphql/lib/graphql/signal-detection/types';

import { ChannelSegmentTypes, WaveformTypes } from '@gms/common-graphql';

/**
 * Determine if FeatureMeasurement Value structure is a NumericMeasurementValue
 * By looking if the reference time and measurement value are populated
 * @param fmv FeatureMeasurementValue (generic)
 * @return boolean
 */
export function isNumericFeatureMeasurementValue(
  featureMeasurementValue: any
): featureMeasurementValue is NumericMeasurementValue {
  const referenceTime = featureMeasurementValue.referenceTime;
  const measurementValue = featureMeasurementValue.measurementValue;
  return referenceTime !== undefined && measurementValue !== undefined;
}

/**
 * Determine if FeatureMeasurement Value structure is a NumericMeasurementValue
 * By looking if the reference time and measurement value are populated
 * @param fmv FeatureMeasurementValue (generic)
 * @return boolean
 */
export function isAmplitudeFeatureMeasurementValue(
  featureMeasurementValue: any
): featureMeasurementValue is AmplitudeMeasurementValue {
  const amplitude = featureMeasurementValue.amplitude;
  const period = featureMeasurementValue.period;
  const startTime = featureMeasurementValue.startTime;
  return amplitude !== undefined && period !== undefined && startTime !== undefined;
}

/**
 * Checks if FK spectra channel segment
 * @param object Channel Segment
 * @returns boolean
 */
// tslint:disable-next-line:max-line-length
export function isFkSpectraChannelSegment(
  object: ChannelSegmentTypes.ChannelSegment<ChannelSegmentTypes.TimeSeries>
): object is ChannelSegmentTypes.ChannelSegment<FkPowerSpectra> {
  return object.timeseriesType === ChannelSegmentTypes.TimeSeriesType.FK_SPECTRA;
}

/**
 * Checks if Signal detection InstantMeasurementValue
 * @param object FeatureMeasurementValue
 * @returns boolean
 */
export function isSdInstantMeasurementValue(object: any): object is InstantMeasurementValue {
  return object.value !== undefined && object.standardDeviation !== undefined;
}

/**
 * Checks if Signal detection AmplitudeMeasurementValue
 * @param object FeatureMeasurementValue
 * @returns boolean
 */
export function isSdFeatureMeasurementValue(object: any): object is AmplitudeMeasurementValue {
  return (
    object.amplitude !== undefined && object.period !== undefined && object.startTime !== undefined
  );
}

/**
 * Creates/Returns an unfiltered waveform filter
 */
export function createUnfilteredWaveformFilter(): WaveformTypes.WaveformFilter {
  return WaveformTypes.UNFILTERED_FILTER as WaveformTypes.WaveformFilter;
}
