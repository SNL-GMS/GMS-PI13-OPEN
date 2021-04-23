import { AppState, UserSessionOperations } from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { withRouter } from 'react-router';
import { bindActionCreators, compose } from 'redux';
import { LoginScreenComponent } from './login-screen-component';
import { LoginScreenProps } from './types';

/**
 * Mapping redux state to the properties of the component
 *
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<LoginScreenProps> => ({
  authenticated: state.userSessionState.authorizationStatus.authenticated,
  authenticationCheckComplete:
    state.userSessionState.authorizationStatus.authenticationCheckComplete,
  failedToConnect: state.userSessionState.authorizationStatus.failedToConnect
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 *
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<LoginScreenProps> =>
  bindActionCreators(
    {
      setAuthStatus: UserSessionOperations.setAuthStatus
    } as any,
    dispatch
  );

/**
 * Connects the login screen to the redux store
 */
export const ReduxLoginScreenContainer: ReactRedux.ConnectedComponent<
  any,
  Pick<unknown, never>
> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  withRouter
)(LoginScreenComponent);
