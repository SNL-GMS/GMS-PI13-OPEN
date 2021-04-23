import { BarChart, LineChart } from '@gms/ui-core-components';
import {
  BarChartProps,
  ChartPadding,
  LineChartProps
} from '@gms/ui-core-components/lib/components/charts/types';
import * as React from 'react';
import { dataAcquisitionUserPreferences } from '~components/data-acquisition-ui/config';

/** The Bar/line chart panel props */
export interface BarLineChartPanelProps {
  id: string;
  widthPx: number;
  heightPx: number;

  barChart?: BarChartProps;
  lineChart?: LineChartProps;
}

/**
 * Bar/line chart panel component - renders a bar chart and line chart
 */
export const BarAndLineChart: React.FunctionComponent<BarLineChartPanelProps> = props => {
  // define the chart padding for the two charts
  const padding: ChartPadding = { top: 16, right: 29, bottom: 120, left: 63 };

  const sharedProps = {
    classNames: 'table-display',
    padding,
    minHeightPx: dataAcquisitionUserPreferences.minChartHeightPx
  };

  return (
    <React.Fragment>
      <div className="legend-and-charts">
        <BarChart id={`bar-chart-${props.id}`} {...sharedProps} {...props?.barChart} />
        <LineChart id={`line-chart${props.id}`} {...sharedProps} {...props?.lineChart} />
      </div>
    </React.Fragment>
  );
};
