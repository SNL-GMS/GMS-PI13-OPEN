import { uuid } from '@gms/common-util';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import { PercentBar, PercentBarProps } from '@gms/ui-core-components';
import React from 'react';
import {
  formatSohValue,
  SohCellRendererProps,
  SohRollupCell
} from '../../../../../src/ts/components/data-acquisition-ui/shared/table/soh-cell-renderers';
import {
  CellStatus,
  DataReceivedStatus
} from '../../../../../src/ts/components/data-acquisition-ui/shared/table/utils';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

uuid.asString = jest.fn().mockReturnValue('1e872474-b19f-4325-9350-e217a6feddc0');

/**
 * Tests the ability to check if the peak trough is in warning
 */
describe('Station cell renderers', () => {
  // start formatSohDetailValue function tests
  test('formatSohDetailValue function can detect non-numbers', () => {
    expect(formatSohValue('NaN' as any, ValueType.PERCENTAGE, true)).toEqual('Unknown');
  });

  test('formatSohDetailValue function correctly formats input number', () => {
    const inputNumber = 999;
    expect(formatSohValue(inputNumber, ValueType.PERCENTAGE, true)).toEqual('999.0');
  });
  // end formatSohDetailValue function tests

  // start Percentage bar pure component tests
  const percentBarProps: PercentBarProps = {
    percentage: 10
  };
  const percentBar = Enzyme.mount(<PercentBar {...percentBarProps} />);
  test('PercentBar should be defined', () => {
    expect(percentBar).toBeDefined();
  });
  test('PercentBar should not have state', () => {
    expect(percentBar.state()).toEqual(null);
  });
  test('PercentBar should have props of type PercentBarProps', () => {
    expect(percentBar.props()).toEqual({ percentage: 10 });
  });
  test('PercentBar match snapshot', () => {
    expect(percentBar).toMatchSnapshot();
  });
  // end Percentage bar tests

  // start StationCellRenderer function component tests
  const stationCellRendererProps: SohCellRendererProps = {
    className: 'string',
    stationId: 'string',
    value: 'string',
    dataReceivedStatus: DataReceivedStatus.RECEIVED,
    cellStatus: CellStatus.GOOD
  };
  const stationCellRenderer = Enzyme.mount(<SohRollupCell {...stationCellRendererProps} />);
  test('stationCellRenderer should be defined', () => {
    expect(stationCellRenderer).toBeDefined();
  });
  test('stationCellRenderer should have props of type stationCellRendererProps', () => {
    expect(stationCellRenderer.props()).toMatchSnapshot();
  });
  test('stationCellRenderer should match snapshot', () => {
    expect(stationCellRenderer).toMatchSnapshot();
  });
  // end StationCellRenderer function component tests
});
