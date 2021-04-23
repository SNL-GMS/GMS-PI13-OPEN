import { CommonTypes, WaveformTypes } from '@gms/common-graphql';
import Immutable from 'immutable';
import uniq from 'lodash/uniq';
import * as Redux from 'redux';
import { Actions, Internal } from './actions';
import * as Types from './types';

/**
 * Redux reducer for setting the mode.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setMode = (
  state: Types.WaveformDisplayMode = Types.WaveformDisplayMode.DEFAULT,
  action: Types.SET_MODE
): Types.WaveformDisplayMode => {
  if (Internal.setMode.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the current stage interval.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setCurrentStageInterval = (
  state: Types.StageInterval = null,
  action: Types.SET_CURRENT_STAGE_INTERVAL
): Types.StageInterval => {
  if (Internal.setCurrentStageInterval.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the signal detection default phase.
 * The selected phase type that will be used for the creation of
 * a new a signal detection.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setDefaultSignalDetectionPhase = (
  state: CommonTypes.PhaseType = CommonTypes.PhaseType.P,
  action: Types.SET_DEFAULT_SIGNAL_DETECTION_PHASE
): CommonTypes.PhaseType => {
  if (Actions.setDefaultSignalDetectionPhase.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the selected event ids.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setSelectedEventIds = (
  state: string[] = [],
  action: Types.SET_SELECTED_EVENT_IDS
): string[] => {
  if (Actions.setSelectedEventIds.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the current open event id.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setOpenEventId = (state: string = null, action: Types.SET_OPEN_EVENT_ID): string => {
  if (Internal.setOpenEventId.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the selected signal detection ids.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setSelectedSdIds = (state: string[] = [], action: Types.SET_SELECTED_SD_IDS): string[] => {
  if (Actions.setSelectedSdIds.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the signal detection ids that
 * have been marked to show FK.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setSdIdsToShowFk = (state: string[] = [], action: Types.SET_SD_IDS_TO_SHOW_FK): string[] => {
  if (Actions.setSdIdsToShowFk.test(action)) {
    return action.payload ? uniq(action.payload) : null;
  }
  return state;
};

/**
 * Redux reducer for setting the selected sort type.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setSelectedSortType = (
  state: Types.WaveformSortType = Types.WaveformSortType.stationName,
  action: Types.SET_SELECTED_SORT_TYPE
): Types.WaveformSortType => {
  if (Actions.setSelectedSortType.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the channel filters.
 * (selected waveform filter for a give channel id)
 *
 * @param state the state to set
 * @param action the redux action
 */
const setChannelFilters = (
  state: Immutable.Map<string, WaveformTypes.WaveformFilter> = Immutable.Map<
    string,
    WaveformTypes.WaveformFilter
  >(),
  action: Types.SET_CHANNEL_FILTERS
): Immutable.Map<string, WaveformTypes.WaveformFilter> => {
  if (Actions.setChannelFilters.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the measurement mode entries.
 * Map of signal detection ids to boolean value indicating if the
 * amplitude measurement should be displayed (visible).
 *
 * @param state the state to set
 * @param action the redux action
 */
const setMeasurementModeEntries = (
  state: Immutable.Map<string, boolean> = Immutable.Map<string, boolean>(),
  action: Types.SET_MEASUREMENT_MODE_ENTRIES
): Immutable.Map<string, boolean> => {
  if (Internal.setMeasurementModeEntries.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the selected location solution set id.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setSelectedLocationSolutionSetId = (
  state: string = null,
  action: Types.SET_SELECTED_LOCATION_SOLUTION_SET_ID
): string => {
  if (Internal.setSelectedLocationSolutionSetId.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the selected location solution id.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setSelectedLocationSolutionId = (
  state: string = null,
  action: Types.SET_SELECTED_LOCATION_SOLUTION_ID
): string => {
  if (Internal.setSelectedLocationSolutionId.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the selected preferred location solution set id.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setSelectedPreferredLocationSolutionSetId = (
  state: string = null,
  action: Types.SET_SELECTED_PREFERRED_LOCATION_SOLUTION_SET_ID
): string => {
  if (Internal.setSelectedPreferredLocationSolutionSetId.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the selected preferred location solution id.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setSelectedPreferredLocationSolutionId = (
  state: string = null,
  action: Types.SET_SELECTED_PREFERRED_LOCATION_SOLUTION_ID
): string => {
  if (Internal.setSelectedPreferredLocationSolutionId.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * The location solution state reducer.
 */
const locationReducer: Redux.Reducer<
  Types.LocationSolutionState,
  Redux.AnyAction
> = Redux.combineReducers({
  selectedLocationSolutionSetId: setSelectedLocationSolutionSetId,
  selectedLocationSolutionId: setSelectedLocationSolutionId,
  selectedPreferredLocationSolutionSetId: setSelectedPreferredLocationSolutionSetId,
  selectedPreferredLocationSolutionId: setSelectedPreferredLocationSolutionId
});

/**
 * Measurement mode reducer.
 */
const measurementModeReducer: Redux.Reducer<
  Types.MeasurementMode,
  Redux.AnyAction
> = Redux.combineReducers({
  mode: setMode,
  entries: setMeasurementModeEntries
});

/**
 * Redux reducer for setting the open layout id.
 *
 * @param state the state to set
 * @param action the redux action
 */
const setOpenLayoutName = (state: string = null, action: Types.SET_OPEN_LAYOUT_NAME): string => {
  if (Actions.setOpenLayoutName.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting the Key Action queue, which is consumed by component trees in the UI
 * to expose global hotkeys to the appropriate consumers
 *
 * @param state the state to set
 * @param action the redux action
 */
const setKeyPressActionQueue = (
  state: Immutable.Map<Types.KeyAction, number> = Immutable.Map<Types.KeyAction, number>(),
  action: Types.SET_KEYPRESS_ACTION_QUEUE
): Immutable.Map<Types.KeyAction, number> => {
  if (Actions.setKeyPressActionQueue.test(action)) {
    return action.payload ? action.payload : null;
  }
  return state;
};

/**
 * Redux reducer for setting if a history action is in progress
 *
 * @param state the state to set
 * @param action the redux action
 */
const handleHistoryAction = (
  state: number = 0,
  action: Types.INCREMENT_HISTORY_ACTION_IN_PROGRESS | Types.DECREMENT_HISTORY_ACTION_IN_PROGRESS
): number => {
  if (Actions.incrementHistoryActionInProgress.test(action)) {
    return state + 1;
  }
  if (Actions.decrementHistoryActionInProgress.test(action)) {
    return state - 1;
  }
  return state;
};

/**
 * Analyst workspace reducer.
 */
export const Reducer: Redux.Reducer<
  Types.AnalystWorkspaceState,
  Redux.AnyAction
> = Redux.combineReducers({
  currentStageInterval: setCurrentStageInterval,
  defaultSignalDetectionPhase: setDefaultSignalDetectionPhase,
  selectedEventIds: setSelectedEventIds,
  openEventId: setOpenEventId,
  selectedSdIds: setSelectedSdIds,
  sdIdsToShowFk: setSdIdsToShowFk,
  selectedSortType: setSelectedSortType,
  channelFilters: setChannelFilters,
  measurementMode: measurementModeReducer,
  location: locationReducer,
  openLayoutName: setOpenLayoutName,
  keyPressActionQueue: setKeyPressActionQueue,
  historyActionInProgress: handleHistoryAction
});
