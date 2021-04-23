import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AnalystWorkspaceOperations, AppState } from '@gms/ui-state';
import React from 'react';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations, ReactApolloQueries } from '~analyst-ui/react-apollo-components';
import { Events } from './events-component';
import { EventsProps, EventsReduxProps } from './types';

// Map parts of redux state into this component as props
const mapStateToProps = (state: AppState): Partial<EventsReduxProps> => ({
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  openEventId: state.analystWorkspaceState.openEventId,
  selectedEventIds: state.analystWorkspaceState.selectedEventIds
});

// Map actions dispatch callbacks into this component as props
const mapDispatchToProps = (dispatch): Partial<EventsReduxProps> =>
  bindActionCreators(
    {
      setOpenEventId: AnalystWorkspaceOperations.setOpenEventId,
      setSelectedEventIds: AnalystWorkspaceActions.setSelectedEventIds
    } as any,
    dispatch
  );

/**
 * Higher-order component react-redux(react-apollo(EventList))
 */
export const ReduxApolloEventsContainer: React.ComponentClass<Pick<{}, never>> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  // Order matters need to initialize updateEvents mutation before graphql autoOpenEvent below is executed
  ReactApolloQueries.graphqlDefaultProcessingStationsQuery(),
  ReactApolloQueries.graphqlWorkspaceStateQuery(),
  ReactApolloMutations.graphqlUpdateEventsMutation<EventsProps>(),
  ReactApolloMutations.graphqlSaveEventMutation<EventsProps>(),
  ReactApolloMutations.graphqlSaveAllModifiedEvents<EventsProps>(),
  ReactApolloQueries.graphqlEventsInTimeRangeQuery<EventsProps>(),
  ReactApolloQueries.graphqlSignalDetectionsByStationQuery<EventsProps>()
)(Events);
