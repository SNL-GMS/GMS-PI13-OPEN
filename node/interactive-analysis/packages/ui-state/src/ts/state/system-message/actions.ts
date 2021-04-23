import { SystemMessageTypes } from '@gms/common-graphql/lib/graphql';
import { ActionCreator, actionCreator } from '../util/action-helper';
import { ActionTypes } from './types';

/** Redux action to set the timestamp of when the system messages were last updated */
const setLastUpdated: ActionCreator<number> = actionCreator(
  ActionTypes.SET_SYSTEM_MESSAGES_LAST_UPDATED
);

/** Redux action to set latest system messages */
const setLatestSystemMessages: ActionCreator<SystemMessageTypes.SystemMessage[]> = actionCreator(
  ActionTypes.SET_LATEST_SYSTEM_MESSAGES
);

/** Redux action to set system messages */
const setSystemMessages: ActionCreator<SystemMessageTypes.SystemMessage[]> = actionCreator(
  ActionTypes.SET_SYSTEM_MESSAGES
);

/**
 * Redux internal actions: should only be called by `operations`. (private - but not strictly forced)
 */
export const Internal = {
  setLastUpdated,
  setLatestSystemMessages,
  setSystemMessages
};

/**
 * Redux actions (public).
 */
export const Actions = {};
