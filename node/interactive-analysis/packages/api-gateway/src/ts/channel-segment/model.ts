import { PhaseType } from '../common/model';
import { FkPowerSpectra, FkPowerSpectraOSD } from '../fk/model';
import { ProcessingChannel } from '../station/processing-station/model';
import { OSDWaveform, Waveform } from '../waveform/model';

/**
 * Represents a channel time segment comprised of one or more waveforms for a (derived or raw).
 * Channel Segment definitions shared across gateway data APIs
 */
export interface ChannelSegment<T extends TimeSeries> {
  id: string;
  channel: ProcessingChannel;
  name: string;
  type: ChannelSegmentType;
  timeseriesType: TimeSeriesType;
  startTime: number;
  endTime: number;
  timeseries: T[];
  requiresSave: boolean;
}

/**
 * OSD channel segment as a generic of OSD time series
 */
export interface OSDChannelSegment<T extends OSDTimeSeries> {
  id: string;
  channel: ProcessingChannel;
  name: string;
  type: ChannelSegmentType;
  timeseriesType: TimeSeriesType;
  startTime: string;
  endTime: string;
  timeseries: T[];
}

/**
 * Channel segment input
 */
export interface ChannelSegmentInput {
  startTime: string;
  endTime: string;
  channelNames: string[];
  withWaveforms: boolean;
}

/**
 * Channel segment by id input
 */
export interface ChannelSegmentByIdInput {
  channelSegmentIds: string[];
  withTimeseries: boolean;
}

/**
 * Represents a generic time series data set output from a channel
 * (derived or raw) within a specified time range.
 */
export interface TimeSeries {
  startTime: number;
  sampleRate: number;
  sampleCount: number;
  type: TimeSeriesType;
}

/**
 * Time series OSD representation
 */
export interface OSDTimeSeries {
  startTime: string;
  sampleRate: number;
  sampleCount: number;
  type: TimeSeriesType;
}

/**
 * Channel Segment type
 */
export enum ChannelSegmentType {
  ACQUIRED = 'ACQUIRED',
  RAW = 'RAW',
  DETECTION_BEAM = 'DETECTION_BEAM',
  FK_BEAM = 'FK_BEAM',
  FK_SPECTRA = 'FK_SPECTRA',
  FILTER = 'FILTER'
}

/**
 * Time series type
 */
export enum TimeSeriesType {
  WAVEFORM = 'WAVEFORM',
  FK_SPECTRA = 'FK_SPECTRA',
  DETECTION_FEATURE_MAP = 'DETECTION_FEATURE_MAP'
}

/**
 * Beam Input Definitions for Compute Beam Streaming Call
 */
export interface BeamFormingInput {
  beamDefinition: BeamDefinition;
  // processingContext: ProcessingContext;
  // outputChannelId: string;
  waveforms: OSDChannelSegment<OSDTimeSeries>[];
}

/**
 * Beam Definition used by BeamFormingInput
 */
export interface BeamDefinition {
  phaseType: PhaseType;
  azimuth: number;
  slowness: number;
  coherent: boolean;
  snappedSampling: boolean;
  twoDimensional: boolean;
  nominalWaveformSampleRate: number;
  waveformSampleRateTolerance: number;
  // beamPoint: Location;
  // OSD "map" is not really a map so type as any
  // relativePositionsByChannelId: any;
  minimumWaveformsForBeam: number;
}

/**
 * Checks if FK spectra channel segment
 * @param object Channel Segment
 * @returns boolean
 */
// tslint:disable-next-line:max-line-length
export function isFkSpectraChannelSegment(
  object: ChannelSegment<TimeSeries>
): object is ChannelSegment<FkPowerSpectra> {
  return object.timeseriesType === TimeSeriesType.FK_SPECTRA;
}

/**
 * Checks id FK spectra channel segment OSD representation
 * @param object OSDChannelSegment
 * @returns boolean
 */
// tslint:disable-next-line:max-line-length
export function isFkSpectraChannelSegmentOSD(
  object: OSDChannelSegment<OSDTimeSeries>
): object is OSDChannelSegment<FkPowerSpectraOSD> {
  return object.timeseriesType === TimeSeriesType.FK_SPECTRA;
}

/**
 * Checks if FK Spectra time series
 * @param object Time Series
 * @param type Time Series Type
 * @returns boolean
 */
export function isFkSpectraTimeSeries(
  object: TimeSeries,
  type: TimeSeriesType
): object is FkPowerSpectra {
  return type === TimeSeriesType.FK_SPECTRA;
}

/**
 * Checks if FK Spectra time series
 * @param object Time Series
 * @param type Time Series Type
 * @returns boolean
 */
export function isWaveformTimeSeries(object: TimeSeries, type: TimeSeriesType): object is Waveform {
  return type === TimeSeriesType.WAVEFORM;
}

/**
 * Checks if waveform channel segment
 * @param object Channel segment
 * @returns boolean
 */
export function isWaveformChannelSegment(
  object: ChannelSegment<TimeSeries>
): object is ChannelSegment<Waveform> {
  return object.timeseriesType === TimeSeriesType.WAVEFORM;
}

/**
 * Checks if waveform channel segment OSD representation
 * @param object Channel segment OSD representation
 * @returns boolean
 */
export function isOSDWaveformChannelSegment(
  object: OSDChannelSegment<OSDTimeSeries>
): object is OSDChannelSegment<OSDWaveform> {
  return object.timeseriesType === TimeSeriesType.WAVEFORM;
}

/**
 * Checks if channel segment OSD representation
 * @param object Channel segment or channel segment OSD representation
 * @returns boolean
 */
export function isOSDChannelSegment(
  object: ChannelSegment<TimeSeries> | OSDChannelSegment<OSDTimeSeries>
): object is OSDChannelSegment<OSDTimeSeries> {
  return typeof object.startTime === 'string';
}

/**
 * Checks if channel segment
 * @param object Channel segment or channel segment OSD representation
 * @returns boolean
 */
export function isChannelSegment(
  object: ChannelSegment<TimeSeries> | OSDChannelSegment<OSDTimeSeries>
): object is ChannelSegment<TimeSeries> {
  return typeof object.startTime === 'number';
}

/**
 * Checks if time series OSD representation
 * @param object Time series or Time Series OSD representation
 */
export function isOSDTimeSeries(object: TimeSeries | OSDTimeSeries): object is OSDTimeSeries {
  return typeof object.startTime === 'string';
}

/**
 * Checks if time series
 * @param object Time series or time series OSD representation
 * @returns boolean
 */
export function isTimeSeries(object: TimeSeries | OSDTimeSeries): object is TimeSeries {
  return typeof object.startTime === 'number';
}
