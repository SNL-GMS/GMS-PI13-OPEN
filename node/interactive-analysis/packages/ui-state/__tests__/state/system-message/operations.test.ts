import { SystemMessageTypes } from '@gms/common-graphql/lib/graphql';
import * as Redux from 'redux';
import createMockStore, { MockStore, MockStoreCreator } from 'redux-mock-store';
import thunk from 'redux-thunk';
import { initialAppState } from '../../../src/ts/state/initial-state';
import { Operations } from '../../../src/ts/state/system-message/operations';
import * as Types from '../../../src/ts/state/system-message/types';
import { AppState } from '../../../src/ts/state/types';

// tslint:disable-next-line: no-magic-numbers
Date.now = jest.fn().mockReturnValue(1575410988600);

const middlewares = [thunk];
const mockStoreCreator: MockStoreCreator<AppState, Redux.AnyAction> = createMockStore(middlewares);
let store: MockStore<AppState, any /* TODO correct the typings */>;

describe('state system message operations', () => {
  describe('operations', () => {
    const lastUpdated = 123456789;

    const systemMessages1: SystemMessageTypes.SystemMessage[] = [
      {
        id: '1',
        type: SystemMessageTypes.SystemMessageType.CHANNEL_MONITOR_TYPE_QUIETED,
        severity: SystemMessageTypes.SystemMessageSeverity.CRITICAL,
        category: SystemMessageTypes.SystemMessageCategory.SOH,
        subCategory: SystemMessageTypes.SystemMessageSubCategory.CAPABILITY,
        time: lastUpdated,
        message: 'sample message'
      }
    ];

    const systemMessages2: SystemMessageTypes.SystemMessage[] = [
      {
        id: '2',
        type: SystemMessageTypes.SystemMessageType.STATION_CAPABILITY_STATUS_CHANGED,
        severity: SystemMessageTypes.SystemMessageSeverity.WARNING,
        category: SystemMessageTypes.SystemMessageCategory.SOH,
        subCategory: SystemMessageTypes.SystemMessageSubCategory.STATUS,
        time: lastUpdated,
        message: 'sample message'
      }
    ];

    const systemMessages3: SystemMessageTypes.SystemMessage[] = [
      {
        id: '3',
        type: SystemMessageTypes.SystemMessageType.STATION_NEEDS_ATTENTION,
        severity: SystemMessageTypes.SystemMessageSeverity.INFO,
        category: SystemMessageTypes.SystemMessageCategory.SOH,
        subCategory: SystemMessageTypes.SystemMessageSubCategory.STATUS,
        time: lastUpdated,
        message: 'sample message'
      }
    ];

    beforeEach(() => {
      store = mockStoreCreator(initialAppState);
    });

    it('should be able to add system messages (undefined)', () => {
      const expectedActions = [];
      store.dispatch(Operations.addSystemMessages(undefined));

      const actions = store.getActions();
      expect(actions).toEqual(expectedActions);
    });

    it('should be able to add system messages (empty)', () => {
      const expectedActions = [];
      store.dispatch(Operations.addSystemMessages([]));

      const actions = store.getActions();
      expect(actions).toEqual(expectedActions);
    });

    it('should be able to add system messages', () => {
      const expectedActions = [
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES,
          payload: [...systemMessages1, ...systemMessages2]
        },
        {
          type: Types.ActionTypes.SET_LATEST_SYSTEM_MESSAGES,
          payload: [...systemMessages1, ...systemMessages2]
        },
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES_LAST_UPDATED,
          payload: Date.now()
        }
      ];
      store.dispatch(Operations.addSystemMessages([...systemMessages1, ...systemMessages2]));
      const actions = store.getActions();
      expect(actions).toEqual(expectedActions);
    });

    it('should be able to add system messages with a limit', () => {
      const expectedActions = [
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES,
          payload: [...systemMessages1, ...systemMessages2]
        },
        {
          type: Types.ActionTypes.SET_LATEST_SYSTEM_MESSAGES,
          payload: [...systemMessages1, ...systemMessages2]
        },
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES_LAST_UPDATED,
          payload: Date.now()
        },
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES,
          payload: [...systemMessages3]
        },
        {
          type: Types.ActionTypes.SET_LATEST_SYSTEM_MESSAGES,
          payload: [...systemMessages3]
        },
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES_LAST_UPDATED,
          payload: Date.now()
        }
      ];
      store.dispatch(Operations.addSystemMessages([...systemMessages1, ...systemMessages2], 2));

      store.dispatch(Operations.addSystemMessages([...systemMessages3], 2));

      const actions = store.getActions();
      expect(actions).toEqual(expectedActions);
    });

    it('should be able to clear all system', () => {
      const expectedActions = [
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES,
          payload: []
        }
      ];
      store.dispatch(Operations.clearAllSystemMessages());

      const actions = store.getActions();
      expect(actions).toEqual(expectedActions);
    });

    it('should be able to clear a message (empty)', () => {
      const expectedActions = [
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES,
          payload: []
        }
      ];
      store.dispatch(Operations.clearSystemMessages(0, 0));

      const actions = store.getActions();
      expect(actions).toEqual(expectedActions);
    });

    it('should be able to clear a message', () => {
      const expectedActions = [
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES,
          payload: [...systemMessages1, ...systemMessages2]
        },
        {
          type: Types.ActionTypes.SET_LATEST_SYSTEM_MESSAGES,
          payload: [...systemMessages1, ...systemMessages2]
        },
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES_LAST_UPDATED,
          payload: Date.now()
        },
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES,
          payload: []
        }
      ];

      store.dispatch(Operations.addSystemMessages([...systemMessages1, ...systemMessages2]));
      store.dispatch(Operations.clearSystemMessages(0, 1));

      const actions = store.getActions();
      expect(actions).toEqual(expectedActions);
    });

    it('should be able to clear expired messages', () => {
      const expectedActions = [
        {
          type: Types.ActionTypes.SET_SYSTEM_MESSAGES,
          payload: []
        }
      ];
      store.dispatch(Operations.clearExpiredSystemMessages(0));

      const actions = store.getActions();
      expect(actions).toEqual(expectedActions);
    });
  });
});
