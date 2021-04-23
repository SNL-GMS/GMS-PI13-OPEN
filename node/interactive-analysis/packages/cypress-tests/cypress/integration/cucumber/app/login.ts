/// <reference types="Cypress" />
// tslint:disable-next-line: match-default-export-name
import { Given, Then } from 'cypress-cucumber-preprocessor/steps';
import * as Common from '../../../fixtures/common';

/**
 * logs into the UI
 */
Given(`I can login as {string}`, user => {
  cy.get('[data-cy=username-input]').type(user);
  cy.get('[data-cy=login-btn]').click();
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});

/**
 * Then function that logs into the UI
 */
Then(`{string} is displayed as my username`, user => {
  cy.get('[data-cy="username"]').should('contain', user);
  cy.wait(Common.SHORT_WAIT_TIME_MS);
});
