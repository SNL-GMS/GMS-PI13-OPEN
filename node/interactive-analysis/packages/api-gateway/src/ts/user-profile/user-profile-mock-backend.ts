import { readJsonData } from '@gms/common-util';
import config from 'config';
import path from 'path';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { HttpMockWrapper } from '../util/http-wrapper';
import { resolveTestDataPaths } from '../util/test-data-util';
import { UserProfile } from './model';

let userProfileData: UserProfile;

/**
 * Configure mock HTTP interfaces for a user preference backend services.
 * @param httpMockWrapper the http mock wrapper
 * @param mockEnabled true if mock is enabled; false otherwise
 */
export function initialize(httpMockWrapper: HttpMockWrapper) {
  logger.info('Initializing mock backend for User Profile data');

  if (!httpMockWrapper) {
    throw new Error(
      'Cannot initialize mock User Profile services with undefined HTTP mock wrapper'
    );
  }

  // Load the Event backend service config settings
  const backendConfig = config.get('userProfile.backend');

  // load test data
  userProfileData = loadTestData();

  logger.info(`mock data loaded for ID:${userProfileData.userId}`);
  logger.info(`mock is enabled for user Profile ${backendConfig}`);
  httpMockWrapper.onMock(backendConfig.services.getUserProfile.requestConfig.url, getUserProfile);
  httpMockWrapper.onMock(backendConfig.services.setUserProfile.requestConfig.url, setUserProfile);
}

/**
 * Load test data into the mock backend data store from the configured test data set.
 */
async function getUserProfile(input: any): Promise<UserProfile> {
  logger.info(`hit the mock service catch with the following input: ${input}`);
  return userProfileData;
}

/**
 * set test data into the mock backend data store from the configured test data set.
 */
async function setUserProfile(input: any): Promise<UserProfile> {
  logger.info(`hit the mock service setting user data`);
  userProfileData = JSON.parse(input);
  return userProfileData;
}

/**
 * Load test data into the mock backend data store from the configured test data set.
 */
function loadTestData(): any {
  const dataPath = resolveTestDataPaths().additionalDataHome;

  // Read the network definitions from the test data file set
  return readJsonData(
    dataPath
      .concat(path.sep)
      .concat(config.get('testData.additionalTestData.defaultUserProfileFile'))
  );
}
