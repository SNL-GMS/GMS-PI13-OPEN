import {
  CommonTypes,
  EventTypes,
  ProcessingStationTypes,
  SignalDetectionTypes
} from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import Immutable from 'immutable';
import { ChildProps, MutationFunction } from 'react-apollo';
import { DefiningChange } from './components/location-signal-detections/types';

export type LocationPanelProps = {
  openEvent: EventTypes.Event;
  signalDetectionsByStation: SignalDetectionTypes.SignalDetection[];
  associatedSignalDetections: SignalDetectionTypes.SignalDetection[];
  distances: EventTypes.LocationToStationDistance[];
  selectedSdIds: string[];
  sdIdsToShowFk: string[];
  measurementMode: AnalystWorkspaceTypes.MeasurementMode;
  widthOfDisplayPx: number;
  location: AnalystWorkspaceTypes.LocationSolutionState;

  setSelectedSdIds(ids: string[]): void;
  setSdIdsToShowFk(signalDetectionIds: string[]): void;
  setMeasurementModeEntries(entries: Immutable.Map<string, boolean>): void;
  setSelectedLocationSolution(locationSolutionSetId: string, locationSolutionId: string): void;
  setSelectedPreferredLocationSolution(
    preferredLocationSolutionSetId: string,
    preferredLocationSolutionId: string
  ): void;
} & ChildProps<LocationMutations>;

export interface LocationPanelState {
  outstandingLocateCall: boolean;
  sdDefiningChanges: SignalDetectionTableRowChanges[];
}

/**
 * Mutations used by the Location
 */
export interface LocationMutations {
  locateEvent: MutationFunction<{}>;
  createEvent: MutationFunction<{}>;
  updateFeaturePredictions: MutationFunction<{}>;
  updateDetections: MutationFunction<{}>;
  rejectDetections: MutationFunction<{}>;
  changeSignalDetectionAssociations: MutationFunction<{}>;
}

/**
 * Changes to LocationSD table per row
 */
export interface SignalDetectionTableRowChanges {
  signalDetectionId: string;
  // Defining diffs
  arrivalTimeDefining: DefiningChange;
  slownessDefining: DefiningChange;
  azimuthDefining: DefiningChange;
}

/**
 * List of tool tip messages for location button
 */
export enum LocateButtonTooltipMessage {
  NotEnoughDefiningBehaviors = 'minimum defining behaviors must be set',
  Correct = 'Calculates a new event location',
  BadLocationAttributes = 'Location attributes (depth, latitude, or longitude) are not within constraints',
  InvalidData = 'Error in props or query'
}

export interface LocationSDRowDiffs {
  isAssociatedDiff: boolean;
  arrivalTimeDefining: DefiningChange;
  slownessDefining: DefiningChange;
  azimuthDefining: DefiningChange;
  // Value diffs
  channelNameDiff?: boolean;
  arrivalTimeDiff?: boolean;
  azimuthObsDiff?: boolean;
  slownessObsDiff?: boolean;
  phaseDiff?: boolean;
}

export interface DefiningStatus {
  arrivalTimeDefining: DefiningChange;
  slownessDefining: DefiningChange;
  azimuthDefining: DefiningChange;
}

export interface SignalDetectionSnapshotWithDiffs extends EventTypes.SignalDetectionSnapshot {
  diffs: LocationSDRowDiffs;
  rejectedOrUnassociated: boolean;
}

/**
 * Props mapped in from Redux state
 */
export interface LocationReduxProps {
  // Passed in from golden-layout
  glContainer?: GoldenLayout.Container;
  // The currently-open processing interval time range
  currentTimeInterval: CommonTypes.TimeRange;
  // The currently-open event hypothesis IDs
  openEventId: string;
  // The currently-selected signal detection IDs
  selectedSdIds: string[];
  // used for additional time range values
  analystActivity: AnalystWorkspaceTypes.AnalystActivity;
  /** The measurement mode */
  measurementMode: AnalystWorkspaceTypes.MeasurementMode;
  /** The signal detections selected to show fk */
  sdIdsToShowFk: string[];
  /** The selected location state */
  location: AnalystWorkspaceTypes.LocationSolutionState;

  setOpenEventId(
    event: EventTypes.Event | undefined,
    latestLocationSolutionSet: EventTypes.LocationSolutionSet | undefined,
    preferredLocationSolutionId: string | undefined
  ): void;
  setSelectedEventIds(ids: string[]): void;
  setSelectedSdIds(ids: string[]): void;
  setSdIdsToShowFk(signalDetectionIds: string[]): void;
  setMeasurementModeEntries(entries: Immutable.Map<string, boolean>): void;
  setSelectedLocationSolution(locationSolutionSetId: string, locationSolutionId: string): void;
  setSelectedPreferredLocationSolution(
    preferredLocationSolutionSetId: string,
    preferredLocationSolutionId: string
  ): void;
}

/**
 * Location Props
 */
export type LocationProps = LocationReduxProps &
  ChildProps<LocationMutations> &
  ProcessingStationTypes.DefaultStationsQueryProps &
  SignalDetectionTypes.SignalDetectionsByStationQueryProps &
  EventTypes.EventsInTimeRangeQueryProps;
