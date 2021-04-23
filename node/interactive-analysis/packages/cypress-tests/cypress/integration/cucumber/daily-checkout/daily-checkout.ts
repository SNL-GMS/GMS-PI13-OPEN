import { Given, Then } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../fixtures/common';
import * as SohCommon from '../../../fixtures/soh/soh-display-all';
import * as SOHOverviewActions from '../../../fixtures/soh/soh-overview';
import { getURLForGateway, testEndpoint } from '../../../fixtures/utils';

const myAssertion = (): UselessType => {
  assert(true);
  return { uselessKey: '' };
};

interface UselessType {
  uselessKey: string;
}

/**
 * Simple given function for testing that we are able to
 * interpret features and cucumber in webpack
 */
// tslint:disable-next-line: no-empty
Given(`webpack is configured`, () => {});

Given('the UI is opened to the SOH Overview Display', () => {
  SohCommon.openSOHDisplay(Common.SOHDisplays.OVERVIEW);
});

/**
 * Checks that station group string exists
 */
Given('the {string} station group exists', groupName => {
  const splits = groupName.split('|');
  splits.forEach(group => {
    cy.log(`Checking for ${group} Display`);
    cy.get(`[data-cy="soh-overview-group-${group}"]`).should('exist');
  });
});

Then('{string} display has {int} stations configured for overview Display', (groupName, tot) => {
  cy.get(`[data-cy="soh-overview-group-${groupName}"]`).should('exist');
  cy.log(`Looking for ${tot} total stations for ${groupName}`);
  SOHOverviewActions.verifyGroupStationCount(groupName, tot);
});

/**
 * simple then to show assertion and cypress navigation work
 * basically our sanity test to make sure are test suit is working correctly
 * requires the UI to be running
 */
Then(`this test should work just fine!`, () => {
  myAssertion();
  cy.visit('.');
});

/**
 * Step to check if the gateway is alive
 */
Given('The gateway is alive', () => {
  const url = getURLForGateway(Cypress.config().baseUrl);
  testEndpoint(`${url}/alive`, 'Test gateway alive');
});

/**
 * Step to check if the gateway is ready
 */
Given('The gateway is ready', () => {
  const url = getURLForGateway(Cypress.config().baseUrl);
  testEndpoint(`${url}/ready`, 'Test gateway ready');
});

/**
 * Step to check if the gateway is healthy
 */
Then('The gateway is healthy', () => {
  const url = getURLForGateway(Cypress.config().baseUrl);
  testEndpoint(`${url}/health-check`, 'Test gateway healthy');
});

/**
 * Step to check if the ui is alive
 */
Given('The ui is alive', () => testEndpoint('/alive', 'Test ui alive'));

/**
 * Step to check if the ui is ready
 */
Given('The ui is ready', () => testEndpoint('/ready', 'Test ui readonly'));

/**
 * Step to check if the ui is healthy
 */
Given('The ui is healthy', () => testEndpoint('/health-check', 'Test ui healthy'));

/**
 * Step to check if the user can login
 */
Then('The user can login', () => {
  Common.visitApp();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});
