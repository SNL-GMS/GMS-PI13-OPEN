import * as Redux from 'redux';
import { Actions } from './actions';
import {
  initialAnalystWorkspaceState,
  initialAppState,
  initialDataAcquisitionWorkspaceState
} from './initial-state';
import { AppReducer } from './reducers';
import { AppState } from './types';

/**
 * Redux reducer for resetting the app state.
 *
 * @param state the state to set
 * @param action the redux action
 */
export const Reducer = (state: AppState = initialAppState, action: Redux.Action<any>): AppState => {
  if (Actions.resetAppState.test(action)) {
    return {
      analystWorkspaceState: initialAnalystWorkspaceState,
      dataAcquisitionWorkspaceState: initialDataAcquisitionWorkspaceState,
      userSessionState: state.userSessionState,
      systemMessageState: state.systemMessageState
    };
  }
  return AppReducer(state, action);
};
