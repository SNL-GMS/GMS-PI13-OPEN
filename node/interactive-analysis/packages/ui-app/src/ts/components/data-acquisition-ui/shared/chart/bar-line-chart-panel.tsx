import { SohTypes } from '@gms/common-graphql';
import { setDecimalPrecisionAsNumber } from '@gms/common-util';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import { nonIdealStateWithNoSpinner } from '@gms/ui-core-components';
import { BarDefinition, LineDefinition } from '@gms/ui-core-components/lib/components/charts/types';
import { DistinctColorPalette } from '@gms/ui-util';
import Immutable from 'immutable';
import * as React from 'react';
import {
  MAX_BAR_CHART_WIDTH,
  MIN_BAR_CHART_WIDTH,
  TOOLBAR_HEIGHT_PX
} from '~components/data-acquisition-ui/components/missing-lag-history/constants';
import { MISSING_LAG } from '~components/data-acquisition-ui/components/missing-lag-history/types';
import {
  barChartXAxisLabel,
  barChartXAxisTicFormat,
  barChartYAxisLabel,
  barChartYAxisTicFormat,
  getChartData,
  lineChartXAxisLabel,
  lineChartXAxisTicFormat,
  lineChartYAxisLabel,
  lineChartYAxisTicFormat
} from '~components/data-acquisition-ui/components/missing-lag-history/utils';
// tslint:disable-next-line: max-line-length
import { HistoricalSohQueryContext } from '~components/data-acquisition-ui/react-apollo-components/historical-soh-query';
import { gmsColors } from '~scss-config/color-preferences';
import { BarAndLineChart } from './bar-and-line-chart';
import { BarLineChartData } from './types';

/**
 * Dimensions, station data, and props specific
 * to the charts.
 */
export interface BarLineChartPanelProps {
  legendTitle: string;
  startTime: Date;
  endTime: Date;
  heightPx: number;
  widthPx: number;
  entryVisibilityMap: Immutable.Map<string, boolean>;
  colorPalette: DistinctColorPalette;
  monitorType: MISSING_LAG;
  station: SohTypes.UiStationSoh;
  valueType: ValueType;
}

/**
 * Manages the bar line chart data. Updates it only if the time range has changed.
 * @param monitorType Lag or missing
 * @param station the station for which to build the chart data
 * @param colorPalette the color palette to choose from
 * @returns the BarLineChartData for the given station, monitor type and time range.
 */
export const useBarLineChartData = (
  monitorType: MISSING_LAG,
  station: SohTypes.UiStationSoh,
  colorPalette: DistinctColorPalette,
  valueType: ValueType
) => {
  const context = React.useContext(HistoricalSohQueryContext);
  const [chartData, setChartData] = React.useState<BarLineChartData>(undefined);

  // Check if data has changed
  React.useEffect(() => {
    // prepare and get the chart data from the historical data
    setChartData(getChartData(monitorType, station, context, colorPalette, valueType));
  }, [
    context.data?.calculationTimes[0],
    context.data?.calculationTimes[context.data?.calculationTimes.length - 1]
  ]);

  return chartData;
};

/**
 * Bar/line chart panel component -
 * vertically stacked bar and line charts, with appropriate padding.
 * Provides the charts with a color palette based on the channel name
 */
export const BarLineChartPanel: React.FunctionComponent<BarLineChartPanelProps> = props => {
  // calculate the height of each chart
  const heightPx = (props.heightPx - TOOLBAR_HEIGHT_PX) / 2;

  const chartData = useBarLineChartData(
    props.monitorType,
    props.station,
    props.colorPalette,
    props.valueType
  );

  // There is a condition where the chart can be undefined the first render after the query
  // returns. If this is the case return immediately with an empty non-ideal state.
  // This occurs for a fraction of a second.
  if (!chartData) {
    return nonIdealStateWithNoSpinner();
  }

  // ! TODO: sorting should be done at the display level
  const lineDefs =
    chartData?.lineDefs
      .filter((lineDef: LineDefinition) => props.entryVisibilityMap.get(lineDef.id.toString()))
      .sort((a, b) => a.id.toString().localeCompare(b.id.toString())) ?? [];
  const barDefs =
    chartData?.barDefs
      .filter((barDef: BarDefinition) => props.entryVisibilityMap.get(barDef.id.toString()))
      .sort((a, b) => a.id.toString().localeCompare(b.id.toString())) ?? [];

  if (barDefs.length === 0) {
    return nonIdealStateWithNoSpinner('No data to display', 'Data is filtered out');
  }
  return (
    <BarAndLineChart
      id={props.station.stationName}
      widthPx={props.widthPx}
      heightPx={props.heightPx * 2}
      barChart={{
        widthPx: props.widthPx,
        heightPx,
        maxBarWidth: MAX_BAR_CHART_WIDTH,
        minBarWidth: MIN_BAR_CHART_WIDTH,
        scrollBrushColor: gmsColors.gmsMain,
        categories: chartData ? chartData.categories : { x: [], y: [] },
        barDefs,
        yAxisLabel: barChartYAxisLabel(props.monitorType),
        xAxisLabel: barChartXAxisLabel(),
        xTickTooltips: barDefs.map(bar => `${setDecimalPrecisionAsNumber(bar.value.y)}`),
        yTickFormat: barChartYAxisTicFormat,
        xTickFormat: barChartXAxisTicFormat(props.station.stationName),
        thresholdsMarginal: chartData ? chartData.thresholdsMarginal : [],
        thresholdsBad: chartData ? chartData.thresholdsBad : []
      }}
      lineChart={{
        widthPx: props.widthPx,
        heightPx,
        lineDefs,
        yAxisLabel: lineChartYAxisLabel(props.monitorType),
        xAxisLabel: lineChartXAxisLabel(),
        yTickFormat: lineChartYAxisTicFormat,
        xTickFormat: lineChartXAxisTicFormat
      }}
    />
  );
};
