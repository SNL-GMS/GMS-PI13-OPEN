import { UserSessionTypes } from '@gms/ui-state';
import History from 'history';

/** The login screen state */
export interface LoginScreenState {
  username: string;
  password: string;
}

/**
 * The login screen redux props.
 * Note: these props are mapped in from Redux state
 */
export interface LoginScreenReduxProps {
  location: History.Location;
  authenticated: boolean;
  authenticationCheckComplete: boolean;
  failedToConnect: boolean;
  setAuthStatus(auth: UserSessionTypes.AuthStatus): void;
}

export type LoginScreenProps = LoginScreenReduxProps;
