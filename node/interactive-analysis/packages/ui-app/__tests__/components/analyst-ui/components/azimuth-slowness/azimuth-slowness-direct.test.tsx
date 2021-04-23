import { CommonTypes } from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import React from 'react';
import { AzimuthSlowness } from '../../../../../src/ts/components/analyst-ui/components/azimuth-slowness/azimuth-slowness-component';
import { AzimuthSlownessProps } from '../../../../../src/ts/components/analyst-ui/components/azimuth-slowness/types';
import { signalDetectionsData } from '../../../../__data__/signal-detections-data';
import {
  eventId,
  signalDetectionsIds,
  stageInterval,
  timeInterval
} from '../../../../__data__/test-util';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

const azSlowReduxProps: Partial<AzimuthSlownessProps> = {
  location: undefined,
  defaultStationsQuery: {
    defaultProcessingStations: [],
    error: undefined,
    loading: false,
    networkStatus: undefined,
    fetchMore: undefined,
    refetch: undefined,
    startPolling: undefined,
    stopPolling: undefined,
    subscribeToMore: () => () => {
      /**/
    },
    updateQuery: undefined,
    variables: undefined
  },
  uiConfigurationQuery: {
    uiAnalystConfiguration: {
      logLevel: CommonTypes.LogLevel.info,
      defaultNetwork: 'demo',
      acknowledgementQuietDuration: 300000,
      // tslint:disable-next-line: no-magic-numbers
      availableQuietDurations: [300000, 900000],
      redisplayPeriod: 5000,
      reprocessingPeriod: 20,
      sohStationStaleTimeMS: 30000,
      // tslint:disable-next-line: no-magic-numbers
      sohHistoricalDurations: [300000, 900000],
      sohStationGroupNames: [
        {
          name: 'GroupA',
          priority: 1
        }
      ],
      defaultFilters: [],
      systemMessageLimit: 1000
    },
    error: undefined,
    loading: false,
    networkStatus: undefined,
    fetchMore: undefined,
    refetch: undefined,
    startPolling: undefined,
    stopPolling: undefined,
    subscribeToMore: () => () => {
      /**/
    },
    updateQuery: undefined,
    variables: undefined
  },
  signalDetectionsByStationQuery: {
    signalDetectionsByStation: signalDetectionsData,
    error: undefined,
    loading: false,
    networkStatus: undefined,
    fetchMore: undefined,
    refetch: undefined,
    startPolling: undefined,
    stopPolling: undefined,
    subscribeToMore: () => () => {
      /**/
    },
    updateQuery: undefined,
    variables: undefined
  },
  eventsInTimeRangeQuery: {
    eventsInTimeRange: [],
    error: undefined,
    loading: false,
    networkStatus: undefined,
    fetchMore: undefined,
    refetch: undefined,
    startPolling: undefined,
    stopPolling: undefined,
    subscribeToMore: () => () => {
      /**/
    },
    updateQuery: undefined,
    variables: undefined
  },
  currentStageInterval: stageInterval,
  currentTimeInterval: timeInterval,
  selectedSdIds: signalDetectionsIds,
  openEventId: eventId,
  sdIdsToShowFk: [],
  analystActivity: AnalystWorkspaceTypes.AnalystActivity.eventRefinement,
  client: undefined,
  computeFkFrequencyThumbnails: undefined,
  setSelectedSdIds: (ids: string[]) => {
    /* no-op */
  },
  computeFks: undefined,
  setWindowLead: undefined,
  setSdIdsToShowFk: (ids: string[]) => {
    /* no-op */
  },
  markFksReviewed: undefined
};

describe('AzimuthSlowness Direct', () => {
  test('AzimuthSlowness renders directly with data correctly', () => {
    const wrapper = Enzyme.shallow(<AzimuthSlowness {...(azSlowReduxProps as any)} />);
    expect(wrapper).toMatchSnapshot();
  });
});
