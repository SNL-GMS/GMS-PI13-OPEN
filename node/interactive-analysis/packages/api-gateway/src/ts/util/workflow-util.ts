import { uuid4 } from '@gms/common-util';
import config from 'config';
import find from 'lodash/find';
import { TimeRange } from '../common/model';
import { ProcessingStage } from '../workflow/model';

/**
 * Normailzes the start and end times of the inputed parameters
 * @param startTimeSec start time in epoch seconds
 * @param endTimeSec end time in epoch seconds
 * @param intervalDurationSec length of interval
 * @returns a TimeRange
 */
export const normalizeStartEndTimes = (
  startTimeSec: number,
  endTimeSec: number,
  intervalDurationSec: number
): TimeRange => {
  /**
   * Intervals are discrete, two hour segments of time
   * Ie there is an interval from 12:00am to 2:00am, and from 2:00am to 4:00am
   * But not from 1:00am to 3:00am
   *
   * So when we populate intervals, first we normalize to our two hour
   * chunks
   *
   * Because interval creation is cheap, we overestimate needs
   */

  let normalizedStartSec = startTimeSec;
  normalizedStartSec = normalizedStartSec - (normalizedStartSec % intervalDurationSec);
  // An end time secs 'snapped' to a 2 hour time interval
  const normalizedEndSec =
    endTimeSec % intervalDurationSec === 0
      ? endTimeSec
      : endTimeSec + (intervalDurationSec - (endTimeSec % intervalDurationSec));
  return { startTime: normalizedStartSec, endTime: normalizedEndSec };
};

/**
 * Gets initial time range based on config settings
 */
export const getInitialTimeRange = (): TimeRange => {
  const serviceConfig = config.get('workflow.intervalService');
  const intervalCreationStartTimeSec = serviceConfig.intervalCreationStartTimeSec;
  const intervalDurationSec = serviceConfig.intervalDurationSec;
  return normalizeStartEndTimes(
    serviceConfig.mockedIntervalStartTimeSec,
    intervalCreationStartTimeSec,
    intervalDurationSec
  );
};

/**
 * Retrieves the processing stage with the provided ID string.
 * @param id The unique ID of the processing stage to retrieve
 * @returns a ProcessingStage
 */
export function getStage(stages: ProcessingStage[], id: string): ProcessingStage {
  if (id) {
    const stage = find(stages, { id });
    // TODO this should be looked at again once we meaning to our stages and intervals (backend integration)
    const hardCodedStage = stages[0];
    hardCodedStage.id = uuid4();
    return stage ? stage : hardCodedStage;
  }
}
