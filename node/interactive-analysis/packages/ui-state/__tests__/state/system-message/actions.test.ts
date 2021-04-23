import { SystemMessageTypes } from '@gms/common-graphql/lib/graphql';
import * as Redux from 'redux';
import createMockStore, { MockStore, MockStoreCreator } from 'redux-mock-store';
import thunk from 'redux-thunk';
import { initialAppState } from '../../../src/ts/state/initial-state';
import * as Actions from '../../../src/ts/state/system-message/actions';
import * as Types from '../../../src/ts/state/system-message/types';
import { AppState } from '../../../src/ts/state/types';

const middlewares = [thunk];
const mockStoreCreator: MockStoreCreator<AppState, Redux.AnyAction> = createMockStore(middlewares);
let store: MockStore<AppState, Redux.AnyAction>;

describe('state system message actions', () => {
  describe('internal actions', () => {
    beforeEach(() => {
      store = mockStoreCreator(initialAppState);
    });

    it('should set the last updated time', () => {
      const lastUpdatedTime = 123456789;
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SYSTEM_MESSAGES_LAST_UPDATED,
        payload: lastUpdatedTime
      };
      expect(Actions.Internal.setLastUpdated(lastUpdatedTime)).toEqual(expectedAction);
      store.dispatch(Actions.Internal.setLastUpdated(lastUpdatedTime));

      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the system messages', () => {
      const systemMessages: SystemMessageTypes.SystemMessage[] = [
        {
          id: '1',
          type: SystemMessageTypes.SystemMessageType.CHANNEL_MONITOR_TYPE_QUIETED,
          severity: SystemMessageTypes.SystemMessageSeverity.CRITICAL,
          category: SystemMessageTypes.SystemMessageCategory.SOH,
          subCategory: SystemMessageTypes.SystemMessageSubCategory.CAPABILITY,
          time: 123456789,
          message: 'sample message'
        }
      ];
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SYSTEM_MESSAGES,
        payload: systemMessages
      };
      expect(Actions.Internal.setSystemMessages(systemMessages)).toEqual(expectedAction);
      store.dispatch(Actions.Internal.setSystemMessages(systemMessages));

      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });
  });
});
