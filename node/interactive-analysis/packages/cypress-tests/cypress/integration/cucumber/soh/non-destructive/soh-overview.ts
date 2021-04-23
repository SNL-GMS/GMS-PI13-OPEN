import { Given, Then } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../../fixtures/common';
import * as SohCommon from '../../../../fixtures/soh/soh-display-all';
import * as SOHOverviewActions from '../../../../fixtures/soh/soh-overview';

/**
 * opens the soh overview display and makes sure
 * that the data is loaded
 * (note we have to call cy.wait due to a timing bug)
 */
Given('the UI is opened to the SOH Overview Display', () => {
  Common.visitApp();
  SohCommon.openSOHDisplay(Common.SOHDisplays.OVERVIEW);
  SOHOverviewActions.verifyDataIsLoaded();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});

/**
 * Checks that station group All_1 exists
 */
Then('All_1 station group exists', () => {
  cy.get('[data-cy=soh-overview-group-All_1]').should('exist');
});

/**
 * This function goes through the process of going through the
 * filters options
 */
Given('we can filter the SOH Overview Display', SOHOverviewActions.filterOutGoodStations);

/**
 * Checks that all the acknowledged good disappears
 */
Then('acknowledged good is filtered out', () => {
  cy.get('[data-cy=soh-acknowledged] > [data-cy-status="GOOD"]').should('not.exist');
});

/**
 * Opens station group dropdown and filters the first in the list, which is All_1
 */
Given('we can filter SOH station groups', SOHOverviewActions.filterAll1StationGroup);

/**
 * Checks That station group All_1 was removed
 */
Then('All_1 is filtered out', () => {
  cy.get('[data-cy=soh-overview-group-All_1]').should('not.exist');
});

/**
 * Opens station group dropdown and un-filters the first in the list, which is All_1
 */
Given(
  'we can filter SOH station groups they can be unfiltered',
  SOHOverviewActions.filterAll1StationGroup
);

/**
 * Checks That station group All_1 was added back
 */
Then('All_1 is unfiltered', () => {
  cy.get('[data-cy=soh-overview-group-All_1]').should('exist');
});
