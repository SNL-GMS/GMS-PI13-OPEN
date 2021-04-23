import { IconNames } from '@blueprintjs/icons';
import { ToolbarTypes } from '@gms/ui-core-components';
import * as React from 'react';

/**
 * Creates a legend icon hook that keeps it's toggle state
 * @returns a toolbar button and booleans for if legend is visible.
 */
export const useShowLegend = (): [ToolbarTypes.ButtonItem, boolean, (value: boolean) => void] => {
  // custom legend state (boolean)
  // if true legend will show else it wont
  const [isLegendVisible, setShowLegend] = React.useState(false);
  const legend: ToolbarTypes.ButtonItem = {
    type: ToolbarTypes.ToolbarItemType.Button,
    rank: 1,
    tooltip: 'Shows legend for bar and line graph',
    widthPx: 8,
    label: '',
    onClick: () => {
      setShowLegend(!isLegendVisible);
    },
    icon: IconNames.SERIES_FILTERED
  };
  return [legend, isLegendVisible, setShowLegend];
};
