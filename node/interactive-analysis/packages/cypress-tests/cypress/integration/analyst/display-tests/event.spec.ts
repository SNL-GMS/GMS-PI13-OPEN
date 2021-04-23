/// <reference types="Cypress" />
// tslint:disable: no-magic-numbers
import * as EventDisplayActions from '../../../fixtures/analyst//event-display';
import * as Common from '../../../fixtures/common';

describe('Check undoing basic operations', function() {
  before(() => {
    Common.visitApp();
    Common.openFavoriteAnalystInterval();
    Common.checkAreSignalDetectionsLoaded();
  });

  beforeEach(() => {
    // Keep cookie in between tests
    Cypress.Cookies.preserveOnce('InteractiveAnalysis');
  });

  it('can open event', () => {
    cy.wait(500);
    EventDisplayActions.openEventWithMostDetections();
  });

  it('can save open event', () => {
    EventDisplayActions.clickSaveOpenEvent();
  });

  it('can mark the open event complete', () => {
    EventDisplayActions.clickMarkOpenEventComplete();
  });
});
