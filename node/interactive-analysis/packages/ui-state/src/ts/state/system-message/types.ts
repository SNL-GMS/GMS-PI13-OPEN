import { SystemMessageTypes } from '@gms/common-graphql/lib/graphql';
import { ActionWithPayload } from '../util/action-helper';

/** System message action types */
export enum ActionTypes {
  SET_SYSTEM_MESSAGES_LAST_UPDATED = 'SET_SYSTEM_MESSAGES_LAST_UPDATED',
  SET_LATEST_SYSTEM_MESSAGES = 'SET_LATEST_SYSTEM_MESSAGES',
  SET_SYSTEM_MESSAGES = 'SET_SYSTEM_MESSAGES'
}

/** Redux action to set the timestamp of when the system messages were last updated */
export type SET_SYSTEM_MESSAGES_LAST_UPDATED = ActionWithPayload<number>;

/** Redux action to set latest system messages */
export type SET_LATEST_SYSTEM_MESSAGES = ActionWithPayload<SystemMessageTypes.SystemMessage[]>;

/** Redux action to set system messages */
export type SET_SYSTEM_MESSAGES = ActionWithPayload<SystemMessageTypes.SystemMessage[]>;

export interface SystemMessageState {
  /** timestamp of when the data was last updated (EPOCH milliseconds) */
  readonly lastUpdated: number;

  /** the latest systems messages */
  readonly latestSystemMessages: SystemMessageTypes.SystemMessage[];

  /** the systems messages */
  readonly systemMessages: SystemMessageTypes.SystemMessage[];
}
