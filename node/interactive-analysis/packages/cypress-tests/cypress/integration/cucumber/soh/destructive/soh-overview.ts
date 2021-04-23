import { Given, Then } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../../fixtures/common';
import * as SohCommon from '../../../../fixtures/soh/soh-display-all';
import * as SOHOverviewActions from '../../../../fixtures/soh/soh-overview';

/**
 * open the app and load data then verify that all the data is loaded
 * in to the display
 * (note we have to call cy.wait due to a timing bug)
 */
Given('The SOH Overview Display is opened and loads data', () => {
  Common.visitApp();
  SohCommon.openSOHDisplay(Common.SOHDisplays.OVERVIEW);
  cy.wait(Common.SHORT_WAIT_TIME_MS);
  SOHOverviewActions.verifyDataIsLoaded();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});

/**
 * This checks that we can acknowledge in the soh overview display after
 * loading data
 * (note we have to call cy.wait due to a timing bug)
 */
Then('the user can acknowledge in the overview', () => {
  SOHOverviewActions.acknowledge();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});
