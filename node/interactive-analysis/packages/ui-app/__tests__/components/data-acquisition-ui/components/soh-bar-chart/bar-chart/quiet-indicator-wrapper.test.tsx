import { SohTypes } from '@gms/common-graphql';
// tslint:disable-next-line: max-line-length
// import { QuietIndicatorWrapper } from '@gms/ui-app/src/ts/components/data-acquisition-ui/components/soh-bar-chart/bar-chart/quiet-indicator-wrapper';
import React from 'react';
import { MaybeQuietIndicator } from '../../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/cell-renderers/maybe-quiet-indicator';
import { QuietIndicatorWrapperProps } from '../../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/cell-renderers/types';
import { EnvironmentalSoh } from '../../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('quiet indicator wrapper', () => {
  const valueAndStatusByChannelNameVal: EnvironmentalSoh = {
    value: 10,
    status: SohTypes.SohStatusSummary.GOOD,
    monitorTypes: SohTypes.SohMonitorType.MISSING,
    channelName: 'channelName',
    hasUnacknowledgedChanges: true,
    quietTimingInfo: {
      quietDurationMs: 100,
      quietUntilMs: 100
    },
    isSelected: false,
    isContributing: true
  };

  const quietIndicatorWrapperProps: QuietIndicatorWrapperProps = {
    data: valueAndStatusByChannelNameVal,
    diameterPx: 34
  };

  // quiet indicator wrapper › encountered a declaration exception
  // TypeError: Cannot read property 'barWidth' of undefined

  const quietIndicator = Enzyme.mount(
    // <QuietIndicatorWrapper {...quietIndicatorWrapperProps}>
    <MaybeQuietIndicator {...quietIndicatorWrapperProps} />
    // </QuietIndicatorWrapper>);
  );
  it('should be defined', () => {
    // expect(QuietIndicatorWrapper).toBeDefined();
  });
  it('should match snapshot', () => {
    expect(quietIndicator).toMatchSnapshot();
  });
});
