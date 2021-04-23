import * as Entities from '../../../../../../../../entities';

/**
 * Returns true if the data is by sample rate and casts the data appropriately.
 * @param data the data to check
 */
export const isDataBySampleRate = (
  data: Entities.DataBySampleRate | Entities.DataByTime
): data is Entities.DataBySampleRate =>
  (data as Entities.DataBySampleRate).startTimeSecs !== undefined &&
  (data as Entities.DataBySampleRate).sampleRate !== undefined &&
  (data as Entities.DataBySampleRate).values !== undefined;

/**
 * Returns true if the data is by time and casts the data appropriately.
 * @param data the data to check
 */
export const isDataByTime = (
  data: Entities.DataBySampleRate | Entities.DataByTime
): data is Entities.DataByTime => !isDataBySampleRate(data);
