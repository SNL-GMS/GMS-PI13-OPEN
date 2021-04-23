import { ReferenceStationTypes, SohTypes } from '@gms/common-graphql';
import { StationAndStationGroupSoh } from '@gms/common-graphql/lib/graphql/soh/types';
import { ApolloError } from 'apollo-client';
import { ActionWithPayload } from '../util/action-helper';

export type SET_SELECTED_STATION_IDS = ActionWithPayload<string[]>;

export type SET_SELECTED_PROCESSING_STATION = ActionWithPayload<
  ReferenceStationTypes.ReferenceStation
>;

export type SET_UNMODIFIED_PROCESSING_STATION = ActionWithPayload<
  ReferenceStationTypes.ReferenceStation
>;

export type SET_SOH_STATUS = ActionWithPayload<SohStatus>;

export type SET_SELECTED_ACEI_TYPE = ActionWithPayload<SohTypes.AceiType>;

/**
 * The SOH Status
 */
export interface SohStatus {
  /** timestamp of when the data was last updated */
  lastUpdated: number;
  /** true if the initial apollo query is still loading (has not completed) */
  loading: boolean;
  /** any error information that may have occurred on the initial apollo query */
  error: ApolloError;
  /** the station and station group SOH data */
  stationAndStationGroupSoh: StationAndStationGroupSoh;
}

export interface DataAcquisitionWorkspaceState {
  selectedStationIds: string[];
  selectedAceiType: SohTypes.AceiType;
  selectedProcessingStation: ReferenceStationTypes.ReferenceStation;
  unmodifiedProcessingStation: ReferenceStationTypes.ReferenceStation;
  data: {
    sohStatus: SohStatus;
  };
}
