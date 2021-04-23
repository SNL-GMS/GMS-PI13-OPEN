import { uuid } from '@gms/common-util';
import React from 'react';
import { BaseDisplayContext } from '../../../../../src/ts/components/common-ui/components/base-display';
// tslint:disable-next-line: max-line-length
import { Columns } from '../../../../../src/ts/components/data-acquisition-ui/components/station-statistics/column-definitions';
import {
  StationStatisticsContext,
  StationStatisticsContextData
} from '../../../../../src/ts/components/data-acquisition-ui/components/station-statistics/station-statistics-context';
// tslint:disable-next-line: max-line-length
import { StationStatisticsPanel } from '../../../../../src/ts/components/data-acquisition-ui/components/station-statistics/station-statistics-panel';
import { stationAndStationGroupSohStatus } from '../../../../__data__/data-acquisition-ui/soh-overview-data';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

let idCount = 0;
uuid.asString = jest.fn().mockImplementation(() => ++idCount);

const contextValues: StationStatisticsContextData = {
  acknowledgeSohStatus: jest.fn(),
  selectedStationIds: ['H05N', 'H06N'],
  setSelectedStationIds: jest.fn(),
  quietTimerMs: 5000,
  updateIntervalSecs: 5,
  sohStationStaleTimeMS: 30000
};

describe('Station statistics class Panel', () => {
  // tslint:disable-next-line: no-magic-numbers
  Date.now = jest.fn().mockReturnValue(1573244087715);
  const sohPanel = Enzyme.mount(
    <StationStatisticsContext.Provider value={contextValues}>
      <BaseDisplayContext.Provider
        value={{
          glContainer: { width: 150, height: 150 } as any,
          widthPx: 150,
          heightPx: 150
        }}
      >
        <StationStatisticsPanel
          stationGroups={stationAndStationGroupSohStatus.stationGroups}
          stationSohs={stationAndStationGroupSohStatus.stationSoh}
          updateIntervalSecs={contextValues.updateIntervalSecs}
          setSelectedStationIds={jest.fn()}
          selectedStationIds={[]}
        />
      </BaseDisplayContext.Provider>
    </StationStatisticsContext.Provider>
  );

  it('should be defined', () => {
    expect(sohPanel).toBeDefined();
  });

  it('should match snapshot', () => {
    expect(sohPanel).toMatchSnapshot();
  });

  it('should have setIsHighlighted function', () => {
    sohPanel.instance().setIsHighlighted(true);
    expect(sohPanel.state().isHighlighted).toBe(true);
  });

  it('should have setGroupSelected function', () => {
    sohPanel.instance().setGroupSelected('test string');
    expect(sohPanel.state().groupSelected).toBe('test string');
  });

  it('should have setStatusesToDisplay function', () => {
    const myMap = new Map();
    sohPanel.instance().setStatusesToDisplay(myMap);
    expect(sohPanel.state().statusesToDisplay).toBe(myMap);
  });

  it('should have setColumnsToDisplay function', () => {
    const columnsToDisplay = new Map<Columns, boolean>();
    columnsToDisplay.set(Columns.ChannelLag, false);
    sohPanel.instance().setColumnsToDisplay(columnsToDisplay);
    expect(sohPanel.state().columnsToDisplay).toBe(columnsToDisplay);
  });

  it('should have toggleHighlight function', () => {
    sohPanel.instance().setIsHighlighted = jest.fn();
    sohPanel.instance().toggleHighlight({ ref: undefined });
    expect(sohPanel.instance().setIsHighlighted).toHaveBeenCalled();
  });
});
