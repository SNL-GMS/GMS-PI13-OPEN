import isEqual from 'lodash/isEqual';
import { batch } from 'react-redux';
import { Actions } from '../actions';
import { AppState } from '../types';
import { Internal } from './actions';
import { AuthStatus } from './types';

/**
 * Redux operation for setting the authentication status.
 *
 * @param event the event to set
 */
const setAuthStatus = (authStatus: AuthStatus) => (dispatch: any, getState: () => AppState) => {
  const state: AppState = getState();
  if (!isEqual(state.userSessionState.authorizationStatus, authStatus)) {
    if (!authStatus.userName && !authStatus.authenticated) {
      batch(() => {
        // reset the application state
        dispatch(Actions.resetAppState());

        // update the authentication status
        dispatch(Internal.setAuthStatus(authStatus));
      });
    } else {
      batch(() => {
        // update the authentication status
        dispatch(Internal.setAuthStatus(authStatus));
      });
    }
  }
};

/**
 * Redux operations (public).
 */
export const Operations = {
  setAuthStatus
};
