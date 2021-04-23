import { HTMLSelect } from '@blueprintjs/core';
import React from 'react';
import { WidgetTypes } from '..';
import { DropDownProps } from './types';

/**
 * Drop Down menu
 */
const UNSELECTABLE_CUSTOM_VALUE = 'UNSELECTED_CUSTOM_VALUE';

export class DropDown extends React.Component<DropDownProps, WidgetTypes.WidgetState> {
  private constructor(props) {
    super(props);
    this.state = {
      value: this.props.value,
      isValid: true
    };
  }
  /**
   * React component lifecycle.
   */
  public render() {
    const minWidth = `${this.props.widthPx}px`;
    const altStyle = {
      minWidth,
      width: minWidth
    };
    return (
      <HTMLSelect
        title={`${this.props.title}`}
        disabled={this.props.disabled}
        style={this.props.widthPx !== undefined ? altStyle : undefined}
        className={this.props.className}
        onChange={e => {
          const input = e.target.value;
          if (this.props.custom && input === UNSELECTABLE_CUSTOM_VALUE) {
            return;
          }
          this.props.onMaybeValue(input);
        }}
        data-cy={this.props['data-cy']}
        value={this.props.custom ? UNSELECTABLE_CUSTOM_VALUE : this.props.value}
      >
        {this.createDropdownItems(this.props.dropDownItems, this.props.dropdownText)}
        {this.props.custom ? (
          <option key={UNSELECTABLE_CUSTOM_VALUE} value={UNSELECTABLE_CUSTOM_VALUE}>
            Custom
          </option>
        ) : null}
      </HTMLSelect>
    );
  }

  /**
   * Creates the HTML for the dropdown items for the type input
   *
   */
  private readonly createDropdownItems = (enumOfOptions: any, dropdownText: any): JSX.Element[] => {
    const items: any[] = [];
    Object.keys(enumOfOptions).forEach(type => {
      items.push(
        <option key={type} value={enumOfOptions[type]}>
          {dropdownText ? dropdownText[type] : enumOfOptions[type]}
        </option>
      );
    });
    return items;
  }
}
