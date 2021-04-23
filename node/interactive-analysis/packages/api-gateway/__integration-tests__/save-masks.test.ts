// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import { uuid4 } from '@gms/common-util';
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('qcMask.backend.services.saveMasks.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('save qc masks', () => {
  test('save qc mask to KDAK', async () => {
    const newId = uuid4();
    const maskInput = getIntegrationInput('saveQcMask');
    // Change inputs to be known values (and not interfere with STDS)
    maskInput[0].id = newId;
    maskInput[0].qcMaskVersions[0].startTime = '2010-05-20T19:30:00Z';
    maskInput[0].qcMaskVersions[0].endTime = '2010-05-20T19:32:00Z';

    const response = await httpWrapper.request(requestConfig, maskInput);
    expect(response.status).toEqual(200);
    expect(response.data).toEqual([newId]);
  });
});
