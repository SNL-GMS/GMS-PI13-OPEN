import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AnalystWorkspaceOperations, AppState } from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations, ReactApolloQueries } from '~analyst-ui/react-apollo-components';
import { Map } from './map-component';
import { MapProps, MapReduxProps } from './types';

// map parts of redux state into this component as props
const mapStateToProps = (state: AppState): Partial<MapReduxProps> => ({
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  selectedEventIds: state.analystWorkspaceState.selectedEventIds,
  openEventId: state.analystWorkspaceState.openEventId,
  selectedSdIds: state.analystWorkspaceState.selectedSdIds,
  measurementMode: state.analystWorkspaceState.measurementMode,
  sdIdsToShowFk: state.analystWorkspaceState.sdIdsToShowFk
});

// map actions dispatch callbacks into this component as props
const mapDispatchToProps = (dispatch): Partial<MapReduxProps> =>
  bindActionCreators(
    {
      setSelectedEventIds: AnalystWorkspaceActions.setSelectedEventIds,
      setSelectedSdIds: AnalystWorkspaceActions.setSelectedSdIds,
      setOpenEventId: AnalystWorkspaceOperations.setOpenEventId,
      setSdIdsToShowFk: AnalystWorkspaceActions.setSdIdsToShowFk,
      setMeasurementModeEntries: AnalystWorkspaceOperations.setMeasurementModeEntries
    } as any,
    dispatch
  );

/**
 * higher-order component react-redux(react-apollo(Map))
 */
export const ReduxApolloMap: React.ComponentClass<Pick<{}, never>> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  ReactApolloQueries.graphqlDefaultProcessingStationsQuery(),
  // Order matters need to initialize updateEvents mutation before graphql autoOpenEvent below is executed
  ReactApolloMutations.graphqlUpdateEventsMutation<MapProps>(),
  ReactApolloQueries.graphqlEventsInTimeRangeQuery<MapProps>(),
  ReactApolloQueries.graphqlSignalDetectionsByStationQuery<MapProps>(),
  ReactApolloMutations.graphqlUpdateDetectionsMutation<MapProps>(),
  ReactApolloMutations.graphqlRejectDetectionsMutation<MapProps>(),
  ReactApolloMutations.graphqlChangeSignalDetectionsAssociationsMutation<MapProps>(),
  ReactApolloMutations.graphqlCreateEventMutation<MapProps>()
)(Map);
