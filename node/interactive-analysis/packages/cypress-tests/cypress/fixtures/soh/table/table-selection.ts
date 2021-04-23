import * as Common from '../../common';
import { selectors } from '../../query-selectors';
import { cellsPerRow, clickOnRow, getCellInRow, getRow } from '../station-statistics';

/**
 * ----- Command -----
 * These functions interact with the UI, but do not verify the results
 * ie: clickThis, scrollThat, etc.
 */

export const selectRows = (indices: number[], key?: Common.ModifierKeys) => {
  for (const i of indices) {
    clickOnRow(i, {}, key);
  }
};

export const selectConsecutiveRows = (n = 0) => {
  selectRows([...Array(n).keys()], Common.ModifierKeys.SHIFT);
};

export const sortTableInReverseAlphabeticalOrder = () =>
  cy
    .get(selectors.stationColumnHeader)
    .first()
    .click({ force: true });

/**
 * ----- Verifiers -----
 * Verify some UI state
 * checkThis, confirmThat
 */

export const verifyLastRowIsSelected = () =>
  cy
    .get(selectors.stationStatisticsIsSelected)
    .last()
    .closest(selectors.tableContainer)
    .find(selectors.tableCell)
    .last()
    .closest(selectors.stationStatisticsIsSelected)
    .should(Common.cypressShouldOptions.EXIST);

export const verifyRowIsSelected = (n = 0) =>
  getRow(n)
    .closest(selectors.stationStatisticsIsSelected)
    .should(Common.cypressShouldOptions.EXIST);

export const verifyRowsAreSelected = (indices: number[]) => {
  for (const i of indices) {
    verifyRowIsSelected(i);
  }
};

export const verifyRowIsNotSelected = (n = 0) =>
  getCellInRow(n)
    .find(selectors.stationStatisticsIsSelected)
    .should(Common.cypressShouldOptions.NOT_EXIST);

export const verifyRowsAreNotSelected = (indices: number[]) => {
  for (const i of indices) {
    verifyRowIsNotSelected(i);
  }
};

export const verifyNumRowsSelected = (n: number) =>
  cy
    .get(selectors.stationStatisticsIsSelected)
    .should(Common.cypressShouldOptions.HAVE_LENGTH, n * cellsPerRow);

export const verifyNoRowsAreSelected = () =>
  cy.get(selectors.stationStatisticsIsSelected).should(Common.cypressShouldOptions.NOT_EXIST);

export const verifySelectedCellIsFirstAlphabetically = cellName =>
  cy.get(selectors.stationStatisticsTitleCell).each((t: any) => {
    assert(
      t.text().localeCompare(cellName.text()) >= 0,
      `selected cell should be first in alphabetical order, ${t.text()} vs ${cellName.text()}`
    );
  });
