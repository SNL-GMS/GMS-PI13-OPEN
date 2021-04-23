import { SohTypes } from '@gms/common-graphql';
import { SohMonitorType } from '@gms/common-graphql/lib/graphql/soh/types';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import { Client } from '@gms/ui-apollo';
import DefaultClient from 'apollo-boost';
import React from 'react';
import { ApolloProvider } from 'react-apollo';
import { act } from 'react-dom/test-utils';
import { BaseDisplayContext } from '../../../../../src/ts/components/common-ui/components/base-display';
import { MissingLagHistoryPanel } from '../../../../../src/ts/components/data-acquisition-ui/components/missing-lag-history';
import * as validateNonIdealStateDependency from '../../../../../src/ts/components/data-acquisition-ui/components/missing-lag-history/non-ideal-states';
import { MissingLagHistoryPanelProps } from '../../../../../src/ts/components/data-acquisition-ui/components/missing-lag-history/types';
import * as Util from '../../../../../src/ts/components/data-acquisition-ui/components/missing-lag-history/utils';
import { BarLineChartData } from '../../../../../src/ts/components/data-acquisition-ui/shared/chart/types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
const HUNDRED_MS = 2000;
const MOCK_TIME_TWO = 1530518207007;
// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();
// Setup
global.Date.now = jest.fn(() => MOCK_TIME_TWO);

const client: Client = new DefaultClient<any>();
const waitForComponentToPaint = async (wrapper: any) => {
  // fixes React warning that "An update to Component inside a test was not wrapped in act(...)."
  // this has something to do with use state or apollo and needs 100ms to figure itself out
  // tslint:disable-next-line: await-promise
  await act(async () => {
    await new Promise(resolve => setTimeout(resolve, HUNDRED_MS));
    wrapper.update();
  });
};
describe('Soh Lag panel', () => {
  const missingLagHistoryPanelProps: MissingLagHistoryPanelProps = {
    monitorType: SohTypes.SohMonitorType.MISSING,
    valueType: ValueType.PERCENTAGE,
    // tslint:disable-next-line: no-magic-numbers
    sohHistoricalDurations: [30000, 90000],
    station: {
      id: '1',
      uuid: '1',
      needsAcknowledgement: true,
      needsAttention: true,
      sohStatusSummary: undefined,
      stationGroups: [],
      statusContributors: [],
      time: undefined,
      stationName: '1',
      allStationAggregates: [],
      channelSohs: [
        {
          channelName: 'adsf',
          channelSohStatus: undefined,
          allSohMonitorValueAndStatuses: [
            {
              monitorType: SohMonitorType.LAG,
              value: 10,
              valuePresent: true,
              status: SohTypes.SohStatusSummary.GOOD,
              hasUnacknowledgedChanges: true,
              contributing: false,
              quietUntilMs: 1,
              thresholdBad: 3,
              thresholdMarginal: 3
            },
            {
              monitorType: SohMonitorType.LAG,
              value: 11,
              valuePresent: true,
              status: SohTypes.SohStatusSummary.GOOD,
              hasUnacknowledgedChanges: true,
              contributing: false,
              quietUntilMs: 1,
              thresholdBad: 3,
              thresholdMarginal: 3
            }
          ]
        },
        {
          channelName: 'adsf2',
          channelSohStatus: undefined,
          allSohMonitorValueAndStatuses: [
            {
              monitorType: SohMonitorType.LAG,
              value: 10,
              valuePresent: true,
              status: SohTypes.SohStatusSummary.GOOD,
              hasUnacknowledgedChanges: true,
              contributing: false,
              quietUntilMs: 1,
              thresholdBad: 3,
              thresholdMarginal: 3
            },
            {
              monitorType: SohMonitorType.LAG,
              value: 11,
              valuePresent: true,
              status: SohTypes.SohStatusSummary.GOOD,
              hasUnacknowledgedChanges: true,
              contributing: false,
              quietUntilMs: 1,
              thresholdBad: 3,
              thresholdMarginal: 3
            }
          ]
        }
      ]
    },
    sohStatus: {
      lastUpdated: 0,
      loading: false,
      error: undefined,
      stationAndStationGroupSoh: {
        isUpdateResponse: false,
        stationGroups: [],
        stationSoh: [
          {
            id: '1',
            uuid: '1',
            needsAcknowledgement: true,
            needsAttention: true,
            sohStatusSummary: undefined,
            stationGroups: [],
            statusContributors: [],
            time: undefined,
            stationName: '1',
            allStationAggregates: [],
            channelSohs: [
              {
                channelName: 'adsf',
                channelSohStatus: undefined,
                allSohMonitorValueAndStatuses: [
                  {
                    monitorType: SohMonitorType.LAG,
                    value: 10,
                    valuePresent: true,
                    status: SohTypes.SohStatusSummary.GOOD,
                    hasUnacknowledgedChanges: true,
                    contributing: false,
                    quietUntilMs: 1,
                    thresholdBad: 3,
                    thresholdMarginal: 3
                  },
                  {
                    monitorType: SohMonitorType.LAG,
                    value: 11,
                    valuePresent: true,
                    status: SohTypes.SohStatusSummary.GOOD,
                    hasUnacknowledgedChanges: true,
                    contributing: false,
                    quietUntilMs: 1,
                    thresholdBad: 3,
                    thresholdMarginal: 3
                  }
                ]
              }
            ]
          }
        ]
      }
    }
  };
  // mock the functions that create data that is passed to the charts
  jest
    .spyOn(validateNonIdealStateDependency, 'validateNonIdealState')
    .mockImplementation(() => undefined);
  const barLineChartData: BarLineChartData = {
    categories: { x: ['x1', 'x2'], y: [] },
    lineDefs: [
      {
        color: 'red',
        id: 1,
        value: [
          { x: 1, y: 1 },
          { x: 2, y: 2 }
        ]
      }
    ],
    barDefs: [
      { color: 'red', id: 3, value: { x: 1, y: 1 } },
      { color: 'red', id: 4, value: { x: 2, y: 2 } }
    ],
    thresholdsMarginal: [1, 2],
    thresholdsBad: [1, 2]
  };
  jest.spyOn(Util, 'getChartData').mockImplementation(() => barLineChartData);
  it('should be defined', () => {
    expect(MissingLagHistoryPanel).toBeDefined();
  });
  it('should match snapshot', async () => {
    const missingLagHistoryPanel = Enzyme.shallow(
      <ApolloProvider client={client}>
        <BaseDisplayContext.Provider
          value={{
            glContainer: { width: 100, height: 100 } as any,
            widthPx: 100,
            heightPx: 100
          }}
        >
          <MissingLagHistoryPanel {...missingLagHistoryPanelProps} />
        </BaseDisplayContext.Provider>
      </ApolloProvider>
    );
    // we gotta wait for the use state
    await waitForComponentToPaint(missingLagHistoryPanel);
    missingLagHistoryPanel.update();
    expect(missingLagHistoryPanel.dive()).toMatchSnapshot();
  });
});
