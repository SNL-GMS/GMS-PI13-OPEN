import {
  AmplitudeMeasurementValue,
  FeatureMeasurement,
  FeatureMeasurementTypeName,
  FeatureMeasurementValue,
  InstantMeasurementValue,
  NumericMeasurementValue,
  PhaseTypeMeasurementValue
} from '../signal-detection/model';
import {
  AmplitudeMeasurementValueOSD,
  FeatureMeasurementValueOSD,
  NumericMeasurementValueOSD
} from '../signal-detection/model-osd';

export const amplitudeTypeNameList: FeatureMeasurementTypeName[] = Object.seal([
  FeatureMeasurementTypeName.AMPLITUDE,
  FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2,
  FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2_OR,
  FeatureMeasurementTypeName.AMPLITUDE_ALR_OVER_2,
  FeatureMeasurementTypeName.AMPLITUDE_ANL_OVER_2,
  FeatureMeasurementTypeName.AMPLITUDE_FKSNR,
  FeatureMeasurementTypeName.AMPLITUDE_SBSNR
]);

/**
 * Searches Feature Measurements for the desired Feature Measurement and returns the feature measurement.
 * @param featureMeasurements List of feature measurements
 * @param featureMeasurementType Enum of desired Feature Measurement desired
 * @returns FeatureMeasurement or undefined if not found
 */
export function findFeatureMeasurement(
  featureMeasurements: FeatureMeasurement[],
  featureMeasurementType: FeatureMeasurementTypeName
): FeatureMeasurement | undefined {
  if (featureMeasurements && featureMeasurementType) {
    return featureMeasurements.find(f => f.featureMeasurementType === featureMeasurementType);
  }
  return undefined;
}

/**
 * Searches Feature Measurements for the desired Feature Measurement and returns the
 * feature measurement value.
 * @param featureMeasurements List of feature measurements
 * @param featureMeasurementType Enum of desired Feature Measurement desired
 * @returns FeatureMeasurementValue or undefined if not found
 */
export function findFeatureMeasurementValue<T extends FeatureMeasurementValue>(
  featureMeasurements: FeatureMeasurement[],
  featureMeasurementType: FeatureMeasurementTypeName
): T | undefined {
  if (featureMeasurements && featureMeasurementType) {
    const fm = featureMeasurements.find(f => f.featureMeasurementType === featureMeasurementType);
    return fm ? (fm.measurementValue as T) : undefined;
  }
  return undefined;
}

/**
 * Searches Feature Measurements for the ArrivalTime Feature Measurement
 * @param featureMeasurements List of feature measurements
 * @returns ArrivalTime FeatureMeasurement or undefined if not found
 */
export function findArrivalTimeFeatureMeasurement(
  featureMeasurements: FeatureMeasurement[]
): FeatureMeasurement | undefined {
  return findFeatureMeasurement(featureMeasurements, FeatureMeasurementTypeName.ARRIVAL_TIME);
}

/**
 * Searches Feature Measurements for the ArrivalTime Feature Measurement Value
 * @param featureMeasurements List of feature measurements
 * @returns ArrivalTime FeatureMeasurementValue or undefined if not found
 */
export function findArrivalTimeFeatureMeasurementValue(
  featureMeasurements: FeatureMeasurement[]
): InstantMeasurementValue | undefined {
  const featureMeasurement = findArrivalTimeFeatureMeasurement(featureMeasurements);
  return featureMeasurement
    ? (featureMeasurement.measurementValue as InstantMeasurementValue)
    : undefined;
}

/**
 * Searches Feature Measurements for the Azimuth Feature Measurement
 * @param featureMeasurements List of feature measurements
 * @returns Azimuth FeatureMeasurement or undefined if not found
 */
export function findAzimuthFeatureMeasurement(
  featureMeasurements: FeatureMeasurement[]
): FeatureMeasurement | undefined {
  const azimuthList: FeatureMeasurementTypeName[] = [
    FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH,
    FeatureMeasurementTypeName.SOURCE_TO_RECEIVER_AZIMUTH
  ];
  // Search FeatureMeasurements to find which type of Azimuth was supplied
  return featureMeasurements.find(
    fm => azimuthList.find(azTypeName => azTypeName === fm.featureMeasurementType) !== undefined
  );
}

/**
 * Searches Feature Measurements for the Azimuth Feature Measurement Value
 * @param featureMeasurements List of feature measurements
 * @returns Azimuth FeatureMeasurementValue or undefined if not found
 */
export function findAzimuthFeatureMeasurementValue(
  featureMeasurements: FeatureMeasurement[]
): NumericMeasurementValue | undefined {
  const featureMeasurement = findAzimuthFeatureMeasurement(featureMeasurements);
  return featureMeasurement
    ? (featureMeasurement.measurementValue as NumericMeasurementValue)
    : undefined;
}

/**
 * Searches Feature Measurements for the Slowness Feature Measurement
 * @param featureMeasurements List of feature measurements
 * @returns Slowness FeatureMeasurement or undefined if not found
 */
export function findSlownessFeatureMeasurement(
  featureMeasurements: FeatureMeasurement[]
): FeatureMeasurement | undefined {
  return findFeatureMeasurement(featureMeasurements, FeatureMeasurementTypeName.SLOWNESS);
}

/**
 * Searches Feature Measurements for the Slowness Feature Measurement Value
 * @param featureMeasurements List of feature measurements
 * @returns Slowness FeatureMeasurementValue or undefined if not found
 */
export function findSlownessFeatureMeasurementValue(
  featureMeasurements: FeatureMeasurement[]
): NumericMeasurementValue | undefined {
  const featureMeasurement = findSlownessFeatureMeasurement(featureMeasurements);
  return featureMeasurement
    ? (featureMeasurement.measurementValue as NumericMeasurementValue)
    : undefined;
}

/**
 * Searches Feature Measurements for the Amplitude Feature Measurement
 * @param featureMeasurements List of feature measurements
 * @returns Phase FeatureMeasurement or undefined if not found
 */
export function findAmplitudeFeatureMeasurement(
  featureMeasurements: FeatureMeasurement[],
  amplitudeName: FeatureMeasurementTypeName
): FeatureMeasurement | undefined {
  // Search FeatureMeasurements to find which type of Amplitude was supplied
  return featureMeasurements.find(fm => fm.featureMeasurementType === amplitudeName);
}

/**
 * Searches Feature Measurements for the Amplitude Feature Measurement Value
 * @param featureMeasurements List of feature measurements
 * @returns Phase FeatureMeasurementValue or undefined if not found
 */
export function findAmplitudeFeatureMeasurementValue(
  featureMeasurements: FeatureMeasurement[],
  amplitudeName: FeatureMeasurementTypeName
): AmplitudeMeasurementValue | undefined {
  const featureMeasurement = findAmplitudeFeatureMeasurement(featureMeasurements, amplitudeName);
  return featureMeasurement
    ? (featureMeasurement.measurementValue as AmplitudeMeasurementValue)
    : undefined;
}

/**
 * Determines if any of the feature measurements are amplitude feature measurements
 * @param featureMeasurements list of feature measurements
 * @returns a boolean determining if any feature measurements are type amplitude
 */
export function hasAmplitudeFeatureMeasurement(featureMeasurements: FeatureMeasurement[]): boolean {
  let hasAmplitudeMeasurement = false;
  for (const amplitudeType of amplitudeTypeNameList) {
    if (findAmplitudeFeatureMeasurement(featureMeasurements, amplitudeType)) {
      hasAmplitudeMeasurement = true;
    }
  }
  return hasAmplitudeMeasurement;
}

/**
 * Searches Feature Measurements for the Phase Feature Measurement
 * @param featureMeasurements List of feature measurements
 * @returns Phase FeatureMeasurement or undefined if not found
 */
export function findPhaseFeatureMeasurement(
  featureMeasurements: FeatureMeasurement[]
): FeatureMeasurement | undefined {
  return findFeatureMeasurement(featureMeasurements, FeatureMeasurementTypeName.PHASE);
}

/**
 * Searches Feature Measurements for the Phase Feature Measurement Value
 * @param featureMeasurements List of feature measurements
 * @returns Phase FeatureMeasurementValue or undefined if not found
 */
export function findPhaseFeatureMeasurementValue(
  featureMeasurements: FeatureMeasurement[]
): PhaseTypeMeasurementValue | undefined {
  const featureMeasurement = findPhaseFeatureMeasurement(featureMeasurements);
  return featureMeasurement
    ? (featureMeasurement.measurementValue as PhaseTypeMeasurementValue)
    : undefined;
}

/**
 * Determine if FeatureMeasurement Value structure is a NumericMeasurementValue
 * By looking if the reference time and measurement value are populated
 * @param fmv FeatureMeasurementValue (generic)
 * @return boolean
 */
export function isNumericFeatureMeasurementValueOSD(
  fmv: FeatureMeasurementValueOSD | FeatureMeasurementValue
): fmv is NumericMeasurementValueOSD | FeatureMeasurementValue {
  const referenceTime = (fmv as NumericMeasurementValue).referenceTime;
  const measurementValue = (fmv as NumericMeasurementValue).measurementValue;
  return referenceTime !== undefined && measurementValue !== undefined;
}

/**
 * Determine if FeatureMeasurement Value structure is a NumericMeasurementValue
 * By looking if the reference time and measurement value are populated
 * @param fmv FeatureMeasurementValue (generic)
 * @return boolean
 */
export function isAmplitudeFeatureMeasurementValueOSD(
  fmv: FeatureMeasurementValueOSD | FeatureMeasurementValue
): fmv is AmplitudeMeasurementValueOSD | FeatureMeasurementValue {
  const amplitude = (fmv as AmplitudeMeasurementValue).amplitude;
  const period = (fmv as AmplitudeMeasurementValue).period;
  const startTime = (fmv as AmplitudeMeasurementValue).startTime;
  return amplitude !== undefined && period !== undefined && startTime !== undefined;
}

/**
 * Wraps flat fm type to object
 * @param fmTypeNames fm type names to wrap to send to OSD
 */
export function wrapFeatureMeasurement(fmTypeName: FeatureMeasurementTypeName) {
  return {
    featureMeasurementTypeName: fmTypeName
  };
}
