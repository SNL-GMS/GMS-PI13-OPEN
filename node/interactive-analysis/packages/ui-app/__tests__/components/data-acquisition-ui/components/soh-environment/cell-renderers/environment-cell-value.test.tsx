import React from 'react';
import { EnvironmentCellValue } from '../../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/cell-renderers/environment-cell-value';
// tslint:disable-next-line: max-line-length
import { EnvironmentCellValueProps } from '../../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/cell-renderers/types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe("environment cell renderer's exports", () => {
  const myPropsNeedsAttention: EnvironmentCellValueProps = {
    hasUnacknowledgedChanges: true,
    value: 999
  };
  const myProps: EnvironmentCellValueProps = {
    hasUnacknowledgedChanges: false,
    value: 999
  };
  const environmentCellValue = Enzyme.mount(<EnvironmentCellValue {...myProps} />);
  const environmentCellValueNeedsAttention = Enzyme.mount(
    <EnvironmentCellValue {...myPropsNeedsAttention} />
  );
  it('should be defined', () => {
    expect(EnvironmentCellValue).toBeDefined();
  });
  it('should match snapshot', () => {
    expect(environmentCellValue).toMatchSnapshot();
  });
  it('should match snapshot with needs attention', () => {
    expect(environmentCellValueNeedsAttention).toMatchSnapshot();
  });
});
