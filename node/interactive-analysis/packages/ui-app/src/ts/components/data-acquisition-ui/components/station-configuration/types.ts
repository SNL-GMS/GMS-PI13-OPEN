import { CommonTypes, ReferenceStationTypes } from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { ChildMutateProps, MutationFunction } from 'react-apollo';

/**
 * StationConfiguration Redux Props
 */
export interface StationConfigurationReduxProps {
  // Redux state, added to props via mapStateToProps
  glContainer?: GoldenLayout.Container;
  // Redux actions, added to props via mapDispatchToProps
  selectedStationIds: string[];
  selectedProcessingStation: ReferenceStationTypes.ReferenceStation;
  unmodifiedProcessingStation: ReferenceStationTypes.ReferenceStation;
  setSelectedStationIds(ids: string[]): void;
  setSelectedProcessingStation(statioRefReferenceStation): void;
  setUnmodifiedProcessingStation(statiRefeReferenceStation): void;
}

/**
 * Mutations used by StationConfiguration
 */
export interface StationConfigurationMutations {
  saveReferenceStation: MutationFunction<{}>;
}

/**
 * StationConfiguration State
 */
export interface StationConfigurationState {
  station: string;
  description: string;
  sites: ReferenceStationTypes.ReferenceSite[];
  latitude: number;
  longitude: number;
  elevation: number;
  stationType: CommonTypes.StationType;
}

/**
 * StationInformation Props
 */
export type StationConfigurationProps = StationConfigurationReduxProps &
  ReferenceStationTypes.DefaultStationsQueryProps &
  ChildMutateProps<StationConfigurationMutations>;
