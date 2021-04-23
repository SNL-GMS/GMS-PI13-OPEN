import { Association, PhaseType, Units } from '../common/model';
import { ProcessingChannel } from '../station/processing-station/model';

/**
 * Model definitions for the signal detection data API
 */

/**
 * Represents a signal detection marking the arrival of a signal of interest on
 * channel within a time interval.
 */
export interface SignalDetection {
  readonly id: string;
  readonly monitoringOrganization: string;
  readonly stationName: string;
  readonly signalDetectionHypotheses: SignalDetectionHypothesis[];
  currentHypothesis: SignalDetectionHypothesis;
  associations: Association[];
  hasConflict: boolean;
}

/**
 * Represents all the UI objects that require a review
 */
export interface Reviewed {
  readonly amplitudeMeasurement: boolean;
}

/**
 * Represents all the UI objects that require a review
 */
export interface RequiresReview {
  readonly amplitudeMeasurement: boolean;
}

/**
 * Input object for service to save
 */
export interface SaveSignalDetectionHypthesesServiceInput {
  readonly signalDetectionHypothesis: SignalDetectionHypothesis;
  readonly stationId: string;
}

/**
 * Represents a proposed explanation for a Signal Detection
 */
export interface SignalDetectionHypothesis {
  readonly id: string;
  readonly parentSignalDetectionId: string;
  readonly monitoringOrganization: string;
  readonly stationName: string;
  readonly parentSignalDetectionHypothesisId?: string;
  readonly rejected: boolean;
  readonly featureMeasurements: FeatureMeasurement[];

  // API Gateway fields (remove before saving)
  readonly modified: boolean;
  readonly reviewed: Reviewed;
}

/**
 * SignalDetectionHypothesisHistory used by SD History Table
 */
export interface SignalDetectionHypothesisHistory {
  readonly id: string;
  readonly phase: string;
  readonly rejected: boolean;
  readonly arrivalTimeSecs: number;
  readonly arrivalTimeUncertainty: number;
}

/**
 * Represents a measurement of a signal detection feature,
 * including arrival time, azimuth, slowness and phase
 */
export interface FeatureMeasurement {
  readonly id: string;
  readonly channel: ProcessingChannel;
  readonly measuredChannelSegmentDescriptor: MeasuredChannelSegmentDescriptor;
  readonly measurementValue: FeatureMeasurementValue;
  readonly featureMeasurementType: FeatureMeasurementTypeName;
}

/**
 * Channel Segment Descriptor (this replaces ChannelSegment Id in FeatureMeasurement)
 */
export interface MeasuredChannelSegmentDescriptor {
  readonly channelName: string;
  readonly measuredChannelSegmentStartTime: number;
  readonly measuredChannelSegmentEndTime: number;
  readonly measuredChannelSegmentCreationTime: number;
}

/**
 * Holds a featureMeasurementTypeName
 */

export interface FeatureMeasurementTypeNameField {
  readonly featureMeasurementTypeName: FeatureMeasurementTypeName;
}

/**
 * Represents Feature Measurement Value (fields are dependent on type of FM)
 */
// tslint:disable-next-line:no-empty-interface
export interface FeatureMeasurementValue {
  // no common members
}
/**
 * Value used in Feature Measurements
 */
export interface DoubleValue {
  readonly value: number;
  readonly standardDeviation: number;
  readonly units: Units;
}
/**
 * Represents Feature Measurement Value for a amplitude type.
 */
export interface AmplitudeMeasurementValue extends FeatureMeasurementValue {
  readonly startTime: number;
  readonly period: number;
  readonly amplitude: DoubleValue;
}

/**
 * Represents Feature Measurement Value for a instant type.
 */
export interface InstantMeasurementValue extends FeatureMeasurementValue {
  readonly value: number;
  readonly standardDeviation: number;
}

/**
 * Represents Feature Measurement Value for a numeric type.
 */
export interface NumericMeasurementValue extends FeatureMeasurementValue {
  readonly referenceTime: number;
  readonly measurementValue: DoubleValue;
}

/**
 * Represents Feature Measurement Value for a phase type.
 */
export interface PhaseTypeMeasurementValue extends FeatureMeasurementValue {
  readonly phase: PhaseType;
  readonly confidence: number;
}

/**
 * Represents Feature Measurement Value with a generic string
 */
export interface StringMeasurementValue extends FeatureMeasurementValue {
  readonly strValue: string;
}

/**
 * Basic info for a hypothesis
 */
export interface ConflictingSdHypData {
  readonly eventId: string;
  readonly phase: PhaseType;
  readonly arrivalTime: number;
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
  AMPLITUDE_ANL_OVER_2 = 'AMPLITUDE_ANL_OVER_2',
  AMPLITUDE_SBSNR = 'AMPLITUDE_SBSNR',
  AMPLITUDE_FKSNR = 'AMPLITUDE_FKSNR',
  FILTERED_BEAM = 'FILTERED_BEAM'

  /* Following have been copied from latest OSD java code: 11/09/2018
    SOURCE_TO_RECEIVER_DISTANCE,
    FIRST_MOTION,
    TODO: the following values are not in the guidance. find out if they are deprecated, or should be removed
    F_STATISTIC,
    FK_QUALITY,

    Saw these in FP Service Log as to acceptable entries
    SIGNAL_DURATION,
    */
}

/**
 * Enumeration of operation types used in defining rules
 */
export enum DefiningOperationType {
  Location = 'Location',
  Magnitude = 'Magnitude'
}

/**
 * Represents the defining relationship (isDefining: true|false) for an operation type (e.g. location, magnitude)
 */
export interface DefiningRule {
  readonly operationType: DefiningOperationType;
  readonly isDefining: boolean;
}

/**
 * Signal Detection Timing. Input object that groups ArrivalTime and AmplitudeMeasurement
 */
export interface SignalDetectionTimingInput {
  // The detection time (seconds since epoch) to assign to the new detection's initial hypothesis
  readonly arrivalTime: number;

  // The uncertainty (seconds) associated with the time input
  readonly timeUncertaintySec: number;

  // The Amplitude Measurement Value
  readonly amplitudeMeasurement?: AmplitudeMeasurementValue;
}
/**
 * Input used to create a new signal detection with an initial hypothesis
 * and time feature measurement
 */
export interface NewDetectionInput {
  readonly stationId: string;
  readonly phase: string;
  // Signal Detection Timing Input for ArrivalTime and Amplitude Measurement
  readonly signalDetectionTiming: SignalDetectionTimingInput;
  readonly eventId?: string;
}

/**
 * Input used to update an existing signal detection
 */
export interface UpdateDetectionInput {
  readonly phase?: string;
  // Signal Detection Timing Input for ArrivalTime and Amplitude Measurement
  readonly signalDetectionTiming?: SignalDetectionTimingInput;
}
