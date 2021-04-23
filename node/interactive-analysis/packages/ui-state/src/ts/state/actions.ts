import { ActionCreatorVoid, actionCreatorVoid } from './util/action-helper';

const resetAppState: ActionCreatorVoid = actionCreatorVoid('RESET_APP_STATE');

/**
 * Redux internal actions: should only be called by `operations`. (private - but not strictly forced)
 */
export const Internal = {};

/**
 * Redux actions (public).
 */
export const Actions = {
  resetAppState
};
