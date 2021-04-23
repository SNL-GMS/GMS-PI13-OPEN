import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AnalystWorkspaceOperations, AppState } from '@gms/ui-state';
import React from 'react';
import { withApollo } from 'react-apollo';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations, ReactApolloQueries } from '~analyst-ui/react-apollo-components';
import { ReactApolloQueries as ReactApolloGeneralQueries } from '~components/react-apollo-components';
import { AzimuthSlowness } from './azimuth-slowness-component';
import { AzimuthSlownessProps, AzimuthSlownessReduxProps } from './types';

/**
 * Container component for Azimuth Slowness
 * Handles mapping of state/props through redux/apollo
 */

/**
 *  Mapping between the current redux state and props for the Azimuth Slowness Display
 */
const mapStateToProps = (state: AppState): Partial<AzimuthSlownessReduxProps> => ({
  currentStageInterval: state.analystWorkspaceState.currentStageInterval,
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  selectedSdIds: state.analystWorkspaceState.selectedSdIds,
  openEventId: state.analystWorkspaceState.openEventId,
  sdIdsToShowFk: state.analystWorkspaceState.sdIdsToShowFk,
  location: state.analystWorkspaceState.location,
  channelFilters: state.analystWorkspaceState.channelFilters,
  defaultSignalDetectionPhase: state.analystWorkspaceState.defaultSignalDetectionPhase,
  selectedSortType: state.analystWorkspaceState.selectedSortType
});

/**
 * Map actions dispatch callbacks into this component as props
 */
const mapDispatchToProps = (dispatch): Partial<AzimuthSlownessReduxProps> =>
  bindActionCreators(
    {
      setSelectedSdIds: AnalystWorkspaceActions.setSelectedSdIds,
      setSdIdsToShowFk: AnalystWorkspaceActions.setSdIdsToShowFk,
      setChannelFilters: AnalystWorkspaceActions.setChannelFilters,
      setMeasurementModeEntries: AnalystWorkspaceOperations.setMeasurementModeEntries
    } as any,
    dispatch
  );

/**
 * The higher-order component react-redux(react-apollo(AzimuthSlowness))
 */
export const ReduxApolloAzimuthSlowness: React.ComponentClass<Pick<{}, never>> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  withApollo,
  ReactApolloQueries.graphqlDefaultProcessingStationsQuery(),
  ReactApolloGeneralQueries.graphqlUIConfigurationQuery(),
  ReactApolloQueries.graphqlSignalDetectionsByStationQuery<AzimuthSlownessProps>(),
  ReactApolloQueries.graphqlEventsInTimeRangeQuery(),
  ReactApolloMutations.graphqlComputeFksMutation<AzimuthSlownessProps>(),
  ReactApolloMutations.graphqlMarkFksReviewedMutation<AzimuthSlownessProps>()
)(AzimuthSlowness);
