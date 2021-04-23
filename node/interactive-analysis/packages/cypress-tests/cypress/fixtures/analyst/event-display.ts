/// <reference types="Cypress" />

import * as Common from '../common';

/**
 * ----- Command -----
 * These functions interact with the UI, but do not verify the results
 * ie: clickThis, scrollThat, etc.
 */

export const openEventDisplay = () => {
  Common.clickGoldenLayoutTab('Events');
};

export const clickSaveOpenEvent = () => {
  cy.get('[data-cy="save-open-event"]').click();
};

export const clickSaveAllEvents = () => {
  cy.get('[data-cy="save-all-events"]').click();
};

export const clickOpenEventRow = () => {
  cy.get('.open-event-row')
    .first()
    .click({ force: true });
};

export const clickMarkOpenEventComplete = () => {
  cy.get('.open-event-row [data-cy=event-mark-complete]').click({ force: true });
};

/**
 * ----- Verifiers -----
 * Verify some UI state
 * checkThis, confirmThat
 */

export const checkEventListHasEventsWithAssociatedSds = () => {
  cy.get('[data-cy="number-of-detections"]').then(cells => {
    const cellList = cells.toArray();
    const filteredList = cellList.filter(cell => parseFloat(cell.innerText) > 0);
    expect(filteredList.length).to.be.greaterThan(0);
  });
};
/**
 * ----- Capability -----
 * Perform an action and verify its result
 * ie: locate, reject
 */

/**
 * Opens event with most associated detections
 * Assumes event is loaded and can be opened
 */
export const openEventWithMostDetections = () => {
  cy.get('[data-cy="number-of-detections"]').then(buttons => {
    const buttonList = buttons.toArray();
    const button = buttonList.reduce(
      (accum, val) => (parseFloat(val.innerText) > parseFloat(accum.innerText) ? val : accum),
      buttonList[0]
    );
    cy.get(button as any).dblclick({ force: true });
  });
};
