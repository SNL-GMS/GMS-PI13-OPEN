import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AnalystWorkspaceOperations, AppState } from '@gms/ui-state';
import React from 'react';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations } from '~analyst-ui/react-apollo-components';
import { WeavessDisplayComponentProps, WeavessDisplayProps } from './types';
import { WeavessDisplay } from './weavess-display-component';

// map parts of redux state into this component as props
const mapStateToProps = (state: AppState): Partial<WeavessDisplayProps> => ({
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  currentOpenEventId: state.analystWorkspaceState.openEventId,
  selectedSdIds: state.analystWorkspaceState.selectedSdIds,
  sdIdsToShowFk: state.analystWorkspaceState.sdIdsToShowFk
});

// map actions dispatch callbacks into this component as props
const mapDispatchToProps = (dispatch): Partial<WeavessDisplayProps> =>
  bindActionCreators(
    {
      setMode: AnalystWorkspaceOperations.setMode,
      setOpenEventId: AnalystWorkspaceOperations.setOpenEventId,
      setSelectedSdIds: AnalystWorkspaceActions.setSelectedSdIds,
      setSdIdsToShowFk: AnalystWorkspaceActions.setSdIdsToShowFk,
      setMeasurementModeEntries: AnalystWorkspaceOperations.setMeasurementModeEntries
    } as any,
    dispatch
  );

/**
 * higher-order component react-redux(react-apollo(WeavessDisplay))
 */
export const ReduxApolloWeavessDisplay: React.ComponentClass<
  WeavessDisplayComponentProps,
  never
> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps, null, { forwardRef: true }),
  ReactApolloMutations.graphqlCreateDetectionMutation<WeavessDisplayProps>(true),
  ReactApolloMutations.graphqlUpdateDetectionsMutation<WeavessDisplayProps>(true),
  ReactApolloMutations.graphqlRejectDetectionsMutation<WeavessDisplayProps>(true),
  ReactApolloMutations.graphqlChangeSignalDetectionsAssociationsMutation<WeavessDisplayProps>(true),
  ReactApolloMutations.graphqlCreateEventMutation<WeavessDisplayProps>(true),
  ReactApolloMutations.graphqlCreateQcMaskMutation<WeavessDisplayProps>(true),
  ReactApolloMutations.graphqlUpdateQcMaskMutation<WeavessDisplayProps>(true),
  ReactApolloMutations.graphqlRejectQcMaskMutation<WeavessDisplayProps>(true)
)(WeavessDisplay);
