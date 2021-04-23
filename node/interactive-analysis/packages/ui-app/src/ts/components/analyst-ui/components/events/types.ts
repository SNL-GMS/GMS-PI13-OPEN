import {
  CommonTypes,
  EventTypes,
  ProcessingStationTypes,
  SignalDetectionTypes
} from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { ChildProps, MutationFunction } from 'react-apollo';
import { gmsColors, semanticColors } from '~scss-config/color-preferences';

/**
 * Types of events which can be show
 */
export enum EventFilters {
  EDGE = 'Edge',
  COMPLETED = 'Completed'
}
export const eventFilterToColorMap: Map<any, string> = new Map();
eventFilterToColorMap.set(EventFilters.EDGE, gmsColors.gmsBackground);
eventFilterToColorMap.set(EventFilters.COMPLETED, semanticColors.analystComplete);

/**
 * Table row object for events
 */
export interface EventsRow {
  id: string;
  eventHypId: string;
  isOpen: boolean;
  stageId: string;
  lat: number;
  lon: number;
  depth: number;
  time: number;
  activeAnalysts: string[];
  numDetections: number;
  status: string;
  edgeEvent: boolean;
  signalDetectionConflicts: SignalDetectionConflict[];
}

export interface SignalDetectionConflict {
  stationName: string;
  id: string;
  arrivalTime: number;
  phase: CommonTypes.PhaseType;
}
/**
 * Event list local state
 */
export interface EventsState {
  currentTimeInterval: CommonTypes.TimeRange;
  suppressScrollOnNewData: boolean;
  showEventOfType: Map<EventFilters, boolean>;
}

/**
 * Mutations used in the event list
 */
export interface EventsMutations {
  updateEvents: MutationFunction<{}>;
  saveEvent: MutationFunction<{}>;
  saveAllModifiedEvents: MutationFunction<{}>;
}

/**
 * Props mapped in from Redux state
 */
export interface EventsReduxProps {
  // Passed in from golden-layout
  glContainer?: GoldenLayout.Container;
  currentTimeInterval: CommonTypes.TimeRange;
  analystActivity: AnalystWorkspaceTypes.AnalystActivity;
  openEventId: string;
  selectedEventIds: string[];

  // callbacks
  setOpenEventId(
    event: EventTypes.Event | undefined,
    latestLocationSolutionSet: EventTypes.LocationSolutionSet | undefined,
    preferredLocationSolutionId: string | undefined
  ): void;
  setSelectedEventIds(ids: string[]): void;
}
export interface SignalDetectionHypothesisWithStation
  extends SignalDetectionTypes.SignalDetectionHypothesis {
  stationName?: string;
}

/**
 * Consolidated props type for event list
 */
export type EventsProps = EventsReduxProps &
  ChildProps<EventsMutations> &
  EventTypes.EventsInTimeRangeQueryProps &
  ProcessingStationTypes.DefaultStationsQueryProps &
  SignalDetectionTypes.SignalDetectionsByStationQueryProps &
  CommonTypes.WorkspaceStateProps;
