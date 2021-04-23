import moment from 'moment';

export const MILLISECONDS_IN_HALF_SECOND = 500;
export const MILLISECONDS_IN_SECOND = 1000;
export const SECONDS_IN_MINUTES = 60;
export const MINUTES_IN_HOUR = 60;
export const HOURS_IN_DAY = 24;
export const DAYS_IN_WEEK = 7;
export const WEEKS_IN_YEAR = 52;

export const MILLIS_IN_MINUTE: number = SECONDS_IN_MINUTES * MILLISECONDS_IN_SECOND;
export const MILLIS_IN_HOUR: number = MINUTES_IN_HOUR * MILLIS_IN_MINUTE;
export const MILLIS_IN_HALF_DAY: number = (HOURS_IN_DAY * MILLIS_IN_HOUR) / 2;
export const MILLIS_IN_DAY: number = HOURS_IN_DAY * MILLIS_IN_HOUR;
export const MILLIS_IN_WEEK: number = DAYS_IN_WEEK * MILLIS_IN_DAY;
export const MILLIS_IN_YEAR: number = MILLIS_IN_WEEK * WEEKS_IN_YEAR;

export enum TIME_UNITS {
  WEEKS = 'WEEKS',
  DAYS = 'DAYS',
  HOURS = 'HOURS',
  MINUTES = 'MINUTES',
  SECONDS = 'SECONDS'
}

// Number of times units that aren't milliseconds in time units interface TODO make a real data structure
const NUMBER_OF_SIGNIFICANT_TIME_UNITS = 5;
const INDEX_OF_LAST_SIGNIFICANT_TIME_UNIT = NUMBER_OF_SIGNIFICANT_TIME_UNITS - 1;

const TIME_UNIT_INDICES = new Map<TIME_UNITS, number>([
  [TIME_UNITS.WEEKS, 0],
  [TIME_UNITS.DAYS, 1],
  [TIME_UNITS.HOURS, 2],
  [TIME_UNITS.MINUTES, 3],
  [TIME_UNITS.SECONDS, 4]
]);
/**
 * Date and time format to the second precision
 */
export const DATE_TIME_FORMAT_S = 'YYYY-MM-DD HH:mm:ss';

/**
 * Date and time format to the sub-second precision (two decimals places)
 */
export const DATE_TIME_FORMAT_MS = 'YYYY-MM-DD HH:mm:ss.SS';

/**
 * Date format when there's no time (beyond 00:00:00) available
 */
export const DATE_FORMAT = 'YYYY-MM-DD';

/**
 * Date format when there's no time (beyond 00:00:00) available
 */
export const HOUR_MINUTE_SECOND = 'HH:mm:ss';

/**
 * Time format to the second precision
 */
export const TIME_FORMAT = 'HH:mm:ss.SS';

/** Time format to the sub-second precision (two decimals places) */
export const TIME_FORMAT_MS = 'HH:mm:ss.SS';

/** ISO time format */
export const DATE_ISO_FORMAT = 'YYYY/MM/DDTHH:mm:ss.SSSSSS';

/** ISO short format */
export const SHORT_ISO_FORMAT = 'YYYY/MM/DDTHH:mm';

/** ISO short format */
export const SHORT_ISO_FORMAT_NO_T = 'YYYY/MM/DD HH:mm';

/** ISO second format */
export const DATE_TO_SECOND_PRECISION = 'YYYY/MM/DDTHH:mm:ss';

/**
 * Format seconds to a Moment  object.
 * @param seconds the seconds
 */
export const toMoment = (secs: number): moment.Moment => moment.unix(secs).utc();

/**
 * Format seconds to a readable date/time string.
 * @param seconds the seconds
 * @param format the format string of the date/time - defaults to
 *  'YYYY-MM-DD HH:mm:ss.SS'
 */
export const toDateTimeString = (secs: number, format: string = DATE_TIME_FORMAT_MS): string =>
  toMoment(secs)
    .utc()
    .format(format);

/**
 * Format seconds to a readable time string
 * @param seconds the seconds
 */
export const toTimeString = (secs: number): string => toDateTimeString(secs, TIME_FORMAT);

/**
 * Format seconds to a JS Date object.
 * Example: "Sun Jul 26 2020 21:20:00 GMT-0600 (Mountain Daylight Time)"
 * @param seconds the seconds
 */
export const toDate = (secs: number): Date =>
  moment
    .unix(secs)
    .utc()
    .toDate();

/**
 * Format seconds to a readable date string
 * @param seconds the seconds
 */
export const toDateString = (secs: number): string => toDateTimeString(secs, DATE_FORMAT);

/**
 * Format a JS date to a readable date/time string.
 * @param date the JS date
 * @param format the format string of the date/time - defaults to
 *  'YYYY-MM-DD HH:mm:ss'
 */
export const dateToString = (date: Date, format: string = DATE_TIME_FORMAT_S): string =>
  moment(date).format(format);
/**
 * Format a JS date to a readable date/time string.
 * @param date the JS date
 * @param format the format string of the date/time - defaults to
 *  'YYYY/MM/DDTHH:mm:ss.SSSSS'
 */

export const dateToISOString = (date: Date, format: string = DATE_ISO_FORMAT): string =>
  moment.utc(date).format(format);
/**
 * Format a JS date to a readable date/time string.
 * @param date the JS date
 * @param format the format string of the date/time - defaults to
 *  'YYYY/MM/DDTHH:mm:ss.SSSSS'
 */

export const dateToShortISOString = (date: Date, format: string = SHORT_ISO_FORMAT): string =>
  moment.utc(date).format(format);
/**
 * Format a time in seconds to a readable date/time string.
 * @param date the JS date
 * @param format the format string of the date/time - defaults to
 *  'YYYY/MM/DDTHH:mm:ss.SSSSS'
 */

export const dateToSecondPrecision = (
  date: Date,
  format: string = DATE_TO_SECOND_PRECISION
): string =>
  moment
    .utc(date)
    .format(format)
    .replace('T', ' ');
/**
 * Format a time in seconds to a readable date/time string.
 * @param date the JS date
 * @param format the format string of the date/time - defaults to
 *  DDTHH:mm:ss'
 */

export const timeSecondsToISOString = (date: Date, format: string = DATE_ISO_FORMAT): string =>
  moment.utc(date).format(format);
/**
 * Convert an iso string to a date.
 * @param isoString the iso date string
 * @param format the format string of the date/time - defaults to
 *  'YYYY/MM/DDTHH:mm:ss.SSSSS'
 */

export const parseISOString = (isoString: string, format: string = DATE_ISO_FORMAT): Date =>
  new Date(moment.utc(isoString, DATE_ISO_FORMAT).valueOf());

/**
 * Format a time in secs to a readable date/time string.
 * @param date the JS date
 * @param format the format string of the date/time - defaults to
 *  'YYYY/MM/DDT'
 */

export const timeSecsToShortISOString = (date: Date, format: string = SHORT_ISO_FORMAT): string =>
  moment.utc(date).format(format);
/**
 * Format a time in secs to a readable date/time string.
 * @param date the JS date
 * @param format the format string of the date/time - defaults to
 *  'YYYY/MM/DDT'
 */

export const dateToHoursMinutesSeconds = (
  date: Date,
  format: string = HOUR_MINUTE_SECOND
): string => moment.utc(date).format(format);
/**
 * Convert an iso string to a date.
 * @param isoString the iso datestring
 * @param format the format string of the date/time - defaults to
 *  'YYYY/MM/DDTHH'
 */

export const parseShortISOString = (isoString: string, format: string = SHORT_ISO_FORMAT): Date =>
  new Date(moment.utc(isoString, DATE_ISO_FORMAT).valueOf());

/**
 * Format a julian date (YYYYDDD) to a readable date/time string.
 * @param julianDate the julian date
 * @param format the format string of the date/time - defaults to
 *  'YYYY/MM/DDTHH:mm:ss.SSSSS'
 */

export const julianDateToISOString = (
  julianDate: string,
  format: string = DATE_ISO_FORMAT
): string => moment.utc(moment(julianDate, 'YYYYDDD')).format(format);

/**
 * Helper function to convert OSD compatible ISO formatted date string to epoch seconds.
 * @param dateString date string in ISO format
 * @returns an epoch seconds representation of the input date spring
 */
export function toEpochSeconds(dateString: string): number {
  if (dateString === undefined || dateString === null || dateString.length === 0) {
    return 0;
  }

  return new Date(dateString).getTime() / MILLISECONDS_IN_SECOND;
}

/**
 * Helper function to get the current time in epoch seconds.
 * @returns an epoch seconds for time now
 */
export function epochSecondsNow(): number {
  return Date.now() / MILLISECONDS_IN_SECOND;
}

/**
 * Helper function to convert epoch seconds to OSD compatible ISO formatted date string.
 * @param epochSeconds seconds since epoch
 * @returns a New Date string in OSD format
 */
export function toOSDTime(epochSeconds: number): string {
  if (isNaN(epochSeconds)) {
    return toOSDTime(0);
  }
  return new Date(epochSeconds * MILLISECONDS_IN_SECOND).toISOString();
}

export interface TimeUnits {
  weeks: number;
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
  milliseconds: number;
}

export const splitMillisIntoTimeUnits = (millis: number): TimeUnits => {
  const weeks = Math.floor(millis / MILLIS_IN_WEEK);
  const remainderAfterWeeks = millis % MILLIS_IN_WEEK;
  const days = Math.floor(remainderAfterWeeks / MILLIS_IN_DAY);
  const remainderAfterDays = millis % MILLIS_IN_DAY;
  const hours = Math.floor(remainderAfterDays / MILLIS_IN_HOUR);
  const remainderAfterHours = millis % MILLIS_IN_HOUR;
  const minutes = Math.floor(remainderAfterHours / MILLIS_IN_MINUTE);
  const remainderAfterMinutes = millis % MILLIS_IN_MINUTE;
  const seconds = Math.floor(remainderAfterMinutes / MILLISECONDS_IN_SECOND);
  const remainderAfterSeconds = millis % MILLISECONDS_IN_SECOND;
  return {
    weeks,
    days,
    hours,
    minutes,
    seconds,
    milliseconds: remainderAfterSeconds
  };
};

export const timeUnitsToString = (timeUnits: TimeUnits, includeMilliseconds?: boolean): string => {
  const result =
    `${timeUnits.weeks > 0 ? ` ${timeUnits.weeks} week${timeUnits.weeks > 1 ? 's' : ''}` : ''}` +
    `${timeUnits.days > 0 ? ` ${timeUnits.days} day${timeUnits.days > 1 ? 's' : ''}` : ''}` +
    `${timeUnits.hours > 0 ? ` ${timeUnits.hours} hour${timeUnits.hours > 1 ? 's' : ''}` : ''}` +
    `${
      timeUnits.minutes > 0 ? ` ${timeUnits.minutes} minute${timeUnits.minutes > 1 ? 's' : ''}` : ''
    }` +
    `${
      timeUnits.seconds > 0 ? ` ${timeUnits.seconds} second${timeUnits.seconds > 1 ? 's' : ''}` : ''
    }` +
    `${
      includeMilliseconds && timeUnits.milliseconds > 0
        ? ` ${timeUnits.milliseconds} millisecond${timeUnits.milliseconds > 1 ? 's' : ''}`
        : ''
    }`;
  // Strip off leading space to the result
  if (result.length > 0) {
    return result.substring(1);
  }
  return '';
};

export const getIndexOfFirstSignificantTimeUnit = (timeUnits: TimeUnits): number =>
  timeUnits.weeks > 0
    ? 0
    : timeUnits.days > 0
    ? 1
    : timeUnits.hours > 0
    ? 2
    : timeUnits.minutes > 0
    ? 3
    : timeUnits.seconds > 0
    ? 4
    : // tslint:disable-next-line: no-magic-numbers
      5;
/**
 * Sets time units so that no more than the maximum time units are non-zero
 * @param timeUnits units to truncate
 * @param maxTimeUnits maximum amount of units to show
 */
export const truncateTimeUnits = (timeUnits: TimeUnits, maxTimeUnits?: number): TimeUnits => {
  const MAX_THRESHOLD = 9999;
  const increaseThresholdBy = getIndexOfFirstSignificantTimeUnit(timeUnits);
  const threshold = maxTimeUnits > 0 ? maxTimeUnits + increaseThresholdBy : MAX_THRESHOLD;
  return {
    weeks: timeUnits.weeks,
    days: threshold >= 2 ? timeUnits.days : 0,
    hours: threshold >= 3 ? timeUnits.hours : 0,
    minutes: threshold >= 4 ? timeUnits.minutes : 0,
    // tslint:disable-next-line: no-magic-numbers
    seconds: threshold >= 5 ? timeUnits.seconds : 0,
    // tslint:disable-next-line: no-magic-numbers
    milliseconds: threshold >= 6 ? timeUnits.milliseconds : 0
  };
};

/**
 * Converts milliseconds into human readable string
 * @param millis milliseconds to split
 * @param maximumPrecision if provided, will reduce outputted time units to a maximum of three
 * @param includeMilliseconds if provided, output may include milliseconds
 */
export const millisToTimeRemaining = (
  millis: number,
  maximumPrecision?: number,
  includeMilliseconds?: boolean
): string => {
  const timeUnits = splitMillisIntoTimeUnits(millis);
  const timeUnitsToShow: TimeUnits = maximumPrecision
    ? truncateTimeUnits(timeUnits, maximumPrecision)
    : timeUnits;
  return timeUnitsToString(timeUnitsToShow, includeMilliseconds);
};

/**
 * Returns weeks, days, etc. optionally to a maximum given precision. Removes leading a trailing units that are zero
 *
 * ie, 3660001 ms, 2 precision => 1 hour 1 minute
 * ie, 6000 ms, 2 precision => 1 minute
 * @param millis Milliseconds to convert
 * @param maximumPrecision maximum number of time units in string
 */
export const millisToStringWithMaxPrecision = (millis: number, maximumPrecision?: number): string =>
  timeUnitsToString(truncateTimeUnits(splitMillisIntoTimeUnits(millis), maximumPrecision));

/**
 * Returns weeks, days, etc. to a set precision.
 * Removes leading insignificant digits, but will preserve trailing insignificant digits to the given precision
 *
 * ie, 3660001 ms, 2 precision => 1 hour 1 minute
 * ie, 60000 ms, 2 precision => 1 minute 0 seconds
 * ie, 59999 ms, 2 precision => 0 minutes 59 seconds
 * @param millis Milliseconds to convert
 * @param precision  precision to convert to
 */
export const millisToStringWithSetPrecision = (millis: number, precision: number): string => {
  const timeUnits = truncateTimeUnits(splitMillisIntoTimeUnits(millis), precision);
  const firstSigUnitIndex = getIndexOfFirstSignificantTimeUnit(timeUnits);

  const lastIndexToShow =
    firstSigUnitIndex + precision > INDEX_OF_LAST_SIGNIFICANT_TIME_UNIT
      ? INDEX_OF_LAST_SIGNIFICANT_TIME_UNIT
      : firstSigUnitIndex + precision;

  const remainingSignificantUnits =
    precision - (lastIndexToShow - firstSigUnitIndex + 1) < 0
      ? 0
      : precision - (lastIndexToShow - firstSigUnitIndex + 1);
  const firstIndexToShow =
    firstSigUnitIndex - remainingSignificantUnits < 0
      ? 0
      : firstSigUnitIndex - remainingSignificantUnits;
  const weeksString =
    TIME_UNIT_INDICES.get(TIME_UNITS.WEEKS) >= firstIndexToShow &&
    TIME_UNIT_INDICES.get(TIME_UNITS.WEEKS) <= lastIndexToShow
      ? `${timeUnits.weeks} week${timeUnits.weeks > 1 ? 's' : ''} `
      : '';

  const daysString =
    TIME_UNIT_INDICES.get(TIME_UNITS.DAYS) >= firstIndexToShow &&
    TIME_UNIT_INDICES.get(TIME_UNITS.DAYS) <= lastIndexToShow
      ? `${timeUnits.days} day${timeUnits.days > 1 ? 's' : ''} `
      : '';
  const hoursString =
    TIME_UNIT_INDICES.get(TIME_UNITS.HOURS) >= firstIndexToShow &&
    TIME_UNIT_INDICES.get(TIME_UNITS.HOURS) <= lastIndexToShow
      ? `${timeUnits.hours} hour${timeUnits.hours > 1 ? 's' : ''} `
      : '';
  const minutesString =
    TIME_UNIT_INDICES.get(TIME_UNITS.MINUTES) >= firstIndexToShow &&
    TIME_UNIT_INDICES.get(TIME_UNITS.MINUTES) <= lastIndexToShow
      ? `${timeUnits.minutes} minute${timeUnits.minutes > 1 ? 's' : ''} `
      : '';
  const secondsString =
    TIME_UNIT_INDICES.get(TIME_UNITS.SECONDS) >= firstIndexToShow &&
    TIME_UNIT_INDICES.get(TIME_UNITS.SECONDS) <= lastIndexToShow
      ? `${timeUnits.seconds} second${timeUnits.seconds > 1 ? 's' : ''} `
      : '';

  return weeksString + daysString + hoursString + minutesString + secondsString;
};

/**
 * Converts epoch seconds to time string in format of 'hh:mm:ss'
 * @param epochSeconds time in epoch seconds
 */
export function toHoursMinuteSeconds(epochSeconds: number): string {
  return moment.utc(new Date(epochSeconds * MILLISECONDS_IN_SECOND)).format('HH:mm:ss');
}

/**
 * Helper function to convert a Moment string and return epoch seconds as a number.
 * @param duration string of duration i.e. 'PT1.60S' returns 1.6 seconds
 * @returns a number
 */
export function getDurationTime(duration: string): number {
  // Using milliseconds since asSeconds loses precision
  return moment.duration(duration).asMilliseconds() / MILLISECONDS_IN_SECOND;
}

/**
 * Helper function to convert a Moment string and return epoch milliseconds as a number.
 * @param duration string of duration i.e. 'PT1.60S' returns 1600 milliseconds
 * @returns a number
 */
export function getDurationMilliTime(duration: string): number {
  return moment.duration(duration).asMilliseconds();
}

/**
 * Helper function to convert a Moment string and return hours as a number.
 * @param duration string of duration i.e. 'PT6H' returns 6
 * @returns a number
 */
export function millisToHours(millis: number): number {
  return millis / MILLIS_IN_HOUR;
}

/**
 * Helper function to format a seconds into duration format.
 * @param duration number of 1.6 seconds returns 'PT1.60S'
 * @returns a string
 */
export function setDurationTime(seconds: number): string {
  // Return formatted string
  return `PT${seconds}S`;
}

/**
 * Calculates what percentage of time remains for a timer.
 * @param timeEndMs the timestamp of the end
 * @param durationMs the timestamp of how long it runs
 */
export function calculatePercentTimeRemaining(timeEndMs: number, durationMs: number) {
  return 1 - ((Date.now() - timeEndMs) * -1) / durationMs;
}
