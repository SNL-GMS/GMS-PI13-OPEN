import React from 'react';
import { DropDown } from '../drop-down';
import { FilterableOptionList } from '../filterable-option-list';
import { IntervalPicker } from '../interval-picker';
import { TextArea } from '../text-area';
import { TimePicker } from '../time-picker';
import * as WidgetTypes from './types';

/**
 * Widget component.
 */
export class Widget extends React.Component<WidgetTypes.WidgetProps, WidgetTypes.WidgetState> {
  private constructor(props) {
    super(props);
    this.state = {
      isValid: this.props.isValid !== undefined ? this.props.isValid : true,
      value: this.props.defaultValue
    };
  }

  /**
   * React component lifecycle.
   */
  public render() {
    switch (this.props.type) {
      case 'DropDown':
        return (
          <DropDown
            value={this.props.defaultValue}
            dropDownItems={this.props.params.dropDownItems}
            dropdownText={this.props.params.dropdownText}
            onMaybeValue={this.props.onMaybeValue}
            title={this.props.params.tooltip ? this.props.params.tooltip : ''}
          />
        );
      case 'TextArea':
        return (
          <TextArea
            maxChar={this.props.params.maxChar ? this.props.params.maxChar : undefined}
            defaultValue={this.props.defaultValue}
            onMaybeValue={this.props.onMaybeValue}
            data-cy={this.props['data-cy']}
            title={this.props.params.tooltip ? this.props.params.tooltip : ''}
          />
        );
      case 'TimePicker':
        return (
          <TimePicker
            date={this.props.defaultValue}
            datePickerEnabled={true}
            onMaybeDate={this.props.onMaybeValue}
            setHold={this.props.onValidStatus}
            hasHold={!this.state.isValid}
          />
        );
        break;
      case 'IntervalPicker':
        return (
          <IntervalPicker
            startDate={this.props.defaultValue.startDate}
            endDate={this.props.defaultValue.endDate}
            onNewInterval={this.props.onMaybeValue}
            onInvalidInterval={
              this.props.params && this.props.params.onInvalidInterval
                ? this.props.params.onInvalidInterval
                : undefined
            }
            onApply={
              this.props.params && this.props.params.onApply ? this.props.params.onApply : undefined
            }
            defaultIntervalInHours={this.props.params && this.props.params.defaultIntervalInHours}
          />
        );
        break;
      case 'FilterableOptionList':
        return (
          <FilterableOptionList
            options={this.props.params.options}
            prioriotyOptions={this.props.params.prioriotyOptions}
            defaultSelection={this.props.defaultValue}
            defaultFilter={this.props.params.defaultFilter}
            onSelection={this.props.onMaybeValue}
            // tslint:disable-next-line:no-magic-numbers
            widthPx={280}
          />
        );
        break;
      default:
    }

    return {};
  }
}
