import { UserMode } from '@gms/common-util';
import { QueryControls } from 'react-apollo';
import { SystemMessageType } from '../system-message/types';

export interface UserProfileProps {
  userProfileQuery: QueryControls<{}> & { userProfile: UserProfile };
}

/**
 * Possible Analyst Layout Names
 */
export enum DefaultLayoutNames {
  ANALYST_LAYOUT = 'ANALYST_LAYOUT',
  SOH_LAYOUT = 'SOH_LAYOUT'
}

/**
 * Arguments for the user profile query
 */
export interface UserProfileQueryArguments {
  defaultLayoutName: DefaultLayoutNames;
}

/**
 * User preferences COI object
 */
export interface UserProfile {
  userId: string;
  defaultLayoutName: string;
  workspaceLayouts: UserLayout[];
  audibleNotifications: AudibleNotification[];
  id: string;
}

/**
 * User layout. LayoutConfiguration is a URI encoded json representation of a golden layout
 */
export interface UserLayout {
  name: string;
  supportedUserInterfaceModes: UserMode[];
  layoutConfiguration: string;
}

/**
 * The union of all allowed audible notification types.
 */
export type AudibleNotificationType = SystemMessageType;

/**
 * A pair of a notification enum type (for example, a SystemMessageType)
 * and a file, which is found in the /sounds/ directory
 */
export interface AudibleNotification {
  notificationType: AudibleNotificationType;
  fileName: string;
}

/**
 * set layout mutation args
 */
export interface SetLayoutMutationArgs {
  workspaceLayoutInput: UserLayout;
  defaultLayoutName: DefaultLayoutNames;
  saveAsDefaultLayout?: DefaultLayoutNames;
}
