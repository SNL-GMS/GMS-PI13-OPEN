import { CommonTypes, EventTypes, SohTypes, WaveformTypes } from '@gms/common-graphql';
import { AnalystWorkspaceTypes, AppState } from '@gms/ui-state';
import { KeyAction } from '@gms/ui-state/lib/state/analyst-workspace/types';
import { NetworkStatus } from 'apollo-client';
import Immutable from 'immutable';
import map from 'lodash/map';
import uniq from 'lodash/uniq';
import uniqBy from 'lodash/uniqBy';
import { QueryControls } from 'react-apollo';
import { AnalystCurrentFk } from '../../src/ts/components/analyst-ui/components/azimuth-slowness/components/fk-rendering/fk-rendering';
import {
  FilterType,
  FkThumbnailSize
  // tslint:disable-next-line: max-line-length
} from '../../src/ts/components/analyst-ui/components/azimuth-slowness/components/fk-thumbnail-list/fk-thumbnails-controls';
import {
  AzimuthSlownessReduxProps,
  AzimuthSlownessState,
  FkUnits
} from '../../src/ts/components/analyst-ui/components/azimuth-slowness/types';
import { eventData } from './event-data';
import { signalDetectionsData } from './signal-detections-data';

export const fkThumbnailSize = FkThumbnailSize.MEDIUM;
export const fkFilterType = FilterType.firstP;

// 11:59:59 05/19/2010
export const startTimeSeconds = 1274313599;

// 02:00:01 05/20/2010
export const endTimeSeconds = 1274320801;

// time block 2 hours = 7200 seconds
export const timeBlock = 7200;

export const timeInterval: CommonTypes.TimeRange = {
  startTime: startTimeSeconds,
  endTime: endTimeSeconds
};

export const stageInterval: AnalystWorkspaceTypes.StageInterval = {
  id: '1',
  name: '2',
  interval: {
    id: '3',
    timeInterval,
    activityInterval: {
      id: '1',
      name: '2',
      analystActivity: AnalystWorkspaceTypes.AnalystActivity.eventRefinement
    }
  }
};

export const currentProcStageIntId = '3';

export const networkStatus = NetworkStatus.ready;

export const analystCurrentFk: AnalystCurrentFk = {
  x: 10,
  y: 11
};

export const eventIds = uniq(map([eventData], 'id'));

export const eventId = uniqBy([eventData], 'eventHypothesisId')[0].id;

export const singleEvent = uniqBy([eventData], 'eventHypothesisId')[0];

const sdIdsFullMap: string[] = signalDetectionsData.map(sd => sd.id);

export const signalDetectionsIds = uniq(sdIdsFullMap);

export const selectedSignalDetectionID = signalDetectionsIds[0];
export const testMagTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes = Immutable.Map<
  any,
  boolean
>();
testMagTypes.set(EventTypes.MagnitudeType.MB, true);
testMagTypes.set(EventTypes.MagnitudeType.MBMLE, true);
testMagTypes.set(EventTypes.MagnitudeType.MS, true);
testMagTypes.set(EventTypes.MagnitudeType.MSMLE, true);

export const analystAppState: AppState = {
  analystWorkspaceState: {
    currentStageInterval: stageInterval,
    defaultSignalDetectionPhase: CommonTypes.PhaseType.P,
    selectedEventIds: eventIds,
    openEventId: eventId,
    selectedSdIds: signalDetectionsIds,
    sdIdsToShowFk: [],
    selectedSortType: AnalystWorkspaceTypes.WaveformSortType.stationName,
    channelFilters: Immutable.Map<string, WaveformTypes.WaveformFilter>(),
    measurementMode: {
      mode: AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT,
      entries: Immutable.Map<string, boolean>()
    },
    location: {
      selectedLocationSolutionSetId: undefined,
      selectedLocationSolutionId: undefined,
      selectedPreferredLocationSolutionSetId: undefined,
      selectedPreferredLocationSolutionId: undefined
    },
    historyActionInProgress: 1,
    keyPressActionQueue: Immutable.Map<KeyAction, number>(),
    openLayoutName: ''
  },
  dataAcquisitionWorkspaceState: {
    selectedAceiType: SohTypes.AceiType.BEGINNING_TIME_OUTAGE,
    selectedStationIds: undefined,
    selectedProcessingStation: undefined,
    unmodifiedProcessingStation: undefined,
    data: {
      sohStatus: {
        lastUpdated: 0,
        loading: false,
        error: undefined,
        stationAndStationGroupSoh: {
          stationGroups: [],
          stationSoh: [],
          isUpdateResponse: false
        }
      }
    }
  },
  userSessionState: {
    authorizationStatus: {
      userName: undefined,
      authenticated: false,
      authenticationCheckComplete: false,
      failedToConnect: false
    },
    connected: true
  },
  systemMessageState: {
    lastUpdated: 0,
    latestSystemMessages: [],
    systemMessages: []
  }
};

export const azSlowProps: AzimuthSlownessReduxProps = {
  location: undefined,
  currentStageInterval: stageInterval,
  currentTimeInterval: timeInterval,
  selectedSdIds: signalDetectionsIds,
  openEventId: eventId,
  sdIdsToShowFk: [],
  analystActivity: AnalystWorkspaceTypes.AnalystActivity.eventRefinement,
  client: undefined,
  setSelectedSdIds: () => {
    // tslint:disable-next-line:no-console
    console.log('azSlowProps - setSelectedSdIds');
  },
  setSdIdsToShowFk: () => {
    // tslint:disable-next-line:no-console
    console.log('azSlowProps - setSdIdsToShowFk');
  },
  channelFilters: undefined,
  selectedSortType: undefined,
  setChannelFilters: jest.fn(),
  setMeasurementModeEntries: jest.fn(),
  defaultSignalDetectionPhase: undefined
};

export const AzSlowState: AzimuthSlownessState = {
  fkThumbnailSizePx: fkThumbnailSize,
  filterType: FilterType.all,
  userInputFkFrequency: {
    minFrequencyHz: 1,
    maxFrequencyHz: 4
  },
  fkUnitsForEachSdId: Immutable.Map<string, FkUnits>(),
  userInputFkWindowParameters: {
    lengthSeconds: 4,
    leadSeconds: 1,
    stepSize: 1
  },
  fkInnerContainerWidthPx: 200,
  numberOfOutstandingComputeFkMutations: 1,
  fkThumbnailColumnSizePx: 255,
  fkFrequencyThumbnails: undefined
};

export const nonDefiningQuery: QueryControls<{}> = {
  loading: true,
  networkStatus: undefined,
  variables: undefined,
  fetchMore: undefined,
  refetch: undefined,
  startPolling: undefined,
  stopPolling: undefined,
  subscribeToMore: undefined,
  updateQuery: undefined
};
