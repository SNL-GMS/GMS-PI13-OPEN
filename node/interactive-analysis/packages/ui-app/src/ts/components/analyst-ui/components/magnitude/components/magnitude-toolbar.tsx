import { Toolbar, ToolbarTypes } from '@gms/ui-core-components';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import React from 'react';
import { MagnitudeConfiguration } from './magnitude-configuration';

const MARGINS_FOR_TOOLBAR_PX = 16;

export interface MagnitudeToolbarProps {
  displayedMagnitudeTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes;
  widthPx: number;
  setDisplayedMagnitudeTypes(
    displayedMagnitudeTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes
  ): void;
}

/**
 * Generates the toolbar to be used in the magnitude display
 * @param props of type MagnitudeToolbarProps
 */
export const MagnitudeToolbar: React.FunctionComponent<MagnitudeToolbarProps> = props => {
  const dropdownContent = (
    <MagnitudeConfiguration
      displayedMagnitudeTypes={props.displayedMagnitudeTypes}
      setCategoryAndTypes={types => {
        props.setDisplayedMagnitudeTypes(types);
      }}
    />
  );
  const dropdownItem: ToolbarTypes.PopoverItem = {
    popoverContent: dropdownContent,
    label: 'Magnitude Configuration',
    rank: 1,
    widthPx: 204,
    tooltip: 'Configure the displayed magnitude types',
    type: ToolbarTypes.ToolbarItemType.Popover,
    onPopoverDismissed: () => {
      return;
    }
  };
  const toolBarItems: ToolbarTypes.ToolbarItem[] = [dropdownItem];
  return <Toolbar toolbarWidthPx={props.widthPx - MARGINS_FOR_TOOLBAR_PX} items={toolBarItems} />;
};
