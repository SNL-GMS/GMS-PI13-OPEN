// tslint:disable: max-line-length
import { CommonTypes, WaveformTypes } from '@gms/common-graphql';
import Immutable from 'immutable';
import {
  ActionCreator,
  actionCreator,
  ActionCreatorVoid,
  actionCreatorVoid
} from '../util/action-helper';
import {
  ActionTypes,
  KeyAction,
  StageInterval,
  WaveformDisplayMode,
  WaveformSortType
} from './types';

const setMode: ActionCreator<WaveformDisplayMode> = actionCreator(ActionTypes.SET_MODE);
const setCurrentStageInterval: ActionCreator<StageInterval> = actionCreator(
  ActionTypes.SET_CURRENT_STAGE_INTERVAL
);
const setDefaultSignalDetectionPhase: ActionCreator<CommonTypes.PhaseType> = actionCreator(
  ActionTypes.SET_DEFAULT_SIGNAL_DETECTION_PHASE
);
const setOpenEventId: ActionCreator<string> = actionCreator(ActionTypes.SET_OPEN_EVENT_ID);
const setSelectedEventIds: ActionCreator<string[]> = actionCreator(
  ActionTypes.SET_SELECTED_EVENT_IDS
);
const setSelectedSdIds: ActionCreator<string[]> = actionCreator(ActionTypes.SET_SELECTED_SD_IDS);
const setSdIdsToShowFk: ActionCreator<string[]> = actionCreator(ActionTypes.SET_SD_IDS_TO_SHOW_FK);
const setSelectedSortType: ActionCreator<WaveformSortType> = actionCreator(
  ActionTypes.SET_SELECTED_SORT_TYPE
);
const setChannelFilters: ActionCreator<Immutable.Map<
  string,
  WaveformTypes.WaveformFilter
>> = actionCreator(ActionTypes.SET_CHANNEL_FILTERS);
const setMeasurementModeEntries: ActionCreator<Immutable.Map<string, boolean>> = actionCreator(
  ActionTypes.SET_MEASUREMENT_MODE_ENTRIES
);
const setSelectedLocationSolutionSetId: ActionCreator<string> = actionCreator(
  ActionTypes.SET_SELECTED_LOCATION_SOLUTION_SET_ID
);
const setSelectedLocationSolutionId: ActionCreator<string> = actionCreator(
  ActionTypes.SET_SELECTED_LOCATION_SOLUTION_ID
);
const setSelectedPreferredLocationSolutionSetId: ActionCreator<string> = actionCreator(
  ActionTypes.SET_SELECTED_PREFERRED_LOCATION_SOLUTION_SET_ID
);
const setSelectedPreferredLocationSolutionId: ActionCreator<string> = actionCreator(
  ActionTypes.SET_SELECTED_PREFERRED_LOCATION_SOLUTION_ID
);
const setOpenLayoutName: ActionCreator<string> = actionCreator(ActionTypes.SET_OPEN_LAYOUT_NAME);
const setKeyPressActionQueue: ActionCreator<Immutable.Map<KeyAction, number>> = actionCreator(
  ActionTypes.SET_KEYPRESS_ACTION_QUEUE
);
const incrementHistoryActionInProgress: ActionCreatorVoid = actionCreatorVoid(
  ActionTypes.INCREMENT_HISTORY_ACTION_IN_PROGRESS
);
const decrementHistoryActionInProgress: ActionCreatorVoid = actionCreatorVoid(
  ActionTypes.DECREMENT_HISTORY_ACTION_IN_PROGRESS
);

/**
 * Redux internal actions: should only be called by `operations`. (private - but not strictly forced)
 */
export const Internal = {
  setMode,
  setCurrentStageInterval,
  setOpenEventId,
  setMeasurementModeEntries,
  setSelectedLocationSolutionSetId,
  setSelectedLocationSolutionId,
  setSelectedPreferredLocationSolutionSetId,
  setSelectedPreferredLocationSolutionId
};

/**
 * Redux actions (public).
 */
export const Actions = {
  setDefaultSignalDetectionPhase,
  setSelectedSortType,
  setSelectedSdIds,
  setSelectedEventIds,
  setSdIdsToShowFk,
  setChannelFilters,
  setOpenLayoutName,
  setKeyPressActionQueue,
  incrementHistoryActionInProgress,
  decrementHistoryActionInProgress
};
