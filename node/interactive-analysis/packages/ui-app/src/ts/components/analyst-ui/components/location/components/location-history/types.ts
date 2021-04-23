import { EventTypes } from '@gms/common-graphql';
import { Row } from '@gms/ui-core-components';
import { AnalystWorkspaceTypes } from '@gms/ui-state';

/**
 * LocationHistory Props
 */
export interface LocationHistoryProps {
  // Current open event
  event: EventTypes.Event;
  location: AnalystWorkspaceTypes.LocationSolutionState;
  setSelectedLocationSolution(locationSolutionSetId: string, locationSolutionId: string): void;
  setSelectedPreferredLocationSolution(
    preferredLocationSolutionSetId: string,
    preferredLocationSolutionId: string
  ): void;
}
/**
 * Location History State
 */
// tslint:disable-next-line: no-empty-interface
export interface LocationHistoryState {}

export interface LocationHistoryRow extends Row {
  locationSolutionId: string;
  locationSetId: string;
  locType: string;
  latestLSSId: string;
  lat: number;
  lon: number;
  depth: number;
  time: string;
  restraint: string;
  smajax: number;
  sminax: number;
  strike: number;
  stdev: number;
  depthRestraintType: EventTypes.DepthRestraintType;
  count: number;
  selectedLocationSolutionSetId: string;
  isLocationSolutionSetPreferred?: boolean;
  isLastInLSSet?: boolean;
  isFirstInLSSet?: boolean;
  preferred?: boolean;
  locationGroup?: LocationHistoryRow[];
  setSelectedPreferredLocationSolution(
    preferredLocationSolutionSetId: string,
    preferredLocationSolutionId: string
  ): void;
  setToSave(locationSolutionSetId: string, locationSolutionId: string): void;
}
