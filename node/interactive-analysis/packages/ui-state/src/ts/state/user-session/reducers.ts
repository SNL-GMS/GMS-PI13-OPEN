import * as Redux from 'redux';
import { Actions, Internal } from './actions';
import * as Types from './types';

/**
 * Redux reducer for setting authentication status.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setAuthStatus = (
  state: Types.AuthStatus = null,
  action: Types.SET_AUTH_STATUS
): Types.AuthStatus => {
  if (Internal.setAuthStatus.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the connection status.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setConnected = (state: boolean = null, action: Types.SET_CONNECTED): boolean => {
  if (Actions.setConnected.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

export const Reducer: Redux.Reducer<
  Types.UserSessionState,
  Redux.AnyAction
> = Redux.combineReducers({
  authorizationStatus: setAuthStatus,
  connected: setConnected
});
