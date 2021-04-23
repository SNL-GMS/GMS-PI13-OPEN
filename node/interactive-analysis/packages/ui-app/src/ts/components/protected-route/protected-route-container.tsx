import { AppState } from '@gms/ui-state';
import { ComponentType } from 'react';
import * as ReactRedux from 'react-redux';
import { withRouter } from 'react-router';
import { bindActionCreators, compose } from 'redux';
import { ProtectedRouteComponent } from './protected-route-component';
import { ProtectedRouteProps } from './types';

/**
 * Mapping redux state to the properties of the component
 *
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<ProtectedRouteProps> => ({
  authenticated: state.userSessionState.authorizationStatus.authenticated
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 *
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<ProtectedRouteProps> =>
  bindActionCreators({} as any, dispatch);

/**
 * Connects the login screen to the redux store
 */
export const ReduxProtectedRouteContainer: ReactRedux.ConnectedComponent<
  ComponentType<ProtectedRouteComponent>,
  Partial<ProtectedRouteProps>
> = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  withRouter
)(ProtectedRouteComponent);
