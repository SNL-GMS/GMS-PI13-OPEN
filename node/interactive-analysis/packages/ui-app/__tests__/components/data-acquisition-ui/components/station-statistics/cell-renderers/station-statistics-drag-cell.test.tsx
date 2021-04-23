import { uuid } from '@gms/common-util';
import { uniqueId } from 'lodash';
import React from 'react';
import {
  StationStatisticsDragCell,
  StationStatisticsDragCellProps
  // tslint:disable-next-line: max-line-length
} from '../../../../../../src/ts/components/data-acquisition-ui/components/station-statistics/cell-renderers/station-statistics-drag-cell';
import { StationStatisticsContext } from '../../../../../../src/ts/components/data-acquisition-ui/components/station-statistics/station-statistics-context';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
uuid.asString = jest.fn().mockImplementation(uniqueId);

describe('Soh Drag Cell', () => {
  const dragCellProps: StationStatisticsDragCellProps = {
    stationId: 'AAA'
  };

  const wrapper = Enzyme.mount(
    <StationStatisticsContext.Provider
      value={{
        updateIntervalSecs: 1,
        quietTimerMs: 1,
        selectedStationIds: ['AAA'],
        setSelectedStationIds: jest.fn(),
        acknowledgeSohStatus: jest.fn(),
        sohStationStaleTimeMS: 30000
      }}
    >
      <StationStatisticsDragCell {...dragCellProps} />
    </StationStatisticsContext.Provider>
  );

  it('is defined', () => {
    expect(wrapper).toBeDefined();
  });

  it('Matches snapshot', () => {
    expect(wrapper).toMatchSnapshot();
  });
});
