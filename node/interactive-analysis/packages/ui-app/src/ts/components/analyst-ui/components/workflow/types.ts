import { CommonTypes, WorkflowTypes } from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { Client } from '@gms/ui-apollo';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { ChildProps, MutationFunction } from 'react-apollo';

/**
 * Mutations used by the workflow display
 */
export interface WorkflowMutations {
  // {} because we don't care about mutation results for now, handling that through subscriptions
  markActivityInterval: MutationFunction<{}>;
  markStageInterval: MutationFunction<{}>;
  setTimeInterval: MutationFunction<{}>;
}

/**
 * Props mapped in from Redux
 */
export interface WorkflowReduxProps {
  // passed in from golden-layout
  glContainer?: GoldenLayout.Container;
  client: Client;
  currentStageInterval: AnalystWorkspaceTypes.StageInterval;
  currentTimeInterval: CommonTypes.TimeRange;
  analystActivity: AnalystWorkspaceTypes.AnalystActivity;
  // redux callbacks
  setCurrentStageInterval(stageInterval: AnalystWorkspaceTypes.StageInterval);
}

export interface ExpansionState {
  stageName: string;
  expanded: boolean;
}
/**
 * State for the workflow display
 */
export interface WorkflowState {
  // in seconds
  startTimeSecs: number;
  // in seconds
  endTimeSecs: number;
  // in seconds
  intervalDurationSecs: number;
  expansionStates: ExpansionState[];
}

export type WorkflowProps = WorkflowReduxProps &
  ChildProps<WorkflowMutations> &
  WorkflowTypes.StagesQueryProps;
