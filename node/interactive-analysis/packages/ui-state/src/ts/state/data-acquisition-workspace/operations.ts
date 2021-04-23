import isEqual from 'lodash/isEqual';
import sortBy from 'lodash/sortBy';
import { batch } from 'react-redux';
import { AppState } from '../types';
import { Actions, Internal } from './actions';
import { DataAcquisitionWorkspaceState, SohStatus } from './types';

const getSohStatusWithClearedChannelData = (state: DataAcquisitionWorkspaceState) => {
  // clear out the channel data
  const sohStatus = state.data.sohStatus;
  sohStatus.stationAndStationGroupSoh.stationSoh.forEach(s => (s.channelSohs = undefined));
  return sohStatus;
};

/**
 * @param ids original ids
 * @param selectedStationIds newly set selected ids
 * @returns whether they are the same, irrespective of order
 */
const idsHaveChanged = (ids, selectedStationIds) => {
  // Sorting to ensure acutal different selections ex: [2, 1] [1, 2] are not really different
  const sortedNewIds = sortBy(ids);
  const sortedOldIds = sortBy(selectedStationIds);
  return !isEqual(sortedNewIds, sortedOldIds);
};

/**
 * dispatches the sohStatuses and ids provided
 * @param dispatch the dispatch function to use
 * @param state the current state in redux
 * @param sohStatus the sohStatus to dispatch
 * @param ids the ids to dispatch
 */
const batchSohStatusAndSelectionDispatches = (
  dispatch: any,
  state: DataAcquisitionWorkspaceState,
  sohStatus: SohStatus,
  ids: string[]
) => {
  // batch the dispatches - this will only result in one combined re-render, not two
  batch(() => {
    if (!isEqual(state.data.sohStatus, sohStatus)) {
      dispatch(Actions.setSohStatus(sohStatus));
    }

    if (idsHaveChanged(ids, state.selectedStationIds)) {
      dispatch(Internal.setSelectedStationIds(ids));
    }
  });
};

/**
 * Redux operation for the selected stations.
 *
 * @param ids the ids to set
 */
const setSelectedStationIds = (ids: string[]) => (dispatch: any, getState: () => AppState) => {
  const state: DataAcquisitionWorkspaceState = getState().dataAcquisitionWorkspaceState;

  if (idsHaveChanged(ids, state.selectedStationIds)) {
    const sohStatus = getSohStatusWithClearedChannelData(state);
    batchSohStatusAndSelectionDispatches(dispatch, state, sohStatus, ids);
  }
};

// Reserved for future redux operations
export const Operations = {
  setSelectedStationIds
};
