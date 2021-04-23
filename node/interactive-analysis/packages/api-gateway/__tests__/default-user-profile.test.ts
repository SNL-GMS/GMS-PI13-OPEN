// tslint:disable:max-line-length
import { graphql } from 'graphql';
import { schema } from '../src/ts/server/api-gateway-schema';
import { mutationVariables, userContext } from './__data__/user-profile-data';

beforeAll(async () => setupTest());

/**
 * Sets up test by loading SDs
 */
// tslint:disable-next-line: no-empty
function setupTest() {}

// ---- Query test cases ---
describe('Default User Profile Getting', () => {
  it('gets the default user profile', async () => {
    const query = `
    query {
      userProfile {
        userId
        defaultLayoutName(defaultLayoutName: "SOH_LAYOUT")
        workspaceLayouts {
          name
          supportedUserInterfaceModes
          layoutConfiguration
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

// ---- Mutation test cases ---
describe('Default User Profile Mutation', () => {
  it('saves the default user profile', async () => {
    const { saveAsDefault, workspaceLayoutInput } = mutationVariables;
    const mutation = `
    mutation setLayout {
      setLayout (
        saveAsDefault: ${saveAsDefault}
        workspaceLayoutInput: {
          name: "${workspaceLayoutInput.name}"
          supportedUserInterfaceModes: "${String(workspaceLayoutInput)}"
          layoutConfiguration: "${workspaceLayoutInput.layoutConfiguration}"
        }
      ) {
        userId,
        defaultLayoutName
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
});
