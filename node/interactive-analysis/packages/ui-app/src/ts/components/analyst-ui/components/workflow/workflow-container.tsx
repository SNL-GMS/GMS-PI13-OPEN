import { compose } from '@gms/common-util';
import { AnalystWorkspaceOperations, AppState } from '@gms/ui-state';
import { withApollo } from 'react-apollo';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations, ReactApolloQueries } from '~analyst-ui/react-apollo-components';
import { WorkflowProps, WorkflowReduxProps } from './types';
import { Workflow } from './workflow-component';

const mapStateToProps = (state: AppState): Partial<WorkflowReduxProps> => ({
  currentStageInterval: state.analystWorkspaceState.currentStageInterval,
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined
});

const mapDispatchToProps = (dispatch): Partial<WorkflowReduxProps> =>
  bindActionCreators(
    {
      setCurrentStageInterval: AnalystWorkspaceOperations.setCurrentStageInterval
    } as any,
    dispatch
  );

export const ReduxApolloWorkflowContainer = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  withApollo,
  ReactApolloQueries.graphqlWorkspaceStateQuery(),
  ReactApolloQueries.graphqlStagesQuery(),
  ReactApolloMutations.graphqlMarkActivityIntervalMutation<WorkflowProps>(),
  ReactApolloMutations.graphqlMarkStageIntervalMutation(),
  ReactApolloMutations.graphqlSetTimeIntervalMutation()
)(Workflow);
