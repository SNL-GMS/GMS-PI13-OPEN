import * as d3 from 'd3';
import orderBy from 'lodash/orderBy';
import * as Entities from '../entities';

/**
 * Input required to create the position buffer
 */
export interface CreatePositionBufferBySampleRateParams {
  /** Minimum GL value */
  glMin: number;

  /** Maximum GL value */
  glMax: number;

  /** Array containing the vertices */
  data: Float32Array | number[];

  /** Start Time Seconds formatted for display */
  displayStartTimeSecs: number;

  /** End Time Seconds formatted for display */
  displayEndTimeSecs: number;

  /** Start Time in seconds */
  startTimeSecs: number;

  /** End Time in seconds */
  sampleRate: number;
}

/**
 * Convert number[] + startTime + sample rate into a position buffer of [x,y,z,x,y,z,...].
 *
 * @param params [[ CreatePositionBufferParams ]]
 *
 * @returns A Float32Array of vertices
 */
export const createPositionBufferForDataBySampleRate = (
  params: CreatePositionBufferBySampleRateParams
): Float32Array => {
  const vertices: Float32Array = new Float32Array(params.data.length * 3);

  const timeToGlScale = d3
    .scaleLinear()
    .domain([params.displayStartTimeSecs, params.displayEndTimeSecs])
    .range([params.glMin, params.glMax]);

  let time = params.startTimeSecs;
  let i = 0;
  for (const sampleValue of params.data) {
    const x = timeToGlScale(time);
    vertices[i] = x;
    vertices[i + 1] = sampleValue;
    vertices[i + 2] = 0;
    i += 3;

    time += 1 / params.sampleRate;
  }

  return vertices;
};

/**
 * Input required to create the position buffer
 */
export interface CreatePositionBufferByTimeParams {
  /** Minimum GL value */
  glMin: number;

  /** Maximum GL value */
  glMax: number;

  data: Entities.DataByTime;

  /** Start Time Seconds formatted for display */
  displayStartTimeSecs: number;

  /** End Time Seconds formatted for display */
  displayEndTimeSecs: number;
}

/**
 * Convert {time,value}[] to position buffer of [x,y,z,x,y,z,...].
 *
 * @param data the data by time
 *
 * @returns A Float32Array of vertices
 */
export const createPositionBufferForDataByTime = (
  params: CreatePositionBufferByTimeParams
): Float32Array => {
  const vertices: Float32Array = new Float32Array(params.data.values.length * 3);

  const timeToGlScale = d3
    .scaleLinear()
    .domain([params.displayStartTimeSecs, params.displayEndTimeSecs])
    .range([params.glMin, params.glMax]);

  const values = orderBy(params.data.values, [value => value.timeSecs]);

  let i = 0;
  for (const value of values) {
    const x = timeToGlScale(value.timeSecs);
    vertices[i] = x;
    vertices[i + 1] = value.value;
    vertices[i + 2] = 0;
    i += 3;
  }
  return vertices;
};
