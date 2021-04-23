import {
  CommonTypes,
  ConfigurationTypes,
  EventTypes,
  ProcessingStationTypes,
  QcMaskTypes,
  SignalDetectionTypes,
  WaveformTypes
} from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { Client } from '@gms/ui-apollo';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { WeavessTypes } from '@gms/weavess';
import Immutable from 'immutable';
import { ChildMutateProps, MutationFunction } from 'react-apollo';
import { QcMaskDisplayFilters } from '~analyst-ui/config';

export enum PhaseAlignments {
  PREDICTED_PHASE = 'Predicted',
  OBSERVED_PHASE = 'Observed'
}

export enum AlignWaveformsOn {
  TIME = 'Time',
  PREDICTED_PHASE = 'Predicted',
  OBSERVED_PHASE = 'Observed'
}

export enum KeyDirection {
  UP = 'Up',
  DOWN = 'Down',
  LEFT = 'Left',
  RIGHT = 'Right'
}

/**
 * Waveform Display display state.
 * keep track of selected channels & signal detections
 */
export interface WaveformDisplayState {
  stations: WeavessTypes.Station[];
  currentTimeInterval: CommonTypes.TimeRange;
  // because the user may load more waveform
  // data than the currently opened time interval
  viewableInterval: CommonTypes.TimeRange;
  loadingWaveforms: boolean;
  loadingWaveformsPercentComplete: number;
  maskDisplayFilters: QcMaskDisplayFilters;
  analystNumberOfWaveforms: number;
  currentOpenEventId: string;
  showPredictedPhases: boolean;
  alignWaveformsOn: AlignWaveformsOn;
  phaseToAlignOn: CommonTypes.PhaseType | undefined;
  isMeasureWindowVisible: boolean;
}

/**
 * Props mapped in from Redux state
 */
export interface WaveformDisplayReduxProps {
  // passed in from golden-layout
  glContainer?: GoldenLayout.Container;
  client: Client;
  currentTimeInterval: CommonTypes.TimeRange;
  defaultSignalDetectionPhase: CommonTypes.PhaseType;
  currentOpenEventId: string;
  selectedSdIds: string[];
  selectedSortType: AnalystWorkspaceTypes.WaveformSortType;
  analystActivity: AnalystWorkspaceTypes.AnalystActivity;
  measurementMode: AnalystWorkspaceTypes.MeasurementMode;
  sdIdsToShowFk: string[];
  location: AnalystWorkspaceTypes.LocationSolutionState;
  channelFilters: Immutable.Map<string, WaveformTypes.WaveformFilter>;
  openEventId: string;
  keyPressActionQueue: Immutable.Map<AnalystWorkspaceTypes.KeyAction, number>;

  // callbacks
  setDefaultSignalDetectionPhase(phase: CommonTypes.PhaseType): void;
  setMode(mode: AnalystWorkspaceTypes.WaveformDisplayMode): void;
  setOpenEventId(
    event: EventTypes.Event | undefined,
    latestLocationSolutionSet: EventTypes.LocationSolutionSet | undefined,
    preferredLocationSolutionId: string | undefined
  ): void;
  setSelectedSdIds(idx: string[]): void;
  setSdIdsToShowFk(signalDetections: string[]): void;
  setSelectedSortType(selectedSortType: AnalystWorkspaceTypes.WaveformSortType): void;
  setChannelFilters(filters: Immutable.Map<string, WaveformTypes.WaveformFilter>);
  setMeasurementModeEntries(entries: Immutable.Map<string, boolean>): void;
  setKeyPressActionQueue(actions: Immutable.Map<AnalystWorkspaceTypes.KeyAction, number>): void;
}

/**
 * Mutations used by the Waveform display
 */
export interface WaveformDisplayMutations {
  createDetection: MutationFunction<{}>;
  updateDetections: MutationFunction<{}>;
  rejectDetections: MutationFunction<{}>;
  createQcMask: MutationFunction<{}>;
  updateQcMask: MutationFunction<{}>;
  rejectQcMask: MutationFunction<{}>;
  updateEvents: MutationFunction<{}>;
  createEvent: MutationFunction<{}>;
  changeSignalDetectionAssociations: MutationFunction<{}>;
  markAmplitudeMeasurementReviewed: MutationFunction<{}>;
}

/**
 * Consolidated props type for waveform display.
 */
export type WaveformDisplayProps = WaveformDisplayReduxProps &
  ChildMutateProps<WaveformDisplayMutations> &
  ProcessingStationTypes.DefaultStationsQueryProps &
  ConfigurationTypes.UIConfigurationQueryProps &
  SignalDetectionTypes.SignalDetectionsByStationQueryProps &
  EventTypes.EventsInTimeRangeQueryProps &
  QcMaskTypes.QcMasksByChannelNameQueryProps;

// Enum to clarify pan button interactions
export enum PanType {
  Left,
  Right
}
