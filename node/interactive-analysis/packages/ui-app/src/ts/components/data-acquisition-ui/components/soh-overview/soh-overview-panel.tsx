import { SohStatusSummary } from '@gms/common-graphql/lib/graphql/soh/types';
import { ToolbarTypes } from '@gms/ui-core-components';
import React from 'react';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';
import { StationDeselectHandler } from '~components/data-acquisition-ui/shared/table/station-deselect-handler';
import { isSohStationStaleTimeMS } from '~components/data-acquisition-ui/shared/table/utils';
import {
  getLatestSohTime,
  initialFiltersToDisplay,
  SohToolbar
} from '~components/data-acquisition-ui/shared/toolbars/soh-toolbar';
import { SohOverviewContext } from './soh-overview-context';
import { StationGroupsLayout } from './station-groups/station-groups-layout';

/**
 * Panel creates the toolbar at the top of the display and calls the SOH Overview component
 * to render it's children.
 */
export const SohOverviewPanel: React.FunctionComponent = () => {
  const context = React.useContext(SohOverviewContext);

  const initialStationGroupsToDisplay = new Map<string, boolean>();
  context.stationGroupSoh.forEach(group =>
    initialStationGroupsToDisplay.set(group.stationGroupName, true)
  );
  const [stationGroupsToDisplay, setStationGroupsToDisplay] = React.useState(
    initialStationGroupsToDisplay
  );

  const [isHighlighted, setIsHighlighted] = React.useState(false);

  const [statusesToDisplay, setStatusesToDisplay] = React.useState(initialFiltersToDisplay);

  const highlightButtonRef = React.useRef(null);

  const statusToDisplayAsArray = [...statusesToDisplay.entries()]
    .map(([key, active]) => (active ? key : undefined))
    .filter(key => key !== undefined);

  const toggleHighlight = (ref: HTMLDivElement) => {
    setIsHighlighted(!isHighlighted);
    highlightButtonRef.current = ref ?? highlightButtonRef.current;
    highlightButtonRef.current?.classList?.toggle('isHighlighted');
  };

  const stationGroupFilterWidthPx = 200;
  const stationGroupsToDisplayCheckBoxDropdown: ToolbarTypes.CheckboxDropdownItem = {
    enumOfKeys: [...stationGroupsToDisplay.keys()],
    label: messageConfig.labels.sohToolbar.filterByStationGroup,
    menuLabel: messageConfig.labels.sohToolbar.filterByStationGroup,
    rank: 0,
    widthPx: stationGroupFilterWidthPx,
    type: ToolbarTypes.ToolbarItemType.CheckboxList,
    tooltip: messageConfig.tooltipMessages.sohToolbar.filerByStationGroup,
    values: stationGroupsToDisplay,
    onChange: setStationGroupsToDisplay,
    cyData: 'filter-by-station-group-soh'
  };

  return (
    <React.Fragment>
      <div className={'soh-overview-toolbar__container'} data-cy={'soh-overview-toolbar'}>
        <SohToolbar
          setStatusesToDisplay={statuses => {
            setStatusesToDisplay(statuses);
          }}
          statusesToDisplay={statusesToDisplay}
          toggleHighlight={toggleHighlight}
          leftItems={[stationGroupsToDisplayCheckBoxDropdown]}
          rightItems={[]}
          statusFilterText={messageConfig.labels.sohToolbar.filterStatuses}
          statusFilterTooltip={messageConfig.tooltipMessages.sohToolbar.selectStatuses}
          updatedAt={getLatestSohTime(context.stationSoh)}
          sohStationStaleTimeMS={context.sohStationStaleTimeMS}
          displayTimeWarning={isSohStationStaleTimeMS(
            getLatestSohTime(context.stationSoh),
            context.sohStationStaleTimeMS
          )}
          updateIntervalSecs={context.updateIntervalSecs}
        />
      </div>

      <StationDeselectHandler
        setSelectedStationIds={(ids: string[]) => context.setSelectedStationIds(ids)}
      >
        <StationGroupsLayout
          statusesToDisplay={statusToDisplayAsArray.map(s => SohStatusSummary[s])}
          isHighlighted={isHighlighted}
          stationGroupsToDisplay={[...stationGroupsToDisplay.keys()].filter(stationGroupName =>
            stationGroupsToDisplay.get(stationGroupName)
          )}
        />
      </StationDeselectHandler>
    </React.Fragment>
  );
};
