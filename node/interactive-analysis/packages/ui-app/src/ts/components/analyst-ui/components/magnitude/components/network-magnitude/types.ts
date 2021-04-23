import { EventTypes } from '@gms/common-graphql';
import { Row } from '@gms/ui-core-components';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { MutationFunction } from 'react-apollo';

/**
 * NetworkMagnitude State
 */
// tslint:disable-next-line:no-empty-interface
export interface NetworkMagnitudeState {}

/**
 * options that can be passed to ag grid
 */

export interface Options {
  alignedGrids: any[];
  rowClass: string;
}

/**
 * NetworkMagnitude Props
 */
// tslint:disable-next-line: no-empty-interface
export interface NetworkMagnitudeProps {
  options?: Options;
  locationSolutionSet: EventTypes.LocationSolutionSet;
  preferredSolutionId: string;
  selectedSolutionId: string;
  computeNetworkMagnitudeSolution: MutationFunction<{}>;
  displayedMagnitudeTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes;

  setSelectedLocationSolution(locationSolutionSetId: string, locationSolutionId: string): void;
}

/**
 * Data mapped to on a per-magnitude basis
 */
export interface NetworkMagnitudeData {
  magnitude: number;
  stdDeviation: number;
  numberOfDefiningStations: number;
  numberOfNonDefiningStations: number;
}

/**
 * Table row object for Network Magnitude
 */
export interface NetworkMagnitudeRow extends Row {
  id: string;
  isPreferred: boolean;
  location: string;
  dataForMagnitude: Map<EventTypes.MagnitudeType, NetworkMagnitudeData>;
}
