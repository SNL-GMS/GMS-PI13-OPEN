import { getRandomInRange, getSecureRandomNumber } from '@gms/common-util';
import { DataPayload } from '../cache/model';
import { Event } from '../event/model-and-schema/model';
import { QcMask } from '../qc-mask/model';
import { SignalDetection } from '../signal-detection/model';

// Constants used in random lat/lon
const LAT_RANGE = 90;
const LON_RANGE = 180;
const FIXED_DECIMAL = 3;
const OFFSET_MIN = 0.1;
const OFFSET_MAX = 2;
const RADIUS = 2;
const RES_RANGE = 4;

/**
 * Returns a random latitude from -90 to 90
 */
export function getRandomLatitude() {
  return getRandomInRange(-LAT_RANGE, LAT_RANGE, FIXED_DECIMAL);
}

/**
 * Returns a random longitude from -180 to 180
 */
export function getRandomLongitude() {
  return getRandomInRange(-LON_RANGE, LON_RANGE, FIXED_DECIMAL);
}
/**
 * Gets random residual between -4 and 4
 */
export function getRandomResidual() {
  return getRandomInRange(-RES_RANGE, RES_RANGE, FIXED_DECIMAL);
}
/**
 * Returns a small offset used in randomizing event location around a station
 */
export function getRandomOffset() {
  const sign = getSecureRandomNumber() < OFFSET_MAX ? -1 : 1;
  return getRandomInRange(OFFSET_MIN, OFFSET_MAX, FIXED_DECIMAL) * sign;
}

/**
 * Returns a point on a circle RADIUS away
 */
export function getRandomLatLonOffset() {
  const angle = getSecureRandomNumber() * Math.PI * 2;
  const x = Math.cos(angle) * RADIUS;
  const y = Math.sin(angle) * RADIUS;
  return {
    lat: x,
    lon: y
  };
}

/**
 * Checks if the object is empty by checking how many keys are present
 * @param object object to check for empty
 * @returns a boolean
 */
export function isObjectEmpty(object: any): boolean {
  return Object.keys(object).length <= 0;
}

/**
 * Walk thru the double array calling fixNaNValues for each row
 * @param values the values
 */
export function fixNaNValuesDoubleArray(values: number[][]) {
  values.forEach(fixNanValues);
}

/**
 * Walks the array and replaces any NaN values with undefined
 * @param values array of numbers
 */
export function fixNanValues(values: number[]) {
  values.forEach((val, index) => {
    if (val !== undefined && isNaN(val)) {
      values[index] = undefined;
    }
  });
}

/**
 * Creates an empty data payload with the given update type
 * @param updateType the update type
 */
export function createEmptyDataPayload(): DataPayload {
  return {
    events: Object.seal([]),
    sds: Object.seal([]),
    qcMasks: Object.seal([])
  };
}

/**
 * Creates a data payload object with the given data
 * @param events events in payload
 * @param sds sds in payload
 * @param qcMasks qc masks in payload
 * @param updateType optional update type, defaults to full event update
 */
export function createDataPayload(
  events: Event[],
  sds: SignalDetection[],
  qcMasks: QcMask[]
): DataPayload {
  return {
    events: Object.seal(events),
    sds: Object.seal(sds),
    qcMasks: Object.seal(qcMasks)
  };
}

/**
 * Replaces an item in the array if the items id is already in the list
 * @param list array of items
 * @param itemToReplaceWithOrAdd item to updated by id in the list
 */
export function replaceByIdOrAddToList<T extends { id: string }>(
  collection: any,
  itemToReplaceWithOrAdd: T
): T[] {
  const list = collection as T[];
  const indexToReplace = list.findIndex(item => item.id === itemToReplaceWithOrAdd.id);
  if (indexToReplace >= 0) {
    list[indexToReplace] = itemToReplaceWithOrAdd;
  } else {
    list.push(itemToReplaceWithOrAdd);
  }
  return list;
}
