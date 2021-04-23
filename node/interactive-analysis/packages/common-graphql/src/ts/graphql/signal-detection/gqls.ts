import gql from 'graphql-tag';
import { channelSegmentFragment } from '../channel-segment/gqls';

/**
 * Represents gql fragment for the Feature Measurement Value for a amplitude type.
 */
export const amplitudeMeasurementValueFragment = gql`
  fragment AmplitudeMeasurementValueFragment on AmplitudeMeasurementValue {
    startTime
    period
    amplitude {
      value
      standardDeviation
      units
    }
  }
`;

/**
 * Represents gql fragment for the Feature Measurement Value for a instant type.
 */
export const instantMeasurementValueFragment = gql`
  fragment InstantMeasurementValueFragment on InstantMeasurementValue {
    value
    standardDeviation
  }
`;

/**
 * Represents gql fragment for the Feature Measurement Value for a numeric type.
 */
export const numericMeasurementValueFragment = gql`
  fragment NumericMeasurementValueFragment on NumericMeasurementValue {
    referenceTime
    measurementValue {
      value
      standardDeviation
      units
    }
  }
`;

/**
 * Represents gql fragment for the Feature Measurement Value for a phase type.
 */
export const phaseTypeMeasurementValueFragment = gql`
  fragment PhaseTypeMeasurementValueFragment on PhaseTypeMeasurementValue {
    phase
    confidence
  }
`;
/**
 * Represents gql fragment for the Feature Measurement Value for a numeric type.
 */
export const stringMeasurementValueFragment = gql`
  fragment StringMeasurementValueFragment on StringMeasurementValue {
    strValue
  }
`;

export const featureMeasurementFragment = gql`
  # union FeatureMeasurementValueUnion = FeatureMeasurementStringValue | FeatureMeasurementNumberValue
  fragment FeatureMeasurementFragment on FeatureMeasurement {
    id
    featureMeasurementType
    channelSegment {
      ...ChannelSegmentFragment
    }
    measurementValue {
      ...AmplitudeMeasurementValueFragment
      ...InstantMeasurementValueFragment
      ...NumericMeasurementValueFragment
      ...PhaseTypeMeasurementValueFragment
      ...StringMeasurementValueFragment
    }
  }
  ${numericMeasurementValueFragment}
  ${instantMeasurementValueFragment}
  ${amplitudeMeasurementValueFragment}
  ${phaseTypeMeasurementValueFragment}
  ${stringMeasurementValueFragment}
  ${channelSegmentFragment}
`;

export const signalDetectionHypothesisHistoryFragment = gql`
  fragment SignalDetectionHypothesisHistoryFragment on SignalDetectionHypothesisHistory {
    id
    phase
    rejected
    arrivalTimeSecs
    arrivalTimeUncertainty
  }
`;

export const signalDetectionHypothesisFragment = gql`
  fragment SignalDetectionHypothesisFragment on SignalDetectionHypothesis {
    id
    rejected
    featureMeasurements {
      ...FeatureMeasurementFragment
    }
  }
  ${featureMeasurementFragment}
`;

export const signalDetectionFragment = gql`
  fragment SignalDetectionFragment on SignalDetection {
    id
    monitoringOrganization
    stationName
    currentHypothesis {
      ...SignalDetectionHypothesisFragment
    }
    conflictingHypotheses {
      eventId
      phase
      arrivalTime
    }
    signalDetectionHypothesisHistory {
      ...SignalDetectionHypothesisHistoryFragment
    }
    modified
    hasConflict
    requiresReview {
      amplitudeMeasurement
    }
    reviewed {
      amplitudeMeasurement
    }
  }
  ${signalDetectionHypothesisFragment}
  ${signalDetectionHypothesisHistoryFragment}
`;
