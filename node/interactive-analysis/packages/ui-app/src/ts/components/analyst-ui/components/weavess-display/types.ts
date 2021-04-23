import {
  CommonTypes,
  EventTypes,
  ProcessingStationTypes,
  QcMaskTypes,
  SignalDetectionTypes,
  WaveformTypes
} from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { WaveformDisplayProps as WeavessProps } from '@gms/weavess/lib/components/waveform-display/types';
import Immutable from 'immutable';
import { ChildMutateProps, MutationFunction } from 'react-apollo';

export interface WeavessDisplayState {
  selectedChannels: string[];
  qcMaskModifyInterval?: CommonTypes.TimeRange;
  selectedQcMask?: QcMaskTypes.QcMask;
}

interface WeavessDisplayReduxProps {
  // passed in from golden-layout
  glContainer?: GoldenLayout.Container;
  currentTimeInterval: CommonTypes.TimeRange;
  currentOpenEventId: string;
  selectedSdIds: string[];
  analystActivity: AnalystWorkspaceTypes.AnalystActivity;
  sdIdsToShowFk: string[];

  // callbacks
  setMode(mode: AnalystWorkspaceTypes.WaveformDisplayMode): void;
  setOpenEventId(event: EventTypes.Event): void;
  setSelectedSdIds(id: string[]): void;
  setSdIdsToShowFk(signalDetections: string[]): void;
}

interface WeavessDisplayMutations {
  createDetection: MutationFunction<{}>;
  updateDetections: MutationFunction<{}>;
  rejectDetections: MutationFunction<{}>;
  createQcMask: MutationFunction<{}>;
  updateQcMask: MutationFunction<{}>;
  rejectQcMask: MutationFunction<{}>;
  updateEvents: MutationFunction<{}>;
  createEvent: MutationFunction<{}>;
  changeSignalDetectionAssociations: MutationFunction<{}>;
}

export interface WeavessDisplayComponentProps {
  weavessProps: Partial<WeavessProps>;
  defaultWaveformFilters: WaveformTypes.WaveformFilter[];
  defaultStations: ProcessingStationTypes.ProcessingStation[];
  defaultSignalDetectionPhase?: CommonTypes.PhaseType;
  eventsInTimeRange: EventTypes.Event[];
  signalDetectionsByStation: SignalDetectionTypes.SignalDetection[];
  qcMasksByChannelName: QcMaskTypes.QcMask[];
  measurementMode: AnalystWorkspaceTypes.MeasurementMode;

  setMeasurementModeEntries(entries: Immutable.Map<string, boolean>): void;
}

export type WeavessDisplayProps = WeavessDisplayReduxProps &
  ChildMutateProps<WeavessDisplayMutations> &
  WeavessDisplayComponentProps;
