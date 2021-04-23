// tslint:disable:max-line-length
import { graphql } from 'graphql';
import { TimeRange } from '../src/ts/common/model';
import { ConfigurationProcessor } from '../src/ts/configuration/configuration-processor';
import { schema } from '../src/ts/server/api-gateway-schema';
import { SignalDetectionProcessor } from '../src/ts/signal-detection/signal-detection-processor';
import { ProcessingStationProcessor } from '../src/ts/station/processing-station/processing-station-processor';
import { userContext } from './__data__/user-profile-data';

beforeAll(async () => setupTest());

let timeRange: TimeRange;
let stationIds: string[];
const sdIds: string[] = [];

/**
 * Setup test
 */
async function setupTest() {
  timeRange = {
    startTime: 1274385600,
    endTime: 1274400000
  };
  stationIds = ['ASAR', 'PDAR'];
  await ConfigurationProcessor.Instance().fetchConfiguration();
  await ProcessingStationProcessor.Instance().fetchStationData();
  await SignalDetectionProcessor.Instance().loadSignalDetections(
    userContext,
    timeRange,
    stationIds
  );
}

// Test case - Query for signal detections by station ID and time range; show hypothesis IDs
it('Querying for signal detections by station and time range should match snapshot', async () => {
  const query = `
  query signalDetectionsByStation {
    signalDetectionsByStation(stationIds: ["${stationIds[0]}"], timeRange: {startTime: 1274385600, endTime: 1274400000}) {
      id
      currentHypothesis {
        id
        rejected
        featureMeasurements {
          featureMeasurementType
        }
      }
    }
  }
  `;

  // Execute the GraphQL query
  const rootValue = {};
  const result = await graphql(schema, query, rootValue, userContext);
  const { data } = result;
  sdIds.push(data.signalDetectionsByStation[0].id);
  sdIds.push(data.signalDetectionsByStation[1].id);
  sdIds.push(data.signalDetectionsByStation[3].id);
  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});

// Test case - Query for signal detection by ID
it('Querying for signal detection by ID should match snapshot', async () => {
  // language=GraphQL
  const query = `
  query signalDetectionsById {
    signalDetectionsById(detectionIds: ["${sdIds[0]}", "${sdIds[1]}"]) {
      id
      currentHypothesis {
        id
        rejected
        featureMeasurements {
          featureMeasurementType
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

// ---- Mutation test cases ----

it('marking an amplitude measurement as reviewed should match snapshot', async () => {
  const mutation = `
  mutation {
    markAmplitudeMeasurementReviewed(signalDetectionIds: ["${sdIds[0]}"]){
      sds {
        requiresReview {
          amplitudeMeasurement
        }
        stationName
      }
    }
  }
  `;

  // Execute the GraphQL query
  const rootValue = {};
  const result = await graphql(schema, mutation, rootValue, userContext);
  const { data } = result;

  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});

// Test case - Create a new signal detection (mutation)
it('Creating a new detection should match snapshot', async () => {
  // language=GraphQL
  const mutation = `
  mutation createDetection {
    createDetection(
      input: {
        stationId: "${stationIds[0]}"
        phase: "P"
        signalDetectionTiming: {
          arrivalTime: 1
          timeUncertaintySec: 0
          amplitudeMeasurement: { startTime: 2, period: 1, amplitude: {value: 2, standardDeviation:0.1, units: UNITLESS} }
        }
      }
    ) {
    	sds {
        currentHypothesis {
          featureMeasurements {
            featureMeasurementType
            measurementValue{
              ...on PhaseTypeMeasurementValue {
                phase
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

  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});

// Test case - Update an existing signal detection phase (mutation)
it('Updating an existing detection phase should match snapshot', async () => {
  // language=GraphQL
  const mutation = `
  mutation updatePhase {
    updateDetection(detectionId: "${sdIds[0]}", input: {phase: "S"}) {
      sds {
      currentHypothesis {
        featureMeasurements {
          featureMeasurementType
          measurementValue{
            ...on PhaseTypeMeasurementValue {
              phase
            }
        	}}
        }
      }
    }
  }
  `;

  // Execute the GraphQL query
  const rootValue = {};
  const result = await graphql(schema, mutation, rootValue, userContext);
  const { data } = result;

  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});

// Test case - Update an existing signal detection time (mutation)
it('Updating an existing detection time should match snapshot', async () => {
  // language=GraphQL
  const mutation = `
  mutation updateTime {
    updateDetection(detectionId: "${sdIds[0]}", input: {time: 1274324501, timeUncertaintySec: 0.1}) {
      sd {
        currentHypothesis {
          featureMeasurements {
            featureMeasurementType
            measurementValue{
              ...on InstantMeasurementValue {
                value
                standardDeviation
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

  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});

// Test case - Update an existing signal detection time & phase (mutation)
xit('Updating an existing detection time & phase should match snapshot', async () => {
  // language=GraphQL
  const mutation = `
  mutation updateTimeAndPhase {
    updateDetection(
      detectionId: "${sdIds[0]}"
      input: {
        phase: "S"
        signalDetectionTiming: {
          arrivalTime: 1274324502
          timeUncertaintySec: 0.1
          amplitudeMeasurement: {
            startTime: 2
            period: 1
            amplitude: { value: 2, standardDeviation: 0.1, units: UNITLESS }
          }
        }
      }
    ) {
      sds {
        currentHypothesis {
          featureMeasurements {
            featureMeasurementType
            measurementValue {
              ... on InstantMeasurementValue {
                value
                standardDeviation
              }
              ... on PhaseTypeMeasurementValue {
                phase
              }
              ... on AmplitudeMeasurementValue {
                startTime
                period
                amplitude {
                  value
                  standardDeviation
                  units
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
  const result = await graphql(schema, mutation, rootValue, userContext);
  const { data } = result;

  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});

// Test case - Update a collection of existing signal detections with new time & phase (mutation)
it('Updating a collection of detections for time & phase should match snapshot', async () => {
  // language=GraphQL
  const mutation = `
  mutation updateMultiTimeAndPhase {
    updateDetections(
      detectionIds: [
        "${sdIds[1]}"
        "${sdIds[2]}"
      ]
      input: {
        phase: "S"
        signalDetectionTiming: {
          arrivalTime: 1274324502
          timeUncertaintySec: 0.1
          amplitudeMeasurement: {
            startTime: 2
            period: 1
            amplitude: { value: 2, standardDeviation: 0.1, units: UNITLESS }
          }
        }
      }
    ) {
      sds{
        currentHypothesis {
          featureMeasurements {
            featureMeasurementType
            measurementValue {
              ... on InstantMeasurementValue {
                value
                standardDeviation
              }
              ... on PhaseTypeMeasurementValue {
                phase
              }
              ... on AmplitudeMeasurementValue {
                startTime
                period
                amplitude {
                  value
                  standardDeviation
                  units
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
  const result = await graphql(schema, mutation, rootValue, userContext);
  const { data } = result;

  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});

// Test case - Reject a collection of existing signal detection hypotheses
it('Rejecting a collection of detection hypotheses should match snapshot', async () => {
  // language=GraphQL
  const mutation = `
  mutation rejectDetection {
    rejectDetections(detectionIds: ["${sdIds[1]}", "${sdIds[2]}"]) {
      sds {
        currentHypothesis {
          rejected
        }
      }
    }
  }
  `;

  // Execute the GraphQL query
  const rootValue = {};
  const result = await graphql(schema, mutation, rootValue, userContext);
  const { data } = result;

  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});
