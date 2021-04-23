import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AnalystWorkspaceOperations, AppState } from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations, ReactApolloQueries } from '~analyst-ui/react-apollo-components';
import { Location } from './location-component';
import { LocationProps, LocationReduxProps } from './types';

/**
 * Mapping redux state to the properties of the component
 *
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<LocationReduxProps> => ({
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  openEventId: state.analystWorkspaceState.openEventId,
  selectedSdIds: state.analystWorkspaceState.selectedSdIds,
  measurementMode: state.analystWorkspaceState.measurementMode,
  sdIdsToShowFk: state.analystWorkspaceState.sdIdsToShowFk,
  location: state.analystWorkspaceState.location
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 *
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<LocationReduxProps> =>
  bindActionCreators(
    {
      setSelectedSdIds: AnalystWorkspaceActions.setSelectedSdIds,
      setOpenEventId: AnalystWorkspaceOperations.setOpenEventId,
      setSelectedEventIds: AnalystWorkspaceActions.setSelectedEventIds,
      setSdIdsToShowFk: AnalystWorkspaceActions.setSdIdsToShowFk,
      setMeasurementModeEntries: AnalystWorkspaceOperations.setMeasurementModeEntries,
      setSelectedLocationSolution: AnalystWorkspaceOperations.setSelectedLocationSolution,
      setSelectedPreferredLocationSolution:
        AnalystWorkspaceOperations.setSelectedPreferredLocationSolution
    } as any,
    dispatch
  );

/**
 * A new redux apollo component, that's wrapping the Location component and injecting in the redux state
 * and apollo graphQL queries and mutations.
 */
export const ReduxApolloLocationContainer: React.ComponentClass<Pick<{}, never>> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  ReactApolloQueries.graphqlDefaultProcessingStationsQuery(),
  ReactApolloMutations.graphqlUpdateEventsMutation<LocationProps>(),
  ReactApolloMutations.graphqlUpdateFeaturePredictionsMutation<LocationProps>(),
  ReactApolloMutations.graphqlLocateEventMutation<LocationProps>(),
  ReactApolloQueries.graphqlEventsInTimeRangeQuery(),
  ReactApolloQueries.graphqlSignalDetectionsByStationQuery<LocationProps>(),
  ReactApolloMutations.graphqlUpdateDetectionsMutation<LocationProps>(),
  ReactApolloMutations.graphqlRejectDetectionsMutation<LocationProps>(),
  ReactApolloMutations.graphqlChangeSignalDetectionsAssociationsMutation<LocationProps>(),
  ReactApolloMutations.graphqlCreateEventMutation<LocationProps>()
)(Location);
