import { UiStationSoh } from '@gms/common-graphql/lib/graphql/soh/types';
import {
  dateToSecondPrecision,
  MILLISECONDS_IN_SECOND,
  millisToStringWithMaxPrecision
} from '@gms/common-util';
import { ToolbarTypes } from '@gms/ui-core-components';
import React from 'react';
import { useBaseDisplaySize } from '~components/common-ui/components/base-display/base-display-hooks';
import {
  FilterableSOHTypes,
  FilterableSohTypesDisplayStrings
} from '~data-acquisition-ui/components/soh-overview/types';
import { dataAcquisitionUserPreferences } from '~data-acquisition-ui/config';
import { messageConfig } from '~data-acquisition-ui/config/message-config';
import { BaseToolbar } from './base-toolbar';

const statusFilterWidthPx = 200;

/** The initial statuses to show */
export const initialFiltersToDisplay = new Map<FilterableSOHTypes, boolean>([
  [FilterableSOHTypes.GOOD, true],
  [FilterableSOHTypes.BAD, true],
  [FilterableSOHTypes.MARGINAL, true],
  [FilterableSOHTypes.NONE, true]
]);

/**
 * Construct the element for displaying the last update time and update interval.
 * @param lastUpdated the date of last update
 * @returns the element
 */
export const updateAndTimeIntervalElement = dateToSecondPrecision;

export interface SohToolbarProps {
  statusesToDisplay: Map<any, boolean>;
  updateIntervalSecs: number;
  widthPx?: number;
  leftItems: ToolbarTypes.ToolbarItem[];
  rightItems: ToolbarTypes.ToolbarItem[];
  statusFilterText: string;
  statusFilterTooltip?: string;
  updatedAt?: number;
  displayTimeWarning?: boolean;
  sohStationStaleTimeMS: number;
  setStatusesToDisplay(statuses: Map<FilterableSOHTypes, boolean>): void;
  toggleHighlight(ref?: HTMLDivElement): void;
}

/**
 * Toolbar used in SOH components
 */
export const SohToolbar: React.FunctionComponent<SohToolbarProps> = props => {
  const { setStatusesToDisplay } = props;
  const [widthPx] = useBaseDisplaySize();

  const statusToDisplayCheckBoxDropdown: ToolbarTypes.CheckboxDropdownItem = {
    enumOfKeys: FilterableSOHTypes,
    label: props.statusFilterText,
    menuLabel: props.statusFilterText,
    rank: 0,
    widthPx: statusFilterWidthPx,
    type: ToolbarTypes.ToolbarItemType.CheckboxList,
    tooltip: props.statusFilterTooltip ? props.statusFilterTooltip : props.statusFilterText,
    values: props.statusesToDisplay,
    enumKeysToDisplayStrings: FilterableSohTypesDisplayStrings,
    onChange: setStatusesToDisplay,
    cyData: 'filter-soh',
    onPopUp: ref => {
      props.toggleHighlight(ref);
    },
    onPopoverDismissed: () => {
      props.toggleHighlight();
    },
    colors: new Map([
      [FilterableSOHTypes.GOOD, dataAcquisitionUserPreferences.colors.ok],
      [FilterableSOHTypes.MARGINAL, dataAcquisitionUserPreferences.colors.warning],
      [FilterableSOHTypes.BAD, dataAcquisitionUserPreferences.colors.strongWarning],
      [FilterableSOHTypes.NONE, 'NULL_CHECKBOX_COLOR_SWATCH']
    ])
  };

  const leftToolbarItemDefs: ToolbarTypes.ToolbarItem[] = [
    statusToDisplayCheckBoxDropdown,
    ...props.leftItems
  ];

  const updateIntervalDisplay: ToolbarTypes.LabelValueItem = {
    type: ToolbarTypes.ToolbarItemType.LabelValue,
    label: messageConfig.labels.sohToolbar.interval,
    tooltip: messageConfig.tooltipMessages.sohToolbar.interval,
    widthPx: 400,
    rank: 0,
    value: `${props.updateIntervalSecs} second${props.updateIntervalSecs !== 1 ? 's' : ''}`
  };

  const updateTimeDisplay: ToolbarTypes.LabelValueItem = {
    type: ToolbarTypes.ToolbarItemType.LabelValue,
    label: messageConfig.labels.sohToolbar.updateTimeDisplay,
    tooltip: messageConfig.tooltipMessages.sohToolbar.lastUpdateTime,
    tooltipForIssue: `SOH data has not updated in over ${millisToStringWithMaxPrecision(
      props.sohStationStaleTimeMS,
      1
    )}`,
    hasIssue: props.displayTimeWarning,
    widthPx: 400,
    rank: 0,
    style: { marginLeft: '1em' },
    value: props.updatedAt
      ? updateAndTimeIntervalElement(new Date(props.updatedAt * MILLISECONDS_IN_SECOND))
      : '-'
  };

  const rightToolbarItemDefs: ToolbarTypes.ToolbarItem[] = [
    ...props.rightItems,
    updateTimeDisplay,
    updateIntervalDisplay
  ];

  return (
    <BaseToolbar
      widthPx={props.widthPx ?? widthPx}
      items={rightToolbarItemDefs}
      itemsLeft={leftToolbarItemDefs}
    />
  );
};

/**
 * Finds the latest time field from a group of station soh objects
 */
export const getLatestSohTime = (stationSohs: UiStationSoh[]): number =>
  stationSohs && stationSohs.length > 0
    ? stationSohs
        .map(soh => (soh.time ? soh.time : 0))
        .reduce((accum, val) => (val > accum ? val : accum), 0)
    : 0;
