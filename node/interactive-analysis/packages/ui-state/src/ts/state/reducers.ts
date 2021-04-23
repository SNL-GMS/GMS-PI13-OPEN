import * as Redux from 'redux';
import { AnalystWorkspaceReducer } from './analyst-workspace';
import { DataAcquisitionWorkspaceReducer } from './data-acquisition-workspace';
import { SystemMessageReducer } from './system-message';
import { AppState } from './types';
import { UserSessionReducer } from './user-session';

/** The application reducer */
export const AppReducer: Redux.Reducer<AppState, Redux.AnyAction> = Redux.combineReducers({
  analystWorkspaceState: AnalystWorkspaceReducer,
  dataAcquisitionWorkspaceState: DataAcquisitionWorkspaceReducer,
  userSessionState: UserSessionReducer,
  systemMessageState: SystemMessageReducer
});
