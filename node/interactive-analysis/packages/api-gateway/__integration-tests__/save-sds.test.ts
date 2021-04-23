// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import { uuid4 } from '@gms/common-util';
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('signalDetection.backend.services.saveSds.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('save signal detections', () => {
  test('save a single signal detection', async () => {
    const newId = uuid4();
    const signalDetectionToSave = getIntegrationInput('saveSignalDetection');
    signalDetectionToSave[0].id = newId;
    const sdHypothesis = signalDetectionToSave[0].signalDetectionHypotheses[0];
    sdHypothesis.id = newId;
    sdHypothesis.parentSignalDetectionId = newId;
    const response = await httpWrapper.request(requestConfig, signalDetectionToSave);
    expect(response.status).toEqual(200);
    expect(response.data).toEqual([newId]);
  });
});
