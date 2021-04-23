import { Given, Then } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../../fixtures/common';
import { selectors } from '../../../../fixtures/query-selectors';
import * as SohCommon from '../../../../fixtures/soh/soh-display-all';
import * as Actions from '../../../../fixtures/soh/station-statistics';

let prevNumberOfCells = -1;
const cellQuery = `${selectors.needsAttentionTable} ${selectors.tableCell}`;
const findStartingNumberOfCells = cells => (prevNumberOfCells = cells.length);

const prepareTestConditions = () => {
  cy.get(cellQuery).then(findStartingNumberOfCells);
  Actions.selectGroupFromDropdown();
};

const ensureGoodIsFilteredOut = () => {
  cy.get(
    '.station-statistics-table-container--acknowledged  .station-statistics__cell--title--good'
  ).should(Common.cypressShouldOptions.NOT_EXIST);
};

/**
 * opens the station statistics display and makes sure
 * that the data is loaded
 */
Given('the UI is opened to the Station Statistics Display', () => {
  Common.visitApp();
  SohCommon.openSOHDisplay(Common.SOHDisplays.STATION_STATISTICS);
  Actions.verifyDataIsLoaded();
});

Given('the user can filter out all but one group', prepareTestConditions);

Then('the number of cells in the acknowledged bin should decrease', () => {
  cy.get(cellQuery).should(Common.cypressShouldOptions.HAVE_LENGTH_LESS_THAN, prevNumberOfCells);
});

Given('the user can filter the SOH display', Actions.filterOutGood);

Then('good selection should disappear', ensureGoodIsFilteredOut);
