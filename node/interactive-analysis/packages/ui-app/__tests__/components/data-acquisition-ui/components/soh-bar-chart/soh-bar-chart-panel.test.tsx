import { CommonTypes, SohTypes } from '@gms/common-graphql';
import { SohMonitorType } from '@gms/common-graphql/lib/graphql/soh/types';
import { uuid } from '@gms/common-util';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import { Client } from '@gms/ui-apollo';
import DefaultClient from 'apollo-boost';
import { uniqueId } from 'lodash';
import React from 'react';
import { ApolloProvider } from 'react-apollo';
import { BaseDisplayContext } from '../../../../../src/ts/components/common-ui/components/base-display';
import { SohBarChartPanel } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-bar-chart/soh-bar-chart-panel';
import { SohBarChartPanelProps } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-bar-chart/types';
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

describe('SohBarChartPanel class', () => {
  const channelStatusesToDisplay: Map<FilterableSOHTypes, boolean> = new Map<
    FilterableSOHTypes,
    boolean
  >();
  const columnHeaderData = FilterableSOHTypes.GOOD;
  channelStatusesToDisplay.set(columnHeaderData, true);
  const monitorStatusesToDisplay: Map<any, boolean> = new Map();
  monitorStatusesToDisplay.set(SohTypes.SohStatusSummary.GOOD, true);
  const myProps: SohBarChartPanelProps = {
    minHeightPx: 100,
    type: SohTypes.SohMonitorType.LAG,
    valueType: ValueType.INTEGER,
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
    },
    uiAnalystConfiguration: {
      logLevel: CommonTypes.LogLevel.info,
      acknowledgementQuietDuration: 0,
      defaultFilters: [],
      defaultNetwork: undefined,
      sohStationStaleTimeMS: 30000,
      sohStationGroupNames: [],
      redisplayPeriod: 5,
      reprocessingPeriod: 20,
      availableQuietDurations: [],
      // tslint:disable-next-line: no-magic-numbers
      sohHistoricalDurations: [300000, 900000],
      systemMessageLimit: 1000
    },
    quietChannelMonitorStatuses: jest.fn()
  };

  const contextDefaults: SohContextData = {
    glContainer: {} as any,
    selectedAceiType: SohTypes.AceiType.BEGINNING_TIME_OUTAGE,
    quietChannelMonitorStatuses: jest.fn(),
    setSelectedAceiType: jest.fn()
  };
  const sohBarChartPanel = Enzyme.mount(
    <ApolloProvider client={client}>
      <BaseDisplayContext.Provider
        value={{
          glContainer: { width: 150, height: 150 } as any,
          widthPx: 150,
          heightPx: 150
        }}
      >
        <SohContext.Provider value={contextDefaults}>
          <SohBarChartPanel {...myProps} />
        </SohContext.Provider>
      </BaseDisplayContext.Provider>
    </ApolloProvider>
  );
  it('should be defined', () => {
    expect(sohBarChartPanel).toBeDefined();
  });
  it('should match snapshot', () => {
    expect(sohBarChartPanel).toMatchSnapshot();
  });
});
