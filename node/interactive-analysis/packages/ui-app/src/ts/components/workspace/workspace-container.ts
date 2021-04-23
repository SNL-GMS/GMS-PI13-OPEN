import { compose } from '@gms/common-util';
import { AnalystWorkspaceActions, AppState, UserSessionOperations } from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { withRouter } from 'react-router';
import { bindActionCreators } from 'redux';
import { WorkspaceProps } from './types';
import { Workspace } from './workspace-component';

/**
 * Mapping redux state to the properties of the component
 *
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<WorkspaceProps> => ({
  userSessionState: state.userSessionState,
  keyPressActionQueue: state.analystWorkspaceState.keyPressActionQueue
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 *
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<any> =>
  bindActionCreators(
    {
      setAuthStatus: UserSessionOperations.setAuthStatus,
      setKeyPressActionQueue: AnalystWorkspaceActions.setKeyPressActionQueue
    } as any,
    dispatch
  );

export const ReduxWorkspaceContainer: React.ComponentClass<Partial<WorkspaceProps>> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  withRouter
)(Workspace);
