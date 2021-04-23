import { CommonTypes, WaveformTypes } from '@gms/common-graphql';
import * as Immutable from 'immutable';
import * as Redux from 'redux';
import createMockStore, { MockStore, MockStoreCreator } from 'redux-mock-store';
import thunk from 'redux-thunk';
import * as Actions from '../../../src/ts/state/analyst-workspace/actions';
import * as Types from '../../../src/ts/state/analyst-workspace/types';
import { initialAppState } from '../../../src/ts/state/initial-state';
import { AppState } from '../../../src/ts/state/types';

const middlewares = [thunk];
const mockStoreCreator: MockStoreCreator<AppState, Redux.AnyAction> = createMockStore(middlewares);
let store: MockStore<AppState, Redux.AnyAction>;

describe('state analyst-workspace actions', () => {
  describe('internal actions', () => {
    beforeEach(() => {
      store = mockStoreCreator(initialAppState);
    });

    it('should set the mode to default', () => {
      const mode = Types.WaveformDisplayMode.DEFAULT;
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_MODE,
        payload: mode
      };
      expect(Actions.Internal.setMode(mode)).toEqual(expectedAction);

      store.dispatch(Actions.Internal.setMode(mode));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the mode to measurement', () => {
      const mode = Types.WaveformDisplayMode.MEASUREMENT;
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_MODE,
        payload: mode
      };
      expect(Actions.Internal.setMode(mode)).toEqual(expectedAction);

      store.dispatch(Actions.Internal.setMode(mode));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the current interval', () => {
      const currentStageInterval: Types.StageInterval = {
        id: 'stage-id',
        name: 'stage-name',
        interval: {
          id: 'interval-id',
          timeInterval: {
            startTime: 100000000000000,
            endTime: 989898989898989
          },
          activityInterval: {
            id: 'activity-id',
            name: 'activity-name',
            analystActivity: Types.AnalystActivity.eventRefinement
          }
        }
      };
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_CURRENT_STAGE_INTERVAL,
        payload: currentStageInterval
      };
      expect(Actions.Internal.setCurrentStageInterval(currentStageInterval)).toEqual(
        expectedAction
      );

      store.dispatch(Actions.Internal.setCurrentStageInterval(currentStageInterval));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the open event id', () => {
      const id = 'event-id';
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_OPEN_EVENT_ID,
        payload: id
      };
      expect(Actions.Internal.setOpenEventId(id)).toEqual(expectedAction);

      store.dispatch(Actions.Internal.setOpenEventId(id));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the measurement mode entries', () => {
      const entries = Immutable.Map<string, boolean>().set('a', true);
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_MEASUREMENT_MODE_ENTRIES,
        payload: entries
      };
      expect(Actions.Internal.setMeasurementModeEntries(entries)).toEqual(expectedAction);

      store.dispatch(Actions.Internal.setMeasurementModeEntries(entries));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the selected location solution set id', () => {
      const id = 'location-solution-set-id';
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SELECTED_LOCATION_SOLUTION_SET_ID,
        payload: id
      };
      expect(Actions.Internal.setSelectedLocationSolutionSetId(id)).toEqual(expectedAction);

      store.dispatch(Actions.Internal.setSelectedLocationSolutionSetId(id));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the selected location solution id', () => {
      const id = 'location-solution-id';
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SELECTED_LOCATION_SOLUTION_ID,
        payload: id
      };
      expect(Actions.Internal.setSelectedLocationSolutionId(id)).toEqual(expectedAction);

      store.dispatch(Actions.Internal.setSelectedLocationSolutionId(id));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the selected preferred location solution set id', () => {
      const id = 'location-preferred-solution-set-id';
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SELECTED_PREFERRED_LOCATION_SOLUTION_SET_ID,
        payload: id
      };
      expect(Actions.Internal.setSelectedPreferredLocationSolutionSetId(id)).toEqual(
        expectedAction
      );

      store.dispatch(Actions.Internal.setSelectedPreferredLocationSolutionSetId(id));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the selected preferred location solution id', () => {
      const id = 'location-solution-id';
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SELECTED_PREFERRED_LOCATION_SOLUTION_ID,
        payload: id
      };
      expect(Actions.Internal.setSelectedPreferredLocationSolutionId(id)).toEqual(expectedAction);

      store.dispatch(Actions.Internal.setSelectedPreferredLocationSolutionId(id));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });
  });

  describe('actions', () => {
    beforeEach(() => {
      store = mockStoreCreator(initialAppState);
    });

    it('should set the default signal detection phase', () => {
      const phase = CommonTypes.PhaseType.Lg;
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_DEFAULT_SIGNAL_DETECTION_PHASE,
        payload: phase
      };
      expect(Actions.Actions.setDefaultSignalDetectionPhase(phase)).toEqual(expectedAction);

      store.dispatch(Actions.Actions.setDefaultSignalDetectionPhase(phase));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the selected sort type', () => {
      const sortType = Types.WaveformSortType.distance;
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SELECTED_SORT_TYPE,
        payload: sortType
      };
      expect(Actions.Actions.setSelectedSortType(sortType)).toEqual(expectedAction);

      store.dispatch(Actions.Actions.setSelectedSortType(sortType));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the selected signal detection ids', () => {
      const ids = ['1', '2', '3'];
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SELECTED_SD_IDS,
        payload: ids
      };
      expect(Actions.Actions.setSelectedSdIds(ids)).toEqual(expectedAction);

      store.dispatch(Actions.Actions.setSelectedSdIds(ids));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the selected event ids', () => {
      const ids = ['1', '2', '3'];
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SELECTED_EVENT_IDS,
        payload: ids
      };
      expect(Actions.Actions.setSelectedEventIds(ids)).toEqual(expectedAction);

      store.dispatch(Actions.Actions.setSelectedEventIds(ids));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the signal detections ids to show fk', () => {
      const ids = ['1', '2', '3'];
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SD_IDS_TO_SHOW_FK,
        payload: ids
      };
      expect(Actions.Actions.setSdIdsToShowFk(ids)).toEqual(expectedAction);

      store.dispatch(Actions.Actions.setSdIdsToShowFk(ids));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });

    it('should set the channel filters', () => {
      const filters = Immutable.Map<string, WaveformTypes.WaveformFilter>().set('a', {
        id: 'id',
        name: 'name',
        description: 'description',
        filterType: 'filter-type',
        filterPassBandType: 'filter-pass-band-type',
        lowFrequencyHz: 0,
        highFrequencyHz: 9,
        order: 1,
        filterSource: 'filter-source',
        filterCausality: 'fitler-causality',
        zeroPhase: true,
        sampleRate: 5,
        sampleRateTolerance: 2,
        validForSampleRate: false,
        aCoefficients: [1, 2, 3],
        // tslint:disable-next-line: no-magic-numbers
        bCoefficients: [5, 6, 7],
        groupDelaySecs: 4
      });
      const expectedAction: Redux.AnyAction = {
        type: Types.ActionTypes.SET_CHANNEL_FILTERS,
        payload: filters
      };
      expect(Actions.Actions.setChannelFilters(filters)).toEqual(expectedAction);

      store.dispatch(Actions.Actions.setChannelFilters(filters));
      const actions = store.getActions();
      expect(actions).toEqual([expectedAction]);
    });
  });
});
