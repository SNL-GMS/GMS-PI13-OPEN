import { getDurationTime, setDurationTime, toEpochSeconds, toOSDTime } from '@gms/common-util';
import sha256 from 'js-sha256';
import cloneDeep from 'lodash/cloneDeep';
import {
  AmplitudeMeasurementValue,
  FeatureMeasurement,
  FeatureMeasurementTypeName,
  FeatureMeasurementValue,
  InstantMeasurementValue,
  MeasuredChannelSegmentDescriptor,
  NumericMeasurementValue,
  PhaseTypeMeasurementValue
} from '../signal-detection/model';
import {
  AmplitudeMeasurementValueOSD,
  FeatureMeasurementOSD,
  FeatureMeasurementValueOSD,
  InstantMeasurementValueOSD,
  NumericMeasurementValueOSD,
  PhaseTypeMeasurementValueOSD
} from '../signal-detection/model-osd';
import {
  isAmplitudeFeatureMeasurementValueOSD,
  isNumericFeatureMeasurementValueOSD
} from './feature-measurement-utils';

// ========= Convert FMs from OSD ============
/**
 * Converts and arrival time feature measurement value to the gateway format
 * @param fmvOSD the osd feature measurement value
 */
export function convertArrivalTimeFMVFromOSD(
  fmvOSD: InstantMeasurementValueOSD
): InstantMeasurementValue {
  return {
    value: toEpochSeconds(fmvOSD.value),
    // as unknown as string
    standardDeviation: getDurationTime(fmvOSD.standardDeviation)
  };
}

/**
 * Convert a numeric feature measurement value to the gateway format
 * @param fmvOSD the osd feature measurement value
 */
export function convertNumericFMVFromOSD(
  fmvOSD: NumericMeasurementValueOSD
): NumericMeasurementValue {
  return {
    ...fmvOSD,
    referenceTime: toEpochSeconds(fmvOSD.referenceTime)
  };
}

/**
 * Converts an amplitude feature measurement value to the gateway format
 */
export function convertAmplitudeFMVFromOSD(
  fmvOSD: AmplitudeMeasurementValueOSD
): AmplitudeMeasurementValue {
  return {
    amplitude: {
      ...fmvOSD.amplitude,
      standardDeviation: isNaN(fmvOSD.amplitude.standardDeviation)
        ? 0
        : fmvOSD.amplitude.standardDeviation
    },
    startTime: toEpochSeconds(fmvOSD.startTime),
    period: getDurationTime(fmvOSD.period)
  };
}

/**
 * Converts a phase feature measurement value to the gateway format
 */
export function convertPhaseFMVFromOSD(
  fmvOSD: PhaseTypeMeasurementValueOSD
): PhaseTypeMeasurementValue {
  return {
    confidence: fmvOSD.confidence,
    phase: fmvOSD.value
  };
}

/**
 * Converts a feature measurement from the OSD to the gateway format
 * @param fmOSD the osd feature measurement
 */
export function convertFeatureMeasurementFromOSD(fmOSD: FeatureMeasurementOSD): FeatureMeasurement {
  const fmOSDClone = cloneDeep(fmOSD);
  const fmOSDCloneType = fmOSDClone.featureMeasurementType;
  const fmvOSD: FeatureMeasurementValueOSD = fmOSDClone.measurementValue;
  const convertedFMV: FeatureMeasurementValue =
    fmOSDCloneType === FeatureMeasurementTypeName.ARRIVAL_TIME
      ? convertArrivalTimeFMVFromOSD(fmvOSD as InstantMeasurementValueOSD)
      : fmOSDClone.featureMeasurementType === FeatureMeasurementTypeName.PHASE
      ? convertPhaseFMVFromOSD((fmvOSD as any) as PhaseTypeMeasurementValueOSD)
      : isNumericFeatureMeasurementValueOSD(fmvOSD)
      ? convertNumericFMVFromOSD((fmvOSD as any) as NumericMeasurementValueOSD)
      : isAmplitudeFeatureMeasurementValueOSD(fmvOSD)
      ? convertAmplitudeFMVFromOSD((fmvOSD as any) as AmplitudeMeasurementValueOSD)
      : fmOSDClone.measurementValue;

  const measuredChannelSegmentDescriptor = {
    channelName: fmOSDClone.measuredChannelSegmentDescriptor.channelName,
    measuredChannelSegmentCreationTime: toEpochSeconds(
      fmOSDClone.measuredChannelSegmentDescriptor.measuredChannelSegmentCreationTime
    ),
    measuredChannelSegmentStartTime: toEpochSeconds(
      fmOSDClone.measuredChannelSegmentDescriptor.measuredChannelSegmentStartTime
    ),
    measuredChannelSegmentEndTime: toEpochSeconds(
      fmOSDClone.measuredChannelSegmentDescriptor.measuredChannelSegmentEndTime
    )
  };
  const fm: FeatureMeasurement = {
    channel: fmOSDClone.channel,
    measuredChannelSegmentDescriptor,
    featureMeasurementType: fmOSDClone.featureMeasurementType,
    measurementValue: convertedFMV,
    id: createFeatureMeasurementId(
      measuredChannelSegmentDescriptor,
      fmOSDClone.featureMeasurementType
    )
  };

  return fm;
}

// ========= Convert FMs to OSD ============
/**
 * Converts an arrival time feature measurement to the osd format
 */
export function convertArrivalTimeFMVToOSD(
  fmv: InstantMeasurementValue
): InstantMeasurementValueOSD {
  return {
    value: toOSDTime(fmv.value),
    // remove set time
    standardDeviation: setDurationTime(fmv.standardDeviation)
  };
}

/**
 * Converts an numeric feature measurement to the osd format
 */
export function convertNumericFMVToOSD(fmv: NumericMeasurementValue): NumericMeasurementValueOSD {
  return {
    ...fmv,
    referenceTime: toOSDTime(fmv.referenceTime)
  };
}

/**
 * Converts an amplitude time feature measurement to the osd format
 */
export function convertAmplitudeFMVToOSD(
  fmv: AmplitudeMeasurementValue
): AmplitudeMeasurementValueOSD {
  return {
    ...fmv,
    startTime: toOSDTime(fmv.startTime),
    period: setDurationTime(fmv.period)
  };
}

/**
 * Converts a phase time feature measurement to the osd format
 */
export function convertPhaseFMVToOSD(fmv: PhaseTypeMeasurementValue): PhaseTypeMeasurementValueOSD {
  return {
    confidence: fmv.confidence,
    value: fmv.phase
  };
}

/**
 * Converts API Gateway FeatureMeasurement to OSD format to send to OSD
 * @param fm to convert
 */
export function convertFeatureMeasurementToOSD(fm: FeatureMeasurement): FeatureMeasurementOSD {
  const fmClone = cloneDeep(fm);
  const fmv = fmClone.measurementValue;
  const fmType = fmClone.featureMeasurementType;

  const fmvOSD: FeatureMeasurementValueOSD =
    fmType === FeatureMeasurementTypeName.ARRIVAL_TIME
      ? convertArrivalTimeFMVToOSD(fmv as InstantMeasurementValue)
      : fmType === FeatureMeasurementTypeName.PHASE
      ? convertPhaseFMVToOSD((fmv as any) as PhaseTypeMeasurementValue)
      : isNumericFeatureMeasurementValueOSD(fmv)
      ? convertNumericFMVToOSD((fmv as any) as NumericMeasurementValue)
      : isAmplitudeFeatureMeasurementValueOSD(fmv)
      ? convertAmplitudeFMVToOSD((fmv as any) as AmplitudeMeasurementValue)
      : fmClone.measurementValue;
  return {
    channel: fm.channel,
    measuredChannelSegmentDescriptor: {
      channelName: fm.measuredChannelSegmentDescriptor.channelName,
      measuredChannelSegmentCreationTime: toOSDTime(
        fm.measuredChannelSegmentDescriptor.measuredChannelSegmentCreationTime
      ),
      measuredChannelSegmentStartTime: toOSDTime(
        fm.measuredChannelSegmentDescriptor.measuredChannelSegmentStartTime
      ),
      measuredChannelSegmentEndTime: toOSDTime(
        fm.measuredChannelSegmentDescriptor.measuredChannelSegmentEndTime
      )
    },
    featureMeasurementType: fm.featureMeasurementType,
    measurementValue: fmvOSD
  };
}

// ========= Other FM Methods ============
/**
 * Creates an ID based on the values inside the feature measurement
 * @param featureMeasurement fm to hash for id
 *
 * @returns sha256 id
 */
export function createFeatureMeasurementId(
  measuredChannelSegmentDescriptor: MeasuredChannelSegmentDescriptor,
  featureMeasurementType: FeatureMeasurementTypeName
): string {
  const objectBytes = [];
  if (measuredChannelSegmentDescriptor) {
    objectBytes.push(...new Uint8Array(Buffer.from(measuredChannelSegmentDescriptor.channelName)));
    objectBytes.push(
      ...new Uint8Array(
        Buffer.from(String(measuredChannelSegmentDescriptor.measuredChannelSegmentStartTime))
      )
    );
    objectBytes.push(
      ...new Uint8Array(
        Buffer.from(String(measuredChannelSegmentDescriptor.measuredChannelSegmentEndTime))
      )
    );
    objectBytes.push(
      ...new Uint8Array(
        Buffer.from(String(measuredChannelSegmentDescriptor.measuredChannelSegmentCreationTime))
      )
    );
  }
  objectBytes.push(...new Uint8Array(Buffer.from(featureMeasurementType)));
  const newId = sha256.sha256(objectBytes);
  return newId;
}
