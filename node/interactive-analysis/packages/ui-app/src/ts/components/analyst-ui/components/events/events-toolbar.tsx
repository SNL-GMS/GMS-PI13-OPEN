import { Toolbar, ToolbarTypes } from '@gms/ui-core-components';
import React from 'react';
import { userPreferences } from '~analyst-ui/config';
import { EventFilters, eventFilterToColorMap } from './types';

export interface EventsToolbarProps {
  completeEventsCount: number;
  eventsInTable: number;
  canSaveEvent: boolean;
  widthPx: number;
  showEventOfType: Map<EventFilters, boolean>;
  disableMarkSelectedComplete: boolean;
  saveAll(): void;
  saveCurrentlyOpenEvent(): void;
  handleMarkSelectedComplete(): void;
  setSelectionOnAll(set: boolean): void;
  onFilterChecked(val: any): void;
}

export const EventsToolbar: React.FunctionComponent<EventsToolbarProps> = (
  props: EventsToolbarProps
) => {
  const toolbarLeftItems: ToolbarTypes.ToolbarItem[] = [];
  const completedEvents: ToolbarTypes.LabelValueItem = {
    rank: 1,
    tooltip: 'Number of completed events',
    label: 'Completed',
    type: ToolbarTypes.ToolbarItemType.LabelValue,
    value: props.completeEventsCount.toString()
  };
  toolbarLeftItems.push(completedEvents);
  const eventsRemaining: ToolbarTypes.LabelValueItem = {
    rank: 2,
    tooltip: 'Number of events to work',
    label: 'Remaining',
    type: ToolbarTypes.ToolbarItemType.LabelValue,
    value: (props.eventsInTable - props.completeEventsCount).toString()
  };
  toolbarLeftItems.push(eventsRemaining);
  const toolbarItems: ToolbarTypes.ToolbarItem[] = [];
  const saveAll: ToolbarTypes.ButtonItem = {
    rank: 1,
    tooltip: 'Saves all events and signal detections in interval',
    label: 'Save All',
    cyData: 'save-all-events',
    widthPx: 71,
    type: ToolbarTypes.ToolbarItemType.Button,
    onClick: () => {
      props.saveAll();
    }
  };
  toolbarItems.push(saveAll);

  const saveEvent: ToolbarTypes.ButtonItem = {
    rank: 2,
    tooltip: 'Saves the currently open event',
    label: 'Save Open',
    cyData: 'save-open-event',
    widthPx: 89,
    type: ToolbarTypes.ToolbarItemType.Button,
    onClick: () => {
      props.saveCurrentlyOpenEvent();
    },
    disabled: props.canSaveEvent
  };
  toolbarItems.push(saveEvent);
  const markComplete: ToolbarTypes.ButtonItem = {
    rank: 3,
    cyData: 'mark-open-complete',
    tooltip: 'Mark selected events complete',
    label: 'Mark Complete',
    widthPx: 119,
    type: ToolbarTypes.ToolbarItemType.Button,
    onClick: () => {
      props.handleMarkSelectedComplete();
    },
    disabled: props.disableMarkSelectedComplete
  };
  toolbarItems.push(markComplete);
  const selectAll: ToolbarTypes.ButtonGroupItem = {
    rank: 4,
    tooltip: 'Select or deselect all events',
    label: 'Selection',
    type: ToolbarTypes.ToolbarItemType.ButtonGroup,
    buttons: [
      {
        label: 'Deselect All',
        tooltip: 'Deselects all items in table',
        type: ToolbarTypes.ToolbarItemType.Button,
        rank: 3,
        widthPx: 98,
        onClick: () => {
          props.setSelectionOnAll(false);
        }
      },
      {
        label: 'Select All',
        tooltip: 'Selects all items in table',
        type: ToolbarTypes.ToolbarItemType.Button,
        rank: 5,
        widthPx: 81,
        onClick: () => {
          props.setSelectionOnAll(true);
        }
      }
    ]
  };
  toolbarItems.push(selectAll);

  const eventsToShow: ToolbarTypes.CheckboxDropdownItem = {
    rank: 5,
    tooltip: 'Select which types of events to show',
    label: 'Show Events',
    type: ToolbarTypes.ToolbarItemType.CheckboxList,
    enumOfKeys: EventFilters,
    colors: eventFilterToColorMap,
    values: props.showEventOfType,
    widthPx: 126,
    onChange: value => {
      props.onFilterChecked(value);
    }
  };
  toolbarItems.push(eventsToShow);

  return (
    <Toolbar
      items={toolbarItems}
      itemsLeft={toolbarLeftItems}
      // ! TODO DO NOT USE `props.glContainer.width` TO CALCULATING WIDTH - COMPONENT MAY NOT BE INSIDE GL
      toolbarWidthPx={props.widthPx}
      minWhiteSpacePx={userPreferences.list.minWidthPx}
    />
  );
};
