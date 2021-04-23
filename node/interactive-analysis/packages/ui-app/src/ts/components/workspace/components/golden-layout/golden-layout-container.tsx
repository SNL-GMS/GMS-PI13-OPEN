import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AppState, UserSessionOperations } from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { ReactApolloMutations, ReactApolloQueries } from '~components/react-apollo-components';
import { GoldenLayoutComponent } from './golden-layout-component';
import { GoldenLayoutComponentProps } from './types';

// Map parts of redux state into this component as props
const mapStateToProps = (state: AppState): Partial<GoldenLayoutComponentProps> => ({
  currentTimeInterval: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.timeInterval
    : undefined,
  analystActivity: state.analystWorkspaceState.currentStageInterval
    ? state.analystWorkspaceState.currentStageInterval.interval.activityInterval.analystActivity
    : undefined,
  openLayoutName: state.analystWorkspaceState.openLayoutName,
  keyPressActionQueue: state.analystWorkspaceState.keyPressActionQueue
});

// Map actions dispatch callbacks into this component as props
const mapDispatchToProps = (dispatch): Partial<GoldenLayoutComponentProps> =>
  bindActionCreators(
    {
      setOpenLayoutName: AnalystWorkspaceActions.setOpenLayoutName,
      setKeyPressActionQueue: AnalystWorkspaceActions.setKeyPressActionQueue,
      setAuthStatus: UserSessionOperations.setAuthStatus
    } as any,
    dispatch
  );

/**
 * Connects the AppToolbar to the Redux store and Apollo
 */
export const GoldenLayoutContainer = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  ReactApolloQueries.graphqlVersionInfoQuery(),
  ReactApolloQueries.graphqlUserProfileQuery(),
  ReactApolloMutations.graphqlSetLayoutMutation()
)(GoldenLayoutComponent);
