import { CommonTypes, WaveformTypes } from '@gms/common-graphql';
import Immutable from 'immutable';
import { AnalystWorkspaceTypes } from './analyst-workspace';
import { AnalystWorkspaceState, WaveformDisplayMode } from './analyst-workspace/types';
import { DataAcquisitionWorkspaceState } from './data-acquisition-workspace/types';
import { SystemMessageState } from './system-message/types';
import { AppState } from './types';
import { UserSessionState } from './user-session/types';

export const initialLocationState: AnalystWorkspaceTypes.LocationSolutionState = {
  selectedLocationSolutionSetId: null,
  selectedLocationSolutionId: null,
  selectedPreferredLocationSolutionSetId: null,
  selectedPreferredLocationSolutionId: null
};

export const initialAnalystWorkspaceState: AnalystWorkspaceState = {
  currentStageInterval: null,
  defaultSignalDetectionPhase: CommonTypes.PhaseType.P,
  selectedEventIds: [],
  openEventId: null,
  selectedSdIds: [],
  sdIdsToShowFk: [],
  selectedSortType: AnalystWorkspaceTypes.WaveformSortType.stationName,
  channelFilters: Immutable.Map<string, WaveformTypes.WaveformFilter>(),
  measurementMode: {
    mode: WaveformDisplayMode.DEFAULT,
    entries: Immutable.Map<string, boolean>()
  },
  location: initialLocationState,
  openLayoutName: null,
  keyPressActionQueue: Immutable.Map<AnalystWorkspaceTypes.KeyAction, number>(),
  historyActionInProgress: 0
};

export const initialDataAcquisitionWorkspaceState: DataAcquisitionWorkspaceState = {
  selectedStationIds: [],
  selectedAceiType: null,
  selectedProcessingStation: null,
  unmodifiedProcessingStation: null,
  data: {
    sohStatus: {
      lastUpdated: undefined,
      loading: true,
      error: undefined,
      stationAndStationGroupSoh: {
        stationSoh: [],
        stationGroups: [],
        isUpdateResponse: false
      }
    }
  }
};

export const initialUserSessionState: UserSessionState = {
  authorizationStatus: {
    userName: null,
    authenticated: false,
    authenticationCheckComplete: false,
    failedToConnect: false
  },
  connected: true
};

/** The initial system message state */
export const initialSystemMessageState: SystemMessageState = {
  lastUpdated: null,
  latestSystemMessages: null,
  systemMessages: null
};

/** The initial application state */
export const initialAppState: AppState = {
  analystWorkspaceState: initialAnalystWorkspaceState,
  dataAcquisitionWorkspaceState: initialDataAcquisitionWorkspaceState,
  userSessionState: initialUserSessionState,
  systemMessageState: initialSystemMessageState
};
