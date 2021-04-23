import {
  MILLIS_IN_DAY,
  MILLIS_IN_MINUTE,
  MILLIS_IN_WEEK,
  MILLISECONDS_IN_SECOND,
  millisToStringWithMaxPrecision,
  millisToStringWithSetPrecision,
  splitMillisIntoTimeUnits,
  timeUnitsToString,
  toOSDTime
} from '../../src/ts/util/time-util';

describe('Time utils', () => {
  const millis =
    MILLIS_IN_WEEK +
    MILLIS_IN_DAY +
    MILLIS_IN_MINUTE +
    // tslint:disable-next-line: no-magic-numbers
    MILLISECONDS_IN_SECOND * 30;

  test('Can split seconds', () => {
    const split = splitMillisIntoTimeUnits(millis);
    expect(split).toMatchSnapshot();
  });
  test('Can put time units into a high precision string', () => {
    const split = splitMillisIntoTimeUnits(millis);
    const asString = timeUnitsToString(split);
    expect(asString).toMatchSnapshot();
  });
  test('Can put time units into a fixed precision string', () => {
    const asString = millisToStringWithSetPrecision(millis, 2);
    expect(asString).toMatchSnapshot(asString);
  });
  test('Can put time units into a max precision string', () => {
    expect(millisToStringWithMaxPrecision(MILLIS_IN_MINUTE, 2));
  });

  test('Can convert to osd time', () => {
    const secondsInMinute = 60;
    expect(toOSDTime(secondsInMinute)).toMatchSnapshot();
  });
});
