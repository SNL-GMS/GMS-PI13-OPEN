import { ActionCreator, actionCreator } from '../util/action-helper';
import { AuthStatus } from './types';

const setAuthStatus: ActionCreator<AuthStatus> = actionCreator('SET_AUTH_STATUS');

const setConnected: ActionCreator<boolean> = actionCreator('SET_CONNECTED');

/**
 * Redux internal actions: should only be called by `operations`. (private - but not strictly forced)
 */
export const Internal = {
  setAuthStatus
};

/**
 * Redux actions (public).
 */
export const Actions = {
  setConnected
};
