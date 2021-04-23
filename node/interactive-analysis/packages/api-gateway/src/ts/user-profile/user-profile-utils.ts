import cloneDeep from 'lodash/cloneDeep';
import { gatewayLogger as logger } from '../log/gateway-logger';
import {
  DefaultLayoutNames,
  SetAudibleNotificationsInput,
  SetLayoutInput,
  UserProfile
} from './model';

/**
 * Creates a new user profile that includes the new/updated layout
 * @param currentProfile the current user profile
 * @param setLayoutInput the user input
 */
export const createNewProfileFromSetLayoutInput = (
  currentProfile: UserProfile,
  setLayoutInput: SetLayoutInput
): UserProfile => {
  const formerLayouts = cloneDeep(currentProfile.workspaceLayouts);
  const newLayouts = [
    setLayoutInput.workspaceLayoutInput,
    ...formerLayouts.filter(wl => wl.name !== setLayoutInput.workspaceLayoutInput.name)
  ];
  const defaultLayouts: { defaultLayoutName: string; sohLayoutName: string } =
    setLayoutInput.saveAsDefaultLayout !== undefined
      ? {
          defaultLayoutName:
            setLayoutInput.saveAsDefaultLayout === DefaultLayoutNames.ANALYST_LAYOUT
              ? setLayoutInput.workspaceLayoutInput.name
              : currentProfile.defaultLayoutName,
          sohLayoutName:
            setLayoutInput.saveAsDefaultLayout === DefaultLayoutNames.SOH_LAYOUT
              ? setLayoutInput.workspaceLayoutInput.name
              : currentProfile.sohLayoutName
        }
      : {
          defaultLayoutName: currentProfile.defaultLayoutName,
          sohLayoutName: currentProfile.sohLayoutName
        };
  const newProfile: UserProfile = {
    userId: currentProfile.userId,
    audibleNotifications: currentProfile.audibleNotifications,
    workspaceLayouts: newLayouts,
    ...defaultLayouts
  };
  return newProfile;
};

/**
 * Creates a new user profile that includes the new/updated audible notification list
 * @param currentProfile the current user profile
 * @param setAudibleNotificationsInput the user input
 */
export const createNewProfileFromSetAudibleNotificationsInput = (
  currentProfile: UserProfile,
  setAudibleNotificationsInput: SetAudibleNotificationsInput
): UserProfile => {
  logger.info(
    `current audible notifications: ${JSON.stringify(currentProfile.audibleNotifications)}`
  );

  const newAudibleNotificationList = [
    ...currentProfile.audibleNotifications.filter(
      n =>
        !setAudibleNotificationsInput.audibleNotificationsInput.find(
          p => p.notificationType === n.notificationType
        )
    ),
    ...setAudibleNotificationsInput.audibleNotificationsInput
  ]
    .sort((a, b) => a.notificationType.localeCompare(b.notificationType))
    .filter(notification => notification.fileName !== '');

  logger.info(
    `input audible notifications: ${JSON.stringify(
      setAudibleNotificationsInput.audibleNotificationsInput
    )}`
  );
  logger.info(`updated audible notifications: ${JSON.stringify(newAudibleNotificationList)}`);
  const newProfile: UserProfile = {
    userId: currentProfile.userId,
    ...currentProfile,
    audibleNotifications: newAudibleNotificationList
  };
  return newProfile;
};
