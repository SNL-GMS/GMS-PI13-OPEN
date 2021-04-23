/// <reference types="Cypress" />
import * as Common from '../common';
import { SOHStrings } from '../descriptive-strings';
import { selectors } from '../query-selectors';

export const cellsPerRow = 3; // sometimes 4 depending on viewport

/**
 * ----- Command -----
 * These functions interact with the UI, but do not verify the results
 * ie: clickThis, scrollThat, etc.
 */

export const clickOnCell = (
  n = 0,
  options?: Partial<Cypress.ClickOptions>,
  modifierKey?: Common.ModifierKeys
) =>
  modifierKey
    ? Common.holdKey(modifierKey)
        .get(selectors.tableCell)
        .eq(n)
        .click({ force: true, ...options })
    : getCell(n).click({ force: true, ...options });

export const getRow = (n = 0) => cy.get(selectors.stationStatisticsRow).eq(n);
export const getRowName = (n = 0): Cypress.Chainable<string> =>
  cy.get(selectors.stationStatisticsRow).then((e: JQuery<HTMLElement>) => {
    if (n >= 0 && e.length > n) {
      return e[n].getAttribute(selectors.tableRowId);
    }
    throw new Error(`index ${n} out of row range`);
  });
export const clickOnRow = (
  n = 0,
  options?: Partial<Cypress.ClickOptions>,
  key?: Common.ModifierKeys
) => {
  key
    ? Common.holdKey(key)
        .get(selectors.stationStatisticsRow)
        .eq(n)
        .click({ force: true, ...options })
    : getRow(n).click({ force: true, ...options });
  Common.releaseHeldKeys();
};

export const clickOutsideDisplay = () =>
  cy
    .get(selectors.displayBackground)
    .eq(1)
    .click();

export const filterOutGood = () => {
  cy.get(`${selectors.sohToolbar} ${selectors.sohFilter} ${selectors.button}`)
    .click({ force: true })
    .get(selectors.checkbox.good)
    .click({ force: true });
};

export function getCell(n = 0) {
  return cy.get(selectors.tableCell).eq(n);
}

export const getCellInRow = (n = 0) => getCell(n * cellsPerRow);

export const selectGroupFromDropdown = () =>
  cy.get(selectors.groupDropdown).select(selectors.groupName);

/**
 * ----- Verifiers -----
 * Verify some UI state
 * checkThis, confirmThat
 */

export const verifyDataIsLoaded = () => {
  cy.get(selectors.tableCell).should(Common.cypressShouldOptions.HAVE_LENGTH_GREATER_THAN, 0);
};

/**
 * Get the number of acknowledged stations
 */
const retrieveNumberAcknowledged = () =>
  cy
    .get(`${selectors.goodAcknowledgedTable} ${selectors.sohNameCell}`)
    .its(Common.cypressItsOptions.LENGTH)
    .then(value => {
      // tslint:disable-next-line: no-console
      console.log(`number of stations acknowledged: ${String(value)}`);
      return value;
    });

/**
 * Right clicks to show the context menu for a row.
 * Assumes an unacknowledged SOH station exists
 */
const showContextMenu = () => {
  cy.root().click();
  cy.get(`${selectors.needsAttentionTable} ${selectors.tableCell}`)
    .first()
    .rightclick();
  cy.wait(Common.COMMON_WAIT_TIME_MS);
};

/**
 * Acknowledge without a comment.
 */
const selectAcknowledgeWithoutComment = () => cy.get(selectors.acknowledge.withoutComment).click();

/**
 * Acknowledge with a comment.
 */
const selectAcknowledgeWithComment = () => cy.get(selectors.acknowledge.withComment).click();

/**
 * Verifies the station acknowledge status.
 */
const validateNumberOfAcknowledged = (value: number) =>
  cy
    .get(`${selectors.goodAcknowledgedTable} ${selectors.sohNameCell}`)
    .should(Common.cypressShouldOptions.HAVE_LENGTH, value);

/**
 * Acknowledges a state of health
 * Assumes a BAD unacknowledged SOH exists
 */
export const acknowledge = () => {
  // acknowledge one immediately; ensure that there is one in the acknowledged block
  // there is no concept of a catch with cypress, thus we must ensure that at least
  // one acknowledged item is in the DOM at the start of the test
  showContextMenu();
  selectAcknowledgeWithoutComment();
  cy.wait(Common.LONG_WAIT_TIME);

  retrieveNumberAcknowledged().then((value: number) => {
    showContextMenu();
    selectAcknowledgeWithoutComment();
    cy.wait(Common.LONG_WAIT_TIME);
    validateNumberOfAcknowledged(value + 1);
  });

  cy.wait(Common.LONG_WAIT_TIME);

  retrieveNumberAcknowledged().then(value => {
    showContextMenu();
    selectAcknowledgeWithComment();
    cy.get(selectors.acknowledge.cancel).click();
    cy.wait(Common.LONG_WAIT_TIME);
    validateNumberOfAcknowledged(value);
  });

  cy.wait(Common.LONG_WAIT_TIME);

  retrieveNumberAcknowledged().then((value: number) => {
    showContextMenu();
    selectAcknowledgeWithComment();
    cy.get(selectors.acknowledge.commentTextArea).type(SOHStrings.ACKNOWLEDGING_WITH_CYPRESS);
    cy.get(selectors.acknowledge.submit).click();
    cy.wait(Common.LONG_WAIT_TIME);
    validateNumberOfAcknowledged(value + 1);
  });
};
