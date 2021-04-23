import { uuid } from '@gms/common-util';
import React from 'react';
import { BaseDisplayContext } from '../../../../../src/ts/components/common-ui/components/base-display';
import {
  SohOverviewContext,
  SohOverviewContextData
} from '../../../../../src/ts/components/data-acquisition-ui/components/soh-overview/soh-overview-context';
import { SohOverviewPanel } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-overview/soh-overview-panel';
import { stationAndStationGroupSohStatus } from '../../../../__data__/data-acquisition-ui/soh-overview-data';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

let idCount = 0;
uuid.asString = jest.fn().mockImplementation(() => ++idCount);

// tslint:disable-next-line: deprecation
const lodash = require.requireActual('lodash');
lodash.uniqueId = () => '1';

const contextValues: SohOverviewContextData = {
  stationSoh: stationAndStationGroupSohStatus.stationSoh,
  stationGroupSoh: stationAndStationGroupSohStatus.stationGroups,
  acknowledgeSohStatus: jest.fn(),
  glContainer: undefined,
  quietTimerMs: 5000,
  updateIntervalSecs: 5,
  selectedStationIds: [],
  setSelectedStationIds: jest.fn(),
  sohStationStaleTimeMS: 30000
};

describe('Soh Panel', () => {
  // tslint:disable-next-line: no-magic-numbers
  Date.now = jest.fn().mockReturnValue(1573244087715);
  const setState = jest.fn();
  const useStateSpy = jest.spyOn(React, 'useState');
  const mock: any = init => [init, setState];
  useStateSpy.mockImplementation(mock);

  const sohPanel = Enzyme.mount(
    <SohOverviewContext.Provider value={contextValues}>
      <BaseDisplayContext.Provider
        value={{
          glContainer: { width: 150, height: 150 } as any,
          widthPx: 150,
          heightPx: 150
        }}
      >
        <SohOverviewPanel />
      </BaseDisplayContext.Provider>
    </SohOverviewContext.Provider>
  );

  it('should be defined', () => {
    expect(sohPanel).toBeDefined();
  });

  it('should match snapshot', () => {
    expect(sohPanel).toMatchSnapshot();
  });
});
