/// <reference types="Cypress" />
import { Classes } from '@blueprintjs/core';
import * as WaveformDisplayActions from '../../fixtures/analyst/waveform-display';
import * as Common from '../../fixtures/common';
import * as HotkeyHelper from '../../fixtures/global-hotkey';

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

  it('Can undo rephase', () => {
    WaveformDisplayActions.maximizeWaveformDisplay();
    WaveformDisplayActions.zoomOutWaveformDisplay();
    WaveformDisplayActions.setPhaseOnSignalDetection('db31fbe6-322f-3e91-911c-578f22f4234b', 'P');
    HotkeyHelper.globalHotkey('z', true, false, false);

    cy.get('[data-cy="pick-marker-db31fbe6-322f-3e91-911c-578f22f4234b"]').should(
      'have.text',
      'Pg'
    );
  });

  it('Can undo un-associate', () => {
    WaveformDisplayActions.unassociateSignalDetection('db31fbe6-322f-3e91-911c-578f22f4234b');
    HotkeyHelper.globalHotkey('z', true, false, false);
    cy.get(`[data-cy="pick-marker-db31fbe6-322f-3e91-911c-578f22f4234b"]`).should(
      'not.have.css',
      'color',
      'rgb(219, 55, 55)'
    );
  });
  it('Can redo and re-undo un-associate', () => {
    HotkeyHelper.globalHotkey('z', true, false, true);
    cy.get(`[data-cy="pick-marker-db31fbe6-322f-3e91-911c-578f22f4234b"]`).should(
      'have.css',
      'color',
      'rgb(219, 55, 55)'
    );
    HotkeyHelper.globalHotkey('z', true, false, false);
    cy.get(`[data-cy="pick-marker-db31fbe6-322f-3e91-911c-578f22f4234b"]`).should(
      'not.have.css',
      'color',
      'rgb(219, 55, 55)'
    );
  });
  it('Can undo reject', () => {
    WaveformDisplayActions.rejectSignalDetection('db31fbe6-322f-3e91-911c-578f22f4234b');
    HotkeyHelper.globalHotkey('z', true, false, false);
    cy.get('[data-cy="pick-marker-db31fbe6-322f-3e91-911c-578f22f4234b"]').should('exist');
  });
  it('Can redo reject', () => {
    HotkeyHelper.globalHotkey('z', true, false, true);
    cy.get('[data-cy="pick-marker-db31fbe6-322f-3e91-911c-578f22f4234b"]').should('not.exist');
  });

  it('Can undo associate', () => {
    WaveformDisplayActions.associateSignalDetection('72c13b5d-efad-3a2c-bf7d-73af1fcb3f09');
    HotkeyHelper.globalHotkey('z', true, false, false);
    cy.get(
      `[data-cy="pick-marker-72c13b5d-efad-3a2c-bf7d-73af1fcb3f09"] > .bp3-icon ${Classes.INTENT_DANGER} pick-marker__conflict`
    ).should('not.exist');
  });

  it('Can redo associate', () => {
    HotkeyHelper.globalHotkey('z', true, false, true);
    cy.get(`[data-cy="pick-marker-72c13b5d-efad-3a2c-bf7d-73af1fcb3f09"] > .bp3-icon`).should(
      'exist'
    );
  });
});
