import config from 'config';
import { testUserProfile } from '../resources/test_data/integration-inputs/test-user-profile';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';

const requestConfig = config.get('userProfile.backend.services.setUserProfile.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  // tslint:disable-next-line: no-magic-numbers
  jest.setTimeout(60000);
});
describe('Set user profile tests', () => {
  test('Sets test user profile', async () => {
    const response = await httpWrapper.request(requestConfig, JSON.stringify(testUserProfile));
    // tslint:disable-next-line: no-magic-numbers
    expect(response.status).toEqual(200);
  });
});
