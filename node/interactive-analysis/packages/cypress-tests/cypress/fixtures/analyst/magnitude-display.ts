/// <reference types="Cypress" />
import * as Common from '../common';

/**
 * ----- Command -----
 * These functions interact with the UI, but do not verify the results
 * ie: clickThis, scrollThat, etc.
 */

export const openMagnitudeDisplay = () => {
  Common.clickGoldenLayoutTab('Magnitude');
};

export const maximizeMagnitudeDisplay = () => {
  cy.contains('Magnitude')
    .get('.lm_maximise')
    .eq(1)
    .click();
  // tslint:disable-next-line: no-magic-numbers
  cy.wait(500);
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
 * Checks the first stations magnitude of the given mag type
 * @param magType the magnitude type ie: MB, MB_MLE
 */
export const checkFirstMagnitudeOfType = magType => {
  cy.get(`[data-cy="mag-defining-checkbox-${magType}"]`)
    .first()
    .then(result => {
      // const wasChecked = (result.toArray()[0] as any).checked;
      cy.get(`[data-cy="mag-defining-checkbox-${magType}"]`)
        .first()
        .click({ force: true });
      // cy.get(`[data-cy="mag-defining-checkbox-${magType}"]`, { timeout: 40000 }).should(
      //   afterTheCheck => {
      //     const isNowChecked = afterTheCheck.toArray()[0].checked;
      //     expect(wasChecked).to.not.equal(isNowChecked);
      //   },
      //   { timeout: 40000 }
      // );
    });
};

/**
 * Checks defining all for stations magnitude of the given mag type
 * @param magType the magnitude type ie: MB, MB_MLE
 */
export const checkDefiningAllOfMagnitudeType = magType => {
  cy.get(`[data-cy="defining-all-${magType}"]`).click({ force: true });
  cy.get(`[data-cy="mag-defining-checkbox-${magType}"]`).should('be.checked');
};

/**
 * Checks defining none for stations magnitude of the given mag type
 * @param magType the magnitude type ie: MB, MB_MLE
 */
export const checkDefiningNoneOfMagnitudeType = magType => {
  cy.get(`[data-cy="defining-none-${magType}"]`).click({ force: true });
  cy.get(`[data-cy="defining-all-${magType}"]`).should('not.be.checked');
};
