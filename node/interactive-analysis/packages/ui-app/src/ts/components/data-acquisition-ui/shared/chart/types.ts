import { ChartTypes } from '@gms/ui-core-components';

export interface BarLineChartData {
  categories: {
    x: string[];
    y: string[];
  };
  lineDefs: ChartTypes.LineDefinition[];
  barDefs: ChartTypes.BarDefinition[];
  thresholdsMarginal: number[];
  thresholdsBad: number[];
}
