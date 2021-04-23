import { Drawer, Position } from '@blueprintjs/core';
import { SohMonitorType } from '@gms/common-graphql/lib/graphql/soh/types';
import { CheckboxListEntry, SimpleCheckboxList } from '@gms/ui-core-components';
import { DistinctColorPalette } from '@gms/ui-util';
import Immutable from 'immutable';
import * as React from 'react';
import { useBaseDisplaySize } from '~components/common-ui/components/base-display/base-display-hooks';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';
import {
  HistoricalSohQuery,
  HistoricalSohQueryContext
} from '~components/data-acquisition-ui/react-apollo-components/historical-soh-query';
import { BarLineChartPanel } from '~components/data-acquisition-ui/shared/chart/bar-line-chart-panel';
import { DrillDownTitle } from '~components/data-acquisition-ui/shared/drill-down-components';
import { BaseToolbar } from '~components/data-acquisition-ui/shared/toolbars/base-toolbar';
import { useTrendTimeIntervalSelector } from '~components/data-acquisition-ui/shared/toolbars/trend-time-interval-hook';
import { TOOLBAR_HEIGHT_PX } from './constants';
import { validateNonIdealState } from './non-ideal-states';
import { useShowLegend } from './shared-hooks';
import { MissingLagHistoryPanelProps } from './types';
import { getChartHeight } from './utils';

/**
 * Map used for keeping track of what channels are visible in the charts
 * @param names string array of channel names
 */
export const useChannelVisibilityMap = (
  names: string[]
): [
  Immutable.Map<string, boolean>,
  React.Dispatch<React.SetStateAction<Immutable.Map<string, boolean>>>
] => {
  let initialChannelVisibilityMap = Immutable.Map<string, boolean>();
  names.forEach(name => {
    initialChannelVisibilityMap = initialChannelVisibilityMap.set(name, true);
  });
  const [channelVisibilityMap, setChannelVisibilityMap] = React.useState<
    Immutable.Map<string, boolean>
  >(initialChannelVisibilityMap);
  return [channelVisibilityMap, setChannelVisibilityMap];
};

/**
 * Renders the missing/lag history panel.
 * Depending on the props passed into this component it will either
 * render LAG or MISSING historical data.
 * @param props the props
 */
export const MissingLagHistoryPanel: React.FunctionComponent<MissingLagHistoryPanelProps> = props => {
  const drawerSizePx = 240;
  const [startTime, endTime, timeIntervalSelector] = useTrendTimeIntervalSelector(
    props.monitorType,
    props.sohHistoricalDurations
  );

  const channelNames = props.station.channelSohs.map(channel => channel.channelName);

  const [colorPalette] = React.useState<DistinctColorPalette>(
    new DistinctColorPalette(channelNames, props.station.stationName)
  );

  const [channelVisibilityMap, setChannelVisibilityMap] = useChannelVisibilityMap(channelNames);

  const onChange = (channelName: string) => {
    setChannelVisibilityMap(
      channelVisibilityMap.set(channelName, !channelVisibilityMap.get(channelName))
    );
  };

  const [legend, isLegendVisible, setShowLegend] = useShowLegend();

  const legendValueToColor = channelNames?.map(name => {
    const checkboxListEntry: CheckboxListEntry = {
      name,
      color: colorPalette.getColorString(name),
      isChecked: channelVisibilityMap.get(name)
    };
    return checkboxListEntry;
  });

  const [widthPx, baseHeightPx] = useBaseDisplaySize();
  const heightPx = getChartHeight(baseHeightPx - TOOLBAR_HEIGHT_PX);

  return (
    <React.Fragment>
      <BaseToolbar widthPx={widthPx} itemsLeft={[legend]} items={[timeIntervalSelector]} />
      <DrillDownTitle
        title={props.station.stationName}
        subtitle={
          props.monitorType === SohMonitorType.MISSING
            ? messageConfig.labels.missingTrendsSubtitle
            : props.monitorType === SohMonitorType.LAG
            ? messageConfig.labels.lagTrendsSubtitle
            : 'unknown monitor type'
        }
      />
      <HistoricalSohQuery
        stationName={props.station.stationName}
        startTime={startTime}
        endTime={endTime}
        sohMonitorType={props.monitorType}
      >
        <HistoricalSohQueryContext.Consumer>
          {context => {
            const nonIdealState = validateNonIdealState(
              props.monitorType,
              context.loading,
              context.data,
              startTime,
              endTime
            );
            if (nonIdealState) {
              return nonIdealState;
            }
            return (
              <React.Fragment>
                <BarLineChartPanel
                  legendTitle={props.station.stationName}
                  startTime={startTime}
                  endTime={endTime}
                  heightPx={heightPx}
                  widthPx={widthPx}
                  entryVisibilityMap={channelVisibilityMap}
                  colorPalette={colorPalette}
                  monitorType={props.monitorType}
                  station={props.station}
                  valueType={props.valueType}
                />
                <Drawer
                  className={'soh-legend'}
                  title={`${props.station.stationName} Channels`}
                  isOpen={isLegendVisible}
                  autoFocus={true}
                  canEscapeKeyClose={true}
                  canOutsideClickClose={true}
                  enforceFocus={false}
                  hasBackdrop={false}
                  position={Position.LEFT}
                  size={drawerSizePx}
                  onClose={() => setShowLegend(!isLegendVisible)}
                  usePortal={false}
                >
                  <SimpleCheckboxList
                    checkBoxListEntries={legendValueToColor}
                    onChange={onChange}
                  />
                </Drawer>
              </React.Fragment>
            );
          }}
        </HistoricalSohQueryContext.Consumer>
      </HistoricalSohQuery>
    </React.Fragment>
  );
};
