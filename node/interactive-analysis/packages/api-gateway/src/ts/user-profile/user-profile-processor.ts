import config from 'config';
import { UserContext } from '../cache/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { HttpClientWrapper } from '../util/http-wrapper';
import { UserProfile } from './model';
import * as userProfileMockBackend from './user-profile-mock-backend';
import { getUserProfileService, setUserProfileService } from './user-profile-services-client';

/**
 * User profile processor obtains user profile for workspaces
 */
export class UserProfileProcessor {
  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): UserProfileProcessor {
    if (UserProfileProcessor.instance === undefined) {
      UserProfileProcessor.instance = new UserProfileProcessor();
      UserProfileProcessor.instance.initialize();
    }
    return UserProfileProcessor.instance;
  }

  /** The singleton instance */
  private static instance: UserProfileProcessor;

  /** Settings for the user profile processor */
  private readonly settings: any;

  /** Axios http wrapper  */
  private readonly httpWrapper: HttpClientWrapper;

  public constructor() {
    this.settings = config.get('userProfile');
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Initialize the user profile processor, setting up a mock backend if configured to do so.
   */
  public initialize(): void {
    logger.info(
      'Initializing the user profile processor - Mock Enable: %s',
      this.settings.backend.mock.enable
    );

    if (this.settings.backend.mock.enable) {
      userProfileMockBackend.initialize(this.httpWrapper.createHttpMockWrapper());
    }
  }

  /**
   * Query web service and get the current user's profile
   * @returns a Promise that resolves to a user profile
   */
  public async loadUserProfile(): Promise<UserProfile> {
    const requestConfig = this.settings.backend.services.getUserProfile.requestConfig;
    const profile = await getUserProfileService(this.httpWrapper, requestConfig);

    logger.info(`got response back for: ${profile.userId}`);
    return profile;
  }

  /**
   * set/save the user profile
   * @param userContext which contains the user cache in which to set the user profile
   * @param newUserProfile The user profile which should be set/saved
   * @returns a Promise that resolves to a user profile
   */
  public async setUserProfile(
    userContext: UserContext,
    newUserProfile: UserProfile
  ): Promise<UserProfile> {
    // set the user profile here by calling the client
    logger.info(
      `calling set user profile web service for ${newUserProfile.userId}` +
        ` with default ${newUserProfile.defaultLayoutName}`
    );
    const requestConfig = this.settings.backend.services.setUserProfile.requestConfig;
    const profile = await setUserProfileService(this.httpWrapper, requestConfig, newUserProfile);
    userContext.userCache.setUserProfile(profile);
    return profile;
  }
}
