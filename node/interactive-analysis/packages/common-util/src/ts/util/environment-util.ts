import { isWindowDefined } from './window-util';

const windowIsDefined = isWindowDefined();

/**
 * The NODE_ENV environment variable.
 */
export const NODE_ENV = process.env.NODE_ENV;

/**
 * True if NODE_ENV is set to development.
 */
export const IS_NODE_ENV_DEVELOPMENT = NODE_ENV === 'development';

/**
 * True if NODE_ENV is set to production.
 */
export const IS_NODE_ENV_PRODUCTION = NODE_ENV === 'production';

/**
 * The Interactive Analysis Mode ['soh' or 'analyst']
 */
export const INTERACTIVE_ANALYSIS_MODE = process.env.INTERACTIVE_ANALYSIS_MODE || undefined;

/**
 * The enum for the user mode. Matches OSD enum and environment variables used in build/start scripts.
 */
export enum UserMode {
  ANALYST = 'ANALYST',
  SOH = 'SOH'
}

/**
 * True if Interactive Analysis is configured for ANALYST; false otherwise.
 */
export const IS_INTERACTIVE_ANALYSIS_MODE_ANALYST =
  process.env.INTERACTIVE_ANALYSIS_MODE === UserMode.ANALYST;

/**
 * True if Interactive Analysis is configured for SOH; false otherwise.
 */
export const IS_INTERACTIVE_ANALYSIS_MODE_SOH =
  process.env.INTERACTIVE_ANALYSIS_MODE === UserMode.SOH;

/**
 * The current user mode, which defines which layouts are supported
 */
export const CURRENT_USER_MODE = IS_INTERACTIVE_ANALYSIS_MODE_ANALYST
  ? UserMode.ANALYST
  : IS_INTERACTIVE_ANALYSIS_MODE_SOH
  ? UserMode.SOH
  : '';

export const SUPPORTED_MODES: UserMode[] =
  CURRENT_USER_MODE === UserMode.SOH
    ? [UserMode.SOH]
    : Object.keys(UserMode).map(mode => UserMode[mode]);

/**
 * The GRAPHQL_PROXY_URI environment variable (or the default value if not set).
 */
export const GRAPHQL_PROXY_URI = windowIsDefined
  ? process.env.GRAPHQL_PROXY_URI || `${window.location.protocol}//${window.location.host}`
  : undefined;

/**
 * The SUBSCRIPTIONS_PROXY_URI environment variable (or the default value if not set).
 */
export const WAVEFORMS_PROXY_URI = windowIsDefined
  ? process.env.WAVEFORMS_PROXY_URI || `${window.location.protocol}//${window.location.host}`
  : undefined;

/**
 * The SUBSCRIPTIONS_PROXY_URI environment variable (or the default value if not set).
 */
export const SUBSCRIPTIONS_PROXY_URI = windowIsDefined
  ? process.env.SUBSCRIPTIONS_PROXY_URI ||
    (window.location.protocol === 'https:' ? 'wss' : 'ws') + `://${window.location.host}`
  : undefined;

/**
 * The API_GATEWAY_URI environment variable (or the default value if not set).
 */
export const API_GATEWAY_URI = GRAPHQL_PROXY_URI;

/**
 * The API_GATEWAY_URI environment variable for checking a user's login status.
 */
export const API_LOGIN_CHECK_URI = GRAPHQL_PROXY_URI + '/auth/checkLogIn';

/**
 * The API_GATEWAY_URI environment variable for accessing the login endpoint.
 */
export const API_LOGIN_URI = GRAPHQL_PROXY_URI + '/auth/logInUser';

/**
 * The API_GATEWAY_URI environment variable for accessing the logout endpoint.
 */
export const API_LOGOUT_URI = GRAPHQL_PROXY_URI + '/auth/logOutUser';

/**
 * The CESIUM_OFFLINE environment variable.
 */
export const CESIUM_OFFLINE = process.env.CESIUM_OFFLINE
  ? !(
      process.env.CESIUM_OFFLINE === 'null' ||
      process.env.CESIUM_OFFLINE === 'undefined' ||
      process.env.CESIUM_OFFLINE === 'false'
    )
  : false;

/**
 * The `AVAILABLE_SOUND_FILES` environment variable.
 * The available configured sound files for the system.
 */
export const AVAILABLE_SOUND_FILES: string[] =
  process.env.AVAILABLE_SOUND_FILES &&
  process.env.AVAILABLE_SOUND_FILES !== 'undefined' &&
  process.env.AVAILABLE_SOUND_FILES !== 'null'
    ? process.env.AVAILABLE_SOUND_FILES.split(';')
    : [];
