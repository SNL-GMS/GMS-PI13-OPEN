import { Button, Icon, Intent, NonIdealState, Spinner } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import {
  CommonTypes,
  EventTypes,
  SignalDetectionQueries,
  SignalDetectionTypes,
  SignalDetectionUtils
} from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { addGlUpdateOnResize, addGlUpdateOnShow, Toaster } from '@gms/ui-util';
import { WeavessTypes } from '@gms/weavess';
import defer from 'lodash/defer';
import difference from 'lodash/difference';
import find from 'lodash/find';
import flattenDeep from 'lodash/flattenDeep';
import includes from 'lodash/includes';
import isEqual from 'lodash/isEqual';
import memoizeOne from 'memoize-one';
import React from 'react';
import { getDistanceToStationsForLocationSolutionId } from '~analyst-ui/common/utils/event-util';
import { isSdInstantMeasurementValue } from '~analyst-ui/common/utils/instance-of-util';
import {
  isPeakTroughInWarning,
  sortAndOrderSignalDetections
} from '~analyst-ui/common/utils/signal-detection-util';
import { toggleWaveformChannelFilters } from '~analyst-ui/common/utils/waveform-util';
import { systemConfig, userPreferences } from '~analyst-ui/config';
import { MaskDisplayFilter } from '~analyst-ui/config/user-preferences';
import { WeavessDisplay } from '../weavess-display';
import { WeavessDisplay as WeavessDisplayComponent } from '../weavess-display/weavess-display-component';
import { WaveformDisplayControls } from './components/waveform-display-controls';
import { DEFAULT_PANNING_PERCENT, ONE_THIRD, TWO_THIRDS_ROUNDED_UP } from './constants';
import {
  AlignWaveformsOn,
  KeyDirection,
  PanType,
  WaveformDisplayProps,
  WaveformDisplayState
} from './types';
import { WaveformClient } from './waveform-client';
import * as WaveformUtil from './weavess-stations-util';

/**
 * Primary waveform display component.
 */
export class WaveformDisplay extends React.PureComponent<
  WaveformDisplayProps,
  WaveformDisplayState
> {
  /** The toaster reference for user notification pop-ups */
  private static readonly toaster: Toaster = new Toaster();

  /** 2.5 minutes in seconds */
  private static readonly twoHalfMinInSeconds: number = 150;

  /** The waveform client, used to fetch and cache waveforms. */
  public readonly waveformClient: WaveformClient;

  /** Index of currently selected filter */
  private selectedFilterIndex: number = -1;

  /** A ref handle to the waveform display controls component */
  private waveformDisplayControls: WaveformDisplayControls;

  /** A ref handle to the weavess display component */
  private weavessDisplay: WeavessDisplayComponent;

  /** Handlers to unsubscribe from apollo subscriptions */
  private readonly unsubscribeHandlers: { (): void }[] = [];

  /** A Ref to the waveform display div */
  private waveformDisplayRef: HTMLDivElement | undefined;

  /**
   * A memoized function for determining the initial zoom range.
   * The memoization function caches the results using
   * the most recent argument and returns the results.
   *
   * @param currentTimeInterval the current time interval
   * @param currentOpenEvent the current open event
   * @param analystActivity the selected analyst activity
   * @param alignWaveformsOn the selected waveform alignment
   * @param phaseToAlignOn the selected phase to align on
   *
   * @returns a time range
   */
  private readonly memoizedGetInitialZoomWindow: (
    currentTimeInterval: CommonTypes.TimeRange,
    currentOpenEventId: string,
    analystActivity: AnalystWorkspaceTypes.AnalystActivity
  ) => WeavessTypes.TimeRange | undefined;

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Constructor.
   *
   * @param props The initial props
   */
  public constructor(props: WaveformDisplayProps) {
    super(props);
    this.waveformClient = new WaveformClient(this.props.client);
    this.memoizedGetInitialZoomWindow = memoizeOne(
      this.getInitialZoomWindow,
      /* tell memoize to use a deep comparison for complex objects */
      // tslint:disable-next-line: no-unbound-method
      isEqual
    );
    this.state = {
      stations: [],
      loadingWaveforms: false,
      loadingWaveformsPercentComplete: 0,
      maskDisplayFilters: userPreferences.colors.maskDisplayFilters,
      analystNumberOfWaveforms:
        this.props.analystActivity === AnalystWorkspaceTypes.AnalystActivity.eventRefinement
          ? systemConfig.eventRefinement.numberOfWaveforms
          : systemConfig.eventGlobalScan.numberOfWaveforms,
      showPredictedPhases: false,
      // the range of waveform data displayed initially
      currentTimeInterval: props.currentTimeInterval,
      alignWaveformsOn: AlignWaveformsOn.TIME,
      phaseToAlignOn: undefined,
      isMeasureWindowVisible: false,
      // the total viewable (scrollable) range of waveforms
      viewableInterval: undefined,
      currentOpenEventId: undefined
    };
  }

  /**
   * Updates the derived state from the next props.
   *
   * @param nextProps The next (new) props
   * @param prevState The previous state
   */
  public static getDerivedStateFromProps(
    nextProps: WaveformDisplayProps,
    prevState: WaveformDisplayState
  ): Partial<WaveformDisplayState> {
    if (
      !isEqual(nextProps.currentTimeInterval, prevState.currentTimeInterval) ||
      !isEqual(nextProps.currentOpenEventId, prevState.currentOpenEventId)
    ) {
      const hasTimeIntervalChanged = !isEqual(
        nextProps.currentTimeInterval,
        prevState.currentTimeInterval
      );
      // update current interval to the selected open interval time
      // reset the interval to the new one, overriding any extra data the user has loaded.
      return {
        stations: hasTimeIntervalChanged ? [] : prevState.stations,
        currentTimeInterval: nextProps.currentTimeInterval,
        viewableInterval: hasTimeIntervalChanged
          ? nextProps.currentTimeInterval && nextProps.analystActivity
            ? systemConfig.getDefaultTimeRange(
                nextProps.currentTimeInterval,
                nextProps.analystActivity
              )
            : undefined
          : prevState.viewableInterval,
        currentOpenEventId: nextProps.currentOpenEventId,
        alignWaveformsOn:
          nextProps.currentOpenEventId === null || nextProps.currentOpenEventId === ''
            ? AlignWaveformsOn.TIME
            : prevState.alignWaveformsOn,
        phaseToAlignOn:
          nextProps.currentOpenEventId === null || nextProps.currentOpenEventId === ''
            ? undefined
            : prevState.phaseToAlignOn
      };
    }

    // return null to indicate no change to state.
    return null;
  }

  /**
   * Invoked when the component mounted.
   */
  public componentDidMount() {
    const callback = () => {
      this.forceUpdate();
      if (this.weavessDisplay) {
        this.weavessDisplay.refresh();
      }
    };
    addGlUpdateOnShow(this.props.glContainer, callback);
    addGlUpdateOnResize(this.props.glContainer, callback);
  }

  /**
   * Invoked when the component has rendered.
   *
   * @param prevProps The previous props
   * @param prevState The previous state
   */
  public componentDidUpdate(prevProps: WaveformDisplayProps) {
    if (
      this.props.currentTimeInterval &&
      !isEqual(this.props.currentTimeInterval, prevProps.currentTimeInterval)
    ) {
      this.setupSubscriptions();
      this.waveformClient.stopAndClear();
    }

    // Checks the activity, and sets waveforms display amount based on result
    if (this.props.analystActivity !== prevProps.analystActivity) {
      const numWaveforms =
        this.props.analystActivity === AnalystWorkspaceTypes.AnalystActivity.eventRefinement
          ? systemConfig.eventRefinement.numberOfWaveforms
          : systemConfig.eventGlobalScan.numberOfWaveforms;
      this.setAnalystNumberOfWaveforms(numWaveforms);
    }

    const maybeToggleUp = this.props.keyPressActionQueue.get(
      AnalystWorkspaceTypes.KeyAction.TOGGLE_FILTERS_UP
    );
    if (!isNaN(maybeToggleUp) && maybeToggleUp > 0) {
      this.handleChannelFilterToggle(KeyDirection.UP);
      this.props.setKeyPressActionQueue(
        this.props.keyPressActionQueue.set(
          AnalystWorkspaceTypes.KeyAction.TOGGLE_FILTERS_UP,
          maybeToggleUp - 1
        )
      );
    }
    const maybeToggleDown = this.props.keyPressActionQueue.get(
      AnalystWorkspaceTypes.KeyAction.TOGGLE_FILTERS_DOWN
    );
    if (!isNaN(maybeToggleDown) && maybeToggleDown > 0) {
      this.handleChannelFilterToggle(KeyDirection.DOWN);
      this.props.setKeyPressActionQueue(
        this.props.keyPressActionQueue.set(
          AnalystWorkspaceTypes.KeyAction.TOGGLE_FILTERS_DOWN,
          maybeToggleDown - 1
        )
      );
    }
    this.updateWeavessStations();
  }

  /**
   * Invoked when the component will unmount.
   */
  public componentWillUnmount() {
    // unsubscribe from all current subscriptions
    this.unsubscribeHandlers.forEach(unsubscribe => unsubscribe());
    this.unsubscribeHandlers.length = 0;
  }

  /**
   * Renders the component.
   */
  // tslint:disable-next-line: cyclomatic-complexity
  public render() {
    // ***************************************
    // BEGIN NON-IDEAL STATE CASES
    // ***************************************
    // ! This case must be first
    // if the golden-layout container is not visible, do not attempt to render
    // the component, this is to prevent JS errors that may occur when trying to
    // render the component while the golden-layout container is hidden
    if (this.props.glContainer) {
      if (this.props.glContainer.isHidden) {
        return <NonIdealState />;
      }
    }
    if (!this.props.currentTimeInterval) {
      return (
        <NonIdealState
          icon={IconNames.TIMELINE_LINE_CHART}
          title="No waveform data currently loaded"
          description="Open an interval to load waveforms"
        />
      );
    }

    if (WaveformUtil.isLoading(this.props, this.state)) {
      const loadingDescription = this.props.defaultStationsQuery.loading
        ? 'Default station set...'
        : // tslint:disable-next-line:max-line-length
        this.state.loadingWaveforms
        ? `Data for current interval across ${this.props.defaultStationsQuery.defaultProcessingStations.length} channels...`
        : this.props.uiConfigurationQuery.loading
        ? 'Default filters...'
        : this.props.eventsInTimeRangeQuery.loading
        ? 'Events...'
        : this.props.signalDetectionsByStationQuery.loading
        ? 'Signal detections...'
        : this.props.qcMasksByChannelNameQuery.loading
        ? 'QC masks...'
        : 'Calculating distance to source';
      return (
        <NonIdealState
          action={
            <Spinner
              intent={Intent.PRIMARY}
              value={
                this.state.loadingWaveforms ? this.state.loadingWaveformsPercentComplete : undefined
              }
            />
          }
          title="Loading:"
          icon={IconNames.TIMELINE_LINE_CHART}
          description={loadingDescription}
        />
      );
    }

    if (WaveformUtil.isError(this.props)) {
      const errorDescription =
        this.props.defaultStationsQuery.error !== undefined
          ? this.props.defaultStationsQuery.error
          : // tslint:disable-next-line:max-line-length
          this.props.uiConfigurationQuery.error !== undefined
          ? this.props.uiConfigurationQuery.error
          : this.props.eventsInTimeRangeQuery.error !== undefined
          ? this.props.eventsInTimeRangeQuery.error
          : // tslint:disable-next-line:max-line-length
          this.props.signalDetectionsByStationQuery.error !== undefined
          ? this.props.signalDetectionsByStationQuery.error
          : this.props.qcMasksByChannelNameQuery.error;
      return (
        <NonIdealState
          icon={IconNames.ERROR}
          action={<Spinner intent={Intent.DANGER} />}
          title="Something went wrong!"
          description={errorDescription.message}
        />
      );
    }

    // ***************************************
    // END NON-IDEAL STATE CASES
    // ***************************************

    const stations: WeavessTypes.Station[] = this.displayNumberOfWaveforms(
      WaveformUtil.sortWaveformList(this.state.stations, this.props.selectedSortType)
    );

    const events = this.getWeavessEvents();
    const measureWindowSelection = this.getMeasureWindowSelection();
    // tslint:disable-next-line: max-line-length
    const customMeasureWindowLabel: React.FunctionComponent<WeavessTypes.LabelProps> = this.getCustomMeasureWindowLabel();
    return (
      <div
        ref={ref => (this.waveformDisplayRef = ref)}
        className={'waveform-display-window gms-body-text'}
      >
        <div
          className={'waveform-display-container'}
          data-cy={'waveform-display-container'}
          tabIndex={-1}
          onKeyDown={e => {
            this.onKeyPress(e);
          }}
        >
          <WaveformDisplayControls
            ref={ref => {
              if (ref) {
                this.waveformDisplayControls = ref;
              }
            }}
            defaultSignalDetectionPhase={this.props.defaultSignalDetectionPhase}
            currentSortType={this.props.selectedSortType}
            currentOpenEventId={this.props.currentOpenEventId}
            analystNumberOfWaveforms={this.state.analystNumberOfWaveforms}
            showPredictedPhases={this.state.showPredictedPhases}
            maskDisplayFilters={this.state.maskDisplayFilters}
            alignWaveformsOn={this.state.alignWaveformsOn}
            phaseToAlignOn={this.state.phaseToAlignOn}
            alignablePhases={WaveformUtil.getAlignablePhases(this.props)}
            glContainer={this.props.glContainer}
            measurementMode={this.props.measurementMode}
            setDefaultSignalDetectionPhase={this.props.setDefaultSignalDetectionPhase}
            setWaveformAlignment={this.setWaveformAlignment}
            setSelectedSortType={this.props.setSelectedSortType}
            setAnalystNumberOfWaveforms={this.setAnalystNumberOfWaveforms}
            setMaskDisplayFilters={this.setMaskDisplayFilters}
            setShowPredictedPhases={this.setShowPredictedPhases}
            setMode={(mode: AnalystWorkspaceTypes.WaveformDisplayMode) => this.setMode(mode)}
            toggleMeasureWindow={this.toggleMeasureWindowVisibility}
            pan={this.pan}
            onKeyPress={this.onKeyPress}
            isMeasureWindowVisible={this.state.isMeasureWindowVisible}
          />
          <WeavessDisplay
            ref={(ref: any) => {
              if (ref) {
                let componentRef = ref;
                // get the `wrapped` component reference; uses `{ withRef: true }`
                while (componentRef && !(componentRef instanceof WeavessDisplayComponent)) {
                  componentRef = componentRef.getWrappedInstance();
                }
                this.weavessDisplay = componentRef;
              }
            }}
            weavessProps={{
              startTimeSecs: this.state.viewableInterval.startTime,
              endTimeSecs: this.state.viewableInterval.endTime,
              showMeasureWindow:
                this.props.measurementMode.mode ===
                AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT,
              initialZoomWindow: this.memoizedGetInitialZoomWindow(
                this.props.currentTimeInterval,
                this.props.currentOpenEventId,
                this.props.analystActivity
              ),
              defaultZoomWindow: {
                startTimeSecs: this.props.currentTimeInterval.startTime,
                endTimeSecs: this.props.currentTimeInterval.endTime
              },
              stations,
              measureWindowSelection,
              events,
              configuration: {
                customMeasureWindowLabel
              }
            }}
            defaultWaveformFilters={
              this.props.uiConfigurationQuery &&
              this.props.uiConfigurationQuery.uiAnalystConfiguration
                ? this.props.uiConfigurationQuery.uiAnalystConfiguration.defaultFilters
                : []
            }
            defaultStations={
              this.props.defaultStationsQuery
                ? this.props.defaultStationsQuery.defaultProcessingStations
                : []
            }
            eventsInTimeRange={
              this.props.eventsInTimeRangeQuery
                ? this.props.eventsInTimeRangeQuery.eventsInTimeRange
                : []
            }
            signalDetectionsByStation={
              this.props.signalDetectionsByStationQuery
                ? this.props.signalDetectionsByStationQuery.signalDetectionsByStation
                : []
            }
            qcMasksByChannelName={
              this.props.qcMasksByChannelNameQuery
                ? this.props.qcMasksByChannelNameQuery.qcMasksByChannelName
                : []
            }
            measurementMode={this.props.measurementMode}
            defaultSignalDetectionPhase={this.props.defaultSignalDetectionPhase}
            setMeasurementModeEntries={this.props.setMeasurementModeEntries}
          />
        </div>
      </div>
    );
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Returns the current open event.
   */
  private readonly currentOpenEvent = (): EventTypes.Event =>
    this.props.eventsInTimeRangeQuery.eventsInTimeRange
      ? this.props.eventsInTimeRangeQuery.eventsInTimeRange.find(
          e => e.id === this.props.currentOpenEventId
        )
      : undefined

  /**
   * Returns the weavess event handler configuration.
   *
   * @returns the events
   */
  private readonly getWeavessEvents = (): WeavessTypes.Events => {
    const channelEvents: WeavessTypes.ChannelEvents = {
      labelEvents: {
        onChannelExpanded: this.onChannelExpanded
      },
      events: {
        onMeasureWindowUpdated: this.onMeasureWindowUpdated
      },
      onKeyPress: this.onKeyPress
    };

    return {
      stationEvents: {
        defaultChannelEvents: channelEvents,
        nonDefaultChannelEvents: channelEvents
      }
    };
  }

  /**
   * Returns the measure window selection based on the current `mode` and
   * the selected signal detection.
   *
   * @returns returns the measure window selection
   */
  private readonly getMeasureWindowSelection = (): WeavessTypes.MeasureWindowSelection => {
    let measureWindowSelection: WeavessTypes.MeasureWindowSelection;
    if (
      this.props.measurementMode.mode === AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT &&
      this.props.selectedSdIds.length === 1
    ) {
      // tslint:disable-next-line: max-line-length
      const signalDetection: SignalDetectionTypes.SignalDetection = this.props.signalDetectionsByStationQuery.signalDetectionsByStation.find(
        sd => sd.id === this.props.selectedSdIds[0]
      );

      if (signalDetection) {
        const station = this.state.stations.find(
          s => s.defaultChannel.id === signalDetection.stationName
        );

        const stationContainsSd: boolean =
          this.props.signalDetectionsByStationQuery.signalDetectionsByStation.find(
            s => s.id === signalDetection.id
          ) !== undefined;

        if (station && stationContainsSd) {
          const arrivalTime: number = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
            signalDetection.currentHypothesis.featureMeasurements
          ).value;
          const startTimeOffsetFromSignalDetection: number =
            systemConfig.measurementMode.displayTimeRange.startTimeOffsetFromSignalDetection;
          const endTimeOffsetFromSignalDetection: number =
            systemConfig.measurementMode.displayTimeRange.endTimeOffsetFromSignalDetection;
          measureWindowSelection = {
            stationId: station.name,
            channel: {
              ...station.defaultChannel,
              waveform: {
                ...station.defaultChannel.waveform,
                markers: {
                  ...station.defaultChannel.markers,
                  // only show the selection windows for the selected signal detection
                  selectionWindows:
                    station.defaultChannel.waveform.markers &&
                    station.defaultChannel.waveform.markers.selectionWindows
                      ? station.defaultChannel.waveform.markers.selectionWindows.filter(selection =>
                          selection.id.includes(this.props.selectedSdIds[0])
                        )
                      : undefined
                }
              }
            },
            startTimeSecs: arrivalTime + startTimeOffsetFromSignalDetection,
            endTimeSecs: arrivalTime + endTimeOffsetFromSignalDetection,
            isDefaultChannel: true
          };
        }
      }
    }
    return measureWindowSelection;
  }

  /**
   * Returns a custom measure window label for measurement mode.
   *
   * @returns a custom measure window label
   */
  private readonly getCustomMeasureWindowLabel = (): React.FunctionComponent<
    WeavessTypes.LabelProps
  > =>
    this.props.measurementMode.mode === AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT
      ? (props: WeavessTypes.LabelProps) => {
          const sdId =
            this.props.signalDetectionsByStationQuery && this.props.selectedSdIds.length === 1
              ? this.props.signalDetectionsByStationQuery.signalDetectionsByStation
                  .map(s => s.id)
                  .find(id => id === this.props.selectedSdIds[0])
              : undefined;

          const sd = sdId
            ? this.props.signalDetectionsByStationQuery.signalDetectionsByStation.find(
                s => s.id === sdId
              )
            : undefined;

          const amplitudeMeasurementValue:
            | SignalDetectionTypes.AmplitudeMeasurementValue
            | undefined = sd
            ? SignalDetectionUtils.findAmplitudeFeatureMeasurementValue(
                sd.currentHypothesis.featureMeasurements,
                SignalDetectionTypes.FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2
              )
            : undefined;

          if (!sd) {
            return <React.Fragment>{props.channel.name}</React.Fragment>;
          }

          const arrivalTime: number = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
            sd.currentHypothesis.featureMeasurements
          ).value;

          let amplitude: number;
          let period: number;
          let troughTime: number;
          let peakTime: number;
          let isWarning = true;

          if (amplitudeMeasurementValue) {
            amplitude = amplitudeMeasurementValue.amplitude.value;
            period = amplitudeMeasurementValue.period;
            troughTime = amplitudeMeasurementValue.startTime;
            peakTime = troughTime + period / 2; // display only period/2
            isWarning = isPeakTroughInWarning(arrivalTime, period, troughTime, peakTime);
          }

          const amplitudeTitle = amplitudeMeasurementValue
            ? 'Amplitude value'
            : 'Error: No measurement value available for amplitude';

          const periodTitle = amplitudeMeasurementValue
            ? !isWarning
              ? 'Period value'
              : `Warning: Period value must be between` +
                `[${systemConfig.measurementMode.peakTroughSelection.warning.min} - ` +
                `${systemConfig.measurementMode.peakTroughSelection.warning.max}]'`
            : 'Error: No measurement value available for period';

          return (
            <React.Fragment>
              {props.channel.name}
              <React.Fragment>
                <br />
                <div title={amplitudeTitle} style={{ whiteSpace: 'nowrap' }}>
                  A5/2:&nbsp;
                  {amplitudeMeasurementValue ? (
                    // tslint:disable-next-line: no-magic-numbers
                    amplitude.toFixed(3)
                  ) : (
                    <Icon title={amplitudeTitle} icon={IconNames.ERROR} intent={Intent.DANGER} />
                  )}
                </div>
                <div title={periodTitle} style={{ whiteSpace: 'nowrap' }}>
                  Period:
                  {amplitudeMeasurementValue ? (
                    <span
                      title={periodTitle}
                      style={{
                        color: isWarning
                          ? systemConfig.measurementMode.peakTroughSelection.warning.textColor
                          : undefined
                      }}
                    >
                      {' '}
                      {amplitudeMeasurementValue.period.toFixed(3)}s&nbsp;
                      {isWarning ? (
                        <Icon
                          title={periodTitle}
                          icon={IconNames.WARNING_SIGN}
                          color={systemConfig.measurementMode.peakTroughSelection.warning.textColor}
                        />
                      ) : (
                        undefined
                      )}
                    </span>
                  ) : (
                    <Icon title={periodTitle} icon={IconNames.ERROR} intent={Intent.DANGER} />
                  )}
                </div>
                {
                  <Button
                    small={true}
                    text="Next"
                    onClick={(event: React.MouseEvent<HTMLElement>) => {
                      event.stopPropagation();
                      this.selectNextAmplitudeMeasurement(sd.id);
                      this.props
                        .markAmplitudeMeasurementReviewed({
                          variables: { signalDetectionIds: [sd.id] }
                        })
                        .catch(e => {
                          UILogger.Instance().error(`failed to mark amplitude as reviewed: ${e}`);
                        });
                    }}
                  />
                }
              </React.Fragment>
            </React.Fragment>
          );
        }
      : undefined

  /**
   * Returns the initial zoom window time range.
   *
   * @param currentTimeInterval the current time interval
   * @param currentOpenEvent the current open event
   * @param analystActivity the selected analyst activity
   *
   * @returns a time range
   */
  private readonly getInitialZoomWindow = (
    currentTimeInterval: CommonTypes.TimeRange,
    currentOpenEventId: string,
    analystActivity: AnalystWorkspaceTypes.AnalystActivity
  ): WeavessTypes.TimeRange | undefined => {
    let initialZoomWindow = this.weavessDisplay
      ? this.weavessDisplay.getCurrentViewRangeInSeconds()
      : undefined;
    const currentOpenEvent = this.currentOpenEvent();
    if (
      currentOpenEvent &&
      analystActivity === AnalystWorkspaceTypes.AnalystActivity.eventRefinement
    ) {
      const hypothesis = currentOpenEvent.currentEventHypothesis.eventHypothesis;
      if (
        hypothesis.signalDetectionAssociations &&
        hypothesis.signalDetectionAssociations.length > 0
      ) {
        const paddingSecs = 60;
        initialZoomWindow = {
          startTimeSecs: hypothesis.preferredLocationSolution.locationSolution.location.time,
          endTimeSecs: hypothesis.associationsMaxArrivalTime + paddingSecs
        };
      }
    } else if (analystActivity === AnalystWorkspaceTypes.AnalystActivity.globalScan) {
      initialZoomWindow = {
        startTimeSecs: currentTimeInterval.startTime - WaveformDisplay.twoHalfMinInSeconds,
        endTimeSecs: currentTimeInterval.startTime + WaveformDisplay.twoHalfMinInSeconds
      };
    }

    return initialZoomWindow;
  }

  /**
   * Sets the mode.
   *
   * @param mode the mode configuration to set
   */
  private readonly setMode = (mode: AnalystWorkspaceTypes.WaveformDisplayMode) => {
    this.props.setMode(mode);

    // auto select the first signal detection if switching to MEASUREMENT mode
    if (mode === AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT) {
      const currentOpenEvent = this.currentOpenEvent();

      if (currentOpenEvent) {
        // tslint:disable-next-line: max-line-length
        const associatedSignalDetectionHypothesisIds = currentOpenEvent.currentEventHypothesis.eventHypothesis.signalDetectionAssociations
          .filter(assoc => !assoc.rejected)
          .map(association => association.signalDetectionHypothesis.id);

        const signalDetections = this.props.signalDetectionsByStationQuery.signalDetectionsByStation.filter(
          sd => this.checkIfSdIsFmPhaseAndAssociated(sd, associatedSignalDetectionHypothesisIds)
        );

        let signalDetectionToSelect: SignalDetectionTypes.SignalDetection;
        const distances = getDistanceToStationsForLocationSolutionId(
          currentOpenEvent,
          this.props.location.selectedPreferredLocationSolutionId
        );
        if (signalDetections.length > 0) {
          // sort the signal detections
          const sortedEntries = sortAndOrderSignalDetections(
            signalDetections,
            this.props.selectedSortType,
            distances
          );
          signalDetectionToSelect = sortedEntries[0];
          this.props.setSelectedSdIds([signalDetectionToSelect.id]);
        } else {
          this.props.setSelectedSdIds([]);
        }

        // mark the measure window as being visible; measurement mode auto shows the measure window
        this.setState({ isMeasureWindowVisible: true });
        // auto set the waveform alignment to align on the default phase
        this.setWaveformAlignment(
          AlignWaveformsOn.PREDICTED_PHASE,
          this.props.defaultSignalDetectionPhase,
          this.state.showPredictedPhases
        );

        // auto zoom the waveform display to match the zoom of the measure window
        if (signalDetectionToSelect) {
          const arrivalTime: number = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
            signalDetectionToSelect.currentHypothesis.featureMeasurements
          ).value;
          const startTimeOffsetFromSignalDetection: number =
            systemConfig.measurementMode.displayTimeRange.startTimeOffsetFromSignalDetection;
          const endTimeOffsetFromSignalDetection: number =
            systemConfig.measurementMode.displayTimeRange.endTimeOffsetFromSignalDetection;
          const startTimeSecs = arrivalTime + startTimeOffsetFromSignalDetection;
          const endTimeSecs = arrivalTime + endTimeOffsetFromSignalDetection;

          // adjust the zoom time window for the selected alignment
          this.zoomToTimeWindow(startTimeSecs, endTimeSecs);
        }
      }
    } else {
      // leaving measurement mode; mark the measurement window as not visible
      this.setState({ isMeasureWindowVisible: false });

      // adjust the zoom time window for the selected alignment
      this.zoomToTimeWindowForAlignment(this.state.alignWaveformsOn, this.state.phaseToAlignOn);
    }
  }

  /**
   * Check if the signal detection is FM Phase and Associated.
   *
   * @param sd the signal detection
   * @param associatedSignalDetectionHypothesisIds string ids
   * @returns a boolean determining if sd is associated and a measurement phase
   */
  private readonly checkIfSdIsFmPhaseAndAssociated = (
    sd: SignalDetectionTypes.SignalDetection,
    associatedSignalDetectionHypothesisIds: string[]
  ): boolean => {
    const phase = SignalDetectionUtils.findPhaseFeatureMeasurementValue(
      sd.currentHypothesis.featureMeasurements
    ).phase;
    // return if associated and a measurement phase
    return (
      includes(associatedSignalDetectionHypothesisIds, sd.currentHypothesis.id) &&
      includes(systemConfig.measurementMode.phases, phase)
    );
  }

  /**
   * Initialize and setup the graphql subscriptions on the apollo client.
   */
  private readonly setupSubscriptions = () => {
    // Unsubscribe from all current subscriptions
    this.unsubscribeHandlers.forEach(unsubscribe => unsubscribe());
    this.unsubscribeHandlers.length = 0;

    // Don't register subscriptions if the current time interval is undefined/null
    if (!this.props.currentTimeInterval) return;

    this.unsubscribeHandlers
      .push
      // TODO: this is throwing an error due to the schema changing, but not currently being used, when we need to
      // TODO: Implement late arriving data should look back into this, since it is more a proof of concept
      // this.props.defaultStationsQuery.subscribeToMore({
      //   document: WaveformSubscriptions.waveformSegmentsAddedSubscription,
      //   updateQuery: (prev: { defaultStations: ProcessingStationTypes.ProcessingStation }, cur) => {
      //     const currentInterval = this.state.viewableInterval;
      //     const data = cur.subscriptionData.data as WaveformTypes.WaveformSegmentsAddedSubscription;
      //     // For each newly-added waveform channel segment received via subscription...
      //     data.waveformChannelSegmentsAdded.forEach(segmentAdded => {
      //       // If the new segment overlaps the current interval,
      //       // Retrieve the waveform samples for the segment
      //       if (segmentAdded.startTime < currentInterval.endTime
      //         && segmentAdded.endTime > currentInterval.startTime) {

      //         const filterIds = this.props.defaultWaveformFiltersQuery
      //           .defaultWaveformFilters.map(filters => filters.id);

      //         this.waveformClient.fetchAndCacheWaveforms(
      //           [segmentAdded.channel.id],
      //           filterIds,
      //           Math.max(segmentAdded.startTime, currentInterval.startTime),
      //           Math.min(segmentAdded.endTime, currentInterval.endTime),
      //           () => {
      //             this.updateWaveformState();
      //           },
      //           () => {
      //             this.updateWeavessStations();
      //           },
      //           1
      //         )
      //           .catch(e => {
      //             UILogger.Instance().error(`setupSubscriptions error: ${e.message}`);
      //           });
      //       }
      //     });

      //     return prev;
      //   }
      // })
      ();
  }

  /**
   * Updates the waveform state on the controls.
   */
  private readonly updateWaveformState = () => {
    if (this.waveformDisplayControls) {
      this.waveformDisplayControls.setState({
        waveformState: this.waveformClient.state
      });
    }
  }

  /**
   * Load waveform data outside the current interval.
   * Assumes data has already been loaded, and the waveform cache has entries.
   *
   * @param startTimeSecs the start time seconds the time range to load
   * @param endTimeSecs the end time seconds of the time range to load
   */
  private readonly fetchDataOutsideInterval = (
    startTimeSecs: number,
    endTimeSecs: number
  ): void => {
    const channelIds = this.waveformClient.getWaveformChannelIds();
    const filterIds = this.props.uiConfigurationQuery.uiAnalystConfiguration.defaultFilters.map(
      filters => filters.id
    );

    // Retrieve waveform sample data for the channel IDs and input time range, adding the waveforms to the cache
    this.fetchSignalDetectionsOutsideInterval(startTimeSecs, endTimeSecs);
    this.waveformClient.fetchAndCacheWaveforms(
      channelIds,
      filterIds,
      startTimeSecs,
      endTimeSecs,
      () => {
        this.updateWaveformState();
      },
      () => {
        this.setState({
          viewableInterval: {
            startTime: Math.min(this.state.viewableInterval.startTime, startTimeSecs),
            endTime: Math.max(this.state.viewableInterval.endTime, endTimeSecs)
          }
        });
      }
    );
  }

  /**
   * Load signal detections outside the current interval.
   *
   * @param startTimeSecs the start time seconds the time range to load
   * @param endTimeSecs the end time seconds of the time range to load
   */
  private readonly fetchSignalDetectionsOutsideInterval = (
    startTimeSecs: number,
    endTimeSecs: number
  ) => {
    const variables: SignalDetectionTypes.SignalDetectionsByStationQueryArgs = {
      stationIds: this.props.defaultStationsQuery.defaultProcessingStations.map(
        station => station.name
      ),
      timeRange: {
        startTime: startTimeSecs,
        endTime: endTimeSecs
      }
    };
    this.props.signalDetectionsByStationQuery
      .fetchMore({
        query: SignalDetectionQueries.signalDetectionsByStationQuery,
        variables,
        updateQuery: (
          prev: {
            signalDetectionsByStation: SignalDetectionTypes.SignalDetection[];
          },
          cur
        ) => {
          const data = cur.fetchMoreResult as SignalDetectionTypes.SignalDetection[];
          const prevSignalDetections = prev.signalDetectionsByStation;
          const newSignalDetections = [...prevSignalDetections];
          if (data.length > 0) {
            data.forEach(signalDetection => {
              // Add the new signal detections
              if (!prevSignalDetections.find(sd => sd.id === signalDetection.id)) {
                newSignalDetections.push(signalDetection);
              }
            });
          }
          return {
            ...prev,
            signalDetectionsByStation: newSignalDetections
          };
        }
      })
      .catch();
  }

  /**
   * Updates the weavess stations based on the current state and props.
   */
  private readonly updateWeavessStations = () => {
    if (this.props.uiConfigurationQuery === undefined) {
      // tslint:disable-next-line: no-console
      console.log('UI Config props not set!');
    } else if (this.props.uiConfigurationQuery.uiAnalystConfiguration === undefined) {
      // tslint:disable-next-line: no-console
      console.log('UI Config props uiConfiguration not set!');
      // tslint:disable-next-line: no-console
      // console.dir(JSON.stringify(this.props.uiConfigurationQuery));
    } else if (
      this.props.uiConfigurationQuery.uiAnalystConfiguration.defaultFilters === undefined
    ) {
      // tslint:disable-next-line: no-console
      console.log('UI Config props filters not set!');
      // tslint:disable-next-line: no-console
      console.log(JSON.stringify(this.props.uiConfigurationQuery.uiAnalystConfiguration));
    }
    if (
      !this.props.currentTimeInterval ||
      !this.props.defaultStationsQuery ||
      !this.props.uiConfigurationQuery // ||
      //  !this.props.uiConfigurationQuery.uiConfiguration ||
      // !this.props.uiConfigurationQuery.uiAnalystConfiguration.defaultFilters
    ) {
      return;
    }
    const stationHeight = this.calculateStationHeight();
    const createWeavessStationsParameters = WaveformUtil.populateCreateWeavessStationsParameters(
      this.props,
      this.state,
      stationHeight,
      this.waveformClient
    );
    const weavessStations = WaveformUtil.createWeavessStations(createWeavessStationsParameters);
    if (!isEqual(this.state.stations, weavessStations)) {
      this.setState({
        stations: weavessStations
      });
    }
  }

  /**
   * Toggle the measure window visibility within weavess.
   */
  private readonly toggleMeasureWindowVisibility = () => {
    if (this.weavessDisplay) {
      this.weavessDisplay.toggleMeasureWindowVisibility();
    }
  }

  /**
   * Event handler for channel expansion
   *
   * @param channelId a Channel Id as a string
   */
  private readonly onChannelExpanded = (channelId: string) => {
    // Get the ids of all sub-channels
    const subChannelIds: string[] = flattenDeep<string>(
      this.props.defaultStationsQuery.defaultProcessingStations
        .find(station => station.name === channelId)
        .channels.map(channel => channel.name)
    );

    // Check if there are any new channel IDs whose waveform data we haven't already cached.
    const channelIdsToFetchAndCache = difference(
      subChannelIds,
      this.waveformClient.getWaveformChannelIds()
    );
    if (channelIdsToFetchAndCache && channelIdsToFetchAndCache.length > 0) {
      const filterIds = this.props.uiConfigurationQuery.uiAnalystConfiguration.defaultFilters.map(
        filters => filters.id
      );
      this.waveformClient.fetchAndCacheWaveforms(
        channelIdsToFetchAndCache,
        filterIds,
        this.state.viewableInterval.startTime,
        this.state.viewableInterval.endTime,
        () => {
          this.updateWaveformState();
        },
        () => {
          this.updateWeavessStations();
        }
      );
    }
  }

  /**
   * Event handler that is invoked and handled when the Measure Window is updated.
   *
   * @param isVisible true if the measure window is updated
   * @param channelId the unique channel id of the channel that the measure window on;
   * channel id is undefined if the measure window is not visible
   * @param startTimeSecs the start time in seconds of the measure window;
   * start time seconds is undefined if the measure window is not visible
   * @param endTimeSecs the end time in seconds of the measure window;
   * end time seconds is undefined if the measure window is not visible
   * @param heightPx the height in pixels of the measure window;
   * height pixels is undefined if the measure window is not visible
   */
  private readonly onMeasureWindowUpdated = (
    isVisible: boolean,
    channelId?: string,
    startTimeSecs?: number,
    endTimeSecs?: number,
    heightPx?: number
  ) => {
    this.setState({ isMeasureWindowVisible: isVisible });
  }

  /**
   * Event handler for when a key is pressed
   *
   * @param e mouse event as React.MouseEvent<HTMLDivElement>
   * @param clientX x location of where the key was pressed
   * @param clientY y location of where the key was pressed
   * @param channelId a Channel Id as a string
   * @param timeSecs epoch seconds of where the key was pressed in respect to the data
   */
  private readonly onKeyPress = (
    e: React.KeyboardEvent<HTMLDivElement>,
    clientX?: number,
    clientY?: number,
    channelId?: string,
    timeSecs?: number
  ) => {
    // handle the default WEAVESS onKeyPressEvents
    if (this.weavessDisplay) {
      this.weavessDisplay.onKeyPress(e, clientX, clientY, channelId, timeSecs);
    }

    if (e.key === 'Escape') {
      this.selectedFilterIndex = -1;
    } else if (e.altKey) {
      switch (e.nativeEvent.code) {
        case 'KeyN':
          this.selectNextAmplitudeMeasurement(this.props.selectedSdIds[0]);
          break;
        case 'KeyP':
          if (this.props.currentOpenEventId) {
            if (this.state.alignWaveformsOn === AlignWaveformsOn.TIME) {
              this.setWaveformAlignment(
                AlignWaveformsOn.PREDICTED_PHASE,
                CommonTypes.PhaseType.P,
                true
              );
            } else {
              this.setWaveformAlignment(
                AlignWaveformsOn.TIME,
                undefined,
                this.state.showPredictedPhases
              );
            }
          } else {
            WaveformDisplay.toaster.toastInfo('Open an event to change waveform alignment');
          }
          break;
        case 'KeyA':
          if (this.waveformDisplayControls) {
            this.waveformDisplayControls.toggleAlignmentDropdown();
          }
          break;
        default:
          return;
      }
    } else if (e.ctrlKey || e.metaKey) {
      switch (e.key) {
        case '-':
          this.setAnalystNumberOfWaveforms(this.state.analystNumberOfWaveforms + 1);
          return;
        case '=':
          this.setAnalystNumberOfWaveforms(this.state.analystNumberOfWaveforms - 1);
          return;
        case 'ArrowLeft':
          this.pan(PanType.Left).catch(error => `Error panning left: ${error}`);
          e.preventDefault();
          return;
        case 'ArrowRight':
          this.pan(PanType.Right).catch(error => `Error panning right: ${error}`);
          e.preventDefault();
          return;
        default:
          return;
      }
    }
  }

  /**
   * Updates the value of the selected filter index
   * @param index index value
   */
  private readonly setSelectedFilterIndex = (index: number): void => {
    this.selectedFilterIndex = index;
  }

  /**
   * Set the mask filters selected in the qc mask legend.
   *
   * @param key the unique key identifier
   * @param maskDisplayFilter the mask display filter
   */
  private readonly setMaskDisplayFilters = (key: string, maskDisplayFilter: MaskDisplayFilter) => {
    this.setState(
      {
        maskDisplayFilters: {
          ...this.state.maskDisplayFilters,
          [key]: maskDisplayFilter
        }
      },
      () => {
        this.updateWeavessStations();
      }
    );
  }

  /**
   * Select the next amplitude measurement when in measurement mode
   *
   * @param signalDetectionId current selected signal detection Id
   */
  private readonly selectNextAmplitudeMeasurement = (signalDetectionId: string): void => {
    if (this.props.measurementMode.mode !== AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT) {
      return;
    }

    const currentOpenEvent = this.currentOpenEvent();
    if (currentOpenEvent) {
      // tslint:disable-next-line: max-line-length
      const associatedSignalDetectionHypothesisIds = currentOpenEvent.currentEventHypothesis.eventHypothesis.signalDetectionAssociations
        .filter(assoc => !assoc.rejected)
        .map(association => association.signalDetectionHypothesis.id);

      const stationIds = this.state.stations.map(station => station.name);

      // get all of the signal detections for the viewable stations
      const signalDetections = this.props.signalDetectionsByStationQuery.signalDetectionsByStation.filter(
        sd => includes(stationIds, sd.stationName)
      );
      const distances = getDistanceToStationsForLocationSolutionId(
        currentOpenEvent,
        this.props.location.selectedPreferredLocationSolutionId
      );
      // sort the signal detections
      const sortedEntries = sortAndOrderSignalDetections(
        signalDetections,
        this.props.selectedSortType,
        distances
      );

      let nextSignalDetectionToSelect: SignalDetectionTypes.SignalDetection;
      if (sortedEntries.length > 0) {
        const foundIndex: number = sortedEntries.findIndex(sd => sd.id === signalDetectionId);
        let index = foundIndex + 1;
        if (index >= sortedEntries.length) {
          index = 0;
        }

        const isAssociatedSdAndInPhaseList = (sd: SignalDetectionTypes.SignalDetection) =>
          this.checkIfSdIsFmPhaseAndAssociated(sd, associatedSignalDetectionHypothesisIds);

        // ensure that the selected index is for an associated signal detection and in the
        // list of phase measurements; increment until start searching from the current index found above
        nextSignalDetectionToSelect = find(sortedEntries, isAssociatedSdAndInPhaseList, index);

        // if the signal detection id is undefined, continue searching, but at index 0
        if (!nextSignalDetectionToSelect) {
          nextSignalDetectionToSelect = find(sortedEntries, isAssociatedSdAndInPhaseList);
        }
      }
      this.props.setSelectedSdIds([nextSignalDetectionToSelect.id]);
    }
  }

  /**
   * Display the number of waveforms chosen by the analyst
   * Also updates the state variable holding the selection
   */
  private readonly displayNumberOfWaveforms = (
    stations: WeavessTypes.Station[]
  ): WeavessTypes.Station[] => {
    const height = this.calculateStationHeight();

    stations.forEach(station => {
      station.defaultChannel.height = height;
      if (station.nonDefaultChannels) {
        station.nonDefaultChannels.forEach(ndc => (ndc.height = height));
      }
    });
    return stations;
  }

  /**
   * Calculate height for the station based of number of display
   */
  private readonly calculateStationHeight = (): number => {
    const waveformDisplayButtonsAndAxisHeightPx = 100;
    return !this.waveformDisplayRef || !this.waveformDisplayRef.clientHeight
      ? systemConfig.defaultWeavessConfiguration.stationHeightPx
      : // tslint:disable-next-line:max-line-length
        (this.waveformDisplayRef.clientHeight - waveformDisplayButtonsAndAxisHeightPx) /
          this.state.analystNumberOfWaveforms;
  }

  /**
   * Sets the waveform alignment and adjust the sort type if necessary.
   *
   * @param alignWaveformsOn the waveform alignment setting
   * @param phaseToAlignOn the phase to align on
   * @param showPredictedPhases true if predicted phases should be displayed
   */
  private readonly setWaveformAlignment = (
    alignWaveformsOn: AlignWaveformsOn,
    phaseToAlignOn: CommonTypes.PhaseType,
    showPredictedPhases: boolean
  ) => {
    this.setState({ alignWaveformsOn, phaseToAlignOn, showPredictedPhases });

    if (alignWaveformsOn !== AlignWaveformsOn.TIME && phaseToAlignOn) {
      this.props.setSelectedSortType(AnalystWorkspaceTypes.WaveformSortType.distance);

      // adjust the zoom time window for the selected alignment
      this.zoomToTimeWindowForAlignment(alignWaveformsOn, phaseToAlignOn);
    }
  }

  /**
   * Sets the waveform alignment zoom time window for the given alignment setting.
   *
   * @param alignWaveformsOn the waveform alignment setting
   * @param phaseToAlignOn the phase to align on
   */
  private readonly zoomToTimeWindowForAlignment = (
    alignWaveformsOn: AlignWaveformsOn,
    phaseToAlignOn: CommonTypes.PhaseType
  ) => {
    if (this.weavessDisplay) {
      if (alignWaveformsOn !== AlignWaveformsOn.TIME) {
        const predictedPhases = WaveformUtil.getFeaturePredictionsForOpenEvent(this.props).filter(
          fp =>
            fp.phase === phaseToAlignOn &&
            fp.predictionType === SignalDetectionTypes.FeatureMeasurementTypeName.ARRIVAL_TIME
        );
        if (predictedPhases && predictedPhases.length > 0) {
          predictedPhases.sort((a, b) => {
            if (
              isSdInstantMeasurementValue(a.predictedValue) &&
              isSdInstantMeasurementValue(b.predictedValue)
            ) {
              const aValue = a.predictedValue.value;
              const bValue = b.predictedValue.value;
              return aValue - bValue;
            }
          });
          const earliestTime: number = isSdInstantMeasurementValue(
            predictedPhases[0].predictedValue
          )
            ? predictedPhases[0].predictedValue.value
            : undefined;
          const prevZoomInterval = this.weavessDisplay.getCurrentViewRangeInSeconds();
          const range = prevZoomInterval.endTimeSecs - prevZoomInterval.startTimeSecs;
          const initialZoomWindow = {
            startTimeSecs: earliestTime - range * ONE_THIRD,
            endTimeSecs: earliestTime + range * TWO_THIRDS_ROUNDED_UP
          };
          this.zoomToTimeWindow(initialZoomWindow.startTimeSecs, initialZoomWindow.endTimeSecs);
        }
      }
    }
  }

  /**
   * Sets the number of waveforms to be displayed.
   *
   * @param value the number of waveforms to display (number)
   * @param valueAsString the number of waveforms to display (string)
   */
  private readonly setAnalystNumberOfWaveforms = (value: number, valueAsString?: string) => {
    const base = 10;
    let analystNumberOfWaveforms = value;

    if (valueAsString) {
      // tslint:disable-next-line:no-parameter-reassignment
      valueAsString = valueAsString.replace(/e|\+|-/, '');
      analystNumberOfWaveforms = isNaN(parseInt(valueAsString, base))
        ? this.state.analystNumberOfWaveforms
        : parseInt(valueAsString, base);
    }

    // Minimum number of waveforms must be 1
    if (analystNumberOfWaveforms < 1) {
      analystNumberOfWaveforms = 1;
    }

    if (this.state.analystNumberOfWaveforms !== analystNumberOfWaveforms) {
      this.setState({
        analystNumberOfWaveforms
      });
    }
  }

  /**
   * Sets the show predicted phases state.
   *
   * @param showPredictedPhases if true shows predicted phases; false otherwise
   */
  private readonly setShowPredictedPhases = (showPredictedPhases: boolean) =>
    this.setState({ showPredictedPhases })

  /**
   * Pan the waveform display.
   *
   * @param panDirection the pan direction
   */
  private readonly pan = async (panDirection: PanType) => {
    if (this.weavessDisplay) {
      const currentWeavessViewRange: WeavessTypes.TimeRange = this.weavessDisplay.getCurrentViewRangeInSeconds();
      const interval: number = Math.abs(
        currentWeavessViewRange.endTimeSecs - currentWeavessViewRange.startTimeSecs
      );
      const timeToPanBy: number = Math.ceil(interval * DEFAULT_PANNING_PERCENT);

      const pannedViewTimeInterval: WeavessTypes.TimeRange =
        panDirection === PanType.Left
          ? {
              startTimeSecs: Number(currentWeavessViewRange.startTimeSecs) - timeToPanBy,
              endTimeSecs: Number(currentWeavessViewRange.endTimeSecs) - timeToPanBy
            }
          : {
              startTimeSecs: Number(currentWeavessViewRange.startTimeSecs) + timeToPanBy,
              endTimeSecs: Number(currentWeavessViewRange.endTimeSecs) + timeToPanBy
            };

      const possibleRangeOfDataToLoad: WeavessTypes.TimeRange =
        panDirection === PanType.Left
          ? {
              startTimeSecs: pannedViewTimeInterval.startTimeSecs,
              endTimeSecs: this.state.viewableInterval.startTime
            }
          : {
              startTimeSecs: this.state.viewableInterval.endTime,
              endTimeSecs: pannedViewTimeInterval.endTimeSecs
            };

      // determine if we need to load data or just pan the current view
      // floor/ceil the values to minimize the chance of erroneous reloading
      if (
        Math.ceil(possibleRangeOfDataToLoad.startTimeSecs) <
          Math.floor(this.state.viewableInterval.startTime) ||
        Math.floor(possibleRangeOfDataToLoad.endTimeSecs) >
          Math.ceil(this.state.viewableInterval.endTime)
      ) {
        this.fetchDataOutsideInterval(
          possibleRangeOfDataToLoad.startTimeSecs,
          possibleRangeOfDataToLoad.endTimeSecs
        );
        this.zoomToTimeWindow(
          pannedViewTimeInterval.startTimeSecs,
          pannedViewTimeInterval.endTimeSecs
        );
      } else {
        this.zoomToTimeWindow(
          pannedViewTimeInterval.startTimeSecs,
          pannedViewTimeInterval.endTimeSecs
        );
      }
    }
    return;
  }

  /**
   * Zooms the (WEAVESS) waveform display to a specific time range.
   *
   * @param startTimeSecs the start time in seconds
   * @param endTimeSecs the end time in seconds
   */
  private readonly zoomToTimeWindow = (startTimeSecs: number, endTimeSecs: number) => {
    if (this.weavessDisplay) {
      defer(() => {
        this.weavessDisplay.zoomToTimeWindow(startTimeSecs, endTimeSecs);
      });
    }
  }

  /**
   * Handles when a filter is toggled
   * @param direction the keypress that triggered the toggle
   */
  private readonly handleChannelFilterToggle = (direction: KeyDirection) => {
    if (this.weavessDisplay) {
      const toggleFilterResults = toggleWaveformChannelFilters(
        direction,
        this.weavessDisplay.state.selectedChannels,
        this.props.uiConfigurationQuery.uiAnalystConfiguration.defaultFilters,
        this.props.defaultStationsQuery.defaultProcessingStations,
        this.selectedFilterIndex,
        this.props.channelFilters
      );
      this.setSelectedFilterIndex(toggleFilterResults.newFilterIndex);
      this.props.setChannelFilters(toggleFilterResults.channelFilters);
    }
  }
}
// tslint:disable-next-line:max-file-line-count
