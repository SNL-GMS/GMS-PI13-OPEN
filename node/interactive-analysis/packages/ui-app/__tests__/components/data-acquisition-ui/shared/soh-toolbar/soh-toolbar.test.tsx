import { SohStatusSummary } from '@gms/common-graphql/lib/graphql/soh/types';
import React from 'react';
import { BaseDisplayContext } from '../../../../../src/ts/components/common-ui/components/base-display';
import {
  SohToolbar,
  SohToolbarProps
} from '../../../../../src/ts/components/data-acquisition-ui/shared/toolbars/soh-toolbar';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('Soh toolbar component', () => {
  const statusesToDisplay: Map<any, boolean> = new Map().set(SohStatusSummary.GOOD, true);
  const item: any = {
    dropdownOptions: {},
    value: {},
    custom: false,
    onChange: jest.fn()
  };
  const sohToolbarProps: SohToolbarProps = {
    statusesToDisplay,
    updateIntervalSecs: 1,
    updatedAt: 10000,
    widthPx: 1000,
    leftItems: [item],
    rightItems: [],
    statusFilterText: '',
    setStatusesToDisplay: jest.fn(),
    toggleHighlight: jest.fn(),
    sohStationStaleTimeMS: 30000,
    displayTimeWarning: false,
    statusFilterTooltip: 'test tooltip'
  };
  const sohToolbar = Enzyme.mount(
    <BaseDisplayContext.Provider
      value={{
        glContainer: { width: 150, height: 150 } as any,
        widthPx: 150,
        heightPx: 150
      }}
    >
      <SohToolbar {...sohToolbarProps} />
    </BaseDisplayContext.Provider>
  );

  it('should be defined', () => {
    expect(SohToolbar).toBeDefined();
  });

  it('should match snapshot', () => {
    expect(sohToolbar).toMatchSnapshot();
  });
});
