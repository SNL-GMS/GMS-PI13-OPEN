import { SohTypes } from '@gms/common-graphql';
import { millisToStringWithMaxPrecision } from '@gms/common-util';
import { DateRangePickerTypes, ToolbarTypes } from '@gms/ui-core-components';
import { useInterval } from '@gms/ui-util';
import { min } from 'lodash';

export const ACEI = 'ACEI';

/** Represents a type of Data for the Interval Selector */
export type TYPE = SohTypes.SohMonitorType.LAG | SohTypes.SohMonitorType.MISSING | 'ACEI';

/** Defines a trend */
export interface Trend {
  /** the description of the trend ; i.e. `last 24 hours` */
  description: string;
  /** the number of milliseconds */
  value: number;
}

/** Returns based on the type provided */
export const getName = (type: TYPE): string => {
  if (type === SohTypes.SohMonitorType.LAG) {
    return 'Lag';
  }

  if (type === SohTypes.SohMonitorType.MISSING) {
    return 'Missing';
  }

  if (type === ACEI) {
    return 'Raw';
  }

  return '';
};

/** Returns the label based on the type provided */
export const getLabel = (type: TYPE): string => `${getName(type)} data`;

/** Returns the time interval selector tool tip text */
const getTimeIntervalSelectorTooltip = (): string => `History Interval`;

/** Returns the time interval selector label text */
const getTimeIntervalSelectorLabel = (): string => 'Set start and end times to display';

/**
 * Creates a custom React hook. This hook sets up and manages the state for
 * two toolbar items a drop down and an interval picker which work in sync with one another
 * for the missing and lag toolbar. The drop down component is used to select
 * one of the default missing or lag trends; and the interval picker is used to
 * set a custom or exact interval.
 * @param type the type of data (MISSING or LAG)
 * @return Returns the the drop down item, interval picker item, and the start, and
 * end times.
 */
export const useTrendTimeIntervalSelector = (
  type: TYPE,
  sohHistoricalDurations: number[]
): [Date, Date, ToolbarTypes.DateRangePickerItem] => {
  const now = new Date(Date.now());
  const defaultStart = new Date(now.getTime() - min(sohHistoricalDurations));

  /**
   * Defines the default trend constants for
   * populating the trend drop downs.
   */
  const defaultTrends: DateRangePickerTypes.Trend[] = sohHistoricalDurations.map(duration => ({
    description: `Last ${millisToStringWithMaxPrecision(duration, 2)}`,
    value: duration
  }));

  // internal interval state (two dates)
  // manages the internal state of the interval picker - this allows the user to be able to change
  // the interval without firing an event, i.e. querying the historical data
  const [internalStartTime, internalEndTime, setInternalInterval] = useInterval(defaultStart, now);

  // the interval state (two dates)
  // this is the `true` state of the interval picker - this is set when the apply button is pressed
  const [startTime, endTime, setInterval] = useInterval(defaultStart, now);

  const intervalSelector: ToolbarTypes.DateRangePickerItem = {
    label: getTimeIntervalSelectorTooltip(),
    tooltip: getTimeIntervalSelectorLabel(),
    type: ToolbarTypes.ToolbarItemType.DateRangePicker,
    rank: 1,
    startDate: internalStartTime,
    endDate: internalEndTime,
    defaultTrends,
    onChange: (start: Date, end: Date) => {
      setInternalInterval(start, end);
    },
    onApplyButton: (start: Date, end: Date) => {
      setInterval(start, end);
    }
  };

  return [startTime, endTime, intervalSelector];
};
