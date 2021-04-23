import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AnalystWorkspaceOperations, AppState } from '@gms/ui-state';
import React from 'react';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations, ReactApolloQueries } from '~analyst-ui/react-apollo-components';
import { SignalDetections } from './signal-detections-component';
import { SignalDetectionsProps, SignalDetectionsReduxProps } from './types';

// Map parts of redux state into this component as props
const mapStateToProps = (state: AppState): Partial<SignalDetectionsReduxProps> => ({
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  selectedSdIds: state.analystWorkspaceState.selectedSdIds,
  openEventId: state.analystWorkspaceState.openEventId,
  measurementMode: state.analystWorkspaceState.measurementMode,
  sdIdsToShowFk: state.analystWorkspaceState.sdIdsToShowFk
});

// Map actions dispatch callbacks into this component as props
const mapDispatchToProps = (dispatch): Partial<SignalDetectionsReduxProps> =>
  bindActionCreators(
    {
      setSelectedSdIds: AnalystWorkspaceActions.setSelectedSdIds,
      setSdIdsToShowFk: AnalystWorkspaceActions.setSdIdsToShowFk,
      setMeasurementModeEntries: AnalystWorkspaceOperations.setMeasurementModeEntries
    } as any,
    dispatch
  );

/**
 * Higher-order component react-redux(react-apollo(SignalDetectionList))
 */
export const ReduxApolloSignalDetectionsContainer: React.ComponentClass<Pick<{}, never>> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  ReactApolloQueries.graphqlDefaultProcessingStationsQuery(),
  ReactApolloQueries.graphqlEventsInTimeRangeQuery(),
  ReactApolloQueries.graphqlSignalDetectionsByStationQuery<SignalDetectionsProps>(),
  ReactApolloMutations.graphqlUpdateDetectionsMutation<SignalDetectionsProps>(),
  ReactApolloMutations.graphqlRejectDetectionsMutation<SignalDetectionsProps>(),
  ReactApolloMutations.graphqlChangeSignalDetectionsAssociationsMutation<SignalDetectionsProps>(),
  ReactApolloMutations.graphqlCreateEventMutation<SignalDetectionsProps>()
)(SignalDetections);
