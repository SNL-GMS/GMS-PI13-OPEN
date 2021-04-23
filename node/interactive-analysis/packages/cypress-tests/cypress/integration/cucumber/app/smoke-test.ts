/// <reference types="Cypress" />
// tslint:disable-next-line: match-default-export-name
import { Given, Then } from 'cypress-cucumber-preprocessor/steps';

/**
 * Simple given function for hitting the gms webpage
 */
// tslint:disable-next-line: no-empty
Given(`we can load the GMS login page`, () => {
  cy.visit('.');
});

/**
 * simple UI smoke test to make sure the bundle was loaded
 */
Then(`I see {string} in the title`, title => {
  cy.title().should('include', title);
});
