import { DataPayload } from 'src/ts/cache/model';
import { Association, Distance, Location, PhaseType, Units } from '../../common/model';
import {
  FeatureMeasurement,
  FeatureMeasurementTypeName,
  FeatureMeasurementValue
} from '../../signal-detection/model';
import { SignalDetectionHypothesisOSD } from '../../signal-detection/model-osd';
import { ProcessingChannel } from '../../station/processing-station/model';
import { EventHypothesisOSD, LocationSolutionOSD, NetworkMagnitudeSolutionOSD } from './model-osd';

/**
 * Model definitions for the event-related data API
 */

/**
 * The enumerated status of an event
 */
// TODO In OSD, this class DNE
export enum EventStatus {
  ReadyForRefinement = 'ReadyForRefinement',
  OpenForRefinement = 'OpenForRefinement',
  AwaitingReview = 'AwaitingReview',
  Complete = 'Complete'
}
/**
 * Represents an event marking the occurrence of some transient
 * source of energy in the ground, oceans, or atmosphere
 */
export interface Event {
  readonly id: string;
  readonly rejectedSignalDetectionAssociations: string[];
  readonly monitoringOrganization: string;
  readonly preferredEventHypothesisHistory: PreferredEventHypothesis[];
  readonly hypotheses: EventHypothesis[];
  readonly finalEventHypothesisHistory?: EventHypothesis[];

  // API Gateway Fields (Remove before saving)
  readonly status: EventStatus;
  readonly currentEventHypothesis: PreferredEventHypothesis;
  readonly associations: Association[];
  readonly signalDetectionIds: string[];
  readonly hasConflict: boolean;
}

/**
 * Represents a proposed explanation for an event, such that the set of
 * event hypotheses grouped by an Event represents the history of that event.
 */
export interface EventHypothesis {
  readonly id: string;
  readonly rejected: boolean;
  readonly eventId: string;
  readonly parentEventHypotheses: string[];
  // This is a list of Sets. Each set (for now) will
  // contain types Depth, Surface and Unrestrained entries
  // For the OSD it is an array since there will only be
  // one set.
  readonly locationSolutionSets: LocationSolutionSet[];
  readonly preferredLocationSolution: PreferredLocationSolution;
  readonly associations: SignalDetectionEventAssociation[];

  // API Gateway only fields (Remove before saving)
  readonly modified: boolean;
}

/**
 *
 * Event Location definition
 */
export interface EventLocation {
  readonly latitudeDegrees: number;
  readonly longitudeDegrees: number;
  readonly depthKm: number;
  readonly time: number;
}

/**
 * The preferred hypothesis for the event at a given processing stage
 */
export interface PreferredEventHypothesis {
  readonly processingStageId: string;
  readonly eventHypothesis: EventHypothesis;
}

/**
 * Represents the linkage between Event Hypotheses and Signal Detection Hypotheses.
 * The rejected attribute is used to ensure that any rejected associations will not
 * be re-formed in subsequent processing stages.
 */
export interface SignalDetectionEventAssociation {
  readonly id: string;
  readonly signalDetectionHypothesisId: string;
  readonly eventHypothesisId: string;
  readonly rejected: boolean;
  readonly modified: boolean;
}

/**
 * Represents a final hypothesis for the event.
 */
export interface FinalEventHypothesis {
  readonly eventHypothesis: EventHypothesis;
}

/**
 * Represents an estimate of the location of an event, defined as latitude, longitude, depth, and time.
 * A location solution is often determined by a location algorithm that minimizes the difference between
 * feature measurements (usually arrival time, azimuth, and slowness) and corresponding feature predictions.
 */
export interface LocationSolution {
  readonly id: string;
  readonly location: EventLocation;
  readonly featurePredictions: FeaturePrediction[];
  readonly locationRestraint: LocationRestraint;
  readonly locationUncertainty?: LocationUncertainty;
  readonly locationBehaviors: LocationBehavior[];
  readonly networkMagnitudeSolutions: NetworkMagnitudeSolution[];
  readonly snapshots: SignalDetectionSnapshot[];
}

/**
 * Location Solution Set
 * defines a list of location solutions for an event hypotheis
 * including a snapshot of association when solutions were created
 */
export interface LocationSolutionSet {
  readonly id: string;
  readonly count: number;
  readonly locationSolutions: LocationSolution[];
}

/**
 * Snapshot of state of associations when location solution was created
 */
export interface SignalDetectionSnapshot {
  readonly signalDetectionId: string;
  readonly signalDetectionHypothesisId: string;
  readonly stationName: string;
  readonly channelName: string;
  readonly phase: PhaseType;
  readonly time: EventSignalDetectionAssociationValues;
  readonly slowness: EventSignalDetectionAssociationValues;
  readonly azimuth: EventSignalDetectionAssociationValues;
  readonly aFiveAmplitude?: AmplitudeSnapshot;
  readonly aLRAmplitude?: AmplitudeSnapshot;
}

/**
 * Generic interface for snapshot values of a signal detection association
 */
export interface EventSignalDetectionAssociationValues {
  readonly defining: boolean;
  readonly observed: number;
  readonly residual: number;
  readonly correction: number;
}

/**
 * Helper interface to contain amplitude and period from amplitude fm's
 */
export interface AmplitudeSnapshot {
  readonly period: number;
  readonly amplitudeValue: number;
}

/**
 * Represents a Feature Prediction as part of the Location Solution. This should represent a
 * predicted location of the event.
 */
export interface FeaturePrediction {
  readonly phase: string;
  readonly featurePredictionComponents: FeaturePredictionComponent[] /* Java declares as a set not sure in JSON */;
  readonly extrapolated: boolean;
  readonly predictionType: FeatureMeasurementTypeName;
  readonly sourceLocation: EventLocation;
  readonly receiverLocation: Location;
  readonly featurePredictionDerivativeMap?: any;
  readonly channelName?: string;
  stationName?: string;
  readonly predictedValue?: FeatureMeasurementValue;
}

/**
 * Location Restraint for event Location Solution
 */
export interface LocationRestraint {
  readonly depthRestraintType: DepthRestraintType;
  readonly depthRestraintKm: number;
  readonly latitudeRestraintType: RestraintType;
  readonly latitudeRestraintDegrees: number;
  readonly longitudeRestraintType: RestraintType;
  readonly longitudeRestraintDegrees: number;
  readonly timeRestraintType: RestraintType;
  readonly timeRestraint: string;
}

/**
 * Location Behavior for event Location Solution
 */
export interface LocationBehavior {
  readonly residual: number;
  readonly weight: number;
  readonly defining: boolean;
  readonly featurePrediction: FeaturePrediction;
  readonly featureMeasurementType: FeatureMeasurementTypeName;
  readonly signalDetectionId: string;
}

/**
 * Location Uncertainty for Location Solution
 */
export interface LocationUncertainty {
  readonly xy: number;
  readonly xz: number;
  readonly xt: number;
  readonly yy: number;
  readonly yz: number;
  readonly yt: number;
  readonly zz: number;
  readonly zt: number;
  readonly tt: number;
  readonly stDevOneObservation: number;
  readonly ellipses: Ellipse[];
  readonly ellipsoids: Ellipsoid[];
}

/**
 * UI specific distance to source object, which has only the fields the UI needs
 */
export interface LocationToStationDistance {
  readonly distance: Distance;
  readonly azimuth: number;
  readonly stationId: string;
}

/**
 * Ellipse for Location Solution
 */
export interface Ellipse {
  readonly scalingFactorType: ScalingFactorType;
  readonly kWeight: number;
  readonly confidenceLevel: number;
  readonly majorAxisLength: string;
  readonly majorAxisTrend: number;
  readonly minorAxisLength: string;
  readonly minorAxisTrend: number;
  readonly depthUncertainty: number;
  readonly timeUncertainty: string;
}

/**
 * Ellipsoid for Location Solution
 */
export interface Ellipsoid {
  readonly scalingFactorType: ScalingFactorType;
  readonly kWeight: number;
  readonly confidenceLevel: number;
  readonly majorAxisLength: number;
  readonly majorAxisTrend: number;
  readonly majorAxisPlunge: number;
  readonly intermediateAxisLength: number;
  readonly intermediateAxisTrend: number;
  readonly intermediateAxisPlunge: number;
  readonly minorAxisLength: number;
  readonly minorAxisTrend: number;
  readonly minorAxisPlunge: number;
  readonly depthUncertainty: number;
  readonly timeUncertainty: string;
}

/**
 * RestraintType for Location Restraint
 */
export enum RestraintType {
  UNRESTRAINED = 'UNRESTRAINED',
  FIXED = 'FIXED'
}

/**
 * DepthRestraintType for Location Restraint
 */
export enum DepthRestraintType {
  UNRESTRAINED = 'UNRESTRAINED',
  FIXED_AT_DEPTH = 'FIXED_AT_DEPTH',
  FIXED_AT_SURFACE = 'FIXED_AT_SURFACE'
}

/**
 * ScalingFactorType in  Ellipse anbd Ellipsoid
 */
export enum ScalingFactorType {
  CONFIDENCE = 'CONFIDENCE',
  COVERAGE = 'COVERAGE',
  K_WEIGHTED = 'K_WEIGHTED'
}

/**
 * Feature Prediction Component definition
 */
export interface FeaturePredictionComponent {
  readonly value: DoubleValue;
  readonly extrapolated: boolean;
  readonly predictionComponentType: FeaturePredictionCorrectionType;
}

/**
 * Feature Prediction Corrections
 */
export interface FeaturePredictionCorrection {
  readonly correctionType: FeaturePredictionCorrectionType;
  readonly usingGlobalVelocity: boolean;
}

/**
 * Enumerated types for the Feature Prediction Corrections
 */
export enum FeaturePredictionCorrectionType {
  BASELINE_PREDICTION = 'BASELINE_PREDICTION',
  ELEVATION_CORRECTION = 'ELEVATION_CORRECTION',
  ELLIPTICITY_CORRECTION = 'ELLIPTICITY_CORRECTION'
}

/**
 * Double Value used in OSD common objects
 */
export interface DoubleValue {
  readonly value: number;
  readonly standardDeviation: number;
  readonly units: Units;
}

/**
 * Feature Prediction input definition for streaming call to compute FP
 */
export interface FeaturePredictionStreamingInput {
  readonly featureMeasurementTypes: FeatureMeasurementTypeName[];
  readonly sourceLocation: LocationSolutionOSD;
  readonly receiverLocations: ProcessingChannel[];
  phase: string;
  readonly model: string;
  readonly corrections?: FeaturePredictionCorrection[];
}

/**
 * Enumerated type of magnitude solution (surface wave, body wave, local, etc.)
 */
// TODO In OSD, this class DNE
export enum MagnitudeType {
  MB = 'MB',
  MB1 = 'MB1',
  MBMLE = 'MBMLE',
  MB1MLE = 'MB1MLE',
  MBREL = 'MBREL',
  MS = 'MS',
  MS1 = 'MS1',
  MSMLE = 'MSMLE',
  MS1MLE = 'MS1MLE',
  MSVMAX = 'MSVMAX',
  ML = 'ML'
}

/**
 * Magnitude types
 */
export enum MagnitudeModel {
  RICHTER = 'RICHTER',
  VEITH_CLAWSON = 'VEITH_CLAWSON',
  REZAPOUR_PEARCE = 'REZAPOUR_PEARCE',
  NUTTLI = 'NUTTLI',
  QFVC = 'QFVC',
  QFVC1 = 'QFVC1'
}

/**
 * Station Magnitude Solution
 */
export interface StationMagnitudeSolution {
  readonly type: MagnitudeType;
  readonly model: MagnitudeModel;
  readonly stationName: string;
  readonly phase: PhaseType;
  readonly magnitude: number;
  readonly magnitudeUncertainty: number;
  readonly modelCorrection: number;
  readonly stationCorrection: number;
  readonly measurement: FeatureMeasurement;
}

/**
 * Network Magnitude Behavior
 */
export interface NetworkMagnitudeBehavior {
  readonly defining: boolean;
  readonly stationMagnitudeSolution: StationMagnitudeSolution;
  readonly residual: number;
  readonly weight: number;
}

/**
 * Represents an estimate of an event's magnitude based on detections from multiple stations.
 */
export interface NetworkMagnitudeSolution {
  readonly uncertainty: number;
  readonly magnitudeType: MagnitudeType;
  readonly magnitude: number;
  readonly networkMagnitudeBehaviors: NetworkMagnitudeBehavior[];
}

/**
 * Input type for computing station mag
 */
export interface ComputeNetworkMagnitudeInput {
  readonly eventHypothesisId: string;
  readonly magnitudeType: MagnitudeType;
  readonly stationNames: string[];
  readonly defining: boolean;
  readonly locationSolutionSetId: string;
}

/**
 * Represents a preference relationship between an event hypothesis and a location solution.
 * Creation information is included in order to capture provenance of the preference.
 */
export interface PreferredLocationSolution {
  readonly locationSolution: LocationSolution;
}

/**
 * Encapsulates a set of event field values to apply to an event.
 */
export interface UpdateEventInput {
  readonly processingStageId: string;
  readonly status: EventStatus; // TODO: field DNE in OSD
  readonly preferredHypothesisId: string;
}

/**
 * Enum of Event Location Algorithms
 */
export enum EventLocationAlgorithm {
  GeigersAlgorithm = 'Geigers',
  ApacheLmAlgorithm = 'ApacheLm'
}
/**
 * Input for locate event
 */
export interface LocateEventParameter {
  readonly pluginInfo: {
    readonly name: string;
    readonly version: string;
  };
  readonly eventLocationDefinition: EventLocationDefinition;
}

/**
 * Event Location Definition base definition (ApacheLM)
 */
export interface EventLocationDefinition {
  readonly type: string;
  readonly maximumIterationCount: number;
  readonly convergenceThreshold: number;
  readonly uncertaintyProbabilityPercentile: number;
  readonly earthModel: string;
  readonly applyTravelTimeCorrections: boolean;
  readonly scalingFactorType: ScalingFactorType;
  readonly kWeight: number;
  readonly aprioriVariance: number;
  readonly minimumNumberOfObservations: number;
  readonly locationRestraints: LocationRestraint[];
}

/**
 * Geigers Event Location Definition. Additional fields to ApacheLm defintion
 */
export interface GeigersEventLocationDefinition extends EventLocationDefinition {
  readonly dampingFactorStep: number;
  readonly deltamThreshold: number;
  readonly depthFixedIterationCount: number;
  readonly lambda0: number;
  readonly lambdaX: number;
  readonly singularValueWFactor: number;
}
/**
 * Encapsulates input used to create a new event hypothesis.
 */
//
export interface CreateEventHypothesisInput {
  readonly associatedSignalDetectionIds: string[];
  readonly eventLocation: EventLocation;
  readonly creatorId: string;
  readonly processingStageId: string;
}

/**
 * Encapsulates input used to update an existing event hypothesis.
 */
export interface UpdateEventHypothesisInput extends CreateEventHypothesisInput {
  readonly rejected: boolean;
}
/**
 * Bundles into a tuple
 */
export interface PreferredEventHypothesisHistoryAndHypothesis {
  readonly preferredEventHypothesisHistory: PreferredEventHypothesis[];
  readonly currentPrefEventHypo: PreferredEventHypothesis;
}
/**
 * Flag used to make station (non)defining in the osc compute newtwork mag call
 */

export interface DefiningBehavior {
  readonly stationName: string;
  readonly magnitudeType: string;
  readonly defining: boolean;
}

/**
 * Various metadata used by compute network mag service
 */
export interface ProcessingMetadata {
  readonly detectionHypotheses: SignalDetectionHypothesisOSD[];
  // In actuality, a map of the station id to hypothesis ids
  readonly stationIdsByDetectionHypothesisIds: { [s: string]: string };
  // map of hypothesis id to location
  readonly stationLocationsByDetectionHypothesisIds: { [s: string]: Location };
}

/**
 * Query for the compute network mag service
 */
export interface NetworkMagnitudeServiceQuery {
  readonly definingBehaviors: DefiningBehavior[];
  readonly processingMetadata: ProcessingMetadata;
  readonly event: EventHypothesisOSD;
  readonly mockedNetworkMagnitudeSolutions?: NetworkMagnitudeSolutionOSD[];
}

/**
 * GraphQL arguments for locate event
 */
export interface LocateEventInput {
  eventHypothesisId: string;
  preferredLocationSolutionId: string;
  locationBehaviors: LocationBehavior[];
}

/**
 * Used to provide rejected input info when Network Magnitude calculations fail but
 * Failure is not an internal server error
 */
export interface RejectedMagnitudeInput {
  stationId: string;
  rational: string;
}

/**
 * Data Payload with a status used to keep track of network errors
 */
export interface ComputeNetworkMagnitudeDataPayload {
  status: RejectedMagnitudeInput[];
  dataPayload: DataPayload;
}
