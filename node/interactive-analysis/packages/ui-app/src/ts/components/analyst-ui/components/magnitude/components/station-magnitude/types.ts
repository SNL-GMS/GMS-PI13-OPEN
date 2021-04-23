import { EventTypes } from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import Immutable from 'immutable';
import { MutationFunction } from 'react-apollo';
import { AmplitudesByStation } from '../../types';

/**
 * StationMagnitude State
 */
// tslint:disable-next-line:no-empty-interface
export interface StationMagnitudeState {
  computeNetworkMagnitudeSolutionStatus: Immutable.Map<
    string,
    [{ stationName: string; rational: string }]
  >;
}

export interface MagnitudeAndSdData {
  magSolution: EventTypes.NetworkMagnitudeSolution;
  sdData: StationMagnitudeSdData;
}

/**
 * options that can be passed to ag grid
 */
export interface Options {
  alignedGrids: any[];
}

/**
 * StationMagnitude Props
 */
export interface StationMagnitudeProps {
  options?: Options;
  amplitudesByStation: AmplitudesByStation[];
  historicalMode: boolean;
  selectedSdIds: string[];
  locationSolution: EventTypes.LocationSolution;
  displayedMagnitudeTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes;
  computeNetworkMagnitudeSolution: MutationFunction<{}>;
  openEventId: string;

  setSelectedSdIds(ids: string[]): void;
  checkBoxCallback(
    magnitudeType: EventTypes.MagnitudeType,
    stationNames: string[],
    defining: boolean
  ): Promise<[{ stationName: string; rational: string }]>;
}

export interface StationMagnitudeSdData {
  channel: string;
  phase: string;
  amplitudeValue: number;
  amplitudePeriod: number;
  signalDetectionId: string;
  time: number;
  stationName: string;
  flagForReview: boolean;
}

export interface MagnitudeDataForRow {
  channel: string;
  signalDetectionId: string;
  phase: string;
  amplitudeValue: number;
  amplitudePeriod: number;
  flagForReview: boolean;
  defining: boolean;
  mag: number;
  res: number;
  hasMagnitudeCalculationError: boolean;
  computeNetworkMagnitudeSolutionStatus: string;
}

/**
 * Table row object for station magnitude
 */
export interface StationMagnitudeRow {
  id: string;
  dataForMagnitude: Map<EventTypes.MagnitudeType, MagnitudeDataForRow>;
  station: string;
  dist: number;
  azimuth: number;
  selectedSdIds: string[];
  historicalMode: boolean;
  azimuthTooltip: string;
  checkBoxCallback(
    magnitudeType: EventTypes.MagnitudeType,
    stationNames: string[],
    defining: boolean
  ): Promise<void>;
}

export enum DefiningTypes {
  MB = 'Mb',
  MS = 'Ms',
  MB_MLE = 'Mb Mle',
  MS_MLE = 'Ms Mle'
}

export enum DefiningChange {
  CHANGED_TO_TRUE,
  CHANGED_TO_FALSE,
  CHANGED_TO_OTHER,
  NO_CHANGE
}

export interface StationMagAndSignalDetection {
  stationName: string;
  magnitudeAndSdData: Map<EventTypes.MagnitudeType, MagnitudeAndSdData>;
}
