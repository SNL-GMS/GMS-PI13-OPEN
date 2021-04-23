import * as d3 from 'd3';
import { cloneDeep } from 'lodash';
import * as React from 'react';
import {
  VictoryAxis,
  VictoryBar,
  VictoryBrushContainer,
  VictoryChart,
  VictoryLine,
  VictoryZoomContainer
} from 'victory';
import { VictoryThemeDefinition } from 'victory-core';
import { Axis } from './axis';
import { BarChartProps } from './types';
import { GMSTheme } from './victory-themes';

/* border color */
const BORDER_COLOR = '#ffffff';

/* offset in pixels to account for the y axis labels */
const yAxisLabelsOffset = 38;
const brushBarsWidth = 10;
/* the scale padding for scale band calculation */
const scalePadding = 0.2;
const scrollWindowHeightPx = 110;
/** Determines whether to show the brush scrolling */
let isScrollingEnabled = false;
/** Padding used to caculate an optimal starting domain when brush scrolling enables */
const brushScrollDefaultPadding = 50;

/**
 * Determines optimal size for individual bars as a the brush increases and is set
 * @param scrollDomain x start and x end on brush
 * @param chartWidth width of the bar chart
 * @param barMaxWidth max bar width from props
 * @param barMinWidth min bar width from props
 * @returns a calculated barwidth
 */
const calculateScrollableBarWidth = (
  scrollDomain: { min: number; max: number },
  chartWidth: number,
  barMaxWidth: number,
  barMinWidth: number
): number => {
  const totalBarsShown = Math.round(scrollDomain.max - scrollDomain.min);
  const padding = 10;
  const barWidth = Math.round(chartWidth / totalBarsShown) - padding;
  if (barWidth >= barMaxWidth) {
    return barMaxWidth;
  }
  if (barWidth <= barMinWidth) {
    return barMinWidth;
  }
  return barWidth;
};

/**
 * Bar chart - renders a Victory bar chart component with axis
 */
export const BarChart: React.FunctionComponent<BarChartProps> = props => {
  const width = props.widthPx - props.padding?.right - props.padding?.left - yAxisLabelsOffset;
  const xAxisScale = d3
    .scaleBand()
    .domain(props.categories.x)
    .range([0, width])
    .padding(scalePadding);

  let barWidth =
    xAxisScale.bandwidth() > props.maxBarWidth ? props.maxBarWidth : xAxisScale.bandwidth();
  const [scrollDomain, setScrollDomain] = React.useState({
    min: 0,
    max: width / (barWidth + brushScrollDefaultPadding)
  });
  isScrollingEnabled = barWidth < props.minBarWidth;
  barWidth = isScrollingEnabled
    ? calculateScrollableBarWidth(scrollDomain, width, props.maxBarWidth, props.minBarWidth)
    : barWidth;
  const padding = (width - barWidth * props.categories.x.length) / (props.categories.x.length + 1);
  const xDomainPadding = barWidth / 2 + padding;

  /* Finds the max of the bad thresholds to ensure domain is above this line,
   if values are not already. Needed to prevent cutting off of threshold line. */
  const maxBadThreshold = props.thresholdsBad ? Math.max(...props.thresholdsBad) : undefined;

  // forcing an any type to incoming props.dataComponent because victory is
  // very picky about the type coming into the label component
  const DataComponent: any = props.dataComponent ? props.dataComponent : null;
  let height = Math.max(props.heightPx, props.minHeightPx ?? 0);
  height = isScrollingEnabled ? height - scrollWindowHeightPx : height;

  // Set custom victory theme for the brush container
  // mostly a copy of GMS Theme - but removes labels and ticks
  const GMSThemeBrushContainer: VictoryThemeDefinition = cloneDeep(GMSTheme);
  GMSThemeBrushContainer.independentAxis.style.tickLabels.opacity = 0;
  GMSThemeBrushContainer.independentAxis.style.tickLabels.fill = 'none';
  GMSThemeBrushContainer.independentAxis.style.ticks.stroke = 'none';
  return (
    <div className={`core-chart ${props.classNames ?? ''}`}>
      <VictoryChart
        key={props.id ?? 'victory-chart'}
        // This height is for the vector <svg> inside the victory container
        height={height}
        width={props.widthPx}
        animate={false}
        theme={GMSTheme}
        // This height is telling the parent victory <div> container to be a certain size
        style={{ parent: { height } }}
        padding={props.padding}
        containerComponent={
          isScrollingEnabled ? (
            <VictoryZoomContainer
              allowZoom={false}
              allowPan={false}
              zoomDomain={{ x: [scrollDomain.min, scrollDomain.max] }}
            />
          ) : (
            undefined
          )
        }
        domainPadding={{ x: xDomainPadding }}
      >
        {/*
          Hide the default axis for Victory. This is required because Victory does not
          recognize that our custom component `Axis` is adding the VictoryAxis component.
          The causes Victory to override our Axis and show only the default.
          A VictoryGroup could have been used but it is recommend to no be used with an axis.
          https://formidable.com/open-source/victory/docs/victory-group
        */}
        <VictoryAxis
          style={{
            axis: { stroke: 'none' },
            ticks: { stroke: 'none' },
            tickLabels: { fill: 'none' }
          }}
        />

        <Axis {...props} rotateAxis={true} />
        {props.barDefs.map((barDef, index) => (
          <VictoryBar
            events={
              props.onContextMenuBar
                ? [
                    {
                      target: 'data',
                      eventHandlers: {
                        onContextMenu: e => [
                          {
                            target: 'data',
                            mutation: passedProps => {
                              props.onContextMenuBar(e, passedProps);
                            }
                          }
                        ]
                      }
                    }
                  ]
                : undefined
            }
            key={index}
            alignment={'middle'}
            style={{
              data: {
                width: barWidth,
                fill: barDef.color
              },
              parent: { border: `1px solid ${BORDER_COLOR}` }
            }}
            data={[{ ...barDef.value, barWidth }]}
            barWidth={barWidth}
            // Unfortunately have to pass the labels props in order to render label component
            labels={DataComponent ? d => d : undefined}
            // note that the label component can only render a svg
            labelComponent={DataComponent ? <DataComponent /> : undefined}
          />
        ))}
        {props.thresholdsMarginal?.map((data, index) => (
          <VictoryLine
            key={index}
            style={{
              data: {
                stroke: '#be8c0b',
                strokeWidth: 2
              }
            }}
            data={[
              // tslint:disable-next-line
              { x: index + 1 - 0.5, y: data },
              // tslint:disable-next-line
              { x: index + 1 + 0.5, y: data }
            ]}
          />
        ))}

        {props.thresholdsBad?.map((data, index) => (
          <VictoryLine
            domain={props.thresholdsBad ? { y: [0, maxBadThreshold + 1] } : undefined}
            key={index}
            style={{
              data: {
                stroke: '#d24c4c',
                strokeWidth: 2
              }
            }}
            data={[
              // tslint:disable-next-line
              { x: index + 1 - 0.5, y: data },
              // tslint:disable-next-line
              { x: index + 1 + 0.5, y: data }
            ]}
          />
        ))}
      </VictoryChart>
      {/* Victory Chart for scroll container*/}
      {isScrollingEnabled ? (
        <VictoryChart
          width={props.widthPx}
          height={scrollWindowHeightPx}
          scale={{ x: 'linear' }}
          padding={{ top: 0, left: 25, right: 25, bottom: 30 }}
          theme={GMSThemeBrushContainer}
          containerComponent={
            <VictoryBrushContainer
              responsive={false}
              brushDimension="x"
              brushDomain={{ x: [scrollDomain.min, scrollDomain.max] }}
              brushStyle={{
                fill: `${props.scrollBrushColor ? props.scrollBrushColor : 'rgb(150, 150, 150)'}`,
                opacity: 0.3
              }}
              defaultBrushArea="move"
              onBrushDomainChange={e => {
                let scrollXDomainStart = e.x[0] as number;
                let scrollXDomainEnd = e.x[1] as number;
                // The two if below are insuring if at the edge of the brush, can go to the end and keep correct values
                if (scrollXDomainStart === 1) {
                  scrollXDomainStart = 0;
                  scrollXDomainEnd -= 1;
                }
                if (scrollXDomainEnd === props.barDefs.length) {
                  scrollXDomainStart += 1;
                  scrollXDomainEnd += 1;
                }
                setScrollDomain({ min: scrollXDomainStart, max: scrollXDomainEnd });
              }}
            />
          }
        >
          {/* Empty victory axis removes Y axis */}
          <VictoryAxis />
          {props.barDefs.map((barDef, index) => (
            <VictoryBar
              key={index}
              alignment={'start'}
              style={{
                data: {
                  width: brushBarsWidth,
                  fill: barDef.color
                },
                parent: { border: `1px solid ${BORDER_COLOR}` }
              }}
              data={[{ ...barDef.value, brushBarsWidth }]}
              barWidth={brushBarsWidth}
            />
          ))}
        </VictoryChart>
      ) : (
        undefined
      )}
    </div>
  );
};
