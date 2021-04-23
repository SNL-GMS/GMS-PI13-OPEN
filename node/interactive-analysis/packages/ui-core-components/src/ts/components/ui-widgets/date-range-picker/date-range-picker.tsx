/*
 * Allows the selection of a time range
 * consisting of a start and end time as input
 * on a single widget.
 * Protects against cases where startTime > endTime
 * Basically a <DateRangeInput /> with an apply button
 */
import { Button, Classes, Label } from '@blueprintjs/core';
import { DateRange, DateRangeInput, TimePrecision } from '@blueprintjs/datetime';
import { dateToSecondPrecision } from '@gms/common-util';
import React from 'react';
import { DateRangePickerProps, DateRangePickerState, Trend } from './types';

export class GMSDateRangePicker extends React.Component<
  DateRangePickerProps,
  DateRangePickerState
> {
  private readonly dateTimePickerLabel: string = 'Time Range';

  /*
    A constructor
    */
  private constructor(props: DateRangePickerProps) {
    super(props);
    this.state = {
      showDatePicker: false,
      selectedDates: [props.startDate, props.endDate]
    };
  }
  /**
   * React component lifecycle.
   */
  public render() {
    return (
      <div className={`date-range-picker`}>
        <Label className={`${Classes.LABEL} ${Classes.INLINE} date-range-picker__label`}>
          {`${this.dateTimePickerLabel}`}
        </Label>
        <DateRangeInput
          className={'date-range-picker__range-input'}
          formatDate={dateToSecondPrecision}
          parseDate={str => new Date(str)}
          value={this.state.selectedDates}
          onChange={this.onChange}
          shortcuts={
            this.props.defaultTrends
              ? this.props.defaultTrends.map((item: Trend) => ({
                  dateRange: [
                    new Date(new Date().setMilliseconds(new Date().getMilliseconds() - item.value)),
                    new Date()
                  ],
                  includeTime: true,
                  label: item.description
                }))
              : false
          }
          closeOnSelection={false}
          singleMonthOnly={false}
          allowSingleDayRange={true}
          timePrecision={TimePrecision.MINUTE}
          popoverProps={{
            minimal: true
          }}
          dayPickerProps={{
            className: 'date-range-picker--column'
          }}
          timePickerProps={{
            precision: TimePrecision.MINUTE,
            showArrowButtons: true,
            useAmPm: false
          }}
        />
        <div>
          <Button className={'date-range-picker__apply-button'} onClick={this.onApply}>
            Apply
          </Button>
        </div>
      </div>
    );
  }

  private readonly onChange = (selectedDates: DateRange) => {
    this.setState({ selectedDates }, () =>
      this.props.onNewInterval(this.state.selectedDates[0], this.state.selectedDates[1])
    );
  }

  private readonly onApply = () => {
    if (this.props.onApply) {
      this.props.onNewInterval(this.state.selectedDates[0], this.state.selectedDates[1]);
      this.props.onApply(this.state.selectedDates[0], this.state.selectedDates[1]);
    }
  }
}
