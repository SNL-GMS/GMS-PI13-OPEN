import { Point } from '@gms/ui-util';
import flatMap from 'lodash/flatMap';
import max from 'lodash/max';
import min from 'lodash/min';
import * as React from 'react';
import { VictoryAxis, VictoryChart, VictoryZoomContainer } from 'victory';
import { Axis } from './axis';
import { Lines } from './line';
import { Domain, LineChartProps } from './types';
import { GMSTheme } from './victory-themes';

/**
 * Sets the y domain but checks against the locally set min y domain.
 * If the domain is less it makes the domain at least min.
 * @param values input values to create domain for
 *
 * @returns minimum and maximum domain taking into account the min
 */
function setYDomain(values: number[]): { minimum: number; maximum: number } {
  const MIN_Y_DOMAIN = 0.25;
  const yMin: number = min(values);
  const yMax: number = max(values);
  const currentDomainSpread = yMax - yMin;
  const potentialDomainPadding = (MIN_Y_DOMAIN - currentDomainSpread) / 2;
  const domain =
    currentDomainSpread > MIN_Y_DOMAIN
      ? [yMin, yMax]
      : [yMin - potentialDomainPadding, yMax + potentialDomainPadding];
  return {
    minimum: domain[0],
    maximum: domain[1]
  };
}

/**
 * Line chart - renders a Victory line chart component with axis
 */
export const LineChart: React.FunctionComponent<LineChartProps> = props => {
  const allPoints: Point[] = props.domain ? [] : flatMap(props.lineDefs.map(def => def.value));
  const xValues = props.domain ? [] : allPoints.map<number>(l => l.x as number);
  const yValues = props.domain ? [] : allPoints.map<number>(l => l.y);
  const [allowZoom, setAllowZoom] = React.useState(false);

  const yDomain = setYDomain(yValues);

  const [domain] = props.domain
    ? [props.domain]
    : React.useState<Domain>({
        x: [min(xValues), max(xValues)],
        y: [yDomain.minimum, yDomain.maximum]
      });
  const [zoomDomain] = props.zoomDomain
    ? [props.zoomDomain]
    : React.useState<Domain>({
        x: [min(xValues), max(xValues)],
        y: [yDomain.minimum, yDomain.maximum]
      });

  const keyListener = (event: KeyboardEvent) => {
    setAllowZoom(event.metaKey || event.ctrlKey);
  };

  const heightPx = Math.max(props.heightPx, props.minHeightPx ?? 0);

  return (
    <div className={`core-chart ${props.classNames ?? ''}`}>
      <VictoryChart
        key={props.id ?? 'victory-chart'}
        height={heightPx}
        width={props.widthPx}
        domain={domain}
        animate={false}
        theme={GMSTheme}
        padding={props.padding}
        containerComponent={
          !props.suppressScrolling ? (
            <VictoryZoomContainer
              zoomDimension="x"
              zoomDomain={zoomDomain}
              allowZoom={allowZoom}
              // tslint:disable-next-line: no-unbound-method
              onZoomDomainChange={props.onZoomDomainChange}
            />
          ) : (
            undefined
          )
        }
        events={[
          {
            target: 'parent',
            eventHandlers: {
              onMouseEnter: () => {
                document.body.addEventListener('keydown', keyListener);
                document.body.addEventListener('keyup', keyListener);

                return [];
              },
              onMouseLeave: () => {
                document.body.removeEventListener('keydown', keyListener);
                document.body.removeEventListener('keyup', keyListener);
                return [];
              }
            }
          }
        ]}
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

        <Axis {...props} />
        <Lines {...props} domain={domain} />
      </VictoryChart>
    </div>
  );
};
