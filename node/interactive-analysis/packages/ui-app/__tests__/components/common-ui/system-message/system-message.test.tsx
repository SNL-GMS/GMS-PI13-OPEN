import { UserMode, uuid } from '@gms/common-util';
import { Client } from '@gms/ui-apollo';
import { createStore } from '@gms/ui-state';
import DefaultClient from 'apollo-boost';
import * as React from 'react';
import { ApolloProvider } from 'react-apollo';
import { act } from 'react-dom/test-utils';
import { Provider } from 'react-redux';
import { BaseDisplayContext } from '../../../../src/ts/components/common-ui/components/base-display';
import { SystemMessage } from '../../../../src/ts/components/common-ui/components/system-message/system-message-component';
import { SystemMessageProps } from '../../../../src/ts/components/common-ui/components/system-message/types';
import { systemMessageDefinitions } from '../../../__data__/common-ui/system-message-definition-data';
import { nonDefiningQuery } from '../../../__data__/test-util';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();
const client: Client = new DefaultClient<any>();
const TIME_TO_WAIT_MS = 2000;
// tslint:disable-next-line: deprecation
const lodash = require.requireActual('lodash');
lodash.uniqueId = () => '1';
let idCount = 0;
uuid.asString = jest.fn().mockImplementation(() => ++idCount);

const waitForComponentToPaint = async (wrapper: any) => {
  // fixes React warning that "An update to Component inside a test was not wrapped in act(...)."
  // this has something to do with use state or apollo and needs 100ms to figure itself out
  // tslint:disable-next-line: await-promise
  await act(async () => {
    await new Promise(resolve => setTimeout(resolve, TIME_TO_WAIT_MS));
    wrapper.update();
  });
};

const systemMessageProps: SystemMessageProps = {
  systemMessageDefinitionsQuery: {
    ...nonDefiningQuery,
    systemMessageDefinitions
  },
  addSystemMessages: jest.fn(),
  clearAllSystemMessages: jest.fn(),
  clearExpiredSystemMessages: jest.fn(),
  clearSystemMessages: jest.fn(),
  systemMessagesState: {
    lastUpdated: 0,
    latestSystemMessages: [],
    systemMessages: []
  },
  setAudibleNotifications: jest.fn(),
  userProfileQuery: {
    ...nonDefiningQuery,
    userProfile: {
      id: 'id',
      userId: '1',
      defaultLayoutName: 'default',
      audibleNotifications: [],
      workspaceLayouts: [
        {
          name: 'default',
          layoutConfiguration: 'test',
          supportedUserInterfaceModes: [UserMode.SOH]
        }
      ]
    }
  }
};

describe('System Message Component', () => {
  const store: any = createStore();

  const systemMessage = Enzyme.mount(
    <Provider store={store}>
      <ApolloProvider client={client}>
        <BaseDisplayContext.Provider
          value={{
            glContainer: { width: 150, height: 150 } as any,
            widthPx: 150,
            heightPx: 150
          }}
        >
          <SystemMessage {...systemMessageProps} />
        </BaseDisplayContext.Provider>
      </ApolloProvider>
    </Provider>
  );
  it('should be defined', async () => {
    // we gotta wait for the use state
    await waitForComponentToPaint(systemMessage);
    systemMessage.update();
    expect(SystemMessage).toBeDefined();
    expect(systemMessage).toBeDefined();
  });

  it('matches snapshot', async () => {
    // we gotta wait for the use state
    await waitForComponentToPaint(systemMessage);
    systemMessage.update();
    expect(systemMessage).toMatchSnapshot();
  });
});
