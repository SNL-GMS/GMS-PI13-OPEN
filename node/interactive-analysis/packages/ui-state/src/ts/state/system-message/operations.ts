import { SystemMessageTypes } from '@gms/common-graphql/lib/graphql';
import orderBy from 'lodash/orderBy';
import reverse from 'lodash/reverse';
import slice from 'lodash/slice';
import { batch } from 'react-redux';
import { AppState } from '../types';
import { Internal } from './actions';
import { SystemMessageState } from './types';

/**
 * Redux operation that adds system messages to the state.
 *
 * @param messages the system messages to add
 * @param limit (optional) limit the number of messages in the redux state
 * when adding new messages; if set when adding new messages the message list
 * result will not be larger than the size of the `limit` specified.
 * @param pageSizeBuffer (optional) the size of the page buffer; if specified and the
 * limit is reached then messages will be removed at increments of the page buffer size
 */
const addSystemMessages = (
  messages: SystemMessageTypes.SystemMessage[],
  limit?: number /* default to delete zero messages */,
  pageSizeBuffer?: number
) => (dispatch: any, getState: () => AppState) => {
  const state: SystemMessageState = getState().systemMessageState;
  if (messages && messages.length > 0) {
    // batch the dispatches - this will only result in one combined re-render, not two
    batch(() => {
      const allMessages = state.systemMessages
        ? [...state.systemMessages, ...messages]
        : [...messages];

      // apply the size limit if necessary before updating the redux state
      const actualLimit = limit && limit > 0 ? limit : 0;
      if (actualLimit > 0 && allMessages.length > actualLimit) {
        const actualPageSizeBuffer =
          pageSizeBuffer && pageSizeBuffer > 0 && pageSizeBuffer < limit ? pageSizeBuffer : 0;
        if (actualPageSizeBuffer > 0) {
          // remove page size increments until the number of messages is less than the limit
          let updatedMessages = reverse([...allMessages]);
          while (updatedMessages.length > limit) {
            updatedMessages = updatedMessages.splice(
              0,
              updatedMessages.length - actualPageSizeBuffer
            );
          }
          dispatch(Internal.setSystemMessages(reverse([...updatedMessages])));
        } else {
          // ensure that the list size is equal to the `limit`
          dispatch(Internal.setSystemMessages(reverse(reverse(allMessages).splice(0, limit))));
        }
      } else {
        dispatch(Internal.setSystemMessages(allMessages));
      }
      dispatch(
        Internal.setLatestSystemMessages(
          orderBy<SystemMessageTypes.SystemMessage>(messages, ['time'], ['desc'])
        )
      );
      dispatch(Internal.setLastUpdated(Date.now()));
    });
  }
};

/**
 * Redux operation that clears all system messages that have expired.
 *
 * @param expiredLimitMs the expired time limit to check; any messages that have a time
 * older than the expired time limit will be removed.
 */
const clearExpiredSystemMessages = (expiredLimitMs: number) => (
  dispatch: any,
  getState: () => AppState
) => {
  const state: SystemMessageState = getState().systemMessageState;
  // batch the dispatches - this will only result in one combined re-render, not two
  batch(() => {
    const now = Date.now();
    const nonExpiredMessages = state.systemMessages
      ? [...state.systemMessages].filter(msg => now - msg.time > expiredLimitMs)
      : [];
    dispatch(Internal.setSystemMessages([...nonExpiredMessages]));
  });
};

/**
 * Redux operation that clears the number of specified system messages starting at `index`
 *
 * @param index the index; starting position to start
 * @param deleteCount the number or messages to remove from the index
 */
const clearSystemMessages = (index: number, deleteCount: number) => (
  dispatch: any,
  getState: () => AppState
) => {
  const state: SystemMessageState = getState().systemMessageState;
  // batch the dispatches - this will only result in one combined re-render, not two
  batch(() => {
    const messages = state.systemMessages
      ? slice([...state.systemMessages], index, index + deleteCount)
      : [];
    dispatch(Internal.setSystemMessages([...messages]));
  });
};

/**
 * Redux operation that clears all system messages to the state.
 */
const clearAllSystemMessages = () => (dispatch: any, getState: () => AppState) => {
  // batch the dispatches - this will only result in one combined re-render, not two
  batch(() => {
    dispatch(Internal.setSystemMessages([]));
  });
};

// Redux operations for system messages
export const Operations = {
  addSystemMessages,
  clearSystemMessages,
  clearExpiredSystemMessages,
  clearAllSystemMessages
};
