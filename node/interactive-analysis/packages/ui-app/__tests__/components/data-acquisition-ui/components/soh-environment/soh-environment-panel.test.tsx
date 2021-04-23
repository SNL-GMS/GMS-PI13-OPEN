import { uuid } from '@gms/common-util';
import { Client } from '@gms/ui-apollo';
import DefaultClient from 'apollo-boost';
import { uniqueId } from 'lodash';
import React from 'react';
import { ApolloProvider } from 'react-apollo';
import { SohTypes } from '../../../../../../common-graphql/src/ts/graphql/soh';
import { BaseDisplayContext } from '../../../../../src/ts/components/common-ui/components/base-display';
import { EnvironmentPanel } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/soh-environment-panel';
import { EnvironmentPanelProps } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/types';
import { FilterableSOHTypes } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-overview/types';
import {
  SohContext,
  SohContextData
} from '../../../../../src/ts/components/data-acquisition-ui/shared/soh-context';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
uuid.asString = jest.fn().mockImplementation(uniqueId);

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

const client: Client = new DefaultClient<any>();

describe('SohEnvironmentPanel class', () => {
  const channel: SohTypes.ChannelSoh[] = [
    {
      allSohMonitorValueAndStatuses: [
        {
          status: SohTypes.SohStatusSummary.GOOD,
          value: 1,
          valuePresent: true,
          monitorType: SohTypes.SohMonitorType.ENV_ZEROED_DATA,
          hasUnacknowledgedChanges: false,
          contributing: false,
          thresholdMarginal: 1,
          thresholdBad: 10,
          quietUntilMs: 1
        }
      ],
      channelName: 'channelName',
      channelSohStatus: SohTypes.SohStatusSummary.GOOD
    }
  ];
  const channelStatusesToDisplay: Map<FilterableSOHTypes, boolean> = new Map<
    FilterableSOHTypes,
    boolean
  >();
  const columnHeaderData = FilterableSOHTypes.GOOD;
  channelStatusesToDisplay.set(columnHeaderData, true);
  const monitorStatusesToDisplay: Map<any, boolean> = new Map();
  monitorStatusesToDisplay.set(SohTypes.SohStatusSummary.GOOD, true);
  const myProps: EnvironmentPanelProps = {
    channelSohs: channel,
    channelStatusesToDisplay,
    // tslint:disable-next-line: no-magic-numbers
    quietingDurationSelections: [1, 5, 10],
    defaultQuietDurationMs: 10,
    monitorStatusesToDisplay,
    stationName: 'AAK',
    isStale: false
    // selectedMonitor: SohTypes.SohMonitorType.ENV_AUTHENTICATION_SEAL_BROKEN,
    // setSelectedMonitor: jest.fn(),
  };

  const contextDefaults: SohContextData = {
    glContainer: {} as any,
    selectedAceiType: SohTypes.AceiType.BEGINNING_TIME_OUTAGE,
    quietChannelMonitorStatuses: jest.fn(),
    setSelectedAceiType: jest.fn()
  };
  const sohEnvironmentPanel = Enzyme.mount(
    <ApolloProvider client={client}>
      <BaseDisplayContext.Provider
        value={{
          glContainer: { width: 150, height: 150 } as any,
          widthPx: 150,
          heightPx: 150
        }}
      >
        <SohContext.Provider value={contextDefaults}>
          <EnvironmentPanel {...myProps} />
        </SohContext.Provider>
      </BaseDisplayContext.Provider>
    </ApolloProvider>
  );
  it('should be defined', () => {
    expect(sohEnvironmentPanel).toBeDefined();
  });
  it('should match snapshot', () => {
    expect(sohEnvironmentPanel).toMatchSnapshot();
  });
});
