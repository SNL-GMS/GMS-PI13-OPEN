import { AudibleNotification } from '@gms/common-graphql/lib/graphql/user-profile/types';
import { UserMode } from '@gms/common-util';

/**
 * Possible Analyst Layout Names
 */
export enum DefaultLayoutNames {
  ANALYST_LAYOUT = 'ANALYST_LAYOUT',
  SOH_LAYOUT = 'SOH_LAYOUT'
}

export interface UserProfileQueryArguments {
  defaultLayoutName: DefaultLayoutNames;
}

/**
 * User preferences COI object
 */
export interface UserProfile {
  userId: string;
  defaultLayoutName: string;
  sohLayoutName: string;
  workspaceLayouts: UserLayout[];
  audibleNotifications: AudibleNotification[];
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
 * User preferences COI object
 */
export interface SetLayoutInput {
  workspaceLayoutInput: UserLayout;
  saveAsDefaultLayout?: DefaultLayoutNames;
}

export interface SetAudibleNotificationsInput {
  audibleNotificationsInput: AudibleNotification[];
}
