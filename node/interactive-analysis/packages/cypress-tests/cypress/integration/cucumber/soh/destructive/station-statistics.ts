import { Given, Then } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../../fixtures/common';
import * as SohCommon from '../../../../fixtures/soh/soh-display-all';
import * as Actions from '../../../../fixtures/soh/station-statistics';

/**
 * Opens up the app then opens the display verifies that
 * the data is loaded
 * (note we have to call cy.wait due to a timing bug)
 */
Given('The Station Statistics Display is opened and loads data', () => {
  Common.visitApp();
  SohCommon.openSOHDisplay(Common.SOHDisplays.STATION_STATISTICS);
  Actions.verifyDataIsLoaded();
  cy.wait(Common.COMMON_WAIT_TIME_MS);
});

/**
 * checking to see if we can acknowledge something in the station statistics display
 * (note we have to call cy.wait due to a timing bug
 */
Then('the user can acknowledge SOH in the station statistics display', () => {
  Actions.acknowledge();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});
