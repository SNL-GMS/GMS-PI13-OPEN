// tslint:disable:max-line-length
import { graphql } from 'graphql';
import { schema } from '../src/ts/server/api-gateway-schema';
import { userContext } from './__data__/user-profile-data';

let newMaskId = '';
// ---- Query test cases ----

// Test case - basic content query for waveforms
it('QC Content query should match snapshot', async () => {
  // language=GraphQL
  const query = `
  query qcMasksByChannelNameQuery {
    qcMasksByChannelName(timeRange: {startTime: 1274392801, endTime: 1274400000}, channelNames: ["KDAK.KDAK.BHZ"]) {
      currentVersion {
        type
        category
        startTime
        endTime
      }
    }
  }
  `;

  const rootValue = {};
  // Execution the GraphQL query with a small delay to allow the API gateway to settle async
  // HTTP requests fetching data from the mock backend
  const result = await graphql(schema, query, rootValue, userContext);
  const { data } = result;

  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});

// eb1f82bb-ed5c-4f6b-873c-0b9515aef272

// ---- Mutation test cases ----

// Test case - basic content query for waveforms
it('QC Create mutation should match snapshot', async () => {
  // language=GraphQL
  const query = `
  mutation createMask {
    createQcMask(channelNames: ["KDAK.KDAK.BHZ"], input: {timeRange: {startTime: 1274393921, endTime: 1274393926}, category: "ANALYST_DEFINED", type: "SPIKE", rationale: "testing"}) {
      qcMasks {
        id
        channelName
        currentVersion {
          startTime
          endTime
          category
          type
          rationale
          version
        }
        qcMaskVersions {
          startTime
          endTime
          category
          type
          rationale
          version
        }
      }
    }
  }
  `;

  const rootValue = {};
  // Execution the GraphQL query with a small delay to allow the API gateway to settle async
  // HTTP requests fetching data from the mock backend
  const result = await graphql(schema, query, rootValue, userContext);
  const { data } = result;
  newMaskId = data.createQcMask.qcMasks[0].id;
  // Check snapshots
  expect(data.createQcMask.qcMasks[0].currentVersion).toMatchSnapshot();
  expect(data.createQcMask.qcMasks[0].qcMaskVersions).toMatchSnapshot();
});

// Test case - basic content query for waveforms
it('QC Update mutation should match snapshot', async () => {
  // language=GraphQL
  // mask Id corresponds a mask on channel AS01/SHZ
  const query = `
  mutation updateMask {
    updateQcMask(qcMaskId: "${newMaskId}", input: {timeRange: {startTime: 1274393921, endTime: 1274393936}, category: "ANALYST_DEFINED", type: "SPIKE", rationale: "Updating Test"}) {
      qcMasks {
        id
        channelName
        currentVersion {
          startTime
          endTime
          category
          type
          rationale
          version
        }
        qcMaskVersions {
          startTime
          endTime
          category
          type
          rationale
          version
        }
      }
    }
  }
  `;

  const rootValue = {};
  // Execution the GraphQL query with a small delay to allow the API gateway to settle async
  // HTTP requests fetching data from the mock backend
  const result = await graphql(schema, query, rootValue, userContext);
  const { data } = result;

  // Compare response to snapshot
  expect(data.updateQcMask.qcMasks[0].currentVersion).toMatchSnapshot();
  expect(data.updateQcMask.qcMasks[0].qcMaskVersions).toMatchSnapshot();
});

// Test case - basic content query for waveforms
// Query for channel AS01/SHZ
it('QC Create reject should match snapshot', async () => {
  // language=GraphQL
  // mask Id corresponds a mask on channel AS01/SHZ
  const query = `
  mutation rejectMask {
    rejectQcMask(qcMaskId: "${newMaskId}", rationale: "This was a test mask anyway") {
      qcMasks{
        id
        channelName
        currentVersion {
          startTime
          endTime
          category
          type
          rationale
          version
        }
        qcMaskVersions {
          startTime
          endTime
          category
          type
          rationale
          version
       }
      }
    }
  }
  `;

  const rootValue = {};
  // Execution the GraphQL query with a small delay to allow the API gateway to settle async
  // HTTP requests fetching data from the mock backend
  const result = await graphql(schema, query, rootValue, userContext);
  const { data } = result;

  // Compare response to snapshot
  expect(data.rejectQcMask.qcMasks[0].currentVersion).toMatchSnapshot();
  expect(data.rejectQcMask.qcMasks[0].qcMaskVersions).toMatchSnapshot();
});
