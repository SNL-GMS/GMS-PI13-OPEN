import { useElementSize } from '@gms/ui-util';
import * as React from 'react';
import { isSohStationStaleTimeMS } from '~components/data-acquisition-ui/shared/table/utils';
import { initialFiltersToDisplay } from '~components/data-acquisition-ui/shared/toolbars/soh-toolbar';
import { FilterableSOHTypes } from '../soh-overview/types';
import { BarChartPanel } from './bar-chart/bar-chart-panel';
import { getChannelSoh, useSortDropdown } from './bar-chart/bar-chart-utils';
import { Toolbar } from './soh-bar-chart-toolbar';
import { SohBarChartPanelProps } from './types';

const DEFAULT_TOP_HEIGHT_PX = 72;

export const SohBarChartPanel: React.FunctionComponent<SohBarChartPanelProps> = props => {
  const [sortDropdown, getSortFunction] = useSortDropdown(props.type);
  const [statusesToDisplay, setStatusesToDisplay] = React.useState(initialFiltersToDisplay);

  const channelSoh = getChannelSoh(props.type, props.station)
    .sort(getSortFunction())
    .filter(c => statusesToDisplay.get((c.status as unknown) as FilterableSOHTypes));

  const isStale = isSohStationStaleTimeMS(
    props.station.time,
    props.uiAnalystConfiguration.sohStationStaleTimeMS
  );

  const [headerRef] = useElementSize();

  const minHeight = Number(props.minHeightPx) + Number(DEFAULT_TOP_HEIGHT_PX);

  return (
    <div className="missing-wrapper" style={{ minHeight }}>
      {
        // !TODO CHANGE THE CLASS NAME ABOVE
      }
      <Toolbar
        statusesToDisplay={statusesToDisplay}
        setStatusesToDisplay={setStatusesToDisplay}
        sortDropdown={sortDropdown}
        forwardRef={headerRef}
        isStale={isStale}
        station={props.station}
        monitorType={props.type}
        uiAnalystConfiguration={props.uiAnalystConfiguration}
      />
      <BarChartPanel
        minHeightPx={props.minHeightPx}
        chartHeaderHeight={DEFAULT_TOP_HEIGHT_PX}
        type={props.type}
        sohStatus={props.sohStatus}
        station={props.station}
        channelSoh={channelSoh}
        uiAnalystConfiguration={props.uiAnalystConfiguration}
        quietChannelMonitorStatuses={props.quietChannelMonitorStatuses}
        valueType={props.valueType}
      />
    </div>
  );
};
