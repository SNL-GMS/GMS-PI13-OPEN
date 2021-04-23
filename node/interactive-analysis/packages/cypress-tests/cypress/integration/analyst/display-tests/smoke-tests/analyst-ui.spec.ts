/// <reference types="Cypress" />
import * as EventDisplayActions from '../../../../fixtures/analyst/event-display';
import * as LocationDisplayActions from '../../../../fixtures/analyst/location-display';
import * as MagnitudeDisplayActions from '../../../../fixtures/analyst/magnitude-display';
import * as SignalDetectionDisplayActions from '../../../../fixtures/analyst/signal-detection-display';
import * as WaveformDisplayActions from '../../../../fixtures/analyst/waveform-display';
import * as Common from '../../../../fixtures/common';

describe('Basic UI Smoke Test / Checklist', function() {
  before(() => {
    Common.visitApp();
    Common.openFavoriteAnalystInterval();
    Common.checkAreSignalDetectionsLoaded();
  });

  beforeEach(() => {
    // Keep cookie in between tests
    Cypress.Cookies.preserveOnce('InteractiveAnalysis');
  });

  it('Event list is populated', () => {
    EventDisplayActions.openEventDisplay();
    EventDisplayActions.checkEventListHasEventsWithAssociatedSds();
  });

  it('Can open event', () => {
    EventDisplayActions.openEventWithMostDetections();
  });

  it('Signal Detection list is populated', () => {
    SignalDetectionDisplayActions.openSignalDetectionDisplay();
    SignalDetectionDisplayActions.checkHasLoadedSds();
  });

  // Toggle predicted phases in the waveform display
  // Verify orange faded out predicted phases are shown
  it('Can show predicted phases', () => {
    WaveformDisplayActions.showPredictedPhases();
  });

  it('Can align on P', () => {
    WaveformDisplayActions.alignOnPredicted();
  });

  it('Can align on time', () => {
    WaveformDisplayActions.alignOnTime();
  });

  it('Can expand ASAR', () => {
    WaveformDisplayActions.openChannel('ASAR/fkb');
  });

  it('Can sort by station name', () => {
    WaveformDisplayActions.sortByStation();
  });

  it('Can show fk', () => {
    WaveformDisplayActions.minimizeWaveformDisplay();
    SignalDetectionDisplayActions.showFk();
  });

  it('Can locate event', () => {
    LocationDisplayActions.openLocationDisplay();
    LocationDisplayActions.maximizeLocationDisplay();
    LocationDisplayActions.locate();
    LocationDisplayActions.minimiseLocationDisplay();
  });

  it('Can refine network magnitude', () => {
    MagnitudeDisplayActions.openMagnitudeDisplay();
    MagnitudeDisplayActions.maximizeMagnitudeDisplay();
    MagnitudeDisplayActions.checkFirstMagnitudeOfType('MB');
  });
});
