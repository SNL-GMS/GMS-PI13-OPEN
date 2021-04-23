import { ApolloError } from 'apollo-server-core';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { HttpClientWrapper, HttpResponse, isHttpResponseError } from '../util/http-wrapper';
import * as model from './model';

/**
 * get a users profile
 * @param httpWrapper the http client wrapper
 * @param requestConfig the request config
 */
export async function getUserProfileService(
  httpWrapper: HttpClientWrapper,
  requestConfig: any
): Promise<model.UserProfile> {
  const query = '"defaultUser"'; // ! Double quote is required
  logger.debug(`Sending service request for User Profile with id: ${query}`);
  const response: HttpResponse = await httpWrapper.request<any>(requestConfig, query);
  logger.debug(`called user profile service got back: ${JSON.stringify(response.data)}`);
  const profile: model.UserProfile = response.data;
  return profile;
}

/**
 * set the user profile with a call out to the profile service
 * @param httpWrapper the http wrapper
 * @param requestConfig the request config
 * @param userProfile the user profile
 */
export async function setUserProfileService(
  httpWrapper: HttpClientWrapper,
  requestConfig: any,
  userProfile: model.UserProfile
): Promise<model.UserProfile> {
  logger.info(`Sending service request set User Profile: ${userProfile.userId}`);
  const response: HttpResponse = await httpWrapper.request<any>(
    requestConfig,
    JSON.stringify(userProfile)
  );
  if (isHttpResponseError(response)) {
    throw new ApolloError('Failed to save User Profile', 'Service Error');
  }
  return userProfile;
}
