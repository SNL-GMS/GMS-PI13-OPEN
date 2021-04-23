import {
  BarChartWrapper,
  BarChartWrapperProps
} from '@gms/ui-app/src/ts/components/data-acquisition-ui/components/soh-bar-chart/bar-chart/bar-chart-wrapper';
import { BarChart } from '@gms/ui-core-components';
import { BarChartProps } from '@gms/ui-core-components/lib/components/charts/types';
import React from 'react';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('Bar Chart Wrapper', () => {
  const barChartProps: BarChartProps = {
    heightPx: 100,
    widthPx: 100,
    maxBarWidth: 25,
    minBarWidth: 5,
    categories: {
      x: ['1', '2', '3'],
      y: ['a', 'b', 'c']
    },
    barDefs: [
      { id: 'first', color: 'tomato', value: { x: 1, y: 1 } },
      { id: 'second', color: 'bisque', value: { x: 2, y: 2 } },
      { id: 'third', color: 'salmon', value: { x: 3, y: 3 } }
    ],
    xTickTooltips: ['one', 'two', 'three']
  };

  const barChartWrapperProps: BarChartWrapperProps = {
    id: '1a',
    widthPx: 100,
    heightPx: 100,
    barChartProps
  };
  const barChart = Enzyme.mount(
    <BarChartWrapper {...barChartWrapperProps}>
      <BarChart {...barChartProps} />
    </BarChartWrapper>
  );

  it('should be defined', () => {
    expect(BarChartWrapper).toBeDefined();
  });
  it('should match snapshot', () => {
    expect(barChart).toMatchSnapshot();
  });
});
