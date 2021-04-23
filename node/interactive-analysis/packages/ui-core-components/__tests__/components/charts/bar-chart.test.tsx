import React from 'react';
import { BarChart } from '../../../src/ts/components/charts';
import { BarChartProps } from '../../../src/ts/components/charts/types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('BarChart', () => {
  const barChartProps: BarChartProps = {
    heightPx: 100,
    widthPx: 100,
    maxBarWidth: 150,
    minBarWidth: 25,
    categories: {
      x: ['1', '2', '3'],
      y: ['a', 'b', 'c']
    },
    barDefs: [
      { id: 'first', color: 'tomato', value: { x: 1, y: 1 } },
      { id: 'second', color: 'bisque', value: { x: 2, y: 2 } },
      { id: 'third', color: 'salmon', value: { x: 3, y: 3 } }
    ]
  };
  const mockBarChart = Enzyme.shallow(<BarChart {...barChartProps} />);
  it('is exported', () => {
    expect(BarChart).toBeDefined();
  });
  it('Renders', () => {
    expect(mockBarChart).toMatchSnapshot();
  });
});
