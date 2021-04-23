import { Given, Then } from 'cypress-cucumber-preprocessor/steps';
import { SHORT_WAIT_TIME_MS, visitApp } from '../../../fixtures/common';
import { getURLForGateway } from '../../../fixtures/utils';

/**
 * Step to check if the gateway is alive
 */
Given('The gateway is alive', () => {
  cy.log('Test gateway alive');
  const url = getURLForGateway(Cypress.config().baseUrl);
  cy.visit(`${url}/alive`);
  cy.wait(SHORT_WAIT_TIME_MS);
});

/**
 * Step to check if the gateway is ready
 */
Given('The gateway is ready', () => {
  cy.log('Test gateway ready');
  const url = getURLForGateway(Cypress.config().baseUrl);
  cy.visit(`${url}/ready`);
  cy.wait(SHORT_WAIT_TIME_MS);
});

/**
 * Step to check if the gateway is healthy
 */
Then('The gateway is healthy', () => {
  cy.log('Test gateway healthy');
  const url = getURLForGateway(Cypress.config().baseUrl);
  cy.visit(`${url}/health-check`);
  cy.wait(SHORT_WAIT_TIME_MS);
});

/**
 * Step to check if the ui is alive
 */
Given('The ui is alive', () => {
  cy.log('Test ui alive');
  cy.visit('/alive');
  cy.wait(SHORT_WAIT_TIME_MS);
});

/**
 * Step to check if the ui is ready
 */
Given('The ui is ready', () => {
  cy.log('Test ui ready');
  cy.visit('/ready');
  cy.wait(SHORT_WAIT_TIME_MS);
});

/**
 * Step to check if the ui is healthy
 */
Given('The ui is healthy', () => {
  cy.log('Test ui healthy');
  cy.visit('/health-check');
  cy.wait(SHORT_WAIT_TIME_MS);
});

/**
 * Step to check if the user can login
 */
Then('The user can login', () => {
  visitApp();
  cy.wait(SHORT_WAIT_TIME_MS);
});
