/// <reference types="Cypress" />
// tslint:disable: no-magic-numbers
import { SohTypes } from '@gms/common-graphql';
import * as Common from '../common';
import { SOHStrings } from '../descriptive-strings';
import { selectors } from '../query-selectors';

/** The Station Group Name to use for testing */
const GROUP_NAME = 'All_1';

/**
 * ----- Command -----
 * These functions interact with the UI, but do not verify the results
 * ie: clickThis, scrollThat, etc.
 */

export const getTitleOfFirstSelectedStationInOverview = () =>
  cy
    .get(selectors.overviewIsSelected)
    .first()
    .invoke(Common.cypressInvokeOptions.TEXT);

export const filterOutGoodStations = () =>
  cy
    .get(`${selectors.sohOverviewToolbar} ${selectors.sohFilter} ${selectors.button}`)
    .click({ force: true })
    .get(selectors.checkbox.good)
    .click({ force: true });

export const filterAll1StationGroup = () =>
  cy
    .get(`${selectors.sohOverviewToolbar} ${selectors.sohFilterByStationGroup} ${selectors.button}`)
    .click({ force: true })
    .get(selectors.checkbox.all_1)
    .click({ force: true });

/**
 * ----- Verifiers -----
 * Verify some UI state
 * checkThis, confirmThat
 */

export const verifyDataIsLoaded = () => {
  cy.get(selectors.sohOverviewHeader).should(
    Common.cypressShouldOptions.HAVE_LENGTH_GREATER_THAN,
    0
  );
  cy.get(
    `[data-cy="soh-overview-group-${GROUP_NAME}"] ${selectors.unacknowledged} ${selectors.sohOverviewCell}`
  ).should(Common.cypressShouldOptions.HAVE_LENGTH_GREATER_THAN, 0);
};

/**
 * ----- Capability -----
 * Perform an action and verify its result
 * ie: locate, reject
 */

/**
 * Get the number of acknowledged stations.
 */
const retrieveNumberAcknowledged = () =>
  cy
    .get(
      `[data-cy="soh-overview-group-${GROUP_NAME}"] ${selectors.unacknowledged} ${selectors.sohOverviewCell}`
    )
    .its(Common.cypressItsOptions.LENGTH)
    .then(value => {
      // tslint:disable-next-line: no-console
      console.log(`number of stations acknowledged: ${String(value)}`);
      return value;
    });

/**
 * Right click; show context menu.
 */
const showContextMenu = () => {
  cy.root().click();
  cy.get(
    `[data-cy="soh-overview-group-${GROUP_NAME}"] ${selectors.unacknowledged} ${selectors.sohOverviewCell}`
  )
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
 * Validates the number of acknowledged
 * @param value the expected number
 */
const validateNumberOfAcknowledged = (value: number) =>
  cy
    .get(`[data-cy="soh-overview-group-${GROUP_NAME}"] ${selectors.sohOverviewCell}`)
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

/**
 * Gets the count of stations of a certain status
 * @param group group to check
 * @param status status to check (IE Good, Marginal, Bad)
 */
export const getStatusStationCount = (group: string, status: string) => {
  cy.get(`[data-cy="soh-overview-group-${group}"]`).within(() => {
    cy.get(`[data-cy="soh-overview-group-header__count--${status}"]`)
      .invoke('text')
      .then(value => {
        const countValue = parseFloat(String(value));
        cy.wrap(countValue).as(`${status}Count`);
      });
  });
};

/**
 * Gets total count of good bad marginal stations for the specified group
 * @param group group to check
 * @param totalCount total count to check against
 */
export const verifyGroupStationCount = (group: string, totalCount: number) => {
  const bad = SohTypes.SohStatusSummary.BAD.toLocaleLowerCase();
  const marginal = SohTypes.SohStatusSummary.MARGINAL.toLocaleLowerCase();
  const good = SohTypes.SohStatusSummary.GOOD.toLocaleLowerCase();
  getStatusStationCount(group, bad);
  getStatusStationCount(group, marginal);
  getStatusStationCount(group, good);
  let count = 0;
  cy.get(`@${bad}Count`).then(badValue => {
    count += Number(badValue);
    cy.get(`@${marginal}Count`).then(marginalValue => {
      count += Number(marginalValue);
      cy.get(`@${good}Count`).then(goodValue => {
        count += Number(goodValue);
        expect(count).to.be.eq(totalCount);
      });
    });
  });
};
