/// <reference types="Cypress" />
import { Classes } from '@blueprintjs/core';
import * as WaveformDisplayActions from '../../../fixtures/analyst/waveform-display';
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

  it('Can rephase', () => {
    WaveformDisplayActions.maximizeWaveformDisplay();
    WaveformDisplayActions.zoomOutWaveformDisplay();
    WaveformDisplayActions.setPhaseOnSignalDetection('db31fbe6-322f-3e91-911c-578f22f4234b', 'P');
  });

  it('Can un-associate', () => {
    WaveformDisplayActions.unassociateSignalDetection('db31fbe6-322f-3e91-911c-578f22f4234b');
    cy.get(`[data-cy="pick-marker-db31fbe6-322f-3e91-911c-578f22f4234b"]`).should(
      'have.css',
      'color',
      'rgb(219, 55, 55)'
    );
  });

  it('Can reject', () => {
    WaveformDisplayActions.rejectSignalDetection('db31fbe6-322f-3e91-911c-578f22f4234b');
  });

  it('Can associate', () => {
    WaveformDisplayActions.associateSignalDetection('72c13b5d-efad-3a2c-bf7d-73af1fcb3f09');
    cy.get(
      `[data-cy="pick-marker-72c13b5d-efad-3a2c-bf7d-73af1fcb3f09"] > .bp3-icon ${Classes.INTENT_DANGER} pick-marker__conflict`
    ).should('not.exist');
  });

  it('Can create detection', () => {
    WaveformDisplayActions.createDetection('ARCES/fkb-channel');
  });
  it('Can show predicted phases', () => {
    WaveformDisplayActions.showPredictedPhases();
  });

  it('Can align on predicted phase P', () => {
    WaveformDisplayActions.alignOnPredicted();
  });

  it('Can align on time', () => {
    WaveformDisplayActions.alignOnTime();
  });

  it('Can show a5/2 measurement', () => {
    WaveformDisplayActions.showMeasurement('7ec53095-827d-3433-841a-8c16f9eedc8a');
    cy.get('.selection-window-selection').should('have.length.greaterThan', 0);
  });

  it('Can change sorting of stations', () => {
    WaveformDisplayActions.sortByStation();
  });
});
