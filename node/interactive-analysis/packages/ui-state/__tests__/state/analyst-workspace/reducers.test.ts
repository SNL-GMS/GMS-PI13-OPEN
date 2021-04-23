import { CommonTypes, WaveformTypes } from '@gms/common-graphql';
import * as Immutable from 'immutable';
import * as Redux from 'redux';
import { Reducer } from '../../../src/ts/state/analyst-workspace/reducers';
import * as Types from '../../../src/ts/state/analyst-workspace/types';
import {
  initialAnalystWorkspaceState,
  initialLocationState
} from '../../../src/ts/state/initial-state';
import { actionCreator, actionCreatorVoid } from '../../../src/ts/state/util/action-helper';

describe('state analyst-workspace reducers', () => {
  describe('reducer', () => {
    it('should return the initial state', () => {
      expect(Reducer(undefined, actionCreatorVoid(undefined))).toEqual(
        initialAnalystWorkspaceState
      );
      expect(Reducer(undefined, actionCreator(undefined))).toEqual(initialAnalystWorkspaceState);

      expect(Reducer(undefined, actionCreatorVoid(''))).toEqual(initialAnalystWorkspaceState);
      expect(Reducer(undefined, actionCreator(''))).toEqual(initialAnalystWorkspaceState);

      expect(Reducer(initialAnalystWorkspaceState, actionCreatorVoid(undefined))).toEqual(
        initialAnalystWorkspaceState
      );
      expect(Reducer(initialAnalystWorkspaceState, actionCreator(undefined))).toEqual(
        initialAnalystWorkspaceState
      );

      expect(Reducer(initialAnalystWorkspaceState, actionCreatorVoid(''))).toEqual(
        initialAnalystWorkspaceState
      );
      expect(Reducer(initialAnalystWorkspaceState, actionCreator(''))).toEqual(
        initialAnalystWorkspaceState
      );
    });

    it('should set the current stage interval', () => {
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
      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_CURRENT_STAGE_INTERVAL,
        payload: currentStageInterval
      };
      const expectedState: Types.AnalystWorkspaceState = {
        ...initialAnalystWorkspaceState,
        currentStageInterval
      };
      expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
    });

    it('should set the default signal detection phase', () => {
      const phase = CommonTypes.PhaseType.Lg;
      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_DEFAULT_SIGNAL_DETECTION_PHASE,
        payload: phase
      };
      const expectedState: Types.AnalystWorkspaceState = {
        ...initialAnalystWorkspaceState,
        defaultSignalDetectionPhase: phase
      };
      expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
    });

    it('should set the selected event ids', () => {
      const ids = ['1', '2', '3'];
      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SELECTED_EVENT_IDS,
        payload: ids
      };
      const expectedState: Types.AnalystWorkspaceState = {
        ...initialAnalystWorkspaceState,
        selectedEventIds: ids
      };
      expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
    });

    it('should set the open event id', () => {
      const id = '1';
      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_OPEN_EVENT_ID,
        payload: id
      };
      const expectedState: Types.AnalystWorkspaceState = {
        ...initialAnalystWorkspaceState,
        openEventId: id
      };
      expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
    });

    it('should set the selected signal detection ids', () => {
      const ids = ['1', '2', '3'];
      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SELECTED_SD_IDS,
        payload: ids
      };
      const expectedState: Types.AnalystWorkspaceState = {
        ...initialAnalystWorkspaceState,
        selectedSdIds: ids
      };
      expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
    });

    it('should set the signal detection ids to show FK', () => {
      const ids = ['1', '2', '3'];
      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SD_IDS_TO_SHOW_FK,
        payload: ids
      };
      const expectedState: Types.AnalystWorkspaceState = {
        ...initialAnalystWorkspaceState,
        sdIdsToShowFk: ids
      };
      expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
    });

    it('should set the selected sort type', () => {
      const sortType = Types.WaveformSortType.distance;
      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_SELECTED_SORT_TYPE,
        payload: sortType
      };
      const expectedState: Types.AnalystWorkspaceState = {
        ...initialAnalystWorkspaceState,
        selectedSortType: sortType
      };
      expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
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
      const action: Redux.AnyAction = {
        type: Types.ActionTypes.SET_CHANNEL_FILTERS,
        payload: filters
      };
      const expectedState: Types.AnalystWorkspaceState = {
        ...initialAnalystWorkspaceState,
        channelFilters: filters
      };
      expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
    });

    describe('measurement mode reducer', () => {
      const mode = Types.WaveformDisplayMode.MEASUREMENT;
      const entries = Immutable.Map<string, boolean>().set('a', true);

      it('should set the measurement mode', () => {
        const measurementMode: Types.MeasurementMode = {
          mode,
          entries: Immutable.Map<string, boolean>()
        };
        const action: Redux.AnyAction = {
          type: Types.ActionTypes.SET_MODE,
          payload: mode
        };
        const expectedState: Types.AnalystWorkspaceState = {
          ...initialAnalystWorkspaceState,
          measurementMode
        };
        expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
      });

      it('should set the measurement mode entries', () => {
        const measurementMode: Types.MeasurementMode = {
          mode: Types.WaveformDisplayMode.DEFAULT,
          entries
        };
        const action: Redux.AnyAction = {
          type: Types.ActionTypes.SET_MEASUREMENT_MODE_ENTRIES,
          payload: entries
        };
        const expectedState: Types.AnalystWorkspaceState = {
          ...initialAnalystWorkspaceState,
          measurementMode
        };
        expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
      });
    });

    describe('location reducer', () => {
      it('should set the selected location solution set id', () => {
        const id = 'location-solution-set-id';
        const location: Types.LocationSolutionState = {
          ...initialLocationState,
          selectedLocationSolutionSetId: id
        };
        const action: Redux.AnyAction = {
          type: Types.ActionTypes.SET_SELECTED_LOCATION_SOLUTION_SET_ID,
          payload: id
        };
        const expectedState: Types.AnalystWorkspaceState = {
          ...initialAnalystWorkspaceState,
          location
        };
        expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
      });

      it('should set the selected location solution id', () => {
        const id = 'location-solution-id';
        const location: Types.LocationSolutionState = {
          ...initialLocationState,
          selectedLocationSolutionId: id
        };
        const action: Redux.AnyAction = {
          type: Types.ActionTypes.SET_SELECTED_LOCATION_SOLUTION_ID,
          payload: id
        };
        const expectedState: Types.AnalystWorkspaceState = {
          ...initialAnalystWorkspaceState,
          location
        };
        expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
      });

      it('should set the selected preferred location solution set id', () => {
        const id = 'location-preferred-solution-set-id';
        const location: Types.LocationSolutionState = {
          ...initialLocationState,
          selectedPreferredLocationSolutionSetId: id
        };
        const action: Redux.AnyAction = {
          type: Types.ActionTypes.SET_SELECTED_PREFERRED_LOCATION_SOLUTION_SET_ID,
          payload: id
        };
        const expectedState: Types.AnalystWorkspaceState = {
          ...initialAnalystWorkspaceState,
          location
        };
        expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
      });
      it('should set the displayed magnitude type', () => {
        let noMagTypesShowing: Types.DisplayedMagnitudeTypes = Immutable.Map<any, boolean>();
        noMagTypesShowing = noMagTypesShowing.set('test', false);
        const action: Redux.AnyAction = {
          type: Types.ActionTypes.SET_DISPLAYED_MAGNITUDE_TYPES,
          payload: noMagTypesShowing
        };
        const expectedState: Types.AnalystWorkspaceState = {
          ...initialAnalystWorkspaceState
        };
        expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
      });

      it('should set the selected preferred location solution id', () => {
        const id = 'location-solution-id';
        const location: Types.LocationSolutionState = {
          ...initialLocationState,
          selectedPreferredLocationSolutionId: id
        };
        const action: Redux.AnyAction = {
          type: Types.ActionTypes.SET_SELECTED_PREFERRED_LOCATION_SOLUTION_ID,
          payload: id
        };
        const expectedState: Types.AnalystWorkspaceState = {
          ...initialAnalystWorkspaceState,
          location
        };
        expect(Reducer(initialAnalystWorkspaceState, action)).toEqual(expectedState);
      });
    });
  });
});
