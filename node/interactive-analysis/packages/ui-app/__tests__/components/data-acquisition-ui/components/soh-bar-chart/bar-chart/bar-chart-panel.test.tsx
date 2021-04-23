import { CommonTypes, SohTypes } from '@gms/common-graphql';
import { SohMonitorType } from '@gms/common-graphql/lib/graphql/soh/types';
import { uuid } from '@gms/common-util';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import { BaseDisplayContext } from '@gms/ui-app/src/ts/components/common-ui/components/base-display/base-display-context';
import { BarChartPanel } from '@gms/ui-app/src/ts/components/data-acquisition-ui/components/soh-bar-chart/bar-chart/bar-chart-panel';
import { BarChartPanelProps } from '@gms/ui-app/src/ts/components/data-acquisition-ui/components/soh-bar-chart/bar-chart/types';
import React from 'react';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

let idCount = 0;
uuid.asString = jest.fn().mockImplementation(() => ++idCount);

// tslint:disable-next-line: deprecation
const lodash = require.requireActual('lodash');
lodash.uniqueId = () => '1';

const barChartPanelProps: BarChartPanelProps = {
  minHeightPx: 100,
  chartHeaderHeight: 100,
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
  channelSoh: [
    {
      hasUnacknowledgedChanges: false,
      isNullData: false,
      name: 'TestSt.adsf',
      quietDurationMs: undefined,
      quietExpiresAt: 1,
      status: SohTypes.SohStatusSummary.GOOD,
      thresholdBad: 3,
      thresholdMarginal: 3,
      value: 8
    },
    {
      hasUnacknowledgedChanges: true,
      isNullData: false,
      name: 'TestSt.adsf2',
      quietDurationMs: undefined,
      quietExpiresAt: 1,
      status: SohTypes.SohStatusSummary.GOOD,
      thresholdBad: 3,
      thresholdMarginal: 3,
      value: 10
    }
  ],
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
describe('Bar Chart Panel', () => {
  // tslint:disable-next-line: no-magic-numbers
  Date.now = jest.fn().mockReturnValue(1573244087715);
  const setState = jest.fn();
  const useStateSpy = jest.spyOn(React, 'useState');
  const mock: any = init => [init, setState];
  useStateSpy.mockImplementation(mock);

  const barChartPanel = Enzyme.mount(
    <BaseDisplayContext.Provider
      value={{
        glContainer: { width: 150, height: 150 } as any,
        widthPx: 150,
        heightPx: 150
      }}
    >
      <BarChartPanel {...barChartPanelProps} />
    </BaseDisplayContext.Provider>
  );

  it('should be defined', () => {
    expect(barChartPanel).toBeDefined();
  });

  it('should match snapshot', () => {
    expect(barChartPanel).toMatchSnapshot();
  });
});
