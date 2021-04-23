import { QueryControls } from 'react-apollo';
import { DataPayload } from '../cache/types';
import { ChannelSegment, TimeSeries } from '../channel-segment/types';
import { PhaseType, TimeRange, Units } from '../common/types';

// ***************************************
// Mutations
// ***************************************

/**
 * Signal Detection Timing. Input object that groups ArrivalTime and AmplitudeMeasurement
 */
export interface SignalDetectionTimingInput {
  // The detection time (seconds since epoch) to assign to the new detection's initial hypothesis
  arrivalTime: number;

  // The uncertainty (seconds) associated with the time input
  timeUncertaintySec: number;

  // The Amplitude Measurement Value
  amplitudeMeasurement?: AmplitudeMeasurementValue;
}

/**
 * Input used to create a new signal detection with an initial hypothesis
 * and time feature measurement
 */
export interface NewDetectionInput {
  stationId: string;
  phase: string;

  // Signal Detection Timing Input for ArrivalTime and AmplitudeMeasurement
  signalDetectionTiming: SignalDetectionTimingInput;
  eventId?: string;
}

/**
 * Input used to update an existing signal detection
 */
export interface UpdateDetectionInput {
  phase?: string;

  // Signal Detection Timing Input for ArrivalTime and AmplitudeMeasurement
  signalDetectionTiming?: SignalDetectionTimingInput;
}

export interface CreateDetectionMutationArgs {
  input: NewDetectionInput;
}

export interface CreateDetectionMutationData {
  createDetection: DataPayload;
}

export interface CreateDetectionMutationResult {
  data: CreateDetectionMutationData;
}

export interface UpdateDetectionsMutationArgs {
  detectionIds: string[];
  input: UpdateDetectionInput;
}

export interface UpdateDetectionsMutationData {
  updateDetections: DataPayload;
}

export interface UpdateDetectionsMutationResult {
  data: UpdateDetectionsMutationData;
}

export interface RejectDetectionsMutationArgs {
  detectionIds: string[];
}

export interface RejectDetectionsMutationData {
  rejectDetections: DataPayload;
}

export interface RejectDetectionsMutationResult {
  data: RejectDetectionsMutationData;
}

export interface MarkAmplitudeMeasurementReviewedMutationArgs {
  signalDetectionIds: string[];
}

export interface MarkAmplitudeMeasurementReviewedMutationData {
  markAmplitudeMeasurementReviewed: DataPayload;
}

export interface MarkAmplitudeMeasurementReviewedMutationResult {
  data: MarkAmplitudeMeasurementReviewedMutationData;
}

// ***************************************
// Subscriptions
// ***************************************

export interface DetectionsCreatedSubscription {
  detectionsCreated: SignalDetection[];
}

// ***************************************
// Queries
// ***************************************

export interface SignalDetectionsByStationQueryArgs {
  stationIds: string[];
  timeRange: TimeRange;
}

// tslint:disable-next-line:max-line-length interface-over-type-literal
export type SignalDetectionsByStationQueryProps = {
  signalDetectionsByStationQuery: QueryControls<{}> & {
    signalDetectionsByStation: SignalDetection[];
  };
};

export interface SignalDetectionsByEventQueryArgs {
  eventId: string;
}

// tslint:disable-next-line:max-line-length interface-over-type-literal
export type SignalDetectionsByEventQueryProps = {
  signalDetectionsByEventIdQuery: QueryControls<{}> & {
    signalDetectionsByEventId: SignalDetection[];
  };
};

// ***************************************
// Model
// ***************************************

/**
 * Represents a measurement of a signal detection feature,
 * including arrival time, azimuth, slowness and phase
 */
export interface FeatureMeasurement {
  id: string;
  channelSegment?: ChannelSegment<TimeSeries>;
  measurementValue: FeatureMeasurementValue;
  featureMeasurementType: FeatureMeasurementTypeName;
}

/**
 * Represents Feature Measurement Value (fields are dependent on type of FM)
 */
// tslint:disable-next-line:no-empty-interface
export interface FeatureMeasurementValue {
  // no common members
}

/**
 * Represents Feature Measurement Value for a double type.
 */
export interface DoubleValue {
  value: number;
  standardDeviation: number;
  units: Units;
}

/**
 * Represents Feature Measurement Value for a amplitude type.
 */
export interface AmplitudeMeasurementValue extends FeatureMeasurementValue {
  startTime: number;
  period: number;
  amplitude: DoubleValue;
}

/**
 * Represents Feature Measurement Value for a instant type.
 */
export interface InstantMeasurementValue extends FeatureMeasurementValue {
  value: number;
  standardDeviation: number;
}

/**
 * Represents Feature Measurement Value for a numeric type.
 */
export interface NumericMeasurementValue extends FeatureMeasurementValue {
  referenceTime: number;
  measurementValue: DoubleValue;
}

/**
 * Represents Feature Measurement Value for a phase type.
 */
export interface PhaseTypeMeasurementValue extends FeatureMeasurementValue {
  phase: PhaseType;
  confidence: number;
}

/**
 * Represents Feature Measurement Value for a numeric type.
 */
export interface StringMeasurementValue extends FeatureMeasurementValue {
  strValue: string;
}

export enum AmplitudeType {
  AMPLITUDE_A5_OVER_2 = 'AMPLITUDE_A5_OVER_2',
  AMPLITUDE_A5_OVER_2_OR = 'AMPLITUDE_A5_OVER_2_OR',
  AMPLITUDE_ALR_OVER_2 = 'AMPLITUDE_ALR_OVER_2',
  AMPLITUDEh_ALR_OVER_2 = 'AMPLITUDEh_ALR_OVER_2',
  AMPLITUDE_ANL_OVER_2 = 'AMPLITUDE_ANL_OVER_2',
  AMPLITUDE_SBSNR = 'AMPLITUDE_SBSNR',
  AMPLITUDE_FKSNR = 'AMPLITUDE_FKSNR'
}

/**
 * Enumeration of feature measurement type names
 */
export enum FeatureMeasurementTypeName {
  ARRIVAL_TIME = 'ARRIVAL_TIME',
  RECEIVER_TO_SOURCE_AZIMUTH = 'RECEIVER_TO_SOURCE_AZIMUTH',
  SOURCE_TO_RECEIVER_AZIMUTH = 'SOURCE_TO_RECEIVER_AZIMUTH',
  SLOWNESS = 'SLOWNESS',
  PHASE = 'PHASE',
  EMERGENCE_ANGLE = 'EMERGENCE_ANGLE',
  PERIOD = 'PERIOD',
  RECTILINEARITY = 'RECTILINEARITY',
  SNR = 'SNR',
  AMPLITUDE = 'AMPLITUDE',
  AMPLITUDE_A5_OVER_2 = 'AMPLITUDE_A5_OVER_2',
  AMPLITUDE_A5_OVER_2_OR = 'AMPLITUDE_A5_OVER_2_OR',
  AMPLITUDE_ALR_OVER_2 = 'AMPLITUDE_ALR_OVER_2',
  AMPLITUDEh_ALR_OVER_2 = 'AMPLITUDEh_ALR_OVER_2',
  AMPLITUDE_ANL_OVER_2 = 'AMPLITUDE_ANL_OVER_2',
  AMPLITUDE_SBSNR = 'AMPLITUDE_SBSNR',
  AMPLITUDE_FKSNR = 'AMPLITUDE_FKSNR',
  FILTERED_BEAM = 'FILTERED_BEAM'
}
export interface SignalDetectionHypothesis {
  id: string;
  rejected: boolean;
  featureMeasurements: FeatureMeasurement[];
}

/**
 * SignalDetectionHypothesisHistory used by SD History Table
 */
export interface SignalDetectionHypothesisHistory {
  id: string;
  phase: string;
  rejected: boolean;
  arrivalTimeSecs: number;
  arrivalTimeUncertainty: number;
}

/**
 * Represents objects that require a review
 */
export interface RequiresReview {
  amplitudeMeasurement: boolean;
}

/**
 * Represents objects that have been reviewed
 */
export interface Reviewed {
  amplitudeMeasurement: boolean;
}

/**
 * Represents a Signal detection
 */
export interface SignalDetection {
  id: string;
  monitoringOrganization: string;
  stationName: string;
  currentHypothesis: SignalDetectionHypothesis;
  signalDetectionHypothesisHistory: SignalDetectionHypothesisHistory[];
  modified: boolean;
  hasConflict: boolean;
  requiresReview: RequiresReview;
  reviewed: Reviewed;
  conflictingHypotheses: ConflictingSdHypData[];
}

/**
 * Basic info for a hypothesis
 */
export interface ConflictingSdHypData {
  eventId: string;
  phase: PhaseType;
  arrivalTime: number;
  stationName?: string;
  eventTime?: number;
}
