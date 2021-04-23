// tslint:disable:max-line-length
import { graphql } from 'graphql';
import { PhaseType, TimeRange } from '../src/ts/common/model';
import { ConfigurationProcessor } from '../src/ts/configuration/configuration-processor';
import { EventProcessor } from '../src/ts/event/event-processor';
import { MagnitudeType } from '../src/ts/event/model-and-schema/model';
import {
  generateSignalDetectionBehaviorsMap,
  getLocationBehaviorsForSd
} from '../src/ts/event/utils/event-utils';
import { createDefaultDefiningBehaviorForStation } from '../src/ts/event/utils/network-magnitude-utils';
import { schema } from '../src/ts/server/api-gateway-schema';
import { FeatureMeasurementTypeName } from '../src/ts/signal-detection/model';
import { SignalDetectionProcessor } from '../src/ts/signal-detection/signal-detection-processor';
import { ProcessingStationProcessor } from '../src/ts/station/processing-station/processing-station-processor';
import { findPhaseFeatureMeasurementValue } from '../src/ts/util/feature-measurement-utils';
import { userContext } from './__data__/user-profile-data';

beforeAll(async () => setupTest());

let timeRange: TimeRange;
let stationNames: string[];
let eventIdToChange: string;
let assocEventId: string;
let associatedSdId: string;
let pAssociatedEventid: string;

/**
 * Sets up test by loading SDs
 */
async function setupTest() {
  timeRange = {
    startTime: 1274385600,
    endTime: 1274400000
  };
  stationNames = ['ASAR'];
  await ConfigurationProcessor.Instance().fetchConfiguration();
  await ProcessingStationProcessor.Instance().fetchStationData();
  await EventProcessor.Instance().loadEventsInTimeRange(userContext, timeRange);
  await SignalDetectionProcessor.Instance().loadSignalDetections(
    userContext,
    timeRange,
    stationNames
  );
}

// ---- Query test cases ---
describe('Event loading and getting', () => {
  it('loads events in time range', async () => {
    const query = `
      query loadEventsInTimeRange{
        loadEventsInTimeRange(timeRange: {startTime: ${timeRange.startTime}, endTime: ${timeRange.endTime}}) {
          id
          monitoringOrganization
          currentEventHypothesis {
            eventHypothesis {
              id
              rejected
              signalDetectionAssociations {
                signalDetectionHypothesis {
                  id
                }
              }
            }
          }
        }
      }
    `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;

    // Compare response to snapshot
    expect(data).toMatchSnapshot();
  });

  it('get events in time range', async () => {
    const query = `
      query eventsInTimeRange{
        eventsInTimeRange(timeRange: {startTime: ${timeRange.startTime}, endTime: ${timeRange.endTime}}) {
          id
          monitoringOrganization
          currentEventHypothesis {
            eventHypothesis {
              id
              rejected
              signalDetectionAssociations {
                signalDetectionHypothesis {
                  id
                  parentSignalDetectionId
                  featureMeasurements {
                    featureMeasurementType
                    measurementValue {
                      ... on PhaseTypeMeasurementValue {
                        phase
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;
    // Get an event id to update
    eventIdToChange = data.eventsInTimeRange[0].id;
    // Get an event with associations
    const eventWithAssoc = data.eventsInTimeRange.find(
      ev => ev.currentEventHypothesis.eventHypothesis.signalDetectionAssociations.length > 0
    );
    assocEventId = eventWithAssoc.id;
    // Get the sd id for the associated sd
    associatedSdId =
      eventWithAssoc.currentEventHypothesis.eventHypothesis.signalDetectionAssociations[0]
        .signalDetectionHypothesis.parentSignalDetectionId;

    // Find Event with a p association
    data.eventsInTimeRange.forEach(ev => {
      if (ev.currentEventHypothesis.eventHypothesis.signalDetectionAssociations.length > 0) {
        // I have associations
        ev.currentEventHypothesis.eventHypothesis.signalDetectionAssociations.forEach(assoc => {
          // Check each assoc
          const value = findPhaseFeatureMeasurementValue(
            assoc.signalDetectionHypothesis.featureMeasurements
          );
          if (pAssociatedEventid === undefined && value && value.phase === PhaseType.P) {
            pAssociatedEventid = ev.id;
          }
        });
      }
    });

    // Compare response to snapshot
    expect(data).toMatchSnapshot();
  });

  it('get event by id', async () => {
    const query = `
      query eventById{
        eventById(eventId: "${eventIdToChange}") {
          id
          status
          modified
          monitoringOrganization
          hasConflict
          currentEventHypothesis {
            eventHypothesis {
              id
              rejected
              signalDetectionAssociations {
                signalDetectionHypothesis {
                  id
                }
              }
            }
          }
        }
      }
    `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;
    // Check specific fields
    expect(data.eventById.modified).toBeFalsy();

    // Compare response to snapshot
    expect(data).toMatchSnapshot();
  });
});

// ---- Mutation test cases ---
describe('Updating and Saving events', () => {
  it('update event to be in refinement', async () => {
    const mutation = `
      mutation updateEvents{
        updateEvents(eventIds: ["${eventIdToChange}"], input: {status: OpenForRefinement, processingStageId: "000000"}) {
          events {
            id
            status
            modified
          }
        }
      }
    `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, mutation, rootValue, userContext);
    const { data } = result;

    // Check specific fields
    expect(data.updateEvents.events[0].modified).toBeFalsy();
    // Compare response to snapshot
    expect(data).toMatchSnapshot();
  });

  it('change associations on event to modify event', async () => {
    const mutation = `
      mutation changeSignalDetectionAssociations{
        changeSignalDetectionAssociations(eventHypothesisId: "${eventIdToChange}", signalDetectionIds: ["${associatedSdId}"], associate: true) {
          events {
            id
            status
            modified
            hasConflict
            conflictingSdIds
            currentEventHypothesis {
              eventHypothesis {
                signalDetectionAssociations {
                  signalDetectionHypothesis {
                    parentSignalDetectionId
                  }
                }
              }
            }
          }
        }
      }
    `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, mutation, rootValue, userContext);
    const { data } = result;
    // Check specific fields

    const event = data.changeSignalDetectionAssociations.events.find(
      ev => ev.id === eventIdToChange
    );
    expect(event.modified).toBeTruthy();
    // Compare response to snapshot
    expect(data).toMatchSnapshot();
    // Check specific fields
    const affectedEvent = data.changeSignalDetectionAssociations.events.find(
      ev => ev.id === assocEventId
    );
    expect(affectedEvent.modified).toBeFalsy();
  });

  it('get event by id to check for conflict', async () => {
    const query = `
      query eventById{
        eventById(eventId: "${assocEventId}") {
          id
          status
          modified
          monitoringOrganization
          hasConflict
        }
      }
    `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;
    // Check specific fields
    expect(data.eventById.modified).toBeFalsy();
    expect(data.eventById.hasConflict).toBeTruthy();

    // Compare response to snapshot
    expect(data).toMatchSnapshot();
  });

  it('get sd that was just associated and check conflict', async () => {
    const query = `
      query signalDetectionsById {
        signalDetectionsById(detectionIds: ["${associatedSdId}"]) {
          id
          hasConflict
        }
      }
    `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;
    // Check specific fields
    // expect(data.changeSignalDetectionAssociations.events[0].modified).toBeTruthy();
    // Compare response to snapshot
    expect(data).toMatchSnapshot();
  });

  it('save event', async () => {
    const mutation = `
      mutation saveEvent{
        saveEvent(eventId: "${eventIdToChange}") {
          events {
            id
            status
            modified
            hasConflict
            conflictingSdIds
            currentEventHypothesis {
              eventHypothesis {
                signalDetectionAssociations {
                  signalDetectionHypothesis {
                    parentSignalDetectionId
                  }
                }
              }
            }
          }
        }
      }
    `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, mutation, rootValue, userContext);
    const { data } = result;

    // Check specific fields
    expect(data.saveEvent.modified).toBeFalsy();
    // Compare response to snapshot
    expect(data).toMatchSnapshot();
  });
  const fauxSdHyp: any = {
    id: 'test1',
    parentSignalDetectionId: 'sd1',

    featureMeasurements: [
      {
        id: '1',
        featureMeasurementType: FeatureMeasurementTypeName.ARRIVAL_TIME
      },
      {
        id: '2',
        featureMeasurementType: FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH
      }
    ]
  };
  const fauxLocationBehaviors: any = [
    {
      signalDetectionId: 'sd1',
      featureMeasurementType: FeatureMeasurementTypeName.ARRIVAL_TIME,
      defining: true
    },
    {
      signalDetectionId: 'sd1',
      featureMeasurementType: FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH,
      defining: false
    },
    {
      signalDetectionId: 'sd1',
      featureMeasurementType: FeatureMeasurementTypeName.SLOWNESS,
      defining: false
    }
  ];

  it('can get location behaviors for sd', () => {
    const locationBehaviors = getLocationBehaviorsForSd(fauxSdHyp, fauxLocationBehaviors);
    expect(locationBehaviors.length).toEqual(2);
  });

  it('can generate signal detections behaviors map', () => {
    const map = generateSignalDetectionBehaviorsMap(fauxLocationBehaviors, [fauxSdHyp]);
    expect(map).toMatchSnapshot();
  });

  // these test in and around magnitude
  it('can create default defining behaviors for stations', () => {
    const defaultDefining = createDefaultDefiningBehaviorForStation('1', MagnitudeType.MB, true);
    expect(defaultDefining).toEqual({
      stationName: '1',
      magnitudeType: MagnitudeType.MB,
      defining: true
    });
  });

  it('can compute magnitude', async () => {
    const query = `
    mutation computeNetworkMagnitudeSolution {
      computeNetworkMagnitudeSolution(
        computeNetworkMagnitudeSolutionInput: {
          eventHypothesisId: "${pAssociatedEventid}"
          magnitudeType: MB
          stationNames: ["${stationNames[0]}"]
          defining: false
          locationSolutionSetId: "loc-111"
        }
      ) {
        dataPayload {
          events {
            currentEventHypothesis {
              eventHypothesis {
                locationSolutionSets {
                  locationSolutions {
                    networkMagnitudeSolutions {
                      magnitudeType
                      networkMagnitudeBehaviors {
                        defining
                        stationMagnitudeSolution {
                          type
                          model
                          stationName
                          phase
                          modelCorrection
                          stationCorrection
                        }
                        weight
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;
    // Compare response to snapshot
    expect(data).toMatchSnapshot();
  });
});
