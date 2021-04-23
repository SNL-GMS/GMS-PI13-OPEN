import { SystemMessageTypes } from '@gms/common-graphql/lib/graphql';
import * as Redux from 'redux';
import { initialSystemMessageState } from '../../../src/ts/state/initial-state';
import { Reducer } from '../../../src/ts/state/system-message/reducers';
import * as Types from '../../../src/ts/state/system-message/types';
import { actionCreator, actionCreatorVoid } from '../../../src/ts/state/util/action-helper';

describe('state system message reducers', () => {
  describe('reducer', () => {
    it('should return the initial state', () => {
      expect(Reducer(undefined, actionCreatorVoid(undefined))).toEqual(initialSystemMessageState);
      expect(Reducer(undefined, actionCreator(undefined))).toEqual(initialSystemMessageState);

      expect(Reducer(undefined, actionCreatorVoid(''))).toEqual(initialSystemMessageState);
      expect(Reducer(undefined, actionCreator(''))).toEqual(initialSystemMessageState);

      expect(Reducer(initialSystemMessageState, actionCreatorVoid(undefined))).toEqual(
        initialSystemMessageState
      );
      expect(Reducer(initialSystemMessageState, actionCreator(undefined))).toEqual(
        initialSystemMessageState
      );

      expect(Reducer(initialSystemMessageState, actionCreatorVoid(''))).toEqual(
        initialSystemMessageState
      );
      expect(Reducer(initialSystemMessageState, actionCreator(''))).toEqual(
        initialSystemMessageState
      );
    });

    it('should set the last updated time', () => {
      const lastUpdated = 123456789;

      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SYSTEM_MESSAGES_LAST_UPDATED,
        payload: lastUpdated
      };
      const expectedState: Types.SystemMessageState = {
        ...initialSystemMessageState,
        lastUpdated
      };
      expect(Reducer(initialSystemMessageState, action)).toEqual(expectedState);
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
      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SYSTEM_MESSAGES,
        payload: systemMessages
      };
      const expectedState: Types.SystemMessageState = {
        ...initialSystemMessageState,
        systemMessages
      };
      expect(Reducer(initialSystemMessageState, action)).toEqual(expectedState);
    });

    it('should set the latest system messages', () => {
      const latestSystemMessages: SystemMessageTypes.SystemMessage[] = [
        {
          id: '4',
          type: SystemMessageTypes.SystemMessageType.CHANNEL_MONITOR_TYPE_STATUS_CHANGED,
          severity: SystemMessageTypes.SystemMessageSeverity.WARNING,
          category: SystemMessageTypes.SystemMessageCategory.SOH,
          subCategory: SystemMessageTypes.SystemMessageSubCategory.USER,
          time: 123456789,
          message: 'sample message'
        }
      ];
      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_LATEST_SYSTEM_MESSAGES,
        payload: latestSystemMessages
      };

      const expectedState: Types.SystemMessageState = {
        ...initialSystemMessageState,
        latestSystemMessages
      };
      expect(Reducer(initialSystemMessageState, action)).toEqual(expectedState);
    });
  });
});
