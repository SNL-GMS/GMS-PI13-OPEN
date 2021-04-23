import React from 'react';
import { PopoverButton } from '../popover-button';
import * as Renderers from './toolbar-item-renderers';
import * as ToolbarTypes from './types';

export interface ToolbarItemRendererProps {
  item: ToolbarTypes.ToolbarItem;
  hasIssue: boolean;
  addToPopoverMap(rank: number, ref: PopoverButton): void;
}

/**
 * Selects the correct function to render a toolbar item
 */
export const ToolbarItemRenderer = (props: ToolbarItemRendererProps) => {
  const itemTypes = ToolbarTypes.ToolbarItemType;
  switch (props.item.type) {
    case itemTypes.Dropdown: {
      return Renderers.renderDropdown(props.item as ToolbarTypes.DropdownItem);
    }
    case itemTypes.NumericInput: {
      return Renderers.renderNumeric(props.item as ToolbarTypes.NumericInputItem);
    }
    case itemTypes.IntervalPicker: {
      return Renderers.renderIntervalPicker(props.item as ToolbarTypes.IntervalPickerItem);
    }
    case itemTypes.DateRangePicker: {
      return Renderers.renderDateRangePicker(props.item as ToolbarTypes.DateRangePickerItem);
    }
    case itemTypes.Popover: {
      return Renderers.renderPopoverButton(props.item as ToolbarTypes.PopoverItem, (key, val) =>
        props.addToPopoverMap(key, val)
      );
    }
    case itemTypes.Switch: {
      return Renderers.renderSwitch(props.item as ToolbarTypes.SwitchItem);
    }
    case itemTypes.Button: {
      return Renderers.renderButton(props.item as ToolbarTypes.ButtonItem);
    }
    case itemTypes.ButtonGroup: {
      return Renderers.renderButtonGroup(props.item as ToolbarTypes.ButtonGroupItem);
    }
    case itemTypes.LabelValue: {
      return Renderers.renderLabelValue(props.item as ToolbarTypes.LabelValueItem, props.hasIssue);
    }
    case itemTypes.CheckboxList:
      return Renderers.renderCheckboxDropdown(
        props.item as ToolbarTypes.CheckboxDropdownItem,
        (key, val) => props.addToPopoverMap(key, val)
      );
    case itemTypes.LoadingSpinner:
      return Renderers.renderLoadingSpinner(props.item as ToolbarTypes.LoadingSpinnerItem);
    case itemTypes.CustomItem:
      return (props.item as ToolbarTypes.CustomItem).element;
    default: {
      console.warn(`default error`);
      return <div />;
    }
  }
};
