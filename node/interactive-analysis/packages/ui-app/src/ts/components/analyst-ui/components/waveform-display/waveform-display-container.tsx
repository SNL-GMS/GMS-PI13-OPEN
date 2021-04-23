import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AnalystWorkspaceOperations, AppState } from '@gms/ui-state';
import React from 'react';
import { withApollo } from 'react-apollo';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import {
  ReactApolloMutations,
  ReactApolloQueries as ReactApolloUIQueries
} from '~analyst-ui/react-apollo-components';
import { ReactApolloQueries } from '~components/react-apollo-components';
import { WaveformDisplayProps, WaveformDisplayReduxProps } from './types';
import { WaveformDisplay } from './waveform-display-component';

// map parts of redux state into this component as props
const mapStateToProps = (state: AppState): Partial<WaveformDisplayReduxProps> => ({
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  currentOpenEventId: state.analystWorkspaceState.openEventId,
  selectedSdIds: state.analystWorkspaceState.selectedSdIds,
  location: state.analystWorkspaceState.location,
  measurementMode: state.analystWorkspaceState.measurementMode,
  sdIdsToShowFk: state.analystWorkspaceState.sdIdsToShowFk,
  channelFilters: state.analystWorkspaceState.channelFilters,
  defaultSignalDetectionPhase: state.analystWorkspaceState.defaultSignalDetectionPhase,
  selectedSortType: state.analystWorkspaceState.selectedSortType,
  openEventId: state.analystWorkspaceState.openEventId,
  keyPressActionQueue: state.analystWorkspaceState.keyPressActionQueue
});

// map actions dispatch callbacks into this component as props
const mapDispatchToProps = (dispatch): Partial<WaveformDisplayReduxProps> =>
  bindActionCreators(
    {
      setMode: AnalystWorkspaceOperations.setMode,
      setOpenEventId: AnalystWorkspaceOperations.setOpenEventId,
      setSelectedSdIds: AnalystWorkspaceActions.setSelectedSdIds,
      setSdIdsToShowFk: AnalystWorkspaceActions.setSdIdsToShowFk,
      setMeasurementModeEntries: AnalystWorkspaceOperations.setMeasurementModeEntries,
      setChannelFilters: AnalystWorkspaceActions.setChannelFilters,
      setDefaultSignalDetectionPhase: AnalystWorkspaceActions.setDefaultSignalDetectionPhase,
      setSelectedSortType: AnalystWorkspaceActions.setSelectedSortType,
      setKeyPressActionQueue: AnalystWorkspaceActions.setKeyPressActionQueue
    } as any,
    dispatch
  );

/**
 * higher-order component react-redux(react-apollo(WaveformDisplay))
 */
export const ReduxApolloWaveformDisplay: React.ComponentClass<Pick<{}, never>> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  withApollo,
  ReactApolloUIQueries.graphqlDefaultProcessingStationsQuery(),
  ReactApolloQueries.graphqlUIConfigurationQuery(),
  // Order matters need to initialize updateEvents mutation before graphql autoOpenEvent below is executed
  ReactApolloMutations.graphqlUpdateEventsMutation<WaveformDisplayProps>(),
  ReactApolloMutations.graphqlMarkAmplitudeMeasurementReviewed(),
  ReactApolloUIQueries.graphqlEventsInTimeRangeQuery<WaveformDisplayProps>(),
  ReactApolloUIQueries.graphqlSignalDetectionsByStationQuery<WaveformDisplayProps>(),
  ReactApolloUIQueries.graphqlQcMasksByChannelNameQuery<WaveformDisplayProps>(),
  ReactApolloMutations.graphqlCreateDetectionMutation<WaveformDisplayProps>(),
  ReactApolloMutations.graphqlUpdateDetectionsMutation<WaveformDisplayProps>(),
  ReactApolloMutations.graphqlRejectDetectionsMutation<WaveformDisplayProps>(),
  ReactApolloMutations.graphqlChangeSignalDetectionsAssociationsMutation<WaveformDisplayProps>(),
  ReactApolloMutations.graphqlCreateEventMutation<WaveformDisplayProps>(),
  ReactApolloMutations.graphqlCreateQcMaskMutation<WaveformDisplayProps>(),
  ReactApolloMutations.graphqlUpdateQcMaskMutation<WaveformDisplayProps>(),
  ReactApolloMutations.graphqlRejectQcMaskMutation<WaveformDisplayProps>()
)(WaveformDisplay);
