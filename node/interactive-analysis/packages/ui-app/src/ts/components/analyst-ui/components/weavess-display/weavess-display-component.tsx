import { ContextMenu, NonIdealState } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import {
  CommonTypes,
  EventTypes,
  QcMaskTypes,
  SignalDetectionTypes,
  SignalDetectionUtils
} from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { addGlUpdateOnResize, addGlUpdateOnShow, Toaster } from '@gms/ui-util';
import { Weavess, WeavessMessages, WeavessTypes } from '@gms/weavess';
import {
  WaveformDisplayProps,
  WaveformDisplayProps as WeavessProps
} from '@gms/weavess/lib/components/waveform-display/types';
import defaultsDeep from 'lodash/defaultsDeep';
import includes from 'lodash/includes';
import union from 'lodash/union';
import React from 'react';
import { QcMaskContextMenu } from '~analyst-ui/common/context-menus';
import {
  setPhaseContextMenu,
  SignalDetectionContextMenu
} from '~analyst-ui/common/context-menus/signal-detection-context-menu';
import { QcMaskForm, QcMaskOverlap, SignalDetectionDetails } from '~analyst-ui/common/dialogs';
import { QcMaskDialogBoxType } from '~analyst-ui/common/dialogs/types';
import {
  calculateAmplitudeMeasurementValue,
  determineMinMaxForPeakTroughForSignalDetection,
  getSignalDetectionBeams,
  getWaveformValueForSignalDetection,
  isInConflictAndNotAssociatedToOpenEvent,
  scaleAmplitudeMeasurementValue
} from '~analyst-ui/common/utils/signal-detection-util';
import { getWaveformFilterForMode } from '~analyst-ui/common/utils/waveform-util';
import { systemConfig } from '~analyst-ui/config';
import { QcMaskCategory } from '~analyst-ui/config/system-config';
import { gmsColors } from '~scss-config/color-preferences';
import { WeavessDisplayProps, WeavessDisplayState } from './types';

/**
 * Primary waveform display component.
 */
export class WeavessDisplay extends React.PureComponent<WeavessDisplayProps, WeavessDisplayState> {
  /** The toaster reference for user notification pop-ups */
  private static readonly toaster: Toaster = new Toaster();

  /** A ref handle to the weavess component */
  private weavess: Weavess;

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Constructor.
   *
   * @param props The initial props
   */
  public constructor(props: WeavessDisplayProps) {
    super(props);
    this.state = {
      selectedChannels: [],
      qcMaskModifyInterval: undefined,
      selectedQcMask: undefined
    };
  }

  /**
   * Invoked when the component mounted.
   */
  public componentDidMount() {
    const callback = () => {
      this.forceUpdate();
      this.refresh();
    };
    addGlUpdateOnShow(this.props.glContainer, callback);
    addGlUpdateOnResize(this.props.glContainer, callback);
  }

  /**
   * Renders the component.
   */
  public render() {
    // ***************************************
    // BEGIN NONE IDEAL STATE CASES
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
        />
      );
    }

    // ***************************************
    // END NONE IDEAL STATE CASES
    // ***************************************

    const weavessProps: Partial<WaveformDisplayProps> = {};
    defaultsDeep(weavessProps, this.props.weavessProps, this.defaultWeavessProps());

    // Selection for modifying QC Mask
    if (this.state.qcMaskModifyInterval) {
      const maskSelectionWindow: WeavessTypes.SelectionWindow = {
        id: 'selection-qc-mask-modify',
        startMarker: {
          id: 'maskStart',
          color: gmsColors.gmsMain,
          lineStyle: WeavessTypes.LineStyle.DASHED,
          timeSecs: this.state.qcMaskModifyInterval.startTime
        },
        endMarker: {
          id: 'maskEnd',
          color: gmsColors.gmsMain,
          lineStyle: WeavessTypes.LineStyle.DASHED,
          timeSecs: this.state.qcMaskModifyInterval.endTime
        },
        isMoveable: true,
        color: 'rgba(255,255,255,0.2)'
      };
      // add to the selection windows; do not overwrite
      if (!weavessProps.markers) weavessProps.markers = {};
      if (!weavessProps.markers.selectionWindows) {
        weavessProps.markers.selectionWindows = [];
      }
      weavessProps.markers.selectionWindows.push(maskSelectionWindow);
    }

    const currentOpenEvent = this.props.eventsInTimeRange.find(
      e => e.id === this.props.currentOpenEventId
    );

    return (
      <React.Fragment>
        <div className={'weavess-container'} tabIndex={0}>
          <div className={'weavess-container__wrapper'}>
            {weavessProps.stations.length > 0 ? (
              <Weavess
                ref={ref => {
                  if (ref) {
                    this.weavess = ref;
                  }
                }}
                {
                  // tslint:disable-next-line: no-unnecessary-type-assertion
                  ...(weavessProps as WaveformDisplayProps)
                }
              />
            ) : (
              <NonIdealState
                icon={IconNames.TIMELINE_LINE_CHART}
                title={
                  this.props.measurementMode.mode ===
                    AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT &&
                  currentOpenEvent.currentEventHypothesis.eventHypothesis
                    .signalDetectionAssociations.length < 1
                    ? 'Unable to enter measurement mode: No associated ' +
                      'signal detections available'
                    : 'No Waveforms to display'
                }
              />
            )}
          </div>
        </div>
      </React.Fragment>
    );
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Refreshes the WEAVESS display.
   * This function is helpful when the window resizes to ensure
   * that the current zoom display is maintained.
   */
  public readonly refresh = () => {
    if (this.weavess) {
      this.weavess.refresh();
      const currentViewTimeRange = this.getCurrentViewRangeInSeconds();
      this.weavess.zoomToTimeWindow(
        currentViewTimeRange.startTimeSecs,
        currentViewTimeRange.endTimeSecs
      );
    }
  }

  /**
   * Returns true if the measure window is visible; false otherwise.
   *
   * @returns true if visible; false otherwise
   */
  public readonly isMeasureWindowVisible = (): boolean =>
    this.weavess ? this.weavess.isMeasureWindowVisible() : false

  /**
   * Returns the currently displayed viewTimeInterval
   * The start time seconds and end time seconds of the
   * currently displayed view of the waveforms.
   *
   * @returns the current viewable timerange
   */
  public readonly getCurrentViewRangeInSeconds = (): WeavessTypes.TimeRange => {
    if (this.weavess) {
      return this.weavess ? this.weavess.getCurrentViewRangeInSeconds() : undefined;
    }
  }

  /**
   * Toggle the measure window visibility.
   */
  public readonly toggleMeasureWindowVisibility = (): void => {
    if (this.weavess) {
      this.weavess.toggleMeasureWindowVisability();
    }
  }

  /**
   * Zooms to the provided time range [startTimeSecs, endTimeSecs].
   *
   * @param startTimeSecs the start time in seconds
   * @param endTimeSecs the end time in seconds
   */
  public readonly zoomToTimeWindow = (startTimeSecs: number, endTimeSecs: number) => {
    if (this.weavess) {
      this.weavess.zoomToTimeWindow(startTimeSecs, endTimeSecs);
    }
  }

  /**
   * Event handler for when a key is pressed
   *
   * @param e mouse event as React.MouseEvent<HTMLDivElement>
   * @param clientX x location of where the key was pressed
   * @param clientY y location of where the key was pressed
   * @param channelName a channel name as a string
   * @param timeSecs epoch seconds of where the key was pressed in respect to the data
   */
  public readonly onKeyPress = (
    e: React.KeyboardEvent<HTMLDivElement>,
    clientX: number,
    clientY: number,
    channelName: string,
    timeSecs: number
  ) => {
    if (e.key === 'Escape') {
      if (this.state.selectedQcMask) {
        document.body.removeEventListener('click', this.onBodyClick, {
          capture: true
        });
        this.deselectQCMask();
      }
      this.setState({
        selectedChannels: []
      });
      this.props.setSelectedSdIds([]);
    } else if (e.ctrlKey || e.metaKey) {
      switch (e.key) {
        case 'p':
          e.preventDefault();
          if (clientX && clientY) {
            const sds = this.props.signalDetectionsByStation.filter(sd =>
              includes(this.props.selectedSdIds, sd.id)
            );
            const openEvent = this.props.eventsInTimeRange.find(
              ev => this.props.currentOpenEventId === ev.id
            );
            if (sds && openEvent && !isInConflictAndNotAssociatedToOpenEvent(sds, openEvent)) {
              this.showRephaseMenu(clientX, clientY);
            } else {
              WeavessDisplay.toaster.toastInfo(WeavessMessages.signalDetectionInConflict);
            }
          }
          return;
        case 'f':
          this.markSelectedSignalDetectionsToShowFk();
          return;
        case 'a':
          this.selectAllParentChannels();
          return;
        default:
          return;
      }
    }
  }

  /**
   * Returns the default weavess default channel event handlers.
   */
  private readonly defaultWeavessDefaultChannelEvents = (): WeavessTypes.ChannelEvents => ({
    labelEvents: {
      onChannelExpanded: this.onChannelExpanded,
      onChannelCollapsed: this.onChannelCollapsed,
      onChannelLabelClick: this.onChannelLabelClick
    },
    events: {
      onContextMenu: this.onContextMenu,
      onChannelClick: this.onChannelClick,
      onSignalDetectionContextMenu: this.onSignalDetectionContextMenu,
      onSignalDetectionClick: this.onSignalDetectionClick,
      onSignalDetectionDragEnd: this.onSignalDetectionDragEnd,
      onMaskClick: undefined,
      onMaskContextClick: undefined,
      onMaskCreateDragEnd: undefined,
      onMeasureWindowUpdated: this.onMeasureWindowUpdated,
      onUpdateMarker: this.onUpdateChannelMarker,
      onUpdateSelectionWindow: this.onUpdateChannelSelectionWindow,
      onClickSelectionWindow: this.onClickChannelSelectionWindow
    },
    onKeyPress: this.onKeyPress
  })

  /**
   * Returns the default weavess non-default channel event handlers.
   */
  private readonly defaultWeavessNonDefaultChannelEvents = (): WeavessTypes.ChannelEvents => ({
    labelEvents: {
      onChannelExpanded: this.onChannelExpanded,
      onChannelCollapsed: this.onChannelCollapsed,
      onChannelLabelClick: this.onChannelLabelClick
    },
    events: {
      onContextMenu: this.onContextMenu,
      onChannelClick: this.onChannelClick,
      onSignalDetectionContextMenu: undefined,
      onSignalDetectionClick: undefined,
      onSignalDetectionDragEnd: undefined,
      onMaskClick: this.onMaskClick,
      onMaskContextClick: this.onMaskContextClick,
      onMaskCreateDragEnd: this.onMaskCreateDragEnd,
      onMeasureWindowUpdated: this.onMeasureWindowUpdated,
      onUpdateMarker: this.onUpdateChannelMarker,
      onUpdateSelectionWindow: this.onUpdateChannelSelectionWindow
    },
    onKeyPress: this.onKeyPress
  })

  /**
   * Returns the default weavess event handler definitions.
   */
  private readonly defaultWeavessEvents = (): WeavessTypes.Events => ({
    stationEvents: {
      defaultChannelEvents: this.defaultWeavessDefaultChannelEvents(),
      nonDefaultChannelEvents: this.defaultWeavessNonDefaultChannelEvents()
    },
    onUpdateMarker: this.onUpdateMarker,
    onUpdateSelectionWindow: this.onUpdateSelectionWindow
  })

  /**
   * Returns the default weavess props.
   */
  private readonly defaultWeavessProps = (): Partial<WeavessProps> => ({
    selectChannel: this.selectChannel,
    clearSelectedChannels: this.clearSelectedChannels,
    selections: {
      signalDetections: this.props.selectedSdIds,
      channels: this.state.selectedChannels
    },
    configuration: {
      shouldRenderWaveforms: true,
      shouldRenderSpectrograms: false,
      hotKeys: {
        amplitudeScale: systemConfig.defaultWeavessHotKeyOverrides.amplitudeScale,
        amplitudeScaleSingleReset:
          systemConfig.defaultWeavessHotKeyOverrides.amplitudeScaleSingleReset,
        amplitudeScaleReset: systemConfig.defaultWeavessHotKeyOverrides.amplitudeScaleReset,
        maskCreate: systemConfig.defaultWeavessHotKeyOverrides.qcMaskCreate
      },
      defaultChannel: {
        disableMeasureWindow: false,
        disableSignalDetectionModification: false,
        disableMaskModification: true
      },
      nonDefaultChannel: {
        disableMeasureWindow: false,
        disableSignalDetectionModification: true,
        disableMaskModification: false
      }
    },
    events: this.defaultWeavessEvents(),
    flex: false
  })

  /**
   * Event handler for clicking on mask
   *
   * @param event mouse event as React.MouseEvent<HTMLDivElement>
   * @param channelName a channel name as a string
   * @param maskId mask Ids as a string array
   * @param maskCreateHotKey (optional) indicates a hotkey is pressed
   */
  private readonly onMaskClick = (
    event: React.MouseEvent<HTMLDivElement>,
    channelName: string,
    masks: string[],
    maskCreateHotKey?: boolean
  ) => {
    event.preventDefault();

    if (masks && masks.length > 0) {
      const qcMasks: QcMaskTypes.QcMask[] = this.props.qcMasksByChannelName.filter(m =>
        includes(masks, m.id)
      );
      // If shift is pressed, modify mask
      if (event.shiftKey) {
        // If more than one mask, open multi-mask dialog
        if (qcMasks.length > 1) {
          ContextMenu.show(
            <QcMaskOverlap
              masks={qcMasks}
              contextMenuCoordinates={{
                xPx: event.clientX,
                yPx: event.clientY
              }}
              openNewContextMenu={this.openQCMaskMenu}
              selectMask={this.selectMask}
            />,
            { left: event.clientX, top: event.clientY },
            undefined,
            true
          );
        } else {
          const mask = qcMasks[0];
          // Otherwise use the single mask dialog box
          ContextMenu.show(
            <QcMaskForm
              mask={mask}
              applyChanges={this.handleQcMaskMutation}
              qcMaskDialogBoxType={
                QcMaskCategory[mask.currentVersion.category] === QcMaskCategory.REJECTED
                  ? QcMaskDialogBoxType.View
                  : QcMaskDialogBoxType.Modify
              }
            />,
            { left: event.clientX, top: event.clientY },
            undefined,
            true
          );
        }
      } else if (maskCreateHotKey) {
        // Else, begin interactive modification
        if (qcMasks.length === 1) {
          const mask = qcMasks[0];
          if (mask.currentVersion.category !== QcMaskCategory.REJECTED.toUpperCase()) {
            this.selectMask(mask);
          } else {
            WeavessDisplay.toaster.toastWarn('Cannot modify a rejected mask');
          }
        } else {
          ContextMenu.show(
            <QcMaskOverlap
              masks={qcMasks}
              contextMenuCoordinates={{
                xPx: event.clientX,
                yPx: event.clientY
              }}
              openNewContextMenu={this.openQCMaskMenu}
              selectMask={this.selectMask}
            />,
            { left: event.clientX, top: event.clientY },
            undefined,
            true
          );
        }
      }
    }
  }

  /**
   * Selects a mask and sets up boundary indicators.
   *
   * @param mask the qc mask to select
   */
  private readonly selectMask = (mask: QcMaskTypes.QcMask) => {
    ContextMenu.hide();
    if (this.state.selectedQcMask === undefined || this.state.selectedQcMask === null) {
      const qcMaskModifyInterval: CommonTypes.TimeRange = {
        startTime: mask.currentVersion.startTime,
        endTime: mask.currentVersion.endTime
      };
      // Selects the mask's channel
      const selectedChannels = [mask.channelName];
      this.setState({
        qcMaskModifyInterval,
        selectedChannels,
        selectedQcMask: mask
      });
      // Listens for clicks and ends the interactive mask modification if another part of the UI is clicked
      const delayMs = 200;
      setTimeout(() => {
        document.body.addEventListener('click', this.onBodyClick, {
          capture: true,
          once: true
        });
      }, delayMs);
    }
  }

  /**
   * Event handler for updating markers value
   *
   * @param marker the marker
   */
  private readonly onUpdateMarker = (marker: WeavessTypes.Marker): void => {
    /* no-op */
  }

  /**
   * Event handler for updating selections value
   *
   * @param selection the selection
   */
  private readonly onUpdateSelectionWindow = (selection: WeavessTypes.SelectionWindow) => {
    const newStartTime = selection.startMarker.timeSecs;
    const newEndTime = selection.endMarker.timeSecs;

    // handle qc mask modification selection
    if (selection.id === 'selection-qc-mask-modify') {
      const analystDefined = Object.keys(QcMaskCategory).find(
        k => QcMaskCategory[k] === QcMaskCategory.ANALYST_DEFINED
      );
      if (this.state.selectedQcMask) {
        // Sets new time range and mask category to ANALYST_DEFINED
        const qcInput: QcMaskTypes.QcMaskInput = {
          timeRange: {
            startTime: newStartTime,
            endTime: newEndTime
          },
          category: analystDefined,
          type: this.state.selectedQcMask.currentVersion.type,
          rationale: this.state.selectedQcMask.currentVersion.rationale
        };
        const type = QcMaskDialogBoxType.Modify;
        const newInterval: CommonTypes.TimeRange = {
          startTime: newStartTime,
          endTime: newEndTime
        };
        // Must set the modifyInterval or else old values stick around unpredictably
        this.setState({ qcMaskModifyInterval: newInterval });
        this.handleQcMaskMutation(type, this.state.selectedQcMask.id, qcInput);
      }
    }
  }

  /**
   * Event handler for updating markers value
   *
   * @param id the unique channel name of the channel
   * @param marker the marker
   */
  private readonly onUpdateChannelMarker = (id: string, marker: WeavessTypes.Marker) => {
    /* no-op */
  }

  /**
   * Event handler for updating selections value
   *
   * @param id the unique channel id of the channel
   * @param selection the selection
   */
  private readonly onUpdateChannelSelectionWindow = (
    id: string,
    selection: WeavessTypes.SelectionWindow
  ) => {
    // handle amplitude measurement
    if (selection.id.includes(systemConfig.measurementMode.peakTroughSelection.id)) {
      // only allowed calculate measurements in MEASUREMENT mode
      if (
        this.props.measurementMode.mode === AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT
      ) {
        // peak/trough selection region was clicked
        const sdId = selection.id.replace(systemConfig.measurementMode.peakTroughSelection.id, '');

        const signalDetection = this.props.signalDetectionsByStation.find(sd => sd.id === sdId);

        const arrivalTime = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
          signalDetection.currentHypothesis.featureMeasurements
        ).value;

        // Find the station to get sample rate
        // FIXME need nominal Hz rate for station
        const station = this.props.defaultStations.find(
          sta => sta.name === signalDetection.stationName
        );
        const waveformFilter = getWaveformFilterForMode(
          this.props.measurementMode.mode,
          // FIXME need nominal Hz rate for station
          station.channels[0].nominalSampleRateHz,
          this.props.defaultWaveformFilters
        );

        const startTime = selection.startMarker.timeSecs;
        const endTime = selection.endMarker.timeSecs;

        const startValue = getWaveformValueForSignalDetection(
          signalDetection,
          startTime,
          waveformFilter
        );
        const endValue = getWaveformValueForSignalDetection(
          signalDetection,
          endTime,
          waveformFilter
        );

        const minValue: { min: number; time: number } =
          startValue.value <= endValue.value
            ? { min: startValue.value, time: startTime }
            : { min: endValue.value, time: endTime };

        const maxValue: { max: number; time: number } =
          endValue.value >= startValue.value
            ? { max: endValue.value, time: endTime }
            : { max: startValue.value, time: startTime };

        const amplitudeMeasurementValue = calculateAmplitudeMeasurementValue(
          maxValue.max,
          minValue.min,
          maxValue.time,
          minValue.time
        );

        const scaledAmplitudeMeasurementValue = scaleAmplitudeMeasurementValue(
          amplitudeMeasurementValue
        );

        this.updateSignalDetectionMutation(sdId, arrivalTime, scaledAmplitudeMeasurementValue);
      }
    }
  }

  /**
   * Event handler for click events within a selection
   *
   * @param id the unique channel id of the channel
   * @param selection the selection
   * @param timeSecs epoch seconds of where drag ended in respect to the data
   */
  // tslint:disable-next-line: cyclomatic-complexity
  private readonly onClickChannelSelectionWindow = (
    id: string,
    selection: WeavessTypes.SelectionWindow,
    timeSecs: number
  ) => {
    // handle amplitude measurement
    if (
      selection.id.includes(systemConfig.measurementMode.selection.id) ||
      selection.id.includes(systemConfig.measurementMode.peakTroughSelection.id)
    ) {
      // only allowed calculate measurements in MEASUREMENT mode
      if (
        this.props.measurementMode.mode === AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT
      ) {
        const sdId = selection.id.includes(systemConfig.measurementMode.selection.id)
          ? // selection region was clicked
            selection.id.replace(systemConfig.measurementMode.selection.id, '')
          : // peak/trough selection region was clicked
            selection.id.replace(systemConfig.measurementMode.peakTroughSelection.id, '');

        const signalDetection = this.props.signalDetectionsByStation.find(sd => sd.id === sdId);

        const arrivalTime = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
          signalDetection.currentHypothesis.featureMeasurements
        ).value;

        // TODO: Really need the station hertz rate but this can be different based on channel groups
        // Find the station to get sample rate
        // FIXME need nominal Hz rate for station
        const station = this.props.defaultStations.find(
          sta => sta.name === signalDetection.stationName
        );
        const waveformFilter = getWaveformFilterForMode(
          this.props.measurementMode.mode,
          station.channels[0].nominalSampleRateHz,
          this.props.defaultWaveformFilters
        );

        const waveforms = getSignalDetectionBeams([signalDetection], waveformFilter);

        if (waveforms && waveforms.length === 1) {
          const minMax = determineMinMaxForPeakTroughForSignalDetection(
            signalDetection,
            timeSecs,
            waveformFilter
          );

          const amplitudeMeasurementValue = calculateAmplitudeMeasurementValue(
            minMax.max,
            minMax.min,
            minMax.maxTimeSecs,
            minMax.minTimeSecs
          );

          const scaledAmplitudeMeasurementValue = scaleAmplitudeMeasurementValue(
            amplitudeMeasurementValue
          );

          this.updateSignalDetectionMutation(sdId, arrivalTime, scaledAmplitudeMeasurementValue);
        } else {
          this.weavess.toastWarn('Unable to calculate [min,max] for peak/trough, no waveform data');
        }
      }
    }
  }

  /**
   * Listens for clicks and ends the interactive mask modification if
   * another part of the UI is clicked.
   */
  private readonly onBodyClick = (event: any) => {
    // Ignore clicks within the modification widget
    if (
      event.target.className === 'selection-window-selection' ||
      event.target.className === 'moveable-marker' ||
      event.target.className === 'selection-window'
    ) {
      document.body.addEventListener('click', this.onBodyClick, {
        capture: true,
        once: true
      });
    } else {
      this.deselectQCMask();
    }
  }

  /**
   * Deselects all QC Masks.
   */
  private readonly deselectQCMask = () => {
    if (this.state.qcMaskModifyInterval && this.state.selectedQcMask) {
      this.setState({
        qcMaskModifyInterval: undefined,
        selectedQcMask: undefined,
        selectedChannels: []
      });
    }
  }

  /**
   * Opens up the QC mask dialog menu.
   *
   * @param eventX the event x coordinate position
   * @param eventY the event y coordinate position
   * @param qcMask the qc mask
   * @param qcMaskDialogType the qc mask dialog type
   */
  private readonly openQCMaskMenu = (
    eventX: number,
    eventY: number,
    qcMask: QcMaskTypes.QcMask,
    qcMaskDialogType: QcMaskDialogBoxType
  ) => {
    ContextMenu.show(
      <QcMaskForm
        mask={qcMask}
        applyChanges={this.handleQcMaskMutation}
        qcMaskDialogBoxType={qcMaskDialogType}
      />,
      { left: eventX, top: eventY },
      undefined,
      true
    );
  }

  /**
   * Event handler for context clicking on a mask
   *
   * @param event mouse event as React.MouseEvent<HTMLDivElement>
   * @param channelName a channel name as a string
   * @param masks mask Ids as a string array
   */
  private readonly onMaskContextClick = (
    event: React.MouseEvent<HTMLDivElement>,
    channelName: string,
    masks: string[]
  ) => {
    event.stopPropagation();
    if (masks && masks.length > 0) {
      const qcMasks: QcMaskTypes.QcMask[] = this.props.qcMasksByChannelName.filter(m =>
        includes(masks, m.id)
      );
      if (qcMasks.length === 1) {
        const isRejected =
          QcMaskCategory[qcMasks[0].currentVersion.category] === QcMaskCategory.REJECTED;
        const qcContextMenu = QcMaskContextMenu(
          event.clientX,
          event.clientY,
          qcMasks[0],
          this.openQCMaskMenu,
          isRejected
        );
        ContextMenu.show(qcContextMenu, {
          left: event.clientX,
          top: event.clientY
        });
      } else {
        ContextMenu.show(
          <QcMaskOverlap
            masks={qcMasks}
            contextMenuCoordinates={{
              xPx: event.clientX,
              yPx: event.clientY
            }}
            openNewContextMenu={this.openQCMaskMenu}
            selectMask={this.selectMask}
          />,
          {
            left: event.clientX,
            top: event.clientY
          },
          undefined,
          true
        );
      }
    }
  }

  /**
   * Event handler for channel expansion
   *
   * @param channelName a channel name as a string
   */
  private readonly onChannelExpanded = (channelName: string) => {
    /* no-op */
  }

  /**
   * Event handler for channel collapse
   *
   * @param channelName a channel name as a string
   */
  private readonly onChannelCollapsed = (channelName: string) => {
    /* no-op */
  }

  /**
   * Select a channel.
   *
   * @param channelName the unique channel name
   */
  private readonly selectChannel = (channelName: string) => {
    this.setState({
      selectedChannels: [channelName]
    });
  }

  /**
   * Clears the selected channels.
   */
  private readonly clearSelectedChannels = () => {
    this.setState({
      selectedChannels: []
    });
  }

  /**
   * Event handler for when a channel label is clicked
   *
   * @param e mouse event as React.MouseEvent<HTMLDivElement>
   * @param channelName a channel name as a string
   */
  private readonly onChannelLabelClick = (
    e: React.MouseEvent<HTMLDivElement>,
    channelName: string
  ) => {
    e.preventDefault();

    const channelIsAlreadySelected = this.state.selectedChannels.indexOf(channelName) > -1;

    // If ctrl|meta is pressed, append to current list, otherwise new singleton list
    let selectedChannels: string[] =
      e.metaKey || e.ctrlKey
        ? channelIsAlreadySelected
          ? // ctrl|meta + already selected = remove the element
            this.state.selectedChannels.filter(id => id !== channelName)
          : // ctrl|meta + not selected = add to selection list
            [...this.state.selectedChannels, channelName]
        : channelIsAlreadySelected
        ? // already selected = unselect
          []
        : // not selected = select
          [channelName];

    const clickedDefaultStation = this.props.defaultStations.find(
      station => station.name === channelName
    );
    if (e.shiftKey && clickedDefaultStation) {
      // The click occurred on a default channel while the shift key was held down.
      // Look up all of the sub channels that fall under the selected default channel.
      const subChannelIds: string[] = clickedDefaultStation.channels.map(channel => channel.name);

      // If the default channel was previously selected, unselect the default channel
      // and all of its sub channels.  Otherwise, select all of the channels.
      // Toggle the state of selected default channel and its sub channels.
      selectedChannels = channelIsAlreadySelected
        ? // shift + default channel is already selected = unselect the default
          // channel and all of its sub channels.
          this.state.selectedChannels
            .filter(id => subChannelIds.indexOf(id) < 0)
            .filter(id => id !== channelName)
        : // shift + default channel is not selected = select the default
          // channel and all of its sub channels.
          union(this.state.selectedChannels, subChannelIds, [channelName]);
    }
    this.setState({
      selectedChannels
    });
  }

  /**
   * Event handler for when channel is clicked
   *
   * @param e mouse event as React.MouseEvent<HTMLDivElement>
   * @param channelId a Channel Id as a string
   * @param timeSecs epoch seconds of where clicked in respect to the data
   */
  private readonly onChannelClick = (
    e: React.MouseEvent<HTMLDivElement>,
    stationId: string,
    timeSecs: number
  ) => {
    // ctrl or meta click = create a signal detection
    const clickedDefaultChannel = this.props.defaultStations.find(
      station => station.name === stationId
    );
    if (e.ctrlKey || e.metaKey) {
      if (clickedDefaultChannel) {
        const input: SignalDetectionTypes.CreateDetectionMutationArgs = {
          input: {
            stationId,
            phase: this.props.defaultSignalDetectionPhase,
            signalDetectionTiming: {
              arrivalTime: timeSecs,
              timeUncertaintySec: 0.5
            },
            eventId: this.props.currentOpenEventId ? this.props.currentOpenEventId : undefined
          }
        };
        this.props
          .createDetection({
            variables: input
          })
          .catch(err => UILogger.Instance().error(`Failed to create detection: ${err.message}`));
      } else {
        if (this.weavess) {
          this.weavess.toast(WeavessMessages.signalDetectionModificationDisabled);
        }
      }
    } else {
      if (
        this.props.measurementMode.mode === AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT
      ) {
        if (clickedDefaultChannel) {
          // user clicked outside of the measurement selection area
          this.weavess.toastWarn('Must perform measurement calculation inside grey selection area');
        }
      }
    }
  }

  /**
   * Event handler for when signal detection is clicked
   *
   * @param e mouse event as React.MouseEvent<HTMLDivElement>
   * @param sdId a Signal Detection Id as a string
   */
  private readonly onSignalDetectionClick = (e: React.MouseEvent<HTMLDivElement>, sdId: string) => {
    e.preventDefault();
    if (e.altKey) {
      const color = e.currentTarget.style.color;
      // Display information of the signal detection
      const detection = this.props.signalDetectionsByStation.filter(sd => sd.id === sdId);
      ContextMenu.show(
        <SignalDetectionDetails detection={detection[0]} color={color} />,
        { left: e.clientX, top: e.clientY },
        undefined,
        true
      );
    } else {
      const alreadySelected = this.props.selectedSdIds.indexOf(sdId) > -1;

      // If ctrl, meta, or shift is pressed, append to current list, otherwise new singleton list
      const selectedSdIds: string[] =
        e.metaKey || e.ctrlKey || e.shiftKey
          ? alreadySelected
            ? // meta + already selected = remove the element
              this.props.selectedSdIds.filter(id => id !== sdId)
            : // meta + not selected = add to selection list
              [...this.props.selectedSdIds, sdId]
          : alreadySelected
          ? // already selected = unselect
            []
          : // not selected = select
            [sdId];
      if (e.metaKey || e.ctrlKey || selectedSdIds.length > 0) {
        this.props.setSelectedSdIds(selectedSdIds);
      }
    }
  }

  /**
   * Event handler for when a create mask drag ends
   *
   * @param e mouse event as React.MouseEvent<HTMLDivElement>
   * @param startTimeSecs epoch seconds of where clicked started
   * @param endTimeSecs epoch seconds of where clicked ended
   * @param needToDeselect boolean that indicates to deselect the channel
   */
  private readonly onMaskCreateDragEnd = (
    event: React.MouseEvent<HTMLDivElement>,
    startTimeSecs: number,
    endTimeSecs: number,
    needToDeselect: boolean
  ) => {
    ContextMenu.show(
      <QcMaskForm
        qcMaskDialogBoxType={QcMaskDialogBoxType.Create}
        startTimeSecs={startTimeSecs}
        endTimeSecs={endTimeSecs}
        applyChanges={this.handleQcMaskMutation}
      />,
      { left: event.clientX, top: event.clientY },
      () => {
        // menu was closed; callback optional
        this.weavess.clearBrushStroke();
        if (needToDeselect) {
          this.clearSelectedChannels();
        }
      },
      true
    );
  }

  /**
   * Invokes the call to the create QC mask mutation.
   *
   * @param type the qc mask dialog box type
   * @param maskId the unique mask id
   * @param input the qc mask input to the mutation
   */
  private readonly handleQcMaskMutation = (
    type: QcMaskDialogBoxType,
    maskId: string,
    input: QcMaskTypes.QcMaskInput
  ) => {
    if (type === QcMaskDialogBoxType.Create) {
      this.props
        .createQcMask({
          variables: {
            channelNames: this.state.selectedChannels,
            input
          }
        })
        .catch(err => UILogger.Instance().error(`Failed to create mask: ${err.message}`));
    } else if (type === QcMaskDialogBoxType.Modify) {
      this.props
        .updateQcMask({
          variables: {
            maskId,
            input
          }
        })
        .catch(err => UILogger.Instance().error(`Failed to update mask: ${err.message}`));
    } else if (type === QcMaskDialogBoxType.Reject) {
      this.props
        .rejectQcMask({
          variables: {
            maskId,
            inputRationale: input.rationale
          }
        })
        .catch(err => UILogger.Instance().error(`Failed to reject mask: ${err.message}`));
    }
  }

  /**
   * Event handler that is invoked and handled when the Measure Window is updated.
   *
   * @param isVisible true if the measure window is updated
   * @param channelName the unique channel id of the channel that the measure window on;
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
    channelName?: string,
    startTimeSecs?: number,
    endTimeSecs?: number,
    heightPx?: number
  ) => {
    /** no-op */
  }

  /**
   * Event handler for when a signal detection drag ends
   *
   * @param sdId a Signal Detection Id as a string
   * @param timeSecs epoch seconds of where drag ended in respect to the data
   */
  private readonly onSignalDetectionDragEnd = (sdId: string, timeSecs: number) => {
    this.updateSignalDetectionMutation(sdId, timeSecs);
    this.props.setSelectedSdIds([sdId]);
  }

  /**
   * Helper function to call UpdateDetection Mutation
   */
  /**
   * Invokes the call to the update signal detection mutation.
   *
   * @param sdId the unique signal detection id
   * @param timeSecs the epoch seconds time
   * @param amplitudeFeatureMeasurementValue the amplitude feature measurement value
   */
  private updateSignalDetectionMutation(
    sdId: string,
    timeSecs: number,
    amplitudeFeatureMeasurementValue?: SignalDetectionTypes.AmplitudeMeasurementValue
  ) {
    const input: SignalDetectionTypes.UpdateDetectionsMutationArgs = {
      detectionIds: [sdId],
      input: {
        signalDetectionTiming: {
          arrivalTime: timeSecs,
          timeUncertaintySec: 0.5,
          amplitudeMeasurement: amplitudeFeatureMeasurementValue
        }
      }
    };
    this.props
      .updateDetections({
        variables: input
      })
      .catch(err => UILogger.Instance().error(`Failed to update detections: ${err.message}`));
  }

  /**
   * Event handler for when context menu is displayed
   *
   * @param e mouse event as React.MouseEvent<HTMLDivElement>
   * @param channelName a Channel Id as a string
   */
  private readonly onContextMenu = (
    e: React.MouseEvent<HTMLDivElement>,
    channelName: string
  ): void => {
    /* no-op */
  }

  /**
   * Event handler for when context menu is displayed
   *
   * @param e mouse event as React.MouseEvent<HTMLDivElement>
   * @param channelName a Channel Id as a string
   * @param sdId a Signal Detection Id as a string
   */
  private readonly onSignalDetectionContextMenu = (
    e: React.MouseEvent<HTMLDivElement>,
    channelName: string,
    sdId?: string
  ) => {
    e.preventDefault();
    // if provided && not already selected, set the current selection to just the context-menu'd detection
    const detectionIds =
      sdId && this.props.selectedSdIds.indexOf(sdId) === -1 ? [sdId] : this.props.selectedSdIds;
    const sds = this.props.signalDetectionsByStation.filter(
      sd => detectionIds.indexOf(sd.id) !== -1 || sd.id === sdId
    );

    const currentlyOpenEvent: EventTypes.Event | undefined = this.props.eventsInTimeRange.find(
      event => event.id === this.props.currentOpenEventId
    );
    const sdMenu = (
      <SignalDetectionContextMenu
        signalDetections={this.props.signalDetectionsByStation}
        selectedSds={sds}
        currentOpenEvent={currentlyOpenEvent}
        changeAssociation={this.props.changeSignalDetectionAssociations}
        rejectDetections={this.props.rejectDetections}
        updateDetections={this.props.updateDetections}
        setSdIdsToShowFk={this.props.setSdIdsToShowFk}
        sdIdsToShowFk={this.props.sdIdsToShowFk}
        associateToNewEvent={this.props.createEvent}
        measurementMode={this.props.measurementMode}
        setSelectedSdIds={this.props.setSelectedSdIds}
        setMeasurementModeEntries={this.props.setMeasurementModeEntries}
      />
    );
    ContextMenu.show(
      sdMenu,
      {
        left: e.clientX,
        top: e.clientY
      },
      undefined,
      true
    );
  }

  /**
   * Selects all parent channels (default channels in weavess).
   */
  private readonly selectAllParentChannels = () => {
    const parentStationIds = this.props.defaultStations.map(station => station.name);
    this.setState({
      selectedChannels: parentStationIds
    });
  }

  /**
   * Returns true if the selected signal detection can be used to generate an FK.
   */
  private readonly canGenerateFk = (
    signalDetection: SignalDetectionTypes.SignalDetection
  ): boolean => {
    const fmPhase = SignalDetectionUtils.findPhaseFeatureMeasurementValue(
      signalDetection.currentHypothesis.featureMeasurements
    );
    return (
      systemConfig.nonFkSdPhases
        // tslint:disable-next-line:newline-per-chained-call
        .findIndex(phase => phase.toLowerCase() === fmPhase.phase.toString().toLowerCase()) === -1
    );
  }

  /**
   * Mark the selected signal detection ids to show fk.
   */
  private readonly markSelectedSignalDetectionsToShowFk = () => {
    const signalDetections: SignalDetectionTypes.SignalDetection[] = [];
    this.props.selectedSdIds.forEach(selectedId => {
      const signalDetection = this.props.signalDetectionsByStation.find(sd => sd.id === selectedId);
      if (signalDetection && this.canGenerateFk(signalDetection)) {
        signalDetections.push(signalDetection);
      }
    });
    this.props.setSdIdsToShowFk(signalDetections.map(sd => sd.id));
  }

  /**
   * Shows or displays the signal detection re-phase context menu dialog.
   *
   * @param clientX the client x
   * @param clientY the client y
   */
  private readonly showRephaseMenu = (clientX: number, clientY: number) => {
    if (this.props.selectedSdIds.length === 0) return;
    const stageIntervalContextMenu = setPhaseContextMenu(
      this.props.selectedSdIds,
      this.props.updateDetections
    );

    ContextMenu.show(stageIntervalContextMenu, {
      left: clientX,
      top: clientY
    });
  }

  // tslint:disable-next-line: max-file-line-count
}
