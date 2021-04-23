import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AppState } from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations, ReactApolloQueries } from '~analyst-ui/react-apollo-components';
import { HistoryComponent } from './history-component';
import { HistoryProps, HistoryReduxProps } from './types';

/**
 * Mapping redux state to the properties of the component
 *
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<HistoryReduxProps> => ({
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  openEventId: state.analystWorkspaceState.openEventId,
  historyActionInProgress: state.analystWorkspaceState.historyActionInProgress
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 *
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<HistoryReduxProps> =>
  bindActionCreators(
    {
      setKeyPressActionQueue: AnalystWorkspaceActions.setKeyPressActionQueue,
      incrementHistoryActionInProgress: AnalystWorkspaceActions.incrementHistoryActionInProgress,
      decrementHistoryActionInProgress: AnalystWorkspaceActions.decrementHistoryActionInProgress
    } as any,
    dispatch
  );

/**
 * A new redux apollo component, that's wrapping the History component and injecting in the redux state
 * and apollo graphQL queries and mutations.
 */
export const ReactApolloHistoryContainer = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  ReactApolloQueries.graphqlHistoryQuery(),
  ReactApolloQueries.graphqlEventsInTimeRangeQuery(),
  ReactApolloMutations.graphqlUndoHistoryMutation<HistoryProps>(),
  ReactApolloMutations.graphqlRedoHistoryMutation<HistoryProps>(),
  ReactApolloMutations.graphqlUndoHistoryByIdMutation<HistoryProps>(),
  ReactApolloMutations.graphqlRedoHistoryByIdMutation<HistoryProps>(),
  ReactApolloMutations.graphqlUndoEventHistoryMutation<HistoryProps>(),
  ReactApolloMutations.graphqlRedoEventHistoryMutation<HistoryProps>(),
  ReactApolloMutations.graphqlUndoEventHistoryByIdMutation<HistoryProps>(),
  ReactApolloMutations.graphqlRedoEventHistoryByIdMutation<HistoryProps>()
)(HistoryComponent);
