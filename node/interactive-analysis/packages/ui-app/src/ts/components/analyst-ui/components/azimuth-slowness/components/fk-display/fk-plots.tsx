import { NonIdealState } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import {
  CommonTypes,
  EventTypes,
  FkTypes,
  ProcessingStationTypes,
  SignalDetectionTypes,
  SignalDetectionUtils,
  WaveformTypes
} from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { WeavessTypes, WeavessUtils } from '@gms/weavess';
import Immutable from 'immutable';
import isEqual from 'lodash/isEqual';
import React from 'react';
import { getFkData, getFkParamsForSd } from '~analyst-ui/common/utils/fk-utils';
import { createUnfilteredWaveformFilter } from '~analyst-ui/common/utils/instance-of-util';
import {
  determineDetectionColor,
  filterSignalDetectionsByStationId,
  getSignalDetectionBeams,
  getSignalDetectionChannelSegments
} from '~analyst-ui/common/utils/signal-detection-util';
import { WeavessDisplay } from '~analyst-ui/components/weavess-display';
import { systemConfig } from '~analyst-ui/config';
import { gmsColors, semanticColors } from '~scss-config/color-preferences';
import { FkParams } from '../../types';
import { getPredictedPoint } from '../fk-util';

/**
 * FkPlots Props
 */
export interface FkPlotsProps {
  defaultStations: ProcessingStationTypes.ProcessingStation[];
  defaultWaveformFilters: WaveformTypes.WaveformFilter[];
  eventsInTimeRange: EventTypes.Event[];
  currentOpenEvent?: EventTypes.Event;
  signalDetection: SignalDetectionTypes.SignalDetection;
  signalDetectionsByStation: SignalDetectionTypes.SignalDetection[];
  signalDetectionFeaturePredictions: EventTypes.FeaturePrediction[];
  fstatData: FkTypes.FstatData;
  windowParams: FkTypes.WindowParameters;
  configuration: FkTypes.FkConfiguration;
  contribChannels: {
    id: string;
  }[];
  currentMovieSpectrumIndex: number;
  defaultSignalDetectionPhase?: CommonTypes.PhaseType;

  channelFilters: Immutable.Map<string, WaveformTypes.WaveformFilter>;
  setChannelFilters(filters: Immutable.Map<string, WaveformTypes.WaveformFilter>): void;
  changeUserInputFks(
    windowParams: FkTypes.WindowParameters,
    frequencyBand: FkTypes.FrequencyBand
  ): void;
  updateCurrentMovieTimeIndex(time: number): void;
  setMeasurementModeEntries(entries: Immutable.Map<string, boolean>): void;
  onNewFkParams(sdId: string, fkParams: FkParams, fkConfiguration: FkTypes.FkConfiguration): void;
}

/**
 * FkPlots State
 */
export interface FkPlotsState {
  selectionWindow: {
    startTime: number;
    endTime: number;
  };
}

/**
 * Renders the FK waveform data with Weavess
 */
export class FkPlots extends React.PureComponent<FkPlotsProps, FkPlotsState> {
  /** The precision of displayed lead/lag pair */
  private readonly digitPrecision: number = 1;

  /** Hard-coded height of the waveform panel */
  private readonly waveformPanelHeight: number = 70;

  /** Determines if the selection window should snap or not */
  private selectionWindowSnapMode: boolean = false;

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Constructor.
   *
   * @param props The initial props
   */
  public constructor(props: FkPlotsProps) {
    super(props);
    this.state = {
      selectionWindow: undefined
    };
  }

  /**
   * React life update cycle method, that resets the should snap class variable
   * @param prevProps previous component props
   */
  public componentDidUpdate(prevProps: FkPlotsProps) {
    if (
      !isEqual(
        getFkData(prevProps.signalDetection.currentHypothesis.featureMeasurements),
        getFkData(this.props.signalDetection.currentHypothesis.featureMeasurements)
      )
    ) {
      this.selectionWindowSnapMode = false;
    }
  }

  /**
   * Renders the component.
   */
  public render() {
    const arrivalTime = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
      this.props.signalDetection.currentHypothesis.featureMeasurements
    ).value;

    const timePadding: number = systemConfig.fkPlotTimePadding;

    const selectedFilter: WaveformTypes.WaveformFilter = this.props.channelFilters.has(
      this.props.signalDetection.stationName
    )
      ? this.props.channelFilters.get(this.props.signalDetection.stationName)
      : createUnfilteredWaveformFilter();
    const signalDetectionBeam = getSignalDetectionBeams(
      [this.props.signalDetection],
      selectedFilter
    );

    // az, slowness, and fstat have the same rate and num samples
    // but we need to calculate the data to send to weavess for beam
    if (
      !signalDetectionBeam ||
      signalDetectionBeam.length !== 1 ||
      this.fStatDataContainsUndefined(this.props.fstatData)
    ) {
      return (
        <NonIdealState
          icon={IconNames.TIMELINE_LINE_CHART}
          title="Missing waveform data"
          description="Fk plots currently not supported for analyst created SDs"
        />
      );
    }

    const startTimeSecs: number =
      signalDetectionBeam[0].sampleCount > 0
        ? signalDetectionBeam[0].startTime
        : arrivalTime - timePadding / 2;
    const plotEndTimePadding: number =
      signalDetectionBeam[0].sampleCount > 0
        ? signalDetectionBeam[0].sampleCount / signalDetectionBeam[0].sampleRate > timePadding
          ? signalDetectionBeam[0].sampleCount / signalDetectionBeam[0].sampleRate
          : timePadding
        : timePadding;
    const endTimeSecs: number =
      startTimeSecs + plotEndTimePadding <= arrivalTime
        ? startTimeSecs + plotEndTimePadding + timePadding
        : startTimeSecs + plotEndTimePadding;

    const predictedPoint = getPredictedPoint(this.props.signalDetectionFeaturePredictions);

    const signalDetectionsForStation = this.props.signalDetectionsByStation
      ? filterSignalDetectionsByStationId(
          this.props.signalDetection.stationName,
          this.props.signalDetectionsByStation
        )
      : [];

    // If there are Signal Detections populate Weavess Channel Segment from the FK_BEAM
    // else use the default channel Weavess Channel Segment built
    const channelSegments = new Map<string, WeavessTypes.ChannelSegment>();
    if (signalDetectionsForStation && signalDetectionsForStation.length > 0) {
      // clone to add UNFILTERED
      const allFilters = [...this.props.defaultWaveformFilters, WaveformTypes.UNFILTERED_FILTER];
      allFilters.forEach(filter => {
        const signalDetectionChannelSegments = getSignalDetectionChannelSegments(
          signalDetectionsForStation,
          filter
        );
        if (
          signalDetectionChannelSegments &&
          signalDetectionChannelSegments.dataSegments &&
          signalDetectionChannelSegments.dataSegments.length > 0
        ) {
          channelSegments.set(filter.id, signalDetectionChannelSegments);
        }
      });
    }

    const signalDetections = signalDetectionsForStation.map(sd => ({
      timeSecs: SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
        sd.currentHypothesis.featureMeasurements
      ).value,
      id: sd.id,
      label: SignalDetectionUtils.findPhaseFeatureMeasurementValue(
        sd.currentHypothesis.featureMeasurements
      ).phase.toString(),
      color: determineDetectionColor(
        sd,
        this.props.eventsInTimeRange,
        this.props.currentOpenEvent ? this.props.currentOpenEvent.id : undefined
      ),
      isConflicted: sd.hasConflict
    }));

    const KEY = 'data';
    const stations: WeavessTypes.Station[] = [
      // Beam
      {
        id: 'Beam',
        name: 'Beam',
        defaultChannel: {
          id: this.props.signalDetection.stationName,
          name: 'Beam',
          height: this.waveformPanelHeight,
          waveform: {
            channelSegmentId: selectedFilter.id,
            channelSegments,
            signalDetections
          }
        }
      },
      // Fstat
      {
        id: 'Fstat',
        name: 'Fstat',
        defaultChannel: {
          id: `Fstat-${this.props.signalDetection.stationName}`,
          name: 'Fstat',
          height: this.waveformPanelHeight,
          // set the min to zero, so that WEAVESS does not use the calculated min
          defaultRange: {
            min: 0
          },
          waveform: {
            channelSegmentId: KEY,
            channelSegments: new Map([
              [
                KEY,
                {
                  dataSegments: [
                    {
                      color: semanticColors.waveformRaw,
                      pointSize: 2,
                      displayType: [
                        WeavessTypes.DisplayType.LINE,
                        WeavessTypes.DisplayType.SCATTER
                      ],
                      data: {
                        startTimeSecs: this.props.fstatData.fstatWf.startTime,
                        sampleRate: this.props.fstatData.fstatWf.sampleRate,
                        values: this.props.fstatData.fstatWf.values
                      }
                    }
                  ]
                }
              ]
            ])
          }
        }
      },
      // Azimuth
      {
        id: 'Azimuth',
        name: 'Azimuth',
        defaultChannel: {
          id: `Azimuth-${this.props.signalDetection.stationName}`,
          name: (
            <div key="azimuth-name" style={{ whiteSpace: 'nowrap' }}>
              Azimuth <sup key="sup">(&deg;)</sup>
            </div>
          ),
          height: this.waveformPanelHeight,
          // set the min to zero and max to 360, so that WEAVESS does not use the calculated min/max
          defaultRange: {
            min: 0,
            max: 360
          },
          waveform: {
            channelSegmentId: KEY,
            channelSegments: new Map([
              [
                KEY,
                {
                  dataSegments: [
                    {
                      displayType: [
                        WeavessTypes.DisplayType.LINE,
                        WeavessTypes.DisplayType.SCATTER
                      ],
                      color: semanticColors.waveformRaw,
                      pointSize: 2,
                      data: {
                        startTimeSecs: this.props.fstatData.azimuthWf.startTime,
                        sampleRate: this.props.fstatData.azimuthWf.sampleRate,
                        values: this.props.fstatData.azimuthWf.values
                      }
                    }
                  ]
                }
              ]
            ])
          }
        }
      },
      // Slowness
      {
        id: 'Slowness',
        name: 'Slowness',
        defaultChannel: {
          id: `Slowness-${this.props.signalDetection.stationName}`,
          // tslint:disable-next-line: max-line-length
          name: (
            <div key="slowness-name" style={{ whiteSpace: 'nowrap' }}>
              Slowness (<sup key="sup">s</sup>&#8725;<sub key="sub">&deg;</sub>)
            </div>
          ),
          height: this.waveformPanelHeight,
          // set the min to zero and max to the current maximum slowness,
          // so that WEAVESS does not use the calculated min/max
          defaultRange: {
            min: 0,
            max: this.props.configuration.maximumSlowness
          },
          waveform: {
            channelSegmentId: KEY,
            channelSegments: new Map([
              [
                KEY,
                {
                  dataSegments: [
                    {
                      displayType: [
                        WeavessTypes.DisplayType.LINE,
                        WeavessTypes.DisplayType.SCATTER
                      ],
                      color: semanticColors.waveformRaw,
                      pointSize: 2,
                      data: {
                        startTimeSecs: this.props.fstatData.slownessWf.startTime,
                        sampleRate: this.props.fstatData.slownessWf.sampleRate,
                        values: this.props.fstatData.slownessWf.values
                      }
                    }
                  ]
                }
              ]
            ])
          }
        }
      }
    ];

    // add the Azimuth and Slowness flat lines if the appropriate predicted value exists
    if (predictedPoint) {
      stations[2].defaultChannel.waveform.channelSegments
        .get(KEY)
        .dataSegments.push(
          WeavessUtils.Waveform.createFlatLineDataSegment(
            startTimeSecs,
            endTimeSecs,
            predictedPoint.azimuth,
            semanticColors.analystOpenEvent
          )
        );
    }

    if (predictedPoint) {
      stations[3].defaultChannel.waveform.channelSegments
        .get(KEY)
        .dataSegments.push(
          WeavessUtils.Waveform.createFlatLineDataSegment(
            startTimeSecs,
            endTimeSecs,
            predictedPoint.slowness,
            semanticColors.analystOpenEvent
          )
        );
    }

    // Get the SD FK configure to set the start marker lead secs and
    // from there add length to get endMarker in epoch time
    const config = getFkData(this.props.signalDetection.currentHypothesis.featureMeasurements)
      .configuration;
    const startMarkerEpoch: number = this.selectionWindowSnapMode
      ? startTimeSecs + this.props.currentMovieSpectrumIndex * this.props.windowParams.stepSize
      : arrivalTime - config.leadFkSpectrumSeconds;
    const endMarkerEpoch: number = startMarkerEpoch + this.props.windowParams.lengthSeconds;

    return (
      <div>
        <div className="ag-dark fk-plots-wrapper-1">
          <div className="fk-plots-wrapper-2">
            <WeavessDisplay
              weavessProps={{
                startTimeSecs,
                endTimeSecs,
                configuration: {
                  defaultChannel: {
                    disableMeasureWindow: true
                  }
                },
                defaultZoomWindow: {
                  startTimeSecs,
                  endTimeSecs
                },
                initialZoomWindow: {
                  startTimeSecs,
                  endTimeSecs
                },
                stations,
                selections: {
                  signalDetections: [this.props.signalDetection.id]
                },
                events: {
                  // stationEvents: {
                  //   defaultChannelEvents: channelEvents
                  // },
                  onUpdateSelectionWindow: this.onUpdateSelectionWindow
                },
                markers: {
                  selectionWindows: [
                    {
                      id: 'selection',
                      startMarker: {
                        id: 'start',
                        color: gmsColors.gmsProminent,
                        lineStyle: WeavessTypes.LineStyle.DASHED,
                        timeSecs: startMarkerEpoch
                      },
                      endMarker: {
                        id: 'end',
                        color: gmsColors.gmsProminent,
                        lineStyle: WeavessTypes.LineStyle.DASHED,
                        timeSecs: endMarkerEpoch,
                        minTimeSecsConstraint:
                          startMarkerEpoch + Number(config.leadFkSpectrumSeconds)
                      },
                      isMoveable: true,
                      color: 'rgba(200,200,200,0.2)'
                    }
                  ]
                }
              }}
              defaultWaveformFilters={this.props.defaultWaveformFilters}
              defaultStations={this.props.defaultStations}
              defaultSignalDetectionPhase={this.props.defaultSignalDetectionPhase}
              eventsInTimeRange={this.props.eventsInTimeRange}
              signalDetectionsByStation={this.props.signalDetectionsByStation}
              setMeasurementModeEntries={this.props.setMeasurementModeEntries}
              qcMasksByChannelName={[]}
              measurementMode={{
                // do not allow measurement mode for the fk plots, always force to default
                mode: AnalystWorkspaceTypes.WaveformDisplayMode.DEFAULT,
                entries: Immutable.Map<string, boolean>()
              }}
            />
          </div>
        </div>
      </div>
    );
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Event handler for when a key is pressed
   * @param e mouse event as React.MouseEvent<HTMLDivElement>
   */
  // private readonly onKeyPress = (e: React.KeyboardEvent<HTMLDivElement>) => {
  //   // handle the default WEAVESS onKeyPressEvents
  //   if (e.ctrlKey || e.metaKey) {
  //     if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
  //       const toggleFilterResults = toggleWaveformChannelFilters(
  //         e,
  //         [this.props.signalDetection.stationName],
  //         this.props.defaultWaveformFilters,
  //         this.props.defaultStations,
  //         this.selectedFilterIndex,
  //         this.props.channelFilters
  //       );
  //       this.setSelectedFilterIndex(toggleFilterResults.newFilterIndex);
  //       this.setChannelFilters(toggleFilterResults.channelFilters);
  //     }
  //   }
  // }

  /**
   * Updates the value of the selected filter index
   * @param index index value
   */
  // private readonly setSelectedFilterIndex = (index: number): void => {
  //   this.selectedFilterIndex = index;
  // }

  /**
   * Updates the state of the channel filters
   * @param channelFilters map of channel filters
   */
  // private readonly setChannelFilters = (
  //   channelFilters: Immutable.Map<string, WaveformFilter> = Immutable.Map<string, WaveformFilter>()) => {
  //   this.props.setChannelFilters(channelFilters);
  // }

  /**
   * Call back for drag and drop change of the moveable selection
   *
   * @param verticalMarkers List of markers in the fk plot display
   */
  private readonly onUpdateSelectionWindow = (selection: WeavessTypes.SelectionWindow) => {
    const arrivalTime = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
      this.props.signalDetection.currentHypothesis.featureMeasurements
    ).value;

    const lagTime = this.props.windowParams.lengthSeconds - this.props.windowParams.leadSeconds;
    const newLeadTime = parseFloat(
      (arrivalTime - selection.startMarker.timeSecs).toFixed(this.digitPrecision)
    );
    const newLagTime = parseFloat(
      (selection.endMarker.timeSecs - arrivalTime).toFixed(this.digitPrecision)
    );
    const minimumDeltaSize = 0.1;
    const priorParams = getFkParamsForSd(this.props.signalDetection);
    // If duration hasn't changed update new lead seconds and update user input which sets state
    // else call computeFk via onNewFkParams
    const durationDelta = Math.abs(
      this.props.windowParams.lengthSeconds - (newLagTime + newLeadTime)
    );
    if (durationDelta < minimumDeltaSize) {
      this.selectionWindowSnapMode = true;
      this.props.updateCurrentMovieTimeIndex(selection.startMarker.timeSecs);
    } else if (
      newLeadTime > this.props.windowParams.leadSeconds + minimumDeltaSize ||
      newLeadTime < this.props.windowParams.leadSeconds - minimumDeltaSize ||
      newLagTime > lagTime + minimumDeltaSize ||
      newLagTime < lagTime - minimumDeltaSize
    ) {
      const newParams: FkParams = {
        ...priorParams,
        windowParams: {
          ...priorParams.windowParams,
          lengthSeconds: parseFloat(
            (selection.endMarker.timeSecs - selection.startMarker.timeSecs).toFixed(
              this.digitPrecision
            )
          )
        }
      };
      const priorConfig = getFkData(
        this.props.signalDetection.currentHypothesis.featureMeasurements
      ).configuration;
      priorConfig.leadFkSpectrumSeconds = newParams.windowParams.leadSeconds;
      this.selectionWindowSnapMode = false;
      this.props.onNewFkParams(this.props.signalDetection.id, newParams, priorConfig);
    }
  }

  /**
   * Checks for any undefined waveforms inside of fstat data
   *
   * @param fstatData as FkTypes.FstatData
   * @returns boolean if defined or not
   */
  private readonly fStatDataContainsUndefined = (fstatData: FkTypes.FstatData): boolean =>
    !fstatData || !fstatData.azimuthWf || !fstatData.fstatWf || !fstatData.slownessWf
}
