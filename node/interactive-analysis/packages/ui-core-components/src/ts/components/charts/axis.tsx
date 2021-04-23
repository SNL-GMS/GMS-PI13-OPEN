import Immutable from 'immutable';
import * as React from 'react';
import { VictoryAxis, VictoryLabel } from 'victory';
import { AxisProps } from './types';
import { baseLabelStyles } from './victory-themes';

/**
 * Custom label for the axis tic marks.
 * Adds the ability to add tooltips to the tic labels.
 */
const Label: React.FunctionComponent<{
  // the `any` types are used because that is what Victory expects
  index?: any;
  x?: any;
  y?: any;
  text?: any;
  verticalAnchor?: any;
  textAnchor?: any;
  angle?: any;
  transform?: any;
  style?: any;
  events?: any;
  dx?: any;
  dy?: any;
  tooltips?: string[];
  xLabelInfoMap?: Immutable.Map<string, { isDisabled: boolean; tooltip: string }>;
  disabledColor?: string;
  onContextMenuBarLabel?(e: any, index: number): void;
}> = props => {
  const labelInfo =
    props.xLabelInfoMap && props.xLabelInfoMap.has(props.text)
      ? props.xLabelInfoMap.get(props.text)
      : undefined;
  return (
    <VictoryLabel
      events={{ onContextMenu: e => props.onContextMenuBarLabel(e, props.index) }}
      title={`${
        labelInfo
          ? labelInfo.tooltip
          : props.tooltips && props.tooltips[props.index]
          ? props.tooltips[props.index]
          : props.text
      }`}
      {...(props as any)}
      style={{
        ...props.style,
        // Overriding props with disabled fill
        fill: labelInfo && labelInfo.isDisabled ? props.disabledColor : props.style.fill
      }}
    />
  );
};

/**
 * A component that renders the Victory Axis for a Victory Chart.
 */
export const Axis: React.FunctionComponent<AxisProps> = props => {
  /* x-axis tic padding in pixels */
  // tslint:disable-next-line: no-magic-numbers
  const xAxisTicPaddingX = props.rotateAxis ? -25 : 0;
  // tslint:disable-next-line: no-magic-numbers
  const xAxisTicPaddingY = props.rotateAxis ? -10 : 0;

  /* y-axis tic padding in pixels */
  const yAxisTicPaddingX = 6;
  const yAxisTicPaddingY = 0;
  const xTicks: { value: number; tooltip: string }[] = [];
  let labelInfoMap = Immutable.Map<string, { isDisabled: boolean; tooltip: string }>();
  if (props.barDefs) {
    props.barDefs.forEach((barDef, index) => {
      xTicks.push({
        value: barDef.value.y,
        tooltip: props.xTickTooltips ? props.xTickTooltips[index] : undefined
      });
    });
    const isDisabledList = xTicks.map(tick =>
      props.disabled && props.disabled.xTicks
        ? props.disabled.xTicks.disabledCondition(tick)
        : false
    );

    props.barDefs.forEach((barDef, index) => {
      labelInfoMap = labelInfoMap.set(
        props.xTickFormat ? (props.xTickFormat(barDef.id) as string) : (barDef.id as string),
        {
          isDisabled: isDisabledList[index],
          tooltip: props.xTickTooltips ? props.xTickTooltips[index] : undefined
        }
      );
    });
  }
  return (
    <React.Fragment>
      {!props.suppressXAxis ? (
        <VictoryAxis
          {...props}
          dependentAxis={false}
          label={props.xAxisLabel}
          tickCount={props.xTickCount}
          tickValues={props.xTickValues}
          tickFormat={value => (props.xTickFormat ? props.xTickFormat(value) : value)}
          tickLabelComponent={
            <Label
              // tslint:disable-next-line: no-unbound-method
              onContextMenuBarLabel={props.onContextMenuBarLabel}
              dx={xAxisTicPaddingX}
              dy={xAxisTicPaddingY}
              disabledColor={props.disabled?.xTicks?.disabledColor}
              xLabelInfoMap={labelInfoMap}
            />
          }
          style={{
            axisLabel: {
              ...baseLabelStyles,
              // tslint:disable-next-line: no-magic-numbers
              padding: props.rotateAxis ? 82 : 50
            },
            tickLabels: {
              // tslint:disable-next-line: no-magic-numbers
              angle: props.rotateAxis ? -65 : 0,
              fontSize: 10
            }
          }}
        />
      ) : (
        undefined
      )}

      {!props.suppressYAxis ? (
        <VictoryAxis
          {...props}
          dependentAxis={true}
          label={props.yAxisLabel}
          tickCount={props.yTickCount}
          tickValues={props.yTickValues}
          tickFormat={value => (props.yTickFormat ? props.yTickFormat(value) : value)}
          tickLabelComponent={
            <Label tooltips={props.yTickTooltips} dx={yAxisTicPaddingX} dy={yAxisTicPaddingY} />
          }
          style={{
            axisLabel: {
              ...baseLabelStyles,
              padding: 40
            },
            tickLabels: {
              fontSize: 10
            }
          }}
        />
      ) : (
        undefined
      )}
    </React.Fragment>
  );
};
