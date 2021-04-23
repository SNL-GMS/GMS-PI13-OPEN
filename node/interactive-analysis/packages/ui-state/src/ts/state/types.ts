import { AnalystWorkspaceTypes } from './analyst-workspace';
import { DataAcquisitionWorkspaceTypes } from './data-acquisition-workspace';
import { SystemMessageState } from './system-message/types';
import { UserSessionState } from './user-session/types';
import { Action } from './util/action-helper';

export type RESET_APP_STATE = Action;

/** The application state */
export interface AppState {
  analystWorkspaceState: AnalystWorkspaceTypes.AnalystWorkspaceState;
  dataAcquisitionWorkspaceState: DataAcquisitionWorkspaceTypes.DataAcquisitionWorkspaceState;
  userSessionState: UserSessionState;
  systemMessageState: SystemMessageState;
}
