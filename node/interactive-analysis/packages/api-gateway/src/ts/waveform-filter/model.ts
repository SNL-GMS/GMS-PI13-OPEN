import { ChannelSegment, OSDChannelSegment } from '../channel-segment/model';
import { OSDWaveform, Waveform } from '../waveform/model';

/**
 * Calculate waveform segment input
 */
export interface CalculateWaveformSegmentInput {
  channelSegments: OSDChannelSegment<OSDWaveform>[];
  filterDefinition: WaveformFilterDefinition;
}

/**
 * Filtered waveform channel segment which extends ChannelSegment
 */
export interface FilteredWaveformChannelSegment extends ChannelSegment<Waveform> {
  sourceChannelId: string;
  wfFilterId: string;
}

/**
 * Represents a Waveform Filter
 */
export interface WaveformFilterDefinition {
  id: string;
  name: string;
  description: string;
  filterType: string; // FIR_HAMMING
  filterPassBandType: string; // BAND_PASS, HIGH_PASS
  lowFrequencyHz: number;
  highFrequencyHz: number;
  order: number;
  filterSource: string; // SYSTEM
  filterCausality: string; // CAUSAL
  zeroPhase: boolean;
  sampleRate: number;
  sampleRateTolerance: number;
  validForSampleRate: boolean;
  aCoefficients: number[] | null;
  bCoefficients: number[] | null;
  groupDelaySecs: number | null;
}
