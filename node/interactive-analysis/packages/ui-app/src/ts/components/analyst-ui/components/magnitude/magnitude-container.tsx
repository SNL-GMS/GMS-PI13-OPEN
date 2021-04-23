import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AnalystWorkspaceOperations, AppState } from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations, ReactApolloQueries } from '~analyst-ui/react-apollo-components';
import { Magnitude } from './magnitude-component';
import { MagnitudeProps, MagnitudeReduxProps } from './types';

/**
 * Mapping redux state to the properties of the component
 *
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<MagnitudeReduxProps> => ({
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  openEventId: state.analystWorkspaceState.openEventId,
  selectedSdIds: state.analystWorkspaceState.selectedSdIds,
  location: state.analystWorkspaceState.location
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 *
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<MagnitudeReduxProps> =>
  bindActionCreators(
    {
      setSelectedSdIds: AnalystWorkspaceActions.setSelectedSdIds,
      setSelectedLocationSolution: AnalystWorkspaceOperations.setSelectedLocationSolution
    } as any,
    dispatch
  );

/**
 * A new redux apollo component, that's wrapping the Magnitude component and injecting in the redux state
 * and apollo graphQL queries and mutations.
 */
export const ReduxApolloMagnitudeContainer: React.ComponentClass<Pick<{}, never>> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  ReactApolloQueries.graphqlDefaultProcessingStationsQuery(),
  ReactApolloMutations.graphqlUpdateEventsMutation<MagnitudeProps>(),
  ReactApolloMutations.graphqlUpdateFeaturePredictionsMutation<MagnitudeProps>(),
  ReactApolloMutations.graphqlLocateEventMutation<MagnitudeProps>(),
  ReactApolloMutations.graphqlComputeNetworkMagnitudeSolutionMutation<MagnitudeProps>(),
  ReactApolloQueries.graphqlEventsInTimeRangeQuery(),
  ReactApolloQueries.graphqlSignalDetectionsByStationQuery<MagnitudeProps>()
)(Magnitude);
