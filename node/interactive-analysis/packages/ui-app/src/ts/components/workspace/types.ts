import { DialogTypes } from '@gms/ui-core-components';
import { AnalystWorkspaceTypes, UserSessionTypes } from '@gms/ui-state';
import History from 'history';
import Immutable from 'immutable';
import React from 'react';

/**
 * The workspace redux props.
 * Note: these props are mapped in from Redux state
 */
export interface WorkspaceReduxProps {
  userSessionState: UserSessionTypes.UserSessionState;
  keyPressActionQueue: Immutable.Map<AnalystWorkspaceTypes.KeyAction, number>;
  pushKeyPressAction(action: AnalystWorkspaceTypes.KeyAction): void;
  setAuthStatus(auth: UserSessionTypes.AuthStatus): void;
  setKeyPressActionQueue(actions: Immutable.Map<AnalystWorkspaceTypes.KeyAction, number>): void;
}

/**
 * The workspace props.
 */
export type WorkspaceProps = WorkspaceReduxProps & {
  location: History.Location;
};

export type WorkspaceLayout = DialogTypes.SaveableItem;

/**
 * The workspace state.
 */
export interface WorkspaceState {
  triggerAnimation: boolean;
}

export const KeyContext = React.createContext({
  shouldTriggerAnimation: false,
  resetAnimation: () => {
    return;
  }
});
