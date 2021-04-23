import { SohTypes } from '@gms/common-graphql';
import React from 'react';
// tslint:disable-next-line: max-line-length
import { ChannelCellBaseRenderer } from '../../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/cell-renderers/channel-cell-base-renderer';
// tslint:disable-next-line: max-line-length
import { EnvironmentalSoh } from '../../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('base environment cell', () => {
  const envSOH: EnvironmentalSoh = {
    value: 10,
    status: SohTypes.SohStatusSummary.GOOD,
    monitorTypes: SohTypes.SohMonitorType.MISSING,
    channelName: 'channelName',
    hasUnacknowledgedChanges: true,
    quietTimingInfo: {
      quietUntilMs: 100000000,
      quietDurationMs: 100000
    },
    isSelected: false,
    isContributing: true
  };
  const baseEnvironmentCell = Enzyme.mount(<ChannelCellBaseRenderer environmentSoh={envSOH} />);
  const baseEnvironmentCellNull = Enzyme.mount(<ChannelCellBaseRenderer environmentSoh={null} />);
  it('should be defined', () => {
    expect(ChannelCellBaseRenderer).toBeDefined();
  });
  it('should match snapshot', () => {
    expect(baseEnvironmentCell).toMatchSnapshot();
  });
  it('should match snapshot for null data', () => {
    expect(baseEnvironmentCellNull).toMatchSnapshot();
  });
});
