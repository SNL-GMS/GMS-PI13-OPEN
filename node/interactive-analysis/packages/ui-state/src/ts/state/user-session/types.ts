import { ActionWithPayload } from '../util/action-helper';

export interface UserSessionState {
  authorizationStatus: AuthStatus;
  connected: boolean;
}

export interface AuthenticationResponse {
  userName: string;
  successful: boolean;
}

export interface AuthStatus {
  userName: string;
  authenticated: boolean;
  authenticationCheckComplete: boolean;
  failedToConnect: boolean;
}

export type SET_AUTH_STATUS = ActionWithPayload<AuthStatus>;

export type SET_CONNECTED = ActionWithPayload<boolean>;
