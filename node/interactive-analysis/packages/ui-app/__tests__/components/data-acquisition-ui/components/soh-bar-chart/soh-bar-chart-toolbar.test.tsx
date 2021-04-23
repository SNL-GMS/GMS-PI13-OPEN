import { CommonTypes, SohTypes } from '@gms/common-graphql';
import { AnalystConfiguration } from '@gms/common-graphql/lib/graphql/ui-configuration/types';
import { uuid } from '@gms/common-util';
import { DropdownItem } from '@gms/ui-core-components/lib/components/ui-widgets/toolbar/types';
import { uniqueId } from 'lodash';
import React from 'react';
import { BaseDisplayContext } from '../../../../../src/ts/components/common-ui/components/base-display';
import {
  Toolbar,
  ToolbarProps
} from '../../../../../src/ts/components/data-acquisition-ui/components/soh-bar-chart/soh-bar-chart-toolbar';
import { initialFiltersToDisplay } from '../../../../../src/ts/components/data-acquisition-ui/shared/toolbars/soh-toolbar';
import {
  makeLagSortingDropdown,
  SOHLagOptions
} from '../../../../../src/ts/components/data-acquisition-ui/shared/toolbars/soh-toolbar-items';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
uuid.asString = jest.fn().mockImplementation(uniqueId);

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('Toolbar class', () => {
  const sortDropdown: DropdownItem = makeLagSortingDropdown(SOHLagOptions.LAG_HIGHEST, jest.fn());
  // see https://stackoverflow.com/questions/57805917/mocking-refs-in-react-function-component
  const mockUseRef = (obj: any) => () =>
    Object.defineProperty({}, 'current', {
      get: () => obj,
      // tslint:disable-next-line: no-empty
      set: () => {}
    });
  const ref: any = mockUseRef({ refFunction: jest.fn() });
  const station: SohTypes.UiStationSoh = {
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
            monitorType: SohTypes.SohMonitorType.LAG,
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
            monitorType: SohTypes.SohMonitorType.LAG,
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
            monitorType: SohTypes.SohMonitorType.LAG,
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
            monitorType: SohTypes.SohMonitorType.LAG,
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
  };
  const uiAnalystConfiguration: AnalystConfiguration = {
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
  };
  const toolbarProps: ToolbarProps = {
    statusesToDisplay: initialFiltersToDisplay,
    setStatusesToDisplay: jest.fn(),
    sortDropdown,
    forwardRef: ref,
    isStale: false,
    station,
    monitorType: SohTypes.SohMonitorType.LAG,
    uiAnalystConfiguration
  };
  const toolbar = Enzyme.mount(
    <BaseDisplayContext.Provider
      value={{
        glContainer: { width: 150, height: 150 } as any,
        widthPx: 150,
        heightPx: 150
      }}
    >
      <Toolbar {...toolbarProps} />
    </BaseDisplayContext.Provider>
  );
  it('should be defined', () => {
    expect(Toolbar).toBeDefined();
  });
  it('should match snapshot', () => {
    expect(toolbar).toMatchSnapshot();
  });
});
