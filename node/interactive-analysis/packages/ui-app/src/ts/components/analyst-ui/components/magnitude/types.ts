import {
  CommonTypes,
  EventTypes,
  ProcessingStationTypes,
  SignalDetectionTypes
} from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { ChildProps, MutationFunction } from 'react-apollo';
import { StationMagnitudeSdData } from './components/station-magnitude/types';

export enum MagDefiningStates {
  ALL = 'ALL',
  NONE = 'NONE',
  SOME = 'SOME',
  UNDEFINED = 'UNDEFINED'
}
export interface AmplitudesByStation {
  stationName: string;
  validSignalDetectionForMagnitude: Map<EventTypes.MagnitudeType, boolean>;
  magTypeToAmplitudeMap: Map<any, StationMagnitudeSdData>;
}

/**
 * Mutations used by the Magnitude
 */
export interface MagnitudeMutations {
  computeNetworkMagnitudeSolution: MutationFunction<{}>;
}

/**
 * Magnitude State
 */
export interface MagnitudeComponentState {
  displayedMagnitudeTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes;
}

/**
 * Magnitude Props
 */
export interface MagnitudePanelProps {
  stations: ProcessingStationTypes.ProcessingStation[];
  associatedSignalDetections: SignalDetectionTypes.SignalDetection[];
  currentlyOpenEvent: EventTypes.Event;
  location: AnalystWorkspaceTypes.LocationSolutionState;
  widthPx: number;
  selectedSdIds: string[];
  displayedMagnitudeTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes;
  eventsInTimeRange: EventTypes.Event[];
  computeNetworkMagnitudeSolution: MutationFunction<{}>;
  magnitudeTypesForPhase: Map<CommonTypes.PhaseType, EventTypes.MagnitudeType[]>;
  openEventId: string;

  setSelectedSdIds(ids: string[]): void;
  setSelectedLocationSolution(locationSolutionSetId: string, locationSolutionId: string): void;
  setDisplayedMagnitudeTypes(
    displayedMagnitudeTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes
  ): void;
}

/** Repeated arguments for network mag column defs */
export const magnitudeColumnRepeatedArguments = {
  resizable: false,
  sortable: true,
  filter: true,
  lockPosition: true
};

/**
 * Props mapped in from Redux state
 */
export interface MagnitudeReduxProps {
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
  /** The selected location state */
  location: AnalystWorkspaceTypes.LocationSolutionState;

  setSelectedSdIds(ids: string[]): void;
  setSelectedLocationSolution(locationSolutionSetId: string, locationSolutionId: string): void;
}

/**
 * Magnitude Props
 */
export type MagnitudeProps = MagnitudeReduxProps &
  ChildProps<MagnitudeMutations> &
  ProcessingStationTypes.DefaultStationsQueryProps &
  SignalDetectionTypes.SignalDetectionsByStationQueryProps &
  EventTypes.EventsInTimeRangeQueryProps;
