/// <reference types="Cypress" />
// tslint:disable: no-magic-numbers

/**
 * ----- Command -----
 * These functions interact with the UI, but do not verify the results
 * ie: clickThis, scrollThat, etc.
 */

export const maximizeWaveformDisplay = () => {
  cy.contains('Waveforms')
    .get('.lm_maximise')
    .eq(3)
    .click();
  cy.wait(1000);
};

export const minimizeWaveformDisplay = () => {
  cy.contains('Waveforms')
    .get('[title="minimize"]')
    .click();
  cy.wait(1000);
};

export const zoomOutWaveformDisplay = () => {
  cy.get('.weavess-wp', { timeout: 40000 }).dblclick();
};
/**
 * ----- Verifier -----
 * Verify some UI state
 * checkThis, confirmThat
 */

/**
 * ----- Capability -----
 * Perform an action and verify its result
 * ie: locate, reject
 */

export const createDetection = channel => {
  cy.get('[data-cy-color="pick-marker-#d9822b"]').then(result => {
    const asArray = result.toArray();
    const priorCount = asArray.length;
    cy.get(`[data-cy="${channel}"] > .channel-content-container > .contentrenderer`)
      .focus()
      .type('{meta}', { release: false })
      .click('center', { force: true });
    cy.get('[data-cy-color="pick-marker-#d9822b"]').should('have.length.greaterThan', priorCount);
  });
};

/**
 * Sets phase and verifies results
 * @param id string identifier of sd to phase
 * @param phase string of which phase to set
 */
export const setPhaseOnSignalDetection = (id, phase) => {
  cy.get(
    '[data-cy="PDAR/fkb-channel"] > .channel-content-container > .contentrenderer > .contentrenderer-content'
  ).click({ force: true });
  cy.get(`[data-cy="pick-marker-${id}"]`).rightclick('bottomLeft');

  cy.get('[data-cy="set-phase"]').click();
  cy.get(`[data-cy="filterable-option-${phase}"]`).click({ force: true });
  cy.get(`[data-cy="pick-marker-${id}"]`, { timeout: 6000 }).should('have.text', phase);
};

/**
 * Rejects a detection and verifies it was successful
 * @param id string identifier of sd to reject
 */
export const rejectSignalDetection = id => {
  cy.get(
    '[data-cy="PDAR/fkb-channel"] > .channel-content-container > .contentrenderer > .contentrenderer-content'
  ).click({ force: true });
  cy.get(`[data-cy="pick-marker-${id}"]`).rightclick('bottomLeft');

  cy.get('[data-cy="reject-sd"]').click();

  cy.get(`[data-cy="pick-marker-${id}"]`, { timeout: 5000 }).should('not.exist');
};

/**
 * Associates signal detection to the currently open event and verifies
 * @param id string identifier of sd to associate to the currently open event
 */
export const associateSignalDetection = id => {
  cy.get(
    '[data-cy="PDAR/fkb-channel"] > .channel-content-container > .contentrenderer > .contentrenderer-content'
  ).click({ force: true });
  cy.get(`[data-cy="pick-marker-${id}"]`).rightclick('bottomLeft');
  cy.wait(500);
  cy.get('[data-cy="association-menu"]').trigger('mouseover');
  cy.wait(500);
  cy.get('[data-cy="associate-to-open"]').click();

  cy.get(`[data-cy="pick-marker-${id}"] > .bp3-icon`).should('exist');
};

/**
 * Unassociates a signal detection from the open event
 * @param id string identifier of sd to unassociate
 */
export const unassociateSignalDetection = id => {
  cy.get(
    '[data-cy="PDAR/fkb-channel"] > .channel-content-container > .contentrenderer > .contentrenderer-content'
  ).click({ force: true });
  cy.get(`[data-cy="pick-marker-${id}"]`).rightclick('bottomLeft');
  cy.wait(500);
  cy.get('[data-cy="association-menu"]').trigger('mouseover');
  cy.wait(500);
  cy.get('[data-cy="unassociate-to-open"]').click();

  cy.get(`[data-cy="pick-marker-${id}"]`).should('have.css', 'color', 'rgb(219, 55, 55)');
};

/**
 * Shows a measurement for a given signal detection
 * @param id string identifier of sd to show measurement for
 */
export const showMeasurement = id => {
  cy.get(
    '[data-cy="PDAR/fkb-channel"] > .channel-content-container > .contentrenderer > .contentrenderer-content'
  ).click({ force: true });
  cy.get(`[data-cy="pick-marker-${id}"]`).rightclick('bottomLeft');
  cy.wait(500);
  cy.get('[data-cy="measure"]').trigger('mouseover');
  cy.wait(500);
  cy.get('[data-cy="show-hide-measure"]').click();
};

/**
 * Shows predicted phases in the waveform display
 */
export const showPredictedPhases = () => {
  cy.get('[data-cy="Predicted Phases"]').click({ force: true });
  cy.get('[data-cy-is-predicted-phase="true"]', { timeout: 10000 }).should(
    'have.length.greaterThan',
    0
  );
};

/**
 * Aligns waveforms on phase P - assumes that waveforms were not aligned on phase
 */
export const alignOnPredicted = () => {
  cy.get('.waveform-display-container').type('{option}p', { release: false });
  cy.get('[data-cy-is-predicted-phase="true"]').then(results => {
    // const offsetOfPPhase = results
    //   .toArray()
    //   .filter(result => result.getAttribute('data-cy-phase') === 'P')
    //   .map(result => parseFloat(result.getAttribute('data-cy-style-left')));
    // const areAllAligned = offsetOfPPhase.reduce(
    //   (accum, val) => ({
    //     isEqual: accum.isEqual && val === accum.priorValue,
    //     priorValue: val
    //   }),
    //   { priorValue: offsetOfPPhase[0], isEqual: true }
    // );
    // expect(areAllAligned.isEqual).to.equal(true);
  });
};

/**
 * Aligns waveforms by time - assumes waveforms were previously aligned on phases
 */
export const alignOnTime = () => {
  cy.get('.waveform-display-container').type('{option}p', { release: false });
  cy.get('[data-cy="weavess-static-vertical-marker"]').then(finalResults => {
    // const finalArray = finalResults
    //   .toArray()
    //   .filter(fr => fr.getAttribute('data-cy-color') === '#43bf4d');
    // const values = finalArray.map(fa => parseFloat(fa.getAttribute('data-cy-left')));
    // values.sort();
    // const uniqueCount = values.reduce(
    //   (accum, val) => {
    //     const equalsPrior = val === accum.oldVal;
    //     const newCount = !equalsPrior ? accum.uniqueCount + 1 : accum.uniqueCount;
    //     return {
    //       oldVal: val,
    //       uniqueCount: newCount
    //     };
    //   },
    //   { oldVal: values[0], uniqueCount: 1 }
    // );
    // expect(uniqueCount.uniqueCount).to.equal(2);
  });
};

/**
 * Opens the channel with the given name.
 * Assumes the channel is not expanded and that its children will cause an
 * amplitude y-axis to be created
 * @param channelName the same of the channel to be opened
 */
export const openChannel = channelName => {
  cy.get('[data-cy-contains-amplitude-markers="true"]').then(results => {
    const count = results.length;
    cy.get(`[data-cy="weavess-expand-parent"][data-cy-channel-name="${channelName}"]`).click();
    cy.get('[data-cy-contains-amplitude-markers="true"]', { timeout: 40000 }).should(
      'have.length.greaterThan',
      count
    );
  });
};

/**
 * Sorts waveforms by stations
 * Checks that the sort order has changed
 */
export const sortByStation = () => {
  cy.get(
    '.label > .label-container > .label-container-content > .label-container-content-label > :nth-child(1)'
  ).then(result => {
    const asArray = result.toArray();
    const firstElement = asArray[0];
    cy.get(':nth-child(4) > .bp3-select > .form__select').select('Station Name');
    cy.get(
      '.label > .label-container > .label-container-content > .label-container-content-label > :nth-child(1)'
    ).then(result2 => {
      const newArray = result2.toArray();
      const newElement = newArray[0];
      expect(newElement).to.not.equal(firstElement);
    });
  });
};
