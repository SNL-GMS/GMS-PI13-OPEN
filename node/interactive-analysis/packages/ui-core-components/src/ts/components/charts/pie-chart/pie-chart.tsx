import { getDataAttributesFromProps } from '@gms/ui-util';
import * as React from 'react';
import { PieChartProps, PieChartStyle } from './types';

const PIE_CHART = 'pie-chart';
const SLICE = 'pie-slice';
const PIE_CHART_SLICE = `${PIE_CHART}__${SLICE}`;

const defaultBorderWidth = 2;

const getTotalPieChartDiameter = (radiusPx: number, borderPx: number) =>
  radiusPx * 2 + borderPx * 2;

const getRadiusWithBorder = (radiusPx: number, style: PieChartStyle) =>
  radiusPx + (style.borderPx ?? defaultBorderWidth);

const PieSlice: React.FunctionComponent<PieChartProps> = ({
  style,
  percent,
  className,
  pieSliceClass,
  ...rest
}) => {
  const pieRadiusPx = style.diameterPx / 2; // an inner circle at r/2
  const circumferenceOfInnerCirclePx = pieRadiusPx * Math.PI; // an inner circle at r/2
  const dataAttributes = getDataAttributesFromProps(rest);
  return (
    <circle
      className={`${PIE_CHART_SLICE} ${pieSliceClass ?? ''}`}
      r={pieRadiusPx / 2}
      cx={pieRadiusPx}
      cy={pieRadiusPx}
      strokeWidth={pieRadiusPx}
      strokeDasharray={`${circumferenceOfInnerCirclePx * percent} ${circumferenceOfInnerCirclePx} `}
      {...dataAttributes}
    />
  );
};

/**
 *
 * @param diameterPx the diameter, in px, of the indicator background
 * @param percent the percent, from 0 to 1.
 */
const CorePieChart: React.FunctionComponent<PieChartProps> = ({
  style,
  percent,
  className,
  pieSliceClass,
  ...rest
}) => {
  const radiusPx = style.diameterPx / 2;
  const borderRadiusPx = style.borderPx ?? defaultBorderWidth;
  const totalDiameterPx = getTotalPieChartDiameter(radiusPx, borderRadiusPx);
  const dataAttributes = getDataAttributesFromProps(rest);
  return (
    <svg
      className={`${PIE_CHART} ${className}`}
      width={`${totalDiameterPx}px`}
      height={`${totalDiameterPx}px`}
    >
      <g className={`${PIE_CHART}__container`}>
        <circle
          className={`
            ${PIE_CHART}
            ${className}`}
          r={getRadiusWithBorder(radiusPx, style)}
          cx={radiusPx}
          cy={radiusPx}
          {...dataAttributes}
        />
        <PieSlice
          style={style}
          percent={percent}
          pieSliceClass={pieSliceClass}
          {...dataAttributes}
        />
      </g>
    </svg>
  );
};

export const PieChart = React.memo(CorePieChart);
