import { EventTypes } from '@gms/common-graphql';
import Immutable from 'immutable';
import isEqual from 'lodash/isEqual';
import { AppState } from '../types';
import { Actions, Internal } from './actions';
import {
  AnalystWorkspaceState,
  StageInterval,
  WaveformDisplayMode,
  WaveformSortType
} from './types';

/**
 * Redux operation for setting the mode.
 *
 * @param mode the mode to set
 */
const setMode = (mode: WaveformDisplayMode) => (dispatch: any, getState: () => AppState) => {
  dispatch(Internal.setMode(mode));
};

/**
 * Redux operation for setting the measurement mode entries.
 *
 * @param entries the measurement mode entries to set
 */
const setMeasurementModeEntries = (entries: Immutable.Map<string, boolean>) => (
  dispatch: any,
  getState: () => AppState
) => {
  dispatch(Internal.setMeasurementModeEntries(entries));
};

/**
 * Redux operation for setting the current stage interval.
 *
 * @param stageInterval the stage interval to set
 */
const setCurrentStageInterval = (stageInterval: StageInterval) => (
  dispatch: any,
  getState: () => AppState
) => {
  const state: AnalystWorkspaceState = getState().analystWorkspaceState;

  const hasCurrentIntervalChanged =
    !state.currentStageInterval ||
    !stageInterval ||
    state.currentStageInterval.id !== stageInterval.id ||
    !isEqual(state.currentStageInterval.interval.timeInterval, stageInterval.interval.timeInterval);

  // if the time range is not changing and there is no selected event
  // temporarily set the interval to undefined to force the auto open of
  // an event if necessary
  if (!hasCurrentIntervalChanged && !state.openEventId) {
    dispatch(Internal.setCurrentStageInterval(undefined));
  }

  if (!isEqual(state.currentStageInterval, stageInterval)) {
    dispatch(Internal.setCurrentStageInterval(stageInterval));
  }

  // clear out the following
  // if the processing stage interval id (or time interval) has changed
  if (hasCurrentIntervalChanged) {
    if (state.selectedSdIds.length !== 0) {
      dispatch(Actions.setSelectedSdIds([]));
    }

    if (state.openEventId !== undefined && state.openEventId !== null) {
      dispatch(Internal.setOpenEventId(undefined));
    }

    if (state.selectedEventIds.length !== 0) {
      dispatch(Actions.setSelectedEventIds([]));
    }

    if (state.sdIdsToShowFk.length !== 0) {
      dispatch(Actions.setSdIdsToShowFk([]));
    }

    if (state.measurementMode.mode !== WaveformDisplayMode.DEFAULT) {
      dispatch(setMode(WaveformDisplayMode.DEFAULT));
    }

    if (state.measurementMode.entries.size !== 0) {
      dispatch(Internal.setMeasurementModeEntries(Immutable.Map()));
    }

    if (state.selectedSortType !== WaveformSortType.stationName) {
      dispatch(Actions.setSelectedSortType(WaveformSortType.stationName));
    }
  }
};

/**
 * Redux operation for setting the selected location solution.
 *
 * @param locationSolutionSetId the location solution set id
 * @param locationSolutionId the location solution id
 */
const setSelectedLocationSolution = (locationSolutionSetId: string, locationSolutionId: string) => (
  dispatch: any,
  getState: () => AppState
) => {
  const state: AnalystWorkspaceState = getState().analystWorkspaceState;

  if (state.location.selectedLocationSolutionSetId !== locationSolutionSetId) {
    dispatch(Internal.setSelectedLocationSolutionSetId(locationSolutionSetId));
  }

  if (state.location.selectedLocationSolutionId !== locationSolutionId) {
    dispatch(Internal.setSelectedLocationSolutionId(locationSolutionId));
  }
};

/**
 * Redux operation for setting the selected preferred location solution.
 *
 * @param preferredLocationSolutionSetId the preferred location solution set id
 * @param preferredLocationSolutionId the preferred location solution id
 */
const setSelectedPreferredLocationSolution = (
  preferredLocationSolutionSetId: string,
  preferredLocationSolutionId: string
) => (dispatch: any, getState: () => AppState) => {
  const state: AnalystWorkspaceState = getState().analystWorkspaceState;

  if (state.location.selectedPreferredLocationSolutionSetId !== preferredLocationSolutionSetId) {
    dispatch(Internal.setSelectedPreferredLocationSolutionSetId(preferredLocationSolutionSetId));
  }

  if (state.location.selectedPreferredLocationSolutionId !== preferredLocationSolutionId) {
    dispatch(Internal.setSelectedPreferredLocationSolutionId(preferredLocationSolutionId));
  }
};

/**
 * Redux operation for setting the current open event id.
 *
 * @param event the event to set
 */
const setOpenEventId = (
  event: EventTypes.Event | undefined,
  latestLocationSolutionSet: EventTypes.LocationSolutionSet | undefined,
  preferredLocationSolutionId: string | undefined
) => (dispatch: any, getState: () => AppState) => {
  const state: AnalystWorkspaceState = getState().analystWorkspaceState;

  if (state.currentStageInterval && event) {
    if (state.openEventId !== event.id) {
      dispatch(Internal.setOpenEventId(event.id));

      if (!isEqual(state.selectedEventIds, [event.id])) {
        dispatch(Actions.setSelectedEventIds([event.id]));
      }

      if (state.selectedSortType !== WaveformSortType.distance) {
        dispatch(Actions.setSelectedSortType(WaveformSortType.distance));
      }

      // set the default (latest) location solution
      dispatch(
        setSelectedLocationSolution(
          latestLocationSolutionSet ? latestLocationSolutionSet.id : undefined,
          latestLocationSolutionSet ? latestLocationSolutionSet.locationSolutions[0].id : undefined
        )
      );

      // set the default (latest) preferred location solution
      dispatch(
        setSelectedPreferredLocationSolution(
          latestLocationSolutionSet.id,
          preferredLocationSolutionId
        )
      );
    }
  } else {
    if (state.openEventId !== undefined && state.openEventId !== null) {
      dispatch(Internal.setOpenEventId(undefined));
    }

    if (state.selectedEventIds.length !== 0) {
      dispatch(Actions.setSelectedEventIds([]));
    }

    if (state.selectedSortType !== WaveformSortType.stationName) {
      dispatch(Actions.setSelectedSortType(WaveformSortType.stationName));
    }

    if (state.measurementMode.entries.size !== 0) {
      dispatch(Internal.setMeasurementModeEntries(Immutable.Map()));
    }

    // update the selected location and preferred location solutions
    dispatch(setSelectedLocationSolution(undefined, undefined));
    dispatch(setSelectedPreferredLocationSolution(undefined, undefined));
  }

  if (state.selectedSdIds.length !== 0) {
    dispatch(Actions.setSelectedSdIds([]));
  }

  if (state.sdIdsToShowFk.length !== 0) {
    dispatch(Actions.setSdIdsToShowFk([]));
  }

  if (state.measurementMode.mode !== WaveformDisplayMode.DEFAULT) {
    dispatch(setMode(WaveformDisplayMode.DEFAULT));
  }
};

/**
 * Redux operations (public).
 */
export const Operations = {
  setCurrentStageInterval,
  setOpenEventId,
  setMode,
  setMeasurementModeEntries,
  setSelectedLocationSolution,
  setSelectedPreferredLocationSolution
};
