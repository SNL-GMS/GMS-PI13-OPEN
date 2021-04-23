import React from 'react';
// tslint:disable-next-line: max-line-length
import { Columns } from '../../../../../src/ts/components/data-acquisition-ui/components/station-statistics/column-definitions';
import { StationStatisticsContext } from '../../../../../src/ts/components/data-acquisition-ui/components/station-statistics/station-statistics-context';
// tslint:disable-next-line: max-line-length
import { StationStatisticsTable } from '../../../../../src/ts/components/data-acquisition-ui/components/station-statistics/station-statistics-table';
import { tableData } from '../../../../__data__/data-acquisition-ui/station-statistics-data';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

describe('Station Statistics Table', () => {
  const wrapper = Enzyme.mount(
    <StationStatisticsContext.Provider
      value={{
        updateIntervalSecs: 1,
        selectedStationIds: ['station2'],
        setSelectedStationIds: jest.fn(),
        acknowledgeSohStatus: jest.fn(),
        sohStationStaleTimeMS: 30000,
        quietTimerMs: 50
      }}
    >
      <StationStatisticsTable tableData={tableData} id="station-statistics" />
    </StationStatisticsContext.Provider>
  );
  it('is defined', () => {
    expect(wrapper).toBeDefined();
  });

  it('should have updateColumnVisibility function', async () => {
    const ensureGridApiHasBeenSet = async w =>
      new Promise(function(resolve, reject) {
        (function waitForGridReady() {
          if (w.tableRef && w.tableRef.getColumnApi()) {
            resolve(w);
            return;
          }
          // tslint:disable-next-line: no-magic-numbers
          setTimeout(waitForGridReady, 100);
        })();
      });

    // wait for the ag-grid to be ready
    await ensureGridApiHasBeenSet(wrapper.instance());
    const columnsToDisplay = new Map<Columns, boolean>();
    columnsToDisplay.set(Columns.ChannelLag, false);
    wrapper.instance().updateColumnVisibility(columnsToDisplay);
    expect(
      wrapper
        .find('AgGridReact')
        .instance()
        .columnApi.getColumn(Columns.ChannelLag.toString())
        .isVisible()
    ).toBe(false);
  });
});
