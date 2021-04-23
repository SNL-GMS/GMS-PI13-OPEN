import { But, Given, Then, When } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../../fixtures/common';
import { selectors } from '../../../../fixtures/query-selectors';
import * as SohCommon from '../../../../fixtures/soh/soh-display-all';
import * as StationStatisticsActions from '../../../../fixtures/soh/station-statistics';
import {
  selectConsecutiveRows,
  selectRows,
  sortTableInReverseAlphabeticalOrder,
  verifyLastRowIsSelected,
  verifyNoRowsAreSelected,
  verifyNumRowsSelected,
  verifyRowIsNotSelected,
  verifyRowIsSelected,
  verifyRowsAreNotSelected,
  verifyRowsAreSelected,
  verifySelectedCellIsFirstAlphabetically
} from '../../../../fixtures/soh/table/table-selection';

/** * * * * *
 * Givens
 */

Given('the first row is selected', () => {
  selectRows([0]);
});

Given('multiple, contiguous regions are selected', () => {
  selectRows([0, 2], Common.ModifierKeys.SHIFT);
});

Given('multiple, non-contiguous regions are selected', () => {
  selectRows([0, 1], Common.ModifierKeys.SHIFT);
  selectRows([3], Common.ModifierKeys.META);
});

Given('the selected station name is first in alphabetical order', () => {
  cy.get(selectors.stationStatisticsIsSelected)
    .should('have.length', StationStatisticsActions.cellsPerRow)
    .first()
    .then(verifySelectedCellIsFirstAlphabetically);
});

/**
 * opens the station statistics display and makes sure
 * that the data is loaded
 */
Given('the UI is opened to the Station Statistics Display', () => {
  Common.visitApp();
  SohCommon.openSOHDisplay(Common.SOHDisplays.STATION_STATISTICS);
  StationStatisticsActions.verifyDataIsLoaded();
});

/**
 * Single row selection
 */
When('the user clicks on the first row', StationStatisticsActions.clickOnRow);
Then('the first row is selected', verifyRowIsSelected);
Then('a single row should be selected', () => {
  cy.wait(Common.LONG_WAIT_TIME);
  verifyNumRowsSelected(1);
});
Then('the last row should be selected', verifyLastRowIsSelected);

/**
 * Multiple contiguous row selection
 */
When('the user shift+clicks to select the first three rows', () => selectConsecutiveRows(3));
Then('only the first three rows should be selected', () => {
  verifyRowsAreSelected([0, 1, 2]);
  verifyRowsAreNotSelected([3]);
});

/** Multiple non-contiguous row selection */
When('the user selects the first two and fourth rows', () => {
  selectRows([0, 1], Common.ModifierKeys.SHIFT);
  selectRows([3], Common.ModifierKeys.META);
});
Then('the first two and fourth rows are selected', () => {
  verifyRowsAreSelected([0, 1, 3]);
});
Then('the third and fifth rows are not selected', () => {
  verifyRowsAreNotSelected([2, 4]);
});

/** Deselection */
When(
  'the user clicks outside of the cells in the display',
  StationStatisticsActions.clickOutsideDisplay
);
Then('no rows should be selected', verifyNoRowsAreSelected);

When('the user meta-clicks on the first selected row', () => {
  selectRows([0], Common.ModifierKeys.META);
});
Then('the first row should not be selected', () => verifyRowIsNotSelected(0));
But('the remaining rows should still be selected', () => verifyRowsAreSelected([1, 3]));

/** Sorting */
Given('the stations are sorted in reverse alphabetical order', sortTableInReverseAlphabeticalOrder);
