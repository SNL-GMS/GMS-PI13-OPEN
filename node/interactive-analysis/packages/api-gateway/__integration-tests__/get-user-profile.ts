import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';

const requestConfig = config.get('userProfile.backend.services.getUserProfile.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  // tslint:disable-next-line: no-magic-numbers
  jest.setTimeout(60000);
});
describe('Get User Profile tets', () => {
  test('Gets the default user', async () => {
    const response = await httpWrapper.request(requestConfig, '"defaultUser"');
    // tslint:disable-next-line: no-magic-numbers
    expect(response.status).toEqual(200);
    expect(response.data).toBeDefined();
    expect((response.data as any).userId).toEqual('defaultUser');
  });
});
