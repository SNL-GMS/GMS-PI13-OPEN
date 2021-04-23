/// <reference types="Cypress" />
// tslint:disable-next-line: match-default-export-name
import assert from 'assert';
import { Given, Then } from 'cypress-cucumber-preprocessor/steps';

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

/**
 * simple then to show assertion and cypress navigation work
 * basically our sanity test to make sure are test suit is working correctly
 * requires the UI to be running
 */
Then(`this test should work just fine!`, () => {
  myAssertion();
  cy.visit('.');
});
