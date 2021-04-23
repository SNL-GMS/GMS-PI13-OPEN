import { CommonTypes } from '@gms/common-graphql';
import { AnalystConfiguration } from '@gms/common-graphql/lib/graphql/ui-configuration/types';
import { SohMonitorType } from '@gms/common-graphql/src/ts/graphql/soh/types';
import { uuid } from '@gms/common-util';
import { Container } from '@gms/golden-layout';
import { Client } from '@gms/ui-apollo';
import DefaultClient from 'apollo-boost';
import React from 'react';
import { ApolloProvider, QueryControls } from 'react-apollo';
import { SohTypes } from '../../../../../../common-graphql/src/ts/graphql/soh';
import { SohBarChart } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-bar-chart/soh-bar-chart-component';
import { SohBarChartProps } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-bar-chart/types';
// tslint:disable-next-line: max-line-length
// import { DataAcquisitionNonIdealStateDefs } from '../../../../../src/ts/components/data-acquisition-ui/shared/non-ideal-states';
import { nonDefiningQuery } from '../../../../__data__/test-util';

// mock the uuid
uuid.asString = jest.fn().mockImplementation(() => '12345789');

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

const client: Client = new DefaultClient<any>();

uuid.asString = jest.fn().mockReturnValue('1e872474-b19f-4325-9350-e217a6feddc0');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('SohBarChart class', () => {
  // tslint:disable-next-line: no-magic-numbers
  global.Date.now = jest.fn(() => 1530518207007);

  const glWidth = 1000;
  const glHeight = 500;

  const channel: SohTypes.ChannelSoh = {
    allSohMonitorValueAndStatuses: [
      {
        status: SohTypes.SohStatusSummary.GOOD,
        value: 1,
        valuePresent: true,
        monitorType: SohTypes.SohMonitorType.LAG,
        hasUnacknowledgedChanges: false,
        contributing: false,
        thresholdMarginal: 1,
        thresholdBad: 10,
        quietUntilMs: 1
      }
    ],
    channelName: 'AAA111',
    channelSohStatus: SohTypes.SohStatusSummary.GOOD
  };

  const selectedStationIds = ['A'];

  const setSelectedStationIds = jest.fn();

  const myGLContainer: Container = {
    // Container
    width: glWidth,
    height: glHeight,
    parent: undefined,
    tab: undefined,
    title: 'container-title',
    layoutManager: undefined,
    isHidden: false,
    setState: jest.fn(),
    extendState: jest.fn(),
    getState: jest.fn(),
    getElement: jest.fn(),
    hide: jest.fn(),
    show: jest.fn(),
    setSize: jest.fn(),
    setTitle: jest.fn(),
    close: jest.fn(),
    // EventEmitter
    on: jest.fn(),
    emit: jest.fn(),
    trigger: jest.fn(),
    unbind: jest.fn(),
    off: jest.fn()
  };
  const myEnvReduxProps: any = {
    glContainer: myGLContainer,
    selectedStationIds,
    setSelectedStationIds
  };

  const sohStationAndGroupStatusQuery: any = {
    loading: false,
    stationAndStationGroupSoh: {
      stationSoh: [
        {
          stationName: 'A'
        }
      ]
    }
  };

  const channelSohForStationQuery: any = {
    channelSohForStation: {
      channelSohs: [channel],
      stationName: 'A'
    }
  };

  const uiConfigurationQuery: QueryControls<{}> & {
    uiAnalystConfiguration: AnalystConfiguration;
  } = {
    ...nonDefiningQuery,
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
    }
  };

  const sohStatus: any = {
    loading: false,
    stationAndStationGroupSoh: {
      stationSoh: [
        {
          channelSohs: [channel],
          stationName: 'A'
        }
      ]
    }
  };

  const myProps: SohBarChartProps = {
    sohStatus,
    ...myEnvReduxProps,
    channelSohForStationQuery,
    sohStationAndGroupStatusQuery,
    uiConfigurationQuery,
    saveStationGroupSohStatus: jest.fn(),
    quietChannelMonitorStatuses: jest.fn(async () => new Promise(jest.fn())),
    mutate: undefined,
    result: undefined
  };

  const sohBarChart = Enzyme.mount(
    <ApolloProvider client={client}>
      <SohBarChart {...{ ...myProps, type: 'LAG' as any }} />
    </ApolloProvider>
  );

  it('should be defined', () => {
    expect(SohBarChart).toBeDefined();
  });

  it('should get station info', () => {
    sohBarChart.update();
    const stationLagInfo = sohBarChart
      .find(SohBarChart)
      .instance()
      .getStation();
    expect(stationLagInfo).toBeDefined();
  });

  it('should acknowledge channel monitor status', () => {
    const stationName = 'AAA';
    const sohMonType: SohMonitorType = SohMonitorType.LAG;
    const chanMonPair: SohTypes.ChannelMonitorPair = {
      channelName: 'AAA111',
      monitorType: sohMonType
    };
    const channelPairs: SohTypes.ChannelMonitorPair[] = [chanMonPair];
    sohBarChart
      .find(SohBarChart)
      .instance()
      .quietChannelMonitorStatuses(stationName, channelPairs);
    expect(myProps.quietChannelMonitorStatuses).toHaveBeenCalledWith({
      variables: {
        channelMonitorsToQuiet: {
          channelMonitorPairs: [
            {
              channelName: 'AAA111',
              monitorType: 'LAG'
            }
          ],
          stationName: 'AAA'
        }
      }
    });
  });

  it('should match snapshot', () => {
    expect(sohBarChart).toMatchSnapshot();
  });
});
