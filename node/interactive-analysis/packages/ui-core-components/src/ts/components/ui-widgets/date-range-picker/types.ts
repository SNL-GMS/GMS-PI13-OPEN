import { DateRange, TimePrecision } from '@blueprintjs/datetime';

/** Defines a trend */
export interface Trend {
  /** the description of the trend ... i.e. `last 24 hours` */
  description: string;
  /** the number of milliseconds */
  value: number;
}

// tslint:disable-next-line:no-empty-interface
export interface DateRangePickerProps {
  startDate: Date;
  endDate: Date;
  defaultTrends: Trend[];
  renderStacked?: boolean;
  onNewInterval(startDate: Date, endDate: Date);
  onApply?(startDate: Date, endDate: Date);
}
export interface DateRangePickerState {
  allowSingleDayRange?: boolean;
  singleMonthOnly?: boolean;
  contiguousCalendarMonths?: boolean;
  dateRange?: DateRange;
  maxDateIndex?: number;
  minDateIndex?: number;
  reverseMonthAndYearMenus?: boolean;
  shortcuts?: boolean;
  timePrecision?: TimePrecision;
  selectedDates: DateRange;
  showDatePicker: boolean;
}
