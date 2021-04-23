import { OSDTimeSeries, TimeSeries, TimeSeriesType } from '../channel-segment/model';
import { FrequencyBand, PhaseType } from '../common/model';
import { Waveform } from '../waveform/model';

// These interfaces match exactly the OSD common objects definitions which will be useful to unify on as
// the OSD gets hardened.

/**
 * Fk power spectras
 */
export interface FkPowerSpectra extends TimeSeries {
  metadata: FkMetadata;
  type: TimeSeriesType;
  windowLead: number;
  windowLength: number;
  lowFrequency: number;
  highFrequency: number;
  stepSize: number;
  slowCountX: number;
  slowCountY: number;
  reviewed: boolean;
  spectrums: FkPowerSpectrum[];
  fstatData: FstatData;
  configuration: FkConfiguration;
}

/**
 * Fk power spectra OSD representation
 */
export interface FkPowerSpectraOSD extends OSDTimeSeries {
  metadata: FkMetadata;
  windowLead: string;
  windowLength: string;
  lowFrequency: number;
  highFrequency: number;
  sampleCount: number;
  sampleRate: number;
  values: FkPowerSpectrumOSD[];
}

/**
 * Fk meta data from the OSD
 */
export interface FkMetadata {
  phaseType: PhaseType;
  slowDeltaX: number;
  slowDeltaY: number;
  slowStartX: number;
  slowStartY: number;
}

/**
 * FstatData for plots in UI and so we don't return the spectrum list
 */
export interface FstatData {
  azimuthWf: Waveform;
  slownessWf: Waveform;
  fstatWf: Waveform;
}

/**
 * Fk power spectrum part of the FkPowerSpectra definition
 */
export interface FkPowerSpectrum {
  power: number[][];
  fstat: number[][];
  quality: number;
  attributes: FkAttributes;
}

/**
 * Fk power spectrum OSD representation
 */
export interface FkPowerSpectrumOSD {
  power: number[][];
  fstat: number[][];
  quality: number;
  attributes: FkAttributesOSD[];
}

/**
 * Fk attributes OSD representation
 */
export interface FkAttributesOSD {
  peakFStat: number;
  xSlow: number;
  ySlow: number;
  azimuth: number;
  slowness: number;
  azimuthUncertainty: number;
  slownessUncertainty: number;
}

/**
 * Fk attributes part of the FkPowerSpectrum definition
 */
export interface FkAttributes {
  peakFStat: number;
  azimuth: number;
  slowness: number;
  azimuthUncertainty: number;
  slownessUncertainty: number;
}

/**
 * Represents the time window parameters used in the Fk calculation
 */
export interface WindowParameters {
  windowType: string;
  leadSeconds: number;
  lengthSeconds: number;
  stepSize: number;
}

/**
 * FkFrequencyThumbnail preview Fk at a preset FrequencyBand
 */
export interface FkFrequencyThumbnail {
  frequencyBand: FrequencyBand;
  fkSpectra: FkPowerSpectra;
}

/**
 * Collection of thumbnails by signal detection id
 */
export interface FkFrequencyThumbnailBySDId {
  signalDetectionId: string;
  fkFrequencyThumbnails: FkFrequencyThumbnail[];
}

/**
 * Input type for creating new Fks
 */
export interface FkInput {
  stationId: string;
  signalDetectionId: string;
  signalDetectionHypothesisId: string;
  phase: string;
  frequencyBand: FrequencyBand;
  windowParams: WindowParameters;
  configuration: FkConfiguration;
}

/**
 * Input type for creating new Beam
 */
export interface BeamInput {
  signalDetectionId: string;
  windowParams: WindowParameters;
}

/**
 * Input type for UI to API Gateway to set
 * Fk as reviewed or not
 */
export interface MarkFksReviewedInput {
  signalDetectionIds: string[];
  reviewed: boolean;
}

/**
 * Input type for Compute FK service call. This input
 * is compatible with the OSD input i.e. start/end are strings
 */
export interface ComputeFkInput {
  startTime: string;
  sampleRate: number;
  sampleCount: number;
  channelNames: string[];
  windowLead: string;
  windowLength: string;
  lowFrequency: number;
  highFrequency: number;
  useChannelVerticalOffset: boolean;
  phaseType: string;
  normalizeWaveforms: boolean;
  // Optional fields
  slowStartX?: number;
  slowDeltaX?: number;
  slowCountX?: number;
  slowStartY?: number;
  slowDeltaY?: number;
  slowCountY?: number;
}
/**
 * Tracks whether a channel is used to calculate fk
 */

export interface ContributingChannelsConfiguration {
  id: string;
  enabled: boolean;
  name: string;
}
/**
 * Holds the configuration used to calculate an Fk
 */
export interface FkConfiguration {
  maximumSlowness: number;
  mediumVelocity: number;
  numberOfPoints: number;
  normalizeWaveforms: boolean;
  useChannelVerticalOffset: boolean;
  contributingChannelsConfiguration: ContributingChannelsConfiguration[];
  leadFkSpectrumSeconds: number;
}
