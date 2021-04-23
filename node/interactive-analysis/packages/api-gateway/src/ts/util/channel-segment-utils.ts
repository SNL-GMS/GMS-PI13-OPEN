import { toEpochSeconds, toOSDTime } from '@gms/common-util';
import isEmpty from 'lodash/isEmpty';
import { UserContext } from '../cache/model';
import {
  ChannelSegment,
  isFkSpectraTimeSeries,
  isWaveformChannelSegment,
  isWaveformTimeSeries,
  OSDChannelSegment,
  OSDTimeSeries,
  TimeSeries
} from '../channel-segment/model';
import { FkPowerSpectra } from '../fk/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { Waveform } from '../waveform/model';
import { fixNanValues, fixNaNValuesDoubleArray } from './common-utils';
import { convertFktoOSDfromAPI } from './fk-utils';

/**
 * Converts a channel segment in OSD compatible format to api (graphql) format for the frontend.
 * Ideally this method will go away if/when the COI data structures are finalized
 * and we adjust our data structures to match them.
 * @param osdChannelSegment a channel segment in OSD compatible format
 * @param channel segment in api gateway format
 * @returns a time series channel segment
 */
export function convertChannelSegmentFromOSDToAPI(
  userContext: UserContext,
  osdChannelSegment: OSDChannelSegment<OSDTimeSeries>
): ChannelSegment<TimeSeries> {
  if (
    osdChannelSegment &&
    !isEmpty(osdChannelSegment) &&
    osdChannelSegment.timeseries &&
    !isEmpty(osdChannelSegment.timeseries)
  ) {
    try {
      const apiTimeSeries: TimeSeries[] = osdChannelSegment.timeseries.map(
        (timeseries: OSDTimeSeries) => {
          const ts: TimeSeries = {
            ...timeseries,
            startTime: toEpochSeconds(timeseries.startTime)
          };
          if (isFkSpectraTimeSeries(ts, osdChannelSegment.timeseriesType)) {
            ts.reviewed = false;
            ts.spectrums.forEach(spectrum => {
              fixNaNValuesDoubleArray(spectrum.fstat);
              fixNaNValuesDoubleArray(spectrum.power);
            });
          } else if (isWaveformTimeSeries(ts, osdChannelSegment.timeseriesType)) {
            fixNanValues(ts.values);
          }
          return ts;
        }
      );
      return {
        id: osdChannelSegment.id,
        name: osdChannelSegment.name,
        channel: osdChannelSegment.channel,
        type: osdChannelSegment.type,
        startTime: toEpochSeconds(osdChannelSegment.startTime),
        endTime: toEpochSeconds(osdChannelSegment.endTime),
        timeseriesType: osdChannelSegment.timeseriesType,
        requiresSave: false, // coming from OSD means nothing changed
        timeseries: apiTimeSeries
      };
    } catch (error) {
      logger.error(`Invalid OSD Channel segment data; failed to convert: ${error}`);
      return undefined;
    }
  } else {
    return undefined;
  }
}

/**
 * Converts a channel segment in api (graphql) format to an OSD compatible channel segment format.
 * Ideally this method will go away if/when the COI data structures are finalized
 * and we adjust our data structures to match them.
 * @param channelSegment a channel segment in api gateway format
 * @return a OSD compatible channel segment
 */
export function convertChannelSegmentFromAPIToOSD(
  channelSegment: ChannelSegment<TimeSeries>
): OSDChannelSegment<OSDTimeSeries> {
  let osdTimeSeries: OSDTimeSeries[];
  if (isWaveformChannelSegment(channelSegment)) {
    osdTimeSeries = channelSegment.timeseries.map((waveform: Waveform) => ({
      ...waveform,
      startTime: toOSDTime(waveform.startTime)
    }));
  } else {
    osdTimeSeries = convertFktoOSDfromAPI(channelSegment.timeseries as FkPowerSpectra[]);
  }
  const osdChannelSegment: OSDChannelSegment<OSDTimeSeries> = {
    id: channelSegment.id,
    channel: channelSegment.channel,
    name: channelSegment.name,
    timeseriesType: channelSegment.timeseriesType,
    type: channelSegment.type,
    startTime: toOSDTime(channelSegment.startTime),
    endTime: toOSDTime(channelSegment.endTime),
    timeseries: osdTimeSeries
  };
  return osdChannelSegment;
}

/**
 * Truncates the channel segment timeseries to only be within the requested time range
 * if the channel segment timeseries is partially in and partially out of the requested
 * time range.
 * @param channelSegment channel segment to window
 * @param timeRange the time range tto window within
 * @returns a waveform channel segment
 */
export function truncateChannelSegmentTimeseries(
  channelSegment: ChannelSegment<TimeSeries>,
  startTime: number,
  endTime: number
): ChannelSegment<TimeSeries> {
  // Window truncate the channel segments to the time range if there is overlap
  if (
    channelSegment.timeseries &&
    channelSegment.timeseries.length > 0 &&
    (startTime > channelSegment.startTime || endTime < channelSegment.endTime)
  ) {
    const windowedTimeseries = channelSegment.timeseries.map((timeseries: Waveform) => {
      // Calculate the window for the smaller waveform to add
      const startSampleIndex = (startTime - channelSegment.startTime) * timeseries.sampleRate;
      const endSampleIndex = (endTime - startTime) * timeseries.sampleRate + startSampleIndex;

      // Window the values of the times
      const values = timeseries.values.slice(startSampleIndex, endSampleIndex - 1);
      const value = {
        ...timeseries,
        startTime,
        sampleCount: values.length,
        values
      };
      return value;
    });

    // Replace timeseries with windowed timeseries
    return {
      ...channelSegment,
      startTime,
      endTime,
      timeseries: windowedTimeseries
    };
  }
  return channelSegment;
}
