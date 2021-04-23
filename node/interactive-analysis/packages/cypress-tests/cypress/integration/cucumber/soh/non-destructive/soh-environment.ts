import { Given, Then } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../../fixtures/common';
import * as SohCommon from '../../../../fixtures/soh/soh-display-all';
import * as SOHEnvironmentActions from '../../../../fixtures/soh/soh-environment';

/**
 * open the app and load data then verify that all the data is loaded
 * in to the display
 * (note we have to call cy.wait due to a timing bug)
 */
Given('The SOH Environment Display is opened and ready to load data', () => {
  Common.visitApp();
  SohCommon.openSOHDisplay(Common.SOHDisplays.ENVIRONMENT);
  SOHEnvironmentActions.verifyDataIsNotLoaded();
  SohCommon.selectStationSOHandReturn(Common.SOHDisplays.ENVIRONMENT);
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});

/**
 * this checks that we can acknowledge in the soh overview display after
 * loading data
 * (note we have to call cy.wait due to a timing bug)
 */
Then('the user can view the SOH Environment table', () => {
  SOHEnvironmentActions.verifyDataIsLoaded();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});
