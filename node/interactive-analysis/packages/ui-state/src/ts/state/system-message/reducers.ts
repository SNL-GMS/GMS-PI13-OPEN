import { SystemMessageTypes } from '@gms/common-graphql/lib/graphql';
import * as Redux from 'redux';
import { Internal } from './actions';
import {
  SET_LATEST_SYSTEM_MESSAGES,
  SET_SYSTEM_MESSAGES,
  SET_SYSTEM_MESSAGES_LAST_UPDATED,
  SystemMessageState
} from './types';

/**
 * Reducer to set the system messages last updated time in the redux store.
 * @param state the existing state
 * @param action the action
 */
const setLastUpdated = (state: number = null, action: SET_SYSTEM_MESSAGES_LAST_UPDATED): number => {
  if (Internal.setLastUpdated.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Reducer to set the latest system messages in the redux store.
 * @param state the existing state
 * @param action the action
 */
const setLatestSystemMessages = (
  state: SystemMessageTypes.SystemMessage[] = null,
  action: SET_LATEST_SYSTEM_MESSAGES
): SystemMessageTypes.SystemMessage[] => {
  if (Internal.setLatestSystemMessages.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Reducer to set the system messages in the redux store.
 * @param state the existing state
 * @param action the action
 */
const setSystemMessages = (
  state: SystemMessageTypes.SystemMessage[] = null,
  action: SET_SYSTEM_MESSAGES
): SystemMessageTypes.SystemMessage[] => {
  if (Internal.setSystemMessages.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/** The system message reducer */
export const Reducer: Redux.Reducer<SystemMessageState, Redux.AnyAction> = Redux.combineReducers({
  lastUpdated: setLastUpdated,
  latestSystemMessages: setLatestSystemMessages,
  systemMessages: setSystemMessages
});
