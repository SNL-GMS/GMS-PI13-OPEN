/// <reference types="Cypress" />
import * as Common from '../common';

/**
 * ----- Command -----
 * These functions interact with the UI, but do not verify the results
 * ie: clickThis, scrollThat, etc.
 */

export const openLocationDisplay = () => {
  Common.clickGoldenLayoutTab('Location');
};

export const maximizeLocationDisplay = () => {
  cy.contains('Location')
    .get('.lm_maximise')
    .eq(1)
    .click();
  cy.wait(1000);
};

export const minimiseLocationDisplay = () => {
  cy.contains('Waveforms')
    .get('[title="minimize"]')
    .click();
};

/**
 * ----- Verifiers -----
 * Verify some UI state
 * checkThis, confirmThat
 */

/**
 * ----- Capability -----
 * Perform an action and verify its result
 * ie: locate, reject
 */
/**
 * Locates an event. Assumes that a location is possible
 */
export const locate = () => {
  cy.get('[data-cy="location-set-to-save-switch"]').then(results => {
    const priorLocationCount = results.length;
    cy.get('[data-cy="location-locate-button"]').click();
    cy.get('[data-cy="location-set-to-save-switch"]').should(
      'have.length.greaterThan',
      priorLocationCount
    );
  });
};
