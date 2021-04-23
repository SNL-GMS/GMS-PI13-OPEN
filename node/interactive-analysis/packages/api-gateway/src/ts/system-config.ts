import { PhaseType } from './common/model';
import {
  DepthRestraintType,
  MagnitudeType,
  RestraintType,
  ScalingFactorType
} from './event/model-and-schema/model';
import {
  EventLocationDefinitionOSD,
  LocationRestraintOSD
} from './event/model-and-schema/model-osd';
import { FeatureMeasurementTypeName } from './signal-detection/model';

/**
 * Potentially configurable settings
 * They live here for now until we integrate with some configuration mechanism
 */
export interface SystemConfig {
  sohCutoffs: SohCutoffs;
  amplitudeTypeForMagnitude: Map<MagnitudeType, FeatureMeasurementTypeName>;
  // List of magnitude types the OSD can calculate
  phaseForAmplitudeType: Map<FeatureMeasurementTypeName, PhaseType>;
  // A configured default station depth for magnitudes
  defaultDepthForMagnitude: number;
  // The default restraints for a locate call
  defaultRestraints: LocationRestraintOSD[];
  // The default location definition settings
  defaultEventLocationDefinition: EventLocationDefinitionOSD;
  getCalculableMagnitudes(mockEnabled: boolean): MagnitudeType[];
}

export interface SohCutoff {
  marginal: number;
  bad: number;
}

export interface SohCutoffs {
  lag: SohCutoff;
  environment: SohCutoff;
  missing: SohCutoff;
}

const sohCutoffs: SohCutoffs = {
  lag: {
    marginal: 35,
    bad: 45
  },
  environment: {
    marginal: 0.7,
    bad: 0.9
  },
  missing: {
    marginal: 0.7,
    bad: 0.9
  }
};

// Mapping of magnitude types to the amplitude type required to calculate the magnitude
const amplitudeTypeForMagnitude = new Map<MagnitudeType, FeatureMeasurementTypeName>([
  [MagnitudeType.MB, FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2],
  [MagnitudeType.MBMLE, FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2],
  [MagnitudeType.MS, FeatureMeasurementTypeName.AMPLITUDE_ALR_OVER_2],
  [MagnitudeType.MSMLE, FeatureMeasurementTypeName.AMPLITUDE_ALR_OVER_2]
]);

// Mapping of amplitude measurement types to the phases which can hold them

const phaseForAmplitudeType = new Map<FeatureMeasurementTypeName, PhaseType>([
  [FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2, PhaseType.P],
  [FeatureMeasurementTypeName.AMPLITUDE_ALR_OVER_2, PhaseType.LR]
]);

// Calculable magnitude types in OSD mode
const magnitudeTypesForOSD = [MagnitudeType.MB];
// Calculable magnitude types in mock
const magnitudeTypesForMock = [
  MagnitudeType.MB,
  MagnitudeType.MS,
  MagnitudeType.MBMLE,
  MagnitudeType.MSMLE
];

// Default restraints for a locate call
const defaultRestraints: LocationRestraintOSD[] = [
  {
    latitudeRestraintType: RestraintType.UNRESTRAINED,
    latitudeRestraintDegrees: undefined,
    longitudeRestraintType: RestraintType.UNRESTRAINED,
    longitudeRestraintDegrees: undefined,
    depthRestraintType: DepthRestraintType.UNRESTRAINED,
    depthRestraintKm: undefined,
    timeRestraintType: RestraintType.UNRESTRAINED,
    timeRestraint: undefined
  },
  {
    latitudeRestraintType: RestraintType.UNRESTRAINED,
    latitudeRestraintDegrees: undefined,
    longitudeRestraintType: RestraintType.UNRESTRAINED,
    longitudeRestraintDegrees: undefined,
    depthRestraintType: DepthRestraintType.FIXED_AT_SURFACE,
    depthRestraintKm: undefined,
    timeRestraintType: RestraintType.UNRESTRAINED,
    timeRestraint: undefined
  },
  {
    latitudeRestraintType: RestraintType.UNRESTRAINED,
    latitudeRestraintDegrees: undefined,
    longitudeRestraintType: RestraintType.UNRESTRAINED,
    longitudeRestraintDegrees: undefined,
    depthRestraintType: DepthRestraintType.FIXED_AT_DEPTH,
    depthRestraintKm: 50,
    timeRestraintType: RestraintType.UNRESTRAINED,
    timeRestraint: undefined
  }
];

// Default location definition
const defaultEventLocationDefinition: EventLocationDefinitionOSD = {
  maximumIterationCount: 1000,
  convergenceThreshold: 0.01,
  uncertaintyProbabilityPercentile: 0.95,
  earthModel: 'ak135',
  applyTravelTimeCorrections: false,
  scalingFactorType: ScalingFactorType.CONFIDENCE,
  kWeight: 0,
  aprioriVariance: 1,
  minimumNumberOfObservations: 4,
  enableArrivalTimeOutlierCheck: false,
  arrivalTimeOutlierStdDevMultiplier: 0,
  enableSlownessOutlierCheck: false,
  slownessOutlierStdDevMultiplier: 0,
  enableAzimuthOutlierCheck: false,
  azimuthOutlierStdDevMultiplier: 0,
  // signal detection hypothesis id to behavior map
  signalDetectionBehaviorsMap: {},
  locationRestraints: defaultRestraints
};
/**
 * The default configuration - may be replaced by the configuration mechanism
 */
export const systemConfig: SystemConfig = {
  sohCutoffs,
  amplitudeTypeForMagnitude,
  phaseForAmplitudeType,
  defaultDepthForMagnitude: 111.1,
  defaultRestraints,
  defaultEventLocationDefinition,
  getCalculableMagnitudes: (mockEnabled: boolean) =>
    mockEnabled ? magnitudeTypesForMock : magnitudeTypesForOSD
};

/**
 * Hard-coded values used to create a realistic mock mode
 */
export interface MockBackendConfig {
  defaultEventDepth: number;
  defaultOffsetForEventLocation: number;
}

/**
 * Mock backend config
 */
export const mockBackendConfig: MockBackendConfig = {
  defaultEventDepth: 10,
  defaultOffsetForEventLocation: 30
};
