import { SohTypes } from '@gms/common-graphql';
import { Point } from '@gms/ui-util';

// tslint:disable: no-magic-numbers
export const EVERY_20_SECONDS_FOR_A_MINUTE = 3;
export const EVERY_20_SECONDS_FOR_AN_HOUR = EVERY_20_SECONDS_FOR_A_MINUTE * 60;
export const EVERY_20_SECONDS_FOR_A_DAY = EVERY_20_SECONDS_FOR_AN_HOUR * 24;
export const EVERY_20_SECONDS_FOR_A_WEEK = EVERY_20_SECONDS_FOR_A_DAY * 7;
export const EVERY_20_SECONDS_FOR_A_MONTH = EVERY_20_SECONDS_FOR_A_DAY * 30;
// tslint:enable: no-magic-numbers

/** defines a line */
export type Line = Point[];

export interface LineDefinition {
  id: string | number;
  color: string;
  value: Line;
}

export interface BarDefinition {
  id: string | number;
  color: string;
  value: BarValue;
}

export interface BarValue extends Point {
  quietUntilMs?: number;
  quietDurationMs?: number;
  channelStatus?: SohTypes.SohStatusSummary;
}

/** defines a domain tuple */
export type DomainTuple = [number, number];

/** defines a domain */
export interface Domain {
  x: DomainTuple;
  y: DomainTuple;
}

/** defines the chart padding */
export interface ChartPadding {
  top: number;
  left: number;
  bottom: number;
  right: number;
}

/** The base chart props */
export interface BaseChartProps {
  id?: string | number;
  classNames?: string;
  widthPx: number;
  heightPx: number;
  minHeightPx?: number;
  padding?: ChartPadding;
  domain?: Domain;
}

/** The axis props */
export interface AxisProps {
  rotateAxis?: boolean;
  suppressYAxis?: boolean;
  suppressXAxis?: boolean;
  yAxisLabel?: string;
  xAxisLabel?: string;
  yTickCount?: number;
  xTickCount?: number;
  xTickValues?: string[] | number[];
  yTickValues?: string[] | number[];
  xTickTooltips?: string[];
  yTickTooltips?: string[];
  barDefs?: BarDefinition[];
  disabled?: Disabled;
  onContextMenuBar?(e: any, datum: any): void;
  onContextMenuBarLabel?(e: any, index: number): void;
  yTickFormat?(value: string | number): string | number;
  xTickFormat?(value: string | number): string | number;
}

/** The line chart props */
export interface LineChartProps extends BaseChartProps, AxisProps {
  lineDefs: LineDefinition[];
  interpolation?: any;
  stepChart?: boolean;
  suppressScrolling?: boolean;
  zoomDomain?: Domain;
  domain?: Domain;
  onZoomDomainChange?(domain: Domain): void;
}

/** The bar chart props */
export interface BarChartProps extends BaseChartProps, AxisProps {
  maxBarWidth: number;
  minBarWidth: number;
  categories: { x: string[]; y: string[] };
  barDefs: BarDefinition[];
  scrollBrushColor?: string;
  thresholdsMarginal?: number[];
  thresholdsBad?: number[];
  disabled?: Disabled;
  dataComponent?: React.Component;
}

interface Disabled {
  xTicks: {
    disabledColor: string;
    disabledCondition(tick: { value: number; tooltip: string }): boolean;
  };
}
