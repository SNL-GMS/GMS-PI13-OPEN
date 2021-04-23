import { CommonTypes } from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { QcMaskDisplayFilters } from '~analyst-ui/config';
import { MaskDisplayFilter } from '~analyst-ui/config/user-preferences';
import { AlignWaveformsOn, PanType } from '../../types';
import { WaveformClientState } from '../../waveform-client/types';

/**
 * Waveform Display Controls Props
 */
export interface WaveformDisplayControlsProps {
  defaultSignalDetectionPhase: CommonTypes.PhaseType;
  currentSortType: AnalystWorkspaceTypes.WaveformSortType;
  currentOpenEventId: string;
  analystNumberOfWaveforms: number;
  showPredictedPhases: boolean;
  maskDisplayFilters: QcMaskDisplayFilters;
  alignWaveformsOn: AlignWaveformsOn;
  phaseToAlignOn: CommonTypes.PhaseType | undefined;
  alignablePhases: CommonTypes.PhaseType[] | undefined;
  glContainer: GoldenLayout.Container;
  isMeasureWindowVisible: boolean;
  measurementMode: AnalystWorkspaceTypes.MeasurementMode;
  setMode(mode: AnalystWorkspaceTypes.WaveformDisplayMode): void;
  setDefaultSignalDetectionPhase(phase: CommonTypes.PhaseType): void;
  setSelectedSortType(sortType: AnalystWorkspaceTypes.WaveformSortType): void;
  setAnalystNumberOfWaveforms(value: number, valueAsString?: string): void;
  setMaskDisplayFilters(key: string, maskDisplayFilter: MaskDisplayFilter): void;
  setWaveformAlignment(
    alignWaveformsOn: AlignWaveformsOn,
    phaseToAlignOn: CommonTypes.PhaseType,
    showPredictedPhases: boolean
  ): void;
  toggleMeasureWindow(): void;
  setShowPredictedPhases(showPredicted: boolean): void;
  pan(panDirection: PanType): void;
  onKeyPress(
    e: React.KeyboardEvent<HTMLDivElement>,
    clientX?: number,
    clientY?: number,
    channelId?: string,
    timeSecs?: number
  ): void;
}

export interface WaveformDisplayControlsState {
  hasMounted: boolean;
  waveformState: WaveformClientState;
}
