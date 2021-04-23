import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AppState } from '@gms/ui-state';
import React from 'react';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations, ReactApolloQueries } from '~analyst-ui/react-apollo-components';
import { InteractionProvider } from './interaction-provider-component';
import { InteractionProviderProps, InteractionProviderReduxProps } from './types';

// Map parts of redux state into this component as props
const mapStateToProps = (state: AppState): Partial<InteractionProviderReduxProps> => ({
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  openEventId: state.analystWorkspaceState.openEventId,
  historyActionInProgress: state.analystWorkspaceState.historyActionInProgress
});

// Map actions dispatch callbacks into this component as props
const mapDispatchToProps = (dispatch): Partial<InteractionProviderReduxProps> =>
  bindActionCreators(
    {
      incrementHistoryActionInProgress: AnalystWorkspaceActions.incrementHistoryActionInProgress,
      decrementHistoryActionInProgress: AnalystWorkspaceActions.decrementHistoryActionInProgress
    } as any,
    dispatch
  );

/**
 * Higher-order component react-redux
 */
export const ReduxApolloInteractionProviderContainer: React.ComponentClass<Pick<
  {},
  never
>> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  ReactApolloQueries.graphqlWorkspaceStateQuery(),
  ReactApolloMutations.graphqlUpdateEventsMutation<InteractionProviderProps>(),
  ReactApolloMutations.graphqlSaveEventMutation<InteractionProviderProps>(),
  ReactApolloMutations.graphqlSaveAllModifiedEvents<InteractionProviderProps>(),
  ReactApolloMutations.graphqlUndoHistoryMutation<InteractionProviderProps>(),
  ReactApolloMutations.graphqlRedoHistoryMutation<InteractionProviderProps>()
)(InteractionProvider);
