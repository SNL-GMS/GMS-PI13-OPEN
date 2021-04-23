import {
  CommonTypes,
  EventTypes,
  ProcessingStationTypes,
  SignalDetectionTypes
} from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { Row } from '@gms/ui-core-components';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import Immutable from 'immutable';
import { ChildProps, MutationFunction } from 'react-apollo';

/**
 * Different filters that are available
 */
export enum FilterType {
  allRows = 'All Detections',
  openEvent = 'Open Event',
  complete = 'Completed'
}

/**
 * Table row object for signal detections
 */
export interface SignalDetectionsRow extends Row {
  id: string;
  hypothesisId: string;
  station: string;
  phase: string;
  time: number;
  timeUnc: number;
  assocEventId: string;
  color: string;
  // Added for filtering
  isSelectedEvent: boolean;
  // Added for filtering
  isComplete: boolean;
  aFiveMeasurement: { value: number; requiresReview: boolean };
  aFivePeriod: number;
  alrMeasurement: number;
  alrPeriod: number;
  slowness: number;
  azimuth: number;
}

/**
 * Mutations used by the Signal Detections display
 */
export interface SignalDetectionsMutations {
  // {} because we don't care about mutation results for now, handling that through subscriptions
  updateDetections: MutationFunction<{}>;
  rejectDetections: MutationFunction<{}>;
  changeSignalDetectionAssociations: MutationFunction<{}>;
  createEvent: MutationFunction<{}>;
}

/**
 * Props mapped in from Redux state
 */
export interface SignalDetectionsReduxProps {
  // Passed in from golden-layout
  glContainer?: GoldenLayout.Container;
  // The currently-open processing interval time range
  currentTimeInterval: CommonTypes.TimeRange;
  // The currently-open event hypothesis IDs
  openEventId: string;
  // The currently-selected signal detection IDs
  selectedSdIds: string[];
  analystActivity: AnalystWorkspaceTypes.AnalystActivity;
  /** The measurement mode */
  measurementMode: AnalystWorkspaceTypes.MeasurementMode;
  sdIdsToShowFk: string[];

  // callbacks
  setSelectedSdIds(ids: string[]): void;
  setSdIdsToShowFk(signalDetectionIds: string[]): void;
  setMeasurementModeEntries(entries: Immutable.Map<string, boolean>): void;
}

/**
 * Signal detection list local state
 */
export interface SignalDetectionsState {
  selectedFilter: FilterType;
  userSetFilter: boolean;
}

/**
 * Consolidated props type for signal detection list
 */
export type SignalDetectionsProps = SignalDetectionsReduxProps &
  ChildProps<SignalDetectionsMutations> &
  ProcessingStationTypes.DefaultStationsQueryProps &
  SignalDetectionTypes.SignalDetectionsByStationQueryProps &
  EventTypes.EventsInTimeRangeQueryProps;

export type SignalDetectionRejector = (sdIds: string[]) => void;
