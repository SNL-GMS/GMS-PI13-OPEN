import { AnalystWorkspaceTypes } from '@gms/ui-state';
import {
  autoOpenEvent,
  openEvent
} from '../../../../../src/ts/components/analyst-ui/common/actions/event-actions';

// tslint:disable: no-console
window.alert = jest.fn();
window.open = jest.fn();

describe('event actions', () => {
  test('should exist', () => {
    expect(openEvent).toBeDefined();
    expect(autoOpenEvent).toBeDefined();
  });

  test('should set the open event', () => {
    const events: any[] = [
      {
        id: 'id',
        currentEventHypothesis: {
          eventHypothesis: {}
        }
      }
    ];
    const openEventId = 'id';
    const analystActivity = undefined;
    const setOpenEventId = jest.fn(() => true);

    expect(
      // tslint:disable-next-line: no-void-expression
      openEvent(events, openEventId, analystActivity, undefined, setOpenEventId)
    ).toEqual(undefined);
    expect(setOpenEventId).toHaveBeenCalledTimes(1);
    expect(setOpenEventId).toHaveBeenCalledWith(events[0], undefined, undefined);

    //  test it with a processing stage id
    events[0].currentEventHypothesis.processingStage = { id: 'id' };
    expect(
      // tslint:disable-next-line: no-void-expression
      openEvent(events, openEventId, analystActivity, undefined, setOpenEventId)
    ).toEqual(undefined);
    expect(setOpenEventId).toHaveBeenCalledTimes(2);
    expect(setOpenEventId).toHaveBeenCalledWith(events[0], undefined, undefined);
  });

  test('should set the open event with auto open event given the correct time range', () => {
    const events: any[] = [
      {
        id: 'id',
        currentEventHypothesis: {
          eventHypothesis: {}
        }
      }
    ];
    const currentTimeInterval = {
      startTime: 1,
      endTime: 2
    };
    const openEventId = null;
    const analystActivity = AnalystWorkspaceTypes.AnalystActivity.eventRefinement;
    const setOpenEventId = jest.fn(() => true);
    events[0].currentEventHypothesis.eventHypothesis = {
      preferredLocationSolution: {
        locationSolution: {
          location: {
            time: 1.5
          }
        }
      }
    };
    expect(
      // tslint:disable-next-line: no-void-expression
      autoOpenEvent(
        events,
        currentTimeInterval,
        openEventId,
        analystActivity,
        setOpenEventId,
        undefined
      )
    ).toEqual(undefined);
    expect(setOpenEventId).toHaveBeenCalledTimes(1);
    expect(setOpenEventId).toHaveBeenCalledWith(events[0], undefined, undefined);
  });
});
