import gql from 'graphql-tag';
import {
  amplitudeMeasurementValueFragment,
  instantMeasurementValueFragment,
  numericMeasurementValueFragment,
  phaseTypeMeasurementValueFragment,
  stringMeasurementValueFragment
} from '../signal-detection/gqls';

export const networkMagnitudeSolutionFragment = gql`
  fragment NetworkMagnitudeSolutionFragment on NetworkMagnitudeSolution {
    uncertainty
    magnitudeType
    magnitude
    networkMagnitudeBehaviors {
      defining
      stationMagnitudeSolution {
        type
        model
        stationName
        phase
        magnitude
        magnitudeUncertainty
        modelCorrection
        stationCorrection
      }
      residual
      weight
    }
  }
`;

export const signalDetectionEventAssociationFragment = gql`
  fragment SignalDetectionEventAssociationFragment on SignalDetectionEventAssociation {
    id
    rejected
    eventHypothesisId
    signalDetectionHypothesis {
      id
      rejected
      parentSignalDetectionId
    }
  }
`;

export const eventLocationFragment = gql`
  fragment EventLocationFragment on EventLocation {
    latitudeDegrees
    longitudeDegrees
    depthKm
    time
  }
`;

export const locationSolutionFragment = gql`
  fragment LocationSolutionFragment on LocationSolution {
    id
    locationType
    location {
      ...EventLocationFragment
    }
    featurePredictions {
      predictedValue {
        ...AmplitudeMeasurementValueFragment
        ...InstantMeasurementValueFragment
        ...NumericMeasurementValueFragment
        ...PhaseTypeMeasurementValueFragment
        ...StringMeasurementValueFragment
      }
      predictionType
      phase
      channelName
      stationName
    }
    locationRestraint {
      depthRestraintType
      depthRestraintKm
      latitudeRestraintType
      latitudeRestraintDegrees
      longitudeRestraintType
      longitudeRestraintDegrees
      timeRestraintType
      timeRestraint
    }
    locationUncertainty {
      xy
      xz
      xt
      yy
      yz
      yt
      zz
      zt
      tt
      stDevOneObservation
      ellipses {
        scalingFactorType
        kWeight
        confidenceLevel
        majorAxisLength
        majorAxisTrend
        minorAxisLength
        minorAxisTrend
        depthUncertainty
        timeUncertainty
      }
      ellipsoids {
        scalingFactorType
        kWeight
        confidenceLevel
        majorAxisLength
        majorAxisTrend
        majorAxisPlunge
        intermediateAxisLength
        intermediateAxisTrend
        intermediateAxisPlunge
        minorAxisLength
        minorAxisTrend
        minorAxisPlunge
        depthUncertainty
        timeUncertainty
      }
    }
    locationBehaviors {
      residual
      weight
      defining
      featureMeasurementType
      signalDetectionId
    }
    networkMagnitudeSolutions {
      ...NetworkMagnitudeSolutionFragment
    }
    snapshots {
      signalDetectionId
      signalDetectionHypothesisId
      stationName
      channelName
      phase
      time {
        defining
        observed
        residual
        correction
      }
      slowness {
        defining
        observed
        residual
        correction
      }
      azimuth {
        defining
        observed
        residual
        correction
      }
      aFiveAmplitude {
        period
        amplitudeValue
      }
      aLRAmplitude {
        period
        amplitudeValue
      }
    }
    locationToStationDistances {
      distance {
        degrees
        km
      }
      azimuth
      stationId
    }
  }
  ${eventLocationFragment}
  ${numericMeasurementValueFragment}
  ${instantMeasurementValueFragment}
  ${amplitudeMeasurementValueFragment}
  ${phaseTypeMeasurementValueFragment}
  ${stringMeasurementValueFragment}
  ${networkMagnitudeSolutionFragment}
`;

export const locationSolutionSetFragment = gql`
  fragment LocationSolutionSetFragment on LocationSolutionSet {
    id
    count
    locationSolutions {
      ...LocationSolutionFragment
    }
  }
  ${locationSolutionFragment}
`;

export const preferredLocationSolutionFragment = gql`
  fragment PreferredLocationSolutionFragment on PreferredLocationSolution {
    locationSolution {
      ...LocationSolutionFragment
    }
  }
  ${locationSolutionFragment}
`;

export const eventHypothesisFragment = gql`
  fragment EventHypothesisFragment on EventHypothesis {
    id
    rejected
    event {
      id
      status
      modified
      hasConflict
    }
    preferredLocationSolution {
      ...PreferredLocationSolutionFragment
    }
    associationsMaxArrivalTime
    signalDetectionAssociations {
      ...SignalDetectionEventAssociationFragment
    }
    locationSolutionSets {
      ...LocationSolutionSetFragment
    }
  }
  ${signalDetectionEventAssociationFragment}
  ${locationSolutionSetFragment}
  ${preferredLocationSolutionFragment}
`;

export const preferredHypothesisFragment = gql`
  fragment PreferredEventHypothesisFragment on PreferredEventHypothesis {
    processingStage {
      id
    }
    eventHypothesis {
      ...EventHypothesisFragment
    }
  }
  ${eventHypothesisFragment}
`;

export const eventFragment = gql`
  fragment EventFragment on Event {
    id
    status
    modified
    hasConflict
    currentEventHypothesis {
      ...PreferredEventHypothesisFragment
    }
    conflictingSdIds
  }
  ${preferredHypothesisFragment}
`;
