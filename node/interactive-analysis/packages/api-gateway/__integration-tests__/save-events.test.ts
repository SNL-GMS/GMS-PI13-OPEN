// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import { jsonPretty, uuid4 } from '@gms/common-util';
import config from 'config';
import cloneDeep from 'lodash/cloneDeep';
import { SaveEventsServiceResponse } from '../src/ts/event/event-services-client';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('event.backend.services.saveEvents.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('save events', () => {
  const eventToSave = getIntegrationInput('saveEvent')[0];

  test('save a single event', async () => {
    const newId = uuid4();
    eventToSave.id = newId;
    const eventHypothesis = eventToSave.hypotheses[0];
    eventHypothesis.id = newId;
    eventHypothesis.eventId = newId;
    eventHypothesis.locationSolutions[0].id = newId;
    eventHypothesis.preferredLocationSolution.locationSolution.id = newId;
    eventToSave.preferredEventHypothesisHistory[0].eventHypothesisId = newId;
    const response = await httpWrapper.request<SaveEventsServiceResponse>(requestConfig, [
      eventToSave
    ]);
    console.log(jsonPretty(response.data));
    expect(response.status).toEqual(200);
    expect(response.data.storedEvents).toHaveLength(1);
    expect(response.data.updatedEvents).toHaveLength(0);
    expect(response.data.errorEvents).toHaveLength(0);
  });

  test('update a single event', async () => {
    const newId = uuid4();
    const newEventHyp = cloneDeep(eventToSave.hypotheses[0]);
    newEventHyp.id = newId;
    newEventHyp.locationSolutions[0].id = newId;
    newEventHyp.preferredLocationSolution.locationSolution.id = newId;
    const newPreferredEntry = cloneDeep(eventToSave.preferredEventHypothesisHistory[0]);
    eventToSave.hypotheses.push(newEventHyp);
    eventToSave.preferredEventHypothesisHistory.push({
      ...newPreferredEntry,
      eventHypothesisId: newId
    });
    const response = await httpWrapper.request<SaveEventsServiceResponse>(requestConfig, [
      eventToSave
    ]);
    console.log(jsonPretty(response.data));
    expect(response.status).toEqual(200);
    expect(response.data.storedEvents).toHaveLength(0);
    expect(response.data.updatedEvents).toHaveLength(1);
    expect(response.data.errorEvents).toHaveLength(0);
  });
});
