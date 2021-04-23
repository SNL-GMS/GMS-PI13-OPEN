import { LogLevel } from '@gms/common-graphql/lib/graphql/common/types';

/**
 * Common model definitions shared across gateway data APIs
 */

/**
 * Represents an association mapping
 * between an event and signal detection.
 */
export interface Association {
  /** the event association id */
  readonly associationId: string;
  /** the event id */
  readonly eventId: string;
  /** the event hypothesis id */
  readonly eventHypothesisId: string;
  /** the signal detection id */
  readonly signalDetectionId: string;
  /** the signal detection hypothesis id */
  readonly signalDetectionHypothesisId: string;
  /** the association reject flag */
  readonly rejected: boolean;
}

/**
 * Represents a location specified using latitude (degrees), longitude (degrees),
 * and altitude (kilometers).
 */
export interface Location {
  readonly latitudeDegrees: number;
  readonly longitudeDegrees: number;
  readonly elevationKm: number;
  readonly depthKm?: number;
}

/**
 * Represents a frequency range
 */
export interface FrequencyBand {
  readonly minFrequencyHz: number;
  readonly maxFrequencyHz: number;
}

/**
 * Time range
 */
export interface TimeRange {
  readonly startTime: number;
  readonly endTime: number;
}

/**
 * Keeps track of state between different workspaces
 */
export interface WorkspaceState {
  readonly eventToUsers: EventToUsers[];
}

/**
 * Event with list of usernames
 */
export interface EventToUsers {
  readonly eventId: string;
  readonly userNames: string[];
}

/**
 * Enumeration representing the different types of stations in the monitoring network.
 */
export enum StationType {
  Seismic3Component = 'Seismic3Component',
  Seismic1Component = 'Seismic1Component',
  SeismicArray = 'SeismicArray',
  Hydroacoustic = 'Hydroacoustic',
  HydroacousticArray = 'HydroacousticArray',
  Infrasound = 'Infrasound',
  InfrasoundArray = 'InfrasoundArray',
  Weather = 'Weather',
  UNKNOWN = 'UNKNOWN'
}

/**
 * Enumeration representing the different types of processing channels.
 */
export enum ChannelDataType {
  SEISMIC = 'SEISMIC',
  HYDROACOUSTIC = 'HYDROACOUSTIC',
  INFRASOUND = 'INFRASOUND',
  WEATHER = 'WEATHER',
  DIAGNOSTIC_SOH = 'DIAGNOSTIC_SOH',
  DIAGNOSTIC_WEATHER = 'DIAGNOSTIC_WEATHER'
}

/**
 * Processing Context part of data structure that filtered waveform
 * control service uses to tell who/where the request came from. In our case
 * from Interactive UI user
 */
export interface ProcessingContext {
  readonly analystActionReference: AnalystActionReference;
  readonly processingStepReference: ProcessingStepReference;
  readonly storageVisibility: string;
}

/**
 * Analyst action reference
 */
export interface AnalystActionReference {
  readonly processingStageIntervalId: string;
  readonly processingActivityIntervalId: string;
  readonly analystId: string;
}

/**
 * Processing step reference
 */
export interface ProcessingStepReference {
  readonly processingStageIntervalId: string;
  readonly processingSequenceIntervalId: string;
  readonly processingStepId: string;
}

/**
 * Test data paths used when reading in data
 */
export interface TestDataPaths {
  readonly dataHome: string;
  readonly jsonHome: string;
  readonly fpHome: string;
  readonly fkHome: string;
  readonly channelsHome: string;
  readonly additionalDataHome: string;
  readonly integrationDataHome: string;
  readonly tempTigerTeamData: string;
}

/**
 * Represents calibration information associated with a waveform
 */
export interface ProcessingCalibration {
  readonly factor: number;
  readonly factorError: number;
  readonly period: number;
  readonly timeShift: number;
}

/**
 * Relative Position information relative to a location
 */
export interface Position {
  readonly northDisplacementKm: number;
  readonly eastDisplacementKm: number;
  readonly verticalDisplacementKm: number;
}

/**
 * Represents the configured type of data source the API Gateway provides access to - values:
 * Local - The API gateway loads data from local file storage for testing purposes
 * Service - The API gateway uses services to provide access to backend (e.g. OSD) data
 */
export enum AccessorDataSource {
  Local = 'Local',
  Service = 'Service'
}

/**
 * Enumerated list of source types used to compute distances to
 */
export enum DistanceSourceType {
  Event = 'Event',
  UserDefined = 'UserDefined'
}

/**
 * Distance value's units degrees or kilometers
 */
export enum DistanceUnits {
  degrees = 'degrees',
  km = 'km'
}

/**
 * The distance value representing degrees and km.
 */
export interface Distance {
  readonly degrees: number;
  readonly km: number;
}

/**
 * Represents input arguments for calculating distance measurement
 * relative to a specified source location
 */
export interface DistanceToSourceInput {
  // The type of the source the distance is measured to (e.g. and event)
  readonly sourceType: DistanceSourceType;

  // the unique ID of the source object
  readonly sourceId: string;
}

/**
 * Represents a distance measurement relative to a specified source location
 */
export interface DistanceToSource {
  // The distance
  readonly distance: Distance;

  // The azimuth
  readonly azimuth: number;

  // The source location
  readonly sourceLocation: Location;

  // The type of the source the distance is measured to (e.g. and event)
  readonly sourceType: DistanceSourceType;

  // the unique ID of the source object
  readonly sourceId: string;

  // Which station distance to the source
  readonly stationId: string;
}

/**
 * Client log, object that describes the client log message
 */
export interface ClientLog {
  readonly logLevel: LogLevel;
  readonly message: string;
  readonly time?: string;
}

/**
 * Units used in DoubleValue part of feature prediction
 */
export enum Units {
  DEGREES = 'DEGREES',
  RADIANS = 'RADIANS',
  SECONDS = 'SECONDS',
  HERTZ = 'HERTZ',
  SECONDS_PER_DEGREE = 'SECONDS_PER_DEGREE',
  SECONDS_PER_RADIAN = 'SECONDS_PER_RADIAN',
  SECONDS_PER_DEGREE_SQUARED = 'SECONDS_PER_DEGREE_SQUARED',
  SECONDS_PER_KILOMETER_SQUARED = 'SECONDS_PER_KILOMETER_SQUARED',
  SECONDS_PER_KILOMETER = 'SECONDS_PER_KILOMETER',
  SECONDS_PER_KILOMETER_PER_DEGREE = 'SECONDS_PER_KILOMETER_PER_DEGREE',
  ONE_OVER_KM = 'ONE_OVER_KM',
  NANOMETERS = 'NANOMETERS',
  NANOMETERS_PER_SECOND = 'NANOMETERS_PER_SECOND',
  NANOMETERS_PER_COUNT = 'NANOMETERS_PER_COUNT',
  UNITLESS = 'UNITLESS',
  MAGNITUDE = 'MAGNITUDE',
  COUNTS_PER_NANOMETER = 'COUNTS_PER_NANOMETER',
  COUNTS_PER_PASCAL = 'COUNTS_PER_PASCAL',
  PASCALS_PER_COUNT = 'PASCALS_PER_COUNT'
}

/**
 * Phase type list
 */
export enum PhaseType {
  // TODO: need to elaborate with full set of phase labels
  P = 'P',
  S = 'S',
  P3KPbc = 'P3KPbc',
  P4KPdf_B = 'P4KPdf_B',
  P7KPbc = 'P7KPbc',
  P7KPdf_D = 'P7KPdf_D',
  PKiKP = 'PKiKP',
  PKKSab = 'PKKSab',
  PKP2bc = 'PKP2bc',
  PKP3df_B = 'PKP3df_B',
  PKSab = 'PKSab',
  PP_1 = 'PP_1',
  pPKPbc = 'pPKPbc',
  PS = 'PS',
  Rg = 'Rg',
  SKiKP = 'SKiKP',
  SKKSac = 'SKKSac',
  SKPdf = 'SKPdf',
  SKSdf = 'SKSdf',
  sPdiff = 'sPdiff',
  SS = 'SS',
  sSKSdf = 'sSKSdf',
  Lg = 'Lg',
  P3KPbc_B = 'P3KPbc_B',
  P5KPbc = 'P5KPbc',
  P7KPbc_B = 'P7KPbc_B',
  Pb = 'Pb',
  PKKP = 'PKKP',
  PKKSbc = 'PKKSbc',
  PKP2df = 'PKP2df',
  PKPab = 'PKPab',
  PKSbc = 'PKSbc',
  PP_B = 'PP_B',
  pPKPdf = 'pPKPdf',
  PS_1 = 'PS_1',
  SKKP = 'SKKP',
  SKKSac_B = 'SKKSac_B',
  SKS = 'SKS',
  SKSSKS = 'SKSSKS',
  sPKiKP = 'sPKiKP',
  SS_1 = 'SS_1',
  SSS = 'SSS',
  nNL = 'nNL',
  P3KPdf = 'P3KPdf',
  P5KPbc_B = 'P5KPbc_B',
  P7KPbc_C = 'P7KPbc_C',
  PcP = 'PcP',
  PKKPab = 'PKKPab',
  PKKSdf = 'PKKSdf',
  PKP3 = 'PKP3',
  PKPbc = 'PKPbc',
  PKSdf = 'PKSdf',
  pPdiff = 'pPdiff',
  PPP = 'PPP',
  pSdiff = 'pSdiff',
  Sb = 'Sb',
  SKKPab = 'SKKPab',
  SKKSdf = 'SKKSdf',
  SKS2 = 'SKS2',
  Sn = 'Sn',
  sPKP = 'sPKP',
  SS_B = 'SS_B',
  SSS_B = 'SSS_B',
  NP = 'NP',
  P3KPdf_B = 'P3KPdf_B',
  P5KPdf = 'P5KPdf',
  P7KPdf = 'P7KPdf',
  PcS = 'PcS',
  PKKPbc = 'PKKPbc',
  PKP = 'PKP',
  PKP3ab = 'PKP3ab',
  PKPdf = 'PKPdf',
  Pn = 'Pn',
  pPKiKP = 'pPKiKP',
  PPP_B = 'PPP_B',
  pSKS = 'pSKS',
  ScP = 'ScP',
  SKKPbc = 'SKKPbc',
  SKP = 'SKP',
  SKS2ac = 'SKS2ac',
  SnSn = 'SnSn',
  sPKPab = 'sPKPab',
  sSdiff = 'sSdiff',
  NP_1 = 'NP_1',
  P4KPbc = 'P4KPbc',
  P5KPdf_B = 'P5KPdf_B',
  P7KPdf_B = 'P7KPdf_B',
  Pdiff = 'Pdiff',
  PKKPdf = 'PKKPdf',
  PKP2 = 'PKP2',
  PKP3bc = 'PKP3bc',
  PKPPKP = 'PKPPKP',
  PnPn = 'PnPn',
  pPKP = 'pPKP',
  PPS = 'PPS',
  pSKSac = 'pSKSac',
  ScS = 'ScS',
  SKKPdf = 'SKKPdf',
  SKPab = 'SKPab',
  SKS2df = 'SKS2df',
  SP = 'SP',
  sPKPbc = 'sPKPbc',
  sSKS = 'sSKS',
  P4KPdf = 'P4KPdf',
  P5KPdf_C = 'P5KPdf_C',
  P7KPdf_C = 'P7KPdf_C',
  Pg = 'Pg',
  PKKS = 'PKKS',
  PKP2ab = 'PKP2ab',
  PKP3df = 'PKP3df',
  PKS = 'PKS',
  PP = 'PP',
  pPKPab = 'pPKPab',
  PPS_B = 'PPS_B',
  pSKSdf = 'pSKSdf',
  Sdiff = 'Sdiff',
  SKKS = 'SKKS',
  SKPbc = 'SKPbc',
  SKSac = 'SKSac',
  SP_1 = 'SP_1',
  sPKPdf = 'sPKPdf',
  sSKSac = 'sSKSac',
  Sx = 'Sx',
  tx = 'tx',
  N = 'N',
  Px = 'Px',
  PKhKP = 'PKhKP',
  UNKNOWN = 'UNKNOWN',
  LR = 'LR'
}
