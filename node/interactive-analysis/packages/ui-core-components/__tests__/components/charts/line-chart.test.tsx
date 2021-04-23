import React from 'react';
import { LineChart } from '../../../src/ts/components/charts';
import { LineChartProps } from '../../../src/ts/components/charts/types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('LineChart', () => {
  const lineChartProps: LineChartProps = {
    heightPx: 100,
    widthPx: 100,
    lineDefs: [
      {
        id: 1,
        color: 'red',
        value: [
          { x: 1, y: 1 },
          { x: 2, y: 2 },
          { x: 3, y: 3 }
        ]
      }
    ] // single line
  };
  const mockLineChart = Enzyme.shallow(<LineChart {...lineChartProps} />);
  it('is exported', () => {
    expect(LineChart).toBeDefined();
  });
  it('Renders', () => {
    expect(mockLineChart).toMatchSnapshot();
  });
});
