import { ContextMenu, MenuItem } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import React from 'react';
import { CheckboxList } from '../checkbox-list/checkbox-list';
import { DateRangePicker } from '../date-range-picker';
import { IntervalPicker } from '../interval-picker';
import { renderNumeric } from './toolbar-item-renderers';
import * as ToolbarTypes from './types';

export interface ToolbarMenuItemRendererProps {
  item: ToolbarTypes.ToolbarItem;
  hasIssue: boolean;
  menuKey: string;
}

/**
 * Renders items for the toolbar overflow menu
 */
export const ToolbarMenuItemRenderer: React.FunctionComponent<ToolbarMenuItemRendererProps> = props => {
  const itemTypes = ToolbarTypes.ToolbarItemType;
  const item = props.item;
  const hasIssue = props.hasIssue;
  switch (props.item.type) {
    case itemTypes.NumericInput:
      const numericItem = item as ToolbarTypes.NumericInputItem;
      const renderedNumeric = renderNumeric(numericItem);
      return (
        <MenuItem text={numericItem.label} icon={numericItem.icon} key={props.menuKey}>
          {renderedNumeric}
        </MenuItem>
      );
    case itemTypes.IntervalPicker: {
      const intervalItem = item as ToolbarTypes.IntervalPickerItem;
      return (
        <MenuItem text={item.label} icon={item.icon} key={props.menuKey}>
          <IntervalPicker
            renderStacked={true}
            startDate={intervalItem.startDate}
            endDate={intervalItem.endDate}
            shortFormat={intervalItem.shortFormat}
            onNewInterval={(startDate, endDate) => intervalItem.onChange(startDate, endDate)}
            onApply={(startDate: Date, endDate: Date) => {
              intervalItem.onApplyButton(startDate, endDate);
              ContextMenu.hide();
            }}
            defaultIntervalInHours={intervalItem.defaultIntervalInHours}
          />
        </MenuItem>
      );
    }
    case itemTypes.DateRangePicker: {
      return (
        <MenuItem text={item.label} icon={item.icon} key={props.menuKey}>
          <div className={'date-range-picker__menu-popover'}>
            <DateRangePicker
              defaultTrends={(props.item as ToolbarTypes.DateRangePickerItem).defaultTrends}
              startDate={(props.item as ToolbarTypes.DateRangePickerItem).startDate}
              endDate={(props.item as ToolbarTypes.DateRangePickerItem).endDate}
              onNewInterval={(startDate, endDate) =>
                (props.item as ToolbarTypes.DateRangePickerItem).onChange(startDate, endDate)
              }
              onApply={(startDate: Date, endDate: Date) => {
                (props.item as ToolbarTypes.DateRangePickerItem).onApplyButton(startDate, endDate);
                ContextMenu.hide();
              }}
            />
          </div>
        </MenuItem>
      );
    }
    case itemTypes.Popover:
      const popoverItem = item as ToolbarTypes.PopoverItem;
      return (
        <MenuItem
          text={item.menuLabel ? item.menuLabel : item.label}
          icon={item.icon}
          key={props.menuKey}
        >
          {popoverItem.popoverContent}
        </MenuItem>
      );
    case itemTypes.Button:
      const buttonItem = item as ToolbarTypes.ButtonItem;
      return (
        <MenuItem
          text={item.label}
          icon={item.icon}
          disabled={buttonItem.disabled}
          onClick={e => buttonItem.onClick()}
          key={props.menuKey}
        />
      );
      break;
    case itemTypes.Switch:
      const switchItem = item as ToolbarTypes.SwitchItem;

      const label = item.menuLabel ? item.menuLabel : item.label;
      return (
        <MenuItem
          text={label}
          icon={item.icon}
          key={props.menuKey}
          onClick={e => switchItem.onChange(!switchItem.value)}
        />
      );
      break;
    case itemTypes.Dropdown:
      const dropdownItem = item as ToolbarTypes.DropdownItem;
      return (
        <MenuItem text={item.label} icon={item.icon} key={props.menuKey} disabled={item.disabled}>
          {dropdownItem.dropdownOptions
            ? Object.keys(dropdownItem.dropdownOptions).map(ekey => (
                <MenuItem
                  text={dropdownItem.dropdownOptions[ekey]}
                  key={ekey}
                  onClick={e => dropdownItem.onChange(dropdownItem.dropdownOptions[ekey])}
                  icon={
                    dropdownItem.value === dropdownItem.dropdownOptions[ekey]
                      ? IconNames.TICK
                      : undefined
                  }
                />
              ))
            : null}
        </MenuItem>
      );
      break;
    case itemTypes.ButtonGroup: {
      const buttonGroupItem = item as ToolbarTypes.ButtonGroupItem;
      return (
        <MenuItem text={item.label} icon={item.icon} key={props.menuKey} disabled={item.disabled}>
          {buttonGroupItem.buttons
            ? buttonGroupItem.buttons.map(button => (
                <MenuItem
                  text={button.label}
                  icon={button.icon}
                  key={button.label}
                  disabled={button.disabled}
                  onClick={e => {
                    button.onClick();
                  }}
                />
              ))
            : null}
        </MenuItem>
      );
    }
    case itemTypes.LabelValue: {
      const lvItem = item as ToolbarTypes.LabelValueItem;
      return (
        <MenuItem
          className={hasIssue ? 'toolbar-item--issue' : ''}
          title={hasIssue && item.tooltipForIssue ? item.tooltipForIssue : item.tooltip}
          key={props.menuKey}
          text={`${item.label}: ${lvItem.value}`}
        />
      );
    }
    case itemTypes.CheckboxList: {
      const cboxDropdown = item as ToolbarTypes.CheckboxDropdownItem;
      return (
        <MenuItem
          text={item.menuLabel ? item.menuLabel : item.label}
          icon={item.icon}
          key={props.menuKey}
        >
          <CheckboxList
            enumToCheckedMap={cboxDropdown.values}
            enumToColorMap={cboxDropdown.colors}
            checkboxEnum={cboxDropdown.enumOfKeys}
            onChange={value => cboxDropdown.onChange(value)}
          />
        </MenuItem>
      );
    }
    case itemTypes.LoadingSpinner: {
      const lsItem = item as ToolbarTypes.LoadingSpinnerItem;
      const displayString = `Loading ${lsItem.itemsToLoad} ${item.label}`;
      return <MenuItem key={props.menuKey} text={displayString} />;
    }
    default:
      // tslint:disable-next-line:no-console
      console.error('Invalid type for menu item');
      return <MenuItem />;
  }
};
