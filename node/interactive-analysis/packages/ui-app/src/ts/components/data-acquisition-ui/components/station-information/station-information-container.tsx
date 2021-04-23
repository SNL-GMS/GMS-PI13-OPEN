import { compose } from '@gms/common-util';
import {
  AppState,
  DataAcquisitionWorkspaceActions,
  DataAcquisitionWorkspaceOperations
} from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloQueries } from '~data-acquisition-ui/react-apollo-components';
import { StationInformation } from './station-information-component';
import { StationInformationReduxProps } from './types';

/**
 * Mapping redux state to the properties of the component
 *
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<StationInformationReduxProps> => ({
  selectedStationIds: state.dataAcquisitionWorkspaceState.selectedStationIds,
  selectedProcessingStation: state.dataAcquisitionWorkspaceState.selectedProcessingStation,
  unmodifiedProcessingStation: state.dataAcquisitionWorkspaceState.unmodifiedProcessingStation
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 *
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<StationInformationReduxProps> =>
  bindActionCreators(
    {
      setSelectedStationIds: DataAcquisitionWorkspaceOperations.setSelectedStationIds,
      setSelectedProcessingStation: DataAcquisitionWorkspaceActions.setSelectedProcessingStation,
      setUnmodifiedProcessingStation: DataAcquisitionWorkspaceActions.setUnmodifiedProcessingStation
    } as any,
    dispatch
  );

/**
 * A new apollo component that's wrapping the StationConfiguration component and injecting
 * apollo graphQL queries and mutations.
 */
export const ReduxApolloStationInformationContainer: React.ComponentClass<Pick<
  {},
  never
>> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  ReactApolloQueries.graphqlDefaultReferenceStationsQuery()
)(StationInformation);
