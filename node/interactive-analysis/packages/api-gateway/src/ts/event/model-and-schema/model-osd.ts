import { Location, PhaseType } from '../../common/model';
import { FeatureMeasurementTypeName, FeatureMeasurementValue } from '../../signal-detection/model';
import { FeatureMeasurementOSD, SignalDetectionOSD } from '../../signal-detection/model-osd';
import {
  DepthRestraintType,
  FeaturePredictionComponent,
  LocationRestraint,
  LocationUncertainty,
  MagnitudeModel,
  MagnitudeType,
  RestraintType,
  ScalingFactorType,
  SignalDetectionEventAssociation
} from './model';

/**
 * OSD Representation of a potential seismic event
 */
export interface EventOSD {
  readonly id: string;
  readonly rejectedSignalDetectionAssociations: any[];
  readonly monitoringOrganization: string;
  readonly hypotheses: EventHypothesisOSD[];
  readonly finalEventHypothesisHistory?: FinalEventHypothesisOSD[];
  readonly preferredEventHypothesisHistory: PreferredEventHypothesisOSD[];
}
/**
 * OSD Representation of a finalized hypothesis. OSD only returns id.
 * Not going to make FinalEventHypothesis' fields optional
 */
export interface FinalEventHypothesisOSD {
  readonly eventHypothesisId: string;
}
/**
 * OSD Representation of a preferred event hypothesis.
 * API Gateway model is not a claim check for the eventHypothesis
 */
export interface PreferredEventHypothesisOSD {
  readonly eventHypothesisId: string;
  readonly processingStageId: string;
}
/**
 * OSD representation of a proposed explanation for an event, such that the set of
 * event hypotheses grouped by an Event represents the history of that event.
 */
export interface EventHypothesisOSD {
  readonly id: string;
  readonly rejected: boolean;
  readonly eventId: string;
  readonly parentEventHypotheses: string[];
  readonly locationSolutions: LocationSolutionOSD[];
  readonly preferredLocationSolution: PreferredLocationSolutionOSD;
  readonly associations: SignalDetectionEventAssociation[];
}

/**
 * OSD Event Location definition (the time field is a string not number)
 */
export interface EventLocationOSD {
  readonly latitudeDegrees: number;
  readonly longitudeDegrees: number;
  readonly depthKm: number;
  readonly time: string;
}

/**
 * OSD Preferred Location Solution
 */
export interface PreferredLocationSolutionOSD {
  readonly locationSolution: LocationSolutionOSD;
}

/**
 * LocationSolutionOSD OSD returns Location Solution representation
 */
export interface LocationSolutionOSD {
  readonly id: string;
  readonly location: EventLocationOSD;
  readonly featurePredictions: FeaturePredictionOSD[];
  readonly locationRestraint: LocationRestraint;
  readonly locationUncertainty?: LocationUncertainty;
  readonly networkMagnitudeSolutions?: NetworkMagnitudeSolutionOSD[];
  readonly locationBehaviors: LocationBehaviorOSD[];
}
/**
 * Location Behavior for event Location Solution
 */
export interface LocationBehaviorOSD {
  readonly residual: number;
  readonly weight: number;
  readonly defining: boolean;
  readonly featurePrediction: FeaturePredictionOSD;
  readonly featureMeasurement: FeatureMeasurementOSD;
}

/**
 * FeaturePredictionOSD OSD returns representation
 */
export interface FeaturePredictionOSD {
  readonly phase: string;
  readonly featurePredictionComponents: FeaturePredictionComponent[] /* Java declares as a set not sure in JSON */;
  readonly extrapolated: boolean;
  predictionType: FeatureMeasurementTypeName;
  sourceLocation: EventLocationOSD;
  receiverLocation: Location;
  readonly featurePredictionDerivativeMap?: any;
  readonly channelName?: string;
  readonly predictedValue?: FeatureMeasurementValue;
}

/**
 * Network Magnitude Behavior
 */
export interface NetworkMagnitudeBehaviorOSD {
  readonly defining: boolean;
  readonly stationMagnitudeSolution: StationMagnitudeSolutionOSD;
  readonly residual: number;
  readonly weight: number;
}

/**
 * Represents an estimate of an event's magnitude based on detections from multiple stations.
 */
export interface NetworkMagnitudeSolutionOSD {
  readonly uncertainty: number;
  readonly magnitudeType: MagnitudeType;
  readonly magnitude: number;
  readonly networkMagnitudeBehaviors: NetworkMagnitudeBehaviorOSD[];
}

/**
 * Station Magnitude Solution
 */
export interface StationMagnitudeSolutionOSD {
  readonly type: MagnitudeType;
  readonly model: MagnitudeModel;
  readonly stationName: string;
  readonly phase: PhaseType;
  readonly magnitude: number;
  readonly magnitudeUncertainty: number;
  readonly modelCorrection: number;
  readonly stationCorrection: number;
  readonly measurement: FeatureMeasurementOSD;
}

/**
 * Query for locate event
 */
export interface LocateEventQueryOSD {
  readonly eventHypotheses: EventHypothesisOSD[];
  readonly signalDetections: SignalDetectionOSD[];
  readonly parameters: LocateEventParametersOSD;
}

/**
 * Subfields for locate event
 */
export interface LocateEventParametersOSD {
  readonly pluginName: string;
  readonly eventHypothesisToEventLocatorPluginConfigurationOptionMap: {
    [id: string]: EventLocationDefinitionAndFieldMapOSD;
  };
}

/**
 * Contains event location definitions and a fieldmap for locate
 */
export interface EventLocationDefinitionAndFieldMapOSD {
  readonly eventLocationDefinition: EventLocationDefinitionOSD;
  readonly fieldMap: any;
}

/**
 * Defines the location for an event
 */
export interface EventLocationDefinitionOSD {
  readonly maximumIterationCount: number;
  readonly convergenceThreshold: number;
  readonly uncertaintyProbabilityPercentile: number;
  readonly earthModel: string;
  readonly applyTravelTimeCorrections: boolean;
  readonly scalingFactorType: ScalingFactorType;
  readonly kWeight: number;
  readonly aprioriVariance: number;
  readonly minimumNumberOfObservations: number;
  readonly enableArrivalTimeOutlierCheck: boolean;
  readonly arrivalTimeOutlierStdDevMultiplier: number;
  readonly enableSlownessOutlierCheck: boolean;
  readonly slownessOutlierStdDevMultiplier: number;
  readonly enableAzimuthOutlierCheck: boolean;
  readonly azimuthOutlierStdDevMultiplier: number;
  // signal detection hypothesis id to behavior map
  readonly signalDetectionBehaviorsMap: { [id: string]: DefinitionMapContainer };
  readonly locationRestraints: LocationRestraintOSD[];
}

/**
 * Wrapper around the definition map object for locate osd calls
 */
export interface DefinitionMapContainer {
  readonly definitionMap: DefinitionMapOSD;
}

/**
 * Convenience type for mapping to definition settings
 */
export interface DefinitionMapOSD {
  [id: string]: DefinitionSettingsOSD;
}

/**
 * Defining settings for event locate
 */
export interface DefinitionSettingsOSD {
  readonly defining: boolean;
  readonly systemOverridable: boolean;
  readonly overrideThreshold: number;
}

/**
 * Location restraints for osd locate call
 */
export interface LocationRestraintOSD {
  readonly latitudeRestraintType: RestraintType;
  readonly latitudeRestraintDegrees: undefined;
  readonly longitudeRestraintType: RestraintType;
  readonly longitudeRestraintDegrees: undefined;
  readonly depthRestraintType: DepthRestraintType;
  readonly depthRestraintKm: number | undefined;
  readonly timeRestraintType: RestraintType;
  readonly timeRestraint: undefined;
}
