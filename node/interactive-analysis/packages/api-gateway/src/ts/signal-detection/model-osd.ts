import { PhaseType } from '../common/model';
import { ProcessingChannel } from '../station/processing-station/model';
import * as model from './model';

/**
 * Model definitions for the signal detection data API
 */

/**
 * Represents a signal detection marking the arrival of a signal of interest on
 * channel within a time interval.
 */
export interface SignalDetectionOSD {
  readonly id: string;
  readonly monitoringOrganization: string;
  readonly stationName: string;
  readonly signalDetectionHypotheses: SignalDetectionHypothesisOSD[];
}

/**
 * Represents a proposed explanation for a Signal Detection
 */
export interface SignalDetectionHypothesisOSD {
  readonly id: string;
  readonly parentSignalDetectionId: string;
  readonly monitoringOrganization: string;
  readonly stationName: string;
  readonly parentSignalDetectionHypothesisId?: string;
  readonly rejected: boolean;
  readonly featureMeasurements: FeatureMeasurementOSD[];
}
/**
 * Represents a measurement of a signal detection feature,
 * including arrival time, azimuth, slowness and phase
 */
export interface FeatureMeasurementOSD {
  readonly channel: ProcessingChannel;
  readonly measuredChannelSegmentDescriptor: MeasuredChannelSegmentDescriptorOSD;
  readonly measurementValue: FeatureMeasurementValueOSD;
  readonly featureMeasurementType: model.FeatureMeasurementTypeName;
}

/**
 * Represents Feature Measurement Value (fields are dependent on type of FM)
 */
// tslint:disable-next-line:no-empty-interface
export interface FeatureMeasurementValueOSD {
  // no common members
}

/**
 * Channel Segment Descriptor (this replaces ChannelSegment Id in FeatureMeasurement)
 */
export interface MeasuredChannelSegmentDescriptorOSD {
  readonly channelName: string;
  readonly measuredChannelSegmentStartTime: string;
  readonly measuredChannelSegmentEndTime: string;
  readonly measuredChannelSegmentCreationTime: string;
}
/**
 * Represents Feature Measurement Value for a amplitude type.
 */
export interface AmplitudeMeasurementValueOSD extends FeatureMeasurementValueOSD {
  readonly startTime: string;
  readonly period: string;
  readonly amplitude: model.DoubleValue;
}

/**
 * Represents Feature Measurement Value for a phase type.
 */
export interface PhaseTypeMeasurementValueOSD extends FeatureMeasurementValueOSD {
  readonly value: PhaseType;
  readonly confidence: number;
}

/**
 * Represents Feature Measurement Value with a generic string
 */
export interface StringMeasurementValueOSD extends FeatureMeasurementValueOSD {
  readonly strValue: string;
}

/**
 * Represents Feature Measurement Value for a instant type.
 */
export interface InstantMeasurementValueOSD extends FeatureMeasurementValueOSD {
  readonly value: string;
  readonly standardDeviation: string;
}

/**
 * Represents Feature Measurement Value for a numeric type.
 */
export interface NumericMeasurementValueOSD extends FeatureMeasurementValueOSD {
  readonly referenceTime: string;
  readonly measurementValue: model.DoubleValue;
}
