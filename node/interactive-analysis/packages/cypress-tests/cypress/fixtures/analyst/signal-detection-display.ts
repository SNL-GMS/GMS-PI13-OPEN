/// <reference types="Cypress" />
import * as Common from '../common';

/**
 * ----- Command -----
 * These functions interact with the UI, but do not verify the results
 * ie: clickThis, scrollThat, etc.
 */
export const openSignalDetectionDisplay = () => {
  Common.clickGoldenLayoutTab('Signal Detections');
};
/**
 * ----- Verifiers -----
 * Verify some UI state
 * checkThis, confirmThat
 */
export const checkHasLoadedSds = () => {
  cy.get('[data-cy="signal-detection-color-swatch"]').should('have.length.greaterThan', 1);
};
/**
 * ----- Capability -----
 * Perform an action and verify its result
 * ie: locate, reject
 */

/**
 * Shows an fk on the first associated signal detection in the list
 * And opens the Az Slow display to verify it rendered
 */
export const showFk = () => {
  openSignalDetectionDisplay();
  cy.get('[data-cy="signal-detection-filter"]').select('Open Event');
  cy.get('[data-cy="signal-detection-color-swatch"]')
    .first()
    .rightclick();
  cy.get('[data-cy="show-fk"]').click();
  Common.clickGoldenLayoutTab('Azimuth Slowness');
  cy.get('[data-cy="primary-fk-rendering"]', { timeout: 40000 });
  openSignalDetectionDisplay();
};
