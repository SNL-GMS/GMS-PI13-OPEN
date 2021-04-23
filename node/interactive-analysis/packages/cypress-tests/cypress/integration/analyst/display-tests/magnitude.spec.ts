/// <reference types="Cypress" />
// tslint:disable: no-magic-numbers
import * as EventDisplayActions from '../../../fixtures/analyst/event-display';
import * as MagnitudeDisplayActions from '../../../fixtures/analyst/magnitude-display';
import * as Common from '../../../fixtures/common';

describe('Check basic magnitude functionality', function() {
  before(() => {
    Common.visitApp();
    Common.openFavoriteAnalystInterval();
    Common.checkAreSignalDetectionsLoaded();
  });

  beforeEach(() => {
    // Keep cookie in between tests
    Cypress.Cookies.preserveOnce('InteractiveAnalysis');
  });

  it('Can call for initial magnitude', () => {
    EventDisplayActions.openEventDisplay();
    cy.wait(500);
    EventDisplayActions.openEventWithMostDetections();
    MagnitudeDisplayActions.openMagnitudeDisplay();
    cy.wait(500);
    // MagnitudeDisplayActions.maximizeMagnitudeDisplay();
    MagnitudeDisplayActions.checkFirstMagnitudeOfType('MB');
    cy.wait(10000);
    MagnitudeDisplayActions.checkFirstMagnitudeOfType('MB');
    cy.wait(10000);
    cy.get(`[data-cy="mag-defining-checkbox-MB"]`)
      .first()
      .should('be.checked');
  });

  it('can set defining all', () => {
    MagnitudeDisplayActions.checkDefiningAllOfMagnitudeType('MB');
  });

  it('can set defining none', () => {
    MagnitudeDisplayActions.checkDefiningNoneOfMagnitudeType('MB');
  });
});
