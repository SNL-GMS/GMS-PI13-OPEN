// import { CURRENT_USER_MODE } from '@gms/common-utiL';
import { UserContext } from '../cache/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import {
  DefaultLayoutNames,
  SetAudibleNotificationsInput,
  SetLayoutInput,
  UserProfile,
  UserProfileQueryArguments
} from './model';
import { UserProfileProcessor } from './user-profile-processor';
import {
  createNewProfileFromSetAudibleNotificationsInput,
  createNewProfileFromSetLayoutInput
} from './user-profile-utils';

export const resolvers = {
  // The base query for a User Profile
  Query: {
    userProfile: async (_, args, userContext: UserContext): Promise<UserProfile> => {
      logger.info(`User profile get requested for User: ${userContext.userName}`);
      const userProfile = userContext.userCache.getUserProfile();
      const profile =
        userProfile !== undefined
          ? userProfile
          : await UserProfileProcessor.Instance().loadUserProfile();
      userContext.userCache.setUserProfile(profile);
      return profile;
    }
  },

  // Mutation resolvers
  Mutation: {
    setLayout: async (
      _,
      setLayoutInput: SetLayoutInput,
      userContext: UserContext
    ): Promise<UserProfile> => {
      logger.info(`User profile setLayout requested for User: ${userContext.userName}`);

      const userProfile = userContext.userCache.getUserProfile();
      const currentProfile =
        userProfile !== undefined
          ? userProfile
          : await UserProfileProcessor.Instance().loadUserProfile();

      const newProfile = createNewProfileFromSetLayoutInput(currentProfile, setLayoutInput);
      return UserProfileProcessor.Instance().setUserProfile(userContext, newProfile);
    },
    setAudibleNotifications: async (
      _,
      setAudibleNotificationsInput: SetAudibleNotificationsInput,
      userContext: UserContext
    ): Promise<UserProfile> => {
      logger.info(
        `User profile setAudibleNotifications requested for User: ${userContext.userName}`
      );

      const userProfile = userContext.userCache.getUserProfile();
      const currentProfile = userProfile
        ? userProfile
        : await UserProfileProcessor.Instance().loadUserProfile();

      const newProfile = createNewProfileFromSetAudibleNotificationsInput(
        currentProfile,
        setAudibleNotificationsInput
      );
      return UserProfileProcessor.Instance().setUserProfile(userContext, newProfile);
    }
  },

  // Field Resolver for User Profiles
  UserProfile: {
    // resolves an ID field for graphQL
    id: (userProfile: UserProfile) => userProfile.userId,
    defaultLayoutName: (userProfile: UserProfile, args: UserProfileQueryArguments) => {
      switch (args.defaultLayoutName) {
        case DefaultLayoutNames.ANALYST_LAYOUT:
          return userProfile.defaultLayoutName;
        case DefaultLayoutNames.SOH_LAYOUT:
          return userProfile.sohLayoutName;
        default:
          return userProfile.defaultLayoutName;
      }
    }
  }
};
