import { Given, Then } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../../fixtures/common';
import * as ChannelMonitorBarChartActions from '../../../../fixtures/soh/channel-monitor-bar-chart';
import * as SohCommon from '../../../../fixtures/soh/soh-display-all';

/**
 * open the app and load data then verify that all the data is loaded
 * in to the display
 * (note we have to call cy.wait due to a timing bug)
 */
Given('The SOH Lag Display is opened and ready to load data', () => {
  Common.visitApp();
  SohCommon.openSOHDisplay(Common.SOHDisplays.LAG);
  cy.wait(Common.SHORT_WAIT_TIME_MS);
  ChannelMonitorBarChartActions.verifyDataIsNotLoaded();
  SohCommon.selectStationSOHandReturn(Common.SOHDisplays.LAG);
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});

/**
 * this checks that we can cancel all quiet indicators
 */
Then('the user can cancel all quiet indicators using SOH Lag display', () => {
  // ensure valid starting state by quieting and then canceling
  // this will ensure that the starting state is un-quieted
  ChannelMonitorBarChartActions.cancelAllQuietPeriods();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});

/**
 * this checks that we can acknowledge in the soh lag display after
 * loading data
 */
Then('the user can quiet using the SOH Lag display', () => {
  ChannelMonitorBarChartActions.quietChannelMonitor();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});

/**
 * this checks that a user can cancel a channel monitor that is quieted
 */
Then('the user can cancel a channel monitor that is quieted using the SOH Lag display', () => {
  ChannelMonitorBarChartActions.cancelQuietChannelMonitor();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});

/**
 * this checks that we can quiet with a comment in the soh lag display after
 * loading data
 * (note we have to call cy.wait due to a timing bug)
 */
Then('the user can quiet with a comment using the SOH Lag display', () => {
  ChannelMonitorBarChartActions.quietChannelMonitorWithComment();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});

/**
 * this checks that we can can cancel quieting with a comment
 */
Then('the user can cancel quieting with a comment using the SOH Lag display', () => {
  ChannelMonitorBarChartActions.quietChannelMonitorWithCommentCancelled();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});
