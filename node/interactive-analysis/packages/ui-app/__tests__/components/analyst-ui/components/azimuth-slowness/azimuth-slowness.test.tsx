import { CommonTypes } from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import React from 'react';
import renderer from 'react-test-renderer';
import { AzimuthSlowness } from '../../../../../src/ts/components/analyst-ui/components/azimuth-slowness';
import { AzimuthSlownessProps } from '../../../../../src/ts/components/analyst-ui/components/azimuth-slowness/types';
import { signalDetectionsData } from '../../../../__data__/signal-detections-data';
import {
  eventId,
  signalDetectionsIds,
  stageInterval,
  timeInterval
} from '../../../../__data__/test-util';

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
  setSdIdsToShowFk: (ids: string[]) => {
    /* no-op */
  },
  computeFks: undefined,
  setWindowLead: undefined,
  markFksReviewed: undefined
};

it('AzimuthSlowness renders & matches snapshot', () => {
  const tree = renderer
    .create(
      <div
        style={{
          border: `1px solid #111`,
          resize: 'both',
          overflow: 'auto',
          height: '700px',
          width: '1000px'
        }}
      >
        <AzimuthSlowness {...(azSlowReduxProps as any)} />
      </div>
    )
    .toJSON();

  expect(tree).toMatchSnapshot();
});
