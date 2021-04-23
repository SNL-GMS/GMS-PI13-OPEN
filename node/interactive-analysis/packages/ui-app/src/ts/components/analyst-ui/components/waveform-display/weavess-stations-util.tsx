// tslint:disable: max-file-line-count
import {
  ChannelSegmentTypes,
  CommonTypes,
  EventTypes,
  ProcessingStationTypes,
  QcMaskTypes,
  SignalDetectionTypes,
  SignalDetectionUtils,
  WaveformTypes
} from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { WeavessTypes } from '@gms/weavess';
import Immutable from 'immutable';
import flatMap from 'lodash/flatMap';
import flatten from 'lodash/flatten';
import includes from 'lodash/includes';
import orderBy from 'lodash/orderBy';
import sortBy from 'lodash/sortBy';
import { getDistanceToStationsForLocationSolutionId } from '~analyst-ui/common/utils/event-util';
import {
  createUnfilteredWaveformFilter,
  isSdInstantMeasurementValue
} from '~analyst-ui/common/utils/instance-of-util';
import {
  determineDetectionColor,
  determineIfAssociated,
  filterSignalDetectionsByStationId,
  getSignalDetectionChannelSegments,
  isPeakTroughInWarning
} from '~analyst-ui/common/utils/signal-detection-util';
import { getSelectedWaveformFilter } from '~analyst-ui/common/utils/waveform-util';
import { QcMaskDisplayFilters, systemConfig, userPreferences } from '~analyst-ui/config';
import { gmsColors, semanticColors } from '~scss-config/color-preferences';
import { AlignWaveformsOn, WaveformDisplayProps, WaveformDisplayState } from './types';
import { calculateOffsets, Offset } from './utils';
import { WaveformClient } from './waveform-client';

/**
 * Interface used to bundle all of the parameters need to create the
 * weavess stations for the waveform display.
 */
export interface CreateWeavessStationsParameters {
  defaultStations: ProcessingStationTypes.ProcessingStation[];
  measurementMode: AnalystWorkspaceTypes.MeasurementMode;
  featurePredictions: EventTypes.FeaturePrediction[];
  signalDetectionsByStation: SignalDetectionTypes.SignalDetection[];
  eventsInTimeRange: EventTypes.Event[];
  qcMasksByChannelName: QcMaskTypes.QcMask[];
  channelHeight: number;
  maskDisplayFilters: QcMaskDisplayFilters;
  channelFilters: Immutable.Map<string, WaveformTypes.WaveformFilter>;
  waveformClient: WaveformClient;
  defaultWaveformFilters: WaveformTypes.WaveformFilter[];
  startTimeSecs: number;
  endTimeSecs: number;
  currentOpenEvent?: EventTypes.Event;
  showPredictedPhases: boolean;
  distances: EventTypes.LocationToStationDistance[];
  offsets: Offset[];
}

/**
 * Creates CreateWeavessStationsParameters with the required fields used
 * for to creating the weavess stations for the waveform display.
 *
 * @param props The WaveformDisplayProps
 * @param state The WaveformDisplayState
 * @param channelHeight The height of rendered channels in weavess in px
 * @param waveformClient A reference to an instantiated WaveformClient object
 * @returns CreateWeavessStationsParameters
 */
export function populateCreateWeavessStationsParameters(
  props: WaveformDisplayProps,
  state: WaveformDisplayState,
  channelHeight: number,
  waveformClient: WaveformClient
): CreateWeavessStationsParameters {
  const events =
    props.eventsInTimeRangeQuery && props.eventsInTimeRangeQuery.eventsInTimeRange
      ? props.eventsInTimeRangeQuery.eventsInTimeRange
      : [];

  const currentOpenEvent = events.find(event => event.id === state.currentOpenEventId);

  const signalDetectionsByStation =
    props.signalDetectionsByStationQuery &&
    props.signalDetectionsByStationQuery.signalDetectionsByStation
      ? props.signalDetectionsByStationQuery.signalDetectionsByStation
      : [];

  const filteredStations = props.defaultStationsQuery.defaultProcessingStations
    // filter the stations based on the mode setting
    .filter(stationToFilterOnMode =>
      filterStationOnMode(
        props.measurementMode.mode,
        stationToFilterOnMode,
        currentOpenEvent,
        signalDetectionsByStation
      )
    );

  const distances = getDistanceToStationsForLocationSolutionId(
    currentOpenEvent,
    props.location.selectedPreferredLocationSolutionId
  );
  const sortedFilteredDefaultStations = currentOpenEvent
    ? sortProcessingStations(filteredStations, props.selectedSortType, distances)
    : filteredStations;

  const fpList = getFeaturePredictionsForOpenEvent(props)
    .filter(
      fp => fp.predictionType === SignalDetectionTypes.FeatureMeasurementTypeName.ARRIVAL_TIME
    )
    .filter(fpToFilter =>
      filterFeaturePredictionsOnMode(
        props.measurementMode.mode,
        fpToFilter,
        sortedFilteredDefaultStations
      )
    );

  const individualWeavesMeasurementMode: AnalystWorkspaceTypes.MeasurementMode = {
    mode: props.measurementMode.mode,
    entries: props.measurementMode.entries
  };

  const params: CreateWeavessStationsParameters = {
    defaultStations: sortedFilteredDefaultStations,
    measurementMode: individualWeavesMeasurementMode,
    featurePredictions: fpList,
    signalDetectionsByStation,
    eventsInTimeRange: events,
    qcMasksByChannelName:
      props.qcMasksByChannelNameQuery && props.qcMasksByChannelNameQuery.qcMasksByChannelName
        ? props.qcMasksByChannelNameQuery.qcMasksByChannelName
        : [],
    channelHeight,
    maskDisplayFilters: state.maskDisplayFilters,
    channelFilters: props.channelFilters,
    waveformClient,
    defaultWaveformFilters: props.uiConfigurationQuery.uiAnalystConfiguration.defaultFilters,
    startTimeSecs: props.currentTimeInterval.startTime,
    endTimeSecs: props.currentTimeInterval.endTime,
    currentOpenEvent,
    distances,
    showPredictedPhases: state.showPredictedPhases,
    offsets:
      state.alignWaveformsOn === AlignWaveformsOn.TIME
        ? []
        : sortedFilteredDefaultStations !== undefined && sortedFilteredDefaultStations.length > 0
        ? calculateOffsets(
            fpList,
            sortedFilteredDefaultStations[0].channels[0].name,
            state.phaseToAlignOn
          )
        : []
  };
  return params;
}

/**
 * Filter the feature predictions based on the mode setting.
 *
 * @param mode the mode of the waveform display
 * @param featurePrediction the feature prediction to check
 * @param stations the stations
 */
function filterFeaturePredictionsOnMode(
  mode: AnalystWorkspaceTypes.WaveformDisplayMode,
  featurePrediction: EventTypes.FeaturePrediction,
  stations: ProcessingStationTypes.ProcessingStation[]
): boolean {
  if (AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT === mode) {
    return stations.find(station => station.name === featurePrediction.stationName) !== undefined;
  }
  return true; // show all feature predictions (DEFAULT)
}

/**
 * Filter the stations based on the mode setting.
 *
 * @param mode the mode of the waveform display
 * @param station the station
 * @param signalDetectionsByStation the signal detections for all stations
 */
function filterStationOnMode(
  mode: AnalystWorkspaceTypes.WaveformDisplayMode,
  station: ProcessingStationTypes.ProcessingStation,
  currentOpenEvent: EventTypes.Event,
  signalDetectionsByStation: SignalDetectionTypes.SignalDetection[]
): boolean {
  if (AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT === mode) {
    if (currentOpenEvent) {
      // tslint:disable-next-line: max-line-length
      const associatedSignalDetectionHypothesisIds = currentOpenEvent.currentEventHypothesis.eventHypothesis.signalDetectionAssociations.map(
        association => association.signalDetectionHypothesis.id
      );

      const signalDetections = signalDetectionsByStation
        ? signalDetectionsByStation.filter(sd => {
            // filter out the sds for the other stations and the rejected sds
            if (sd.stationName !== station.name || sd.currentHypothesis.rejected) {
              return false;
            }

            // filter sds that are associated to the current open event
            if (includes(associatedSignalDetectionHypothesisIds, sd.currentHypothesis.id)) {
              return true;
            }

            return false;
          })
        : [];
      // display the station only if sds were returned
      return signalDetections.length > 0;
    }
  }

  return true; // show all stations (DEFAULT)
}

/**
 * Returns the `green` interval markers.
 *
 * @param startTimeSecs start time seconds for the interval start marker
 * @param endTimeSecs end time seconds for the interval end marker
 */
function getIntervalMarkers(startTimeSecs: number, endTimeSecs: number): WeavessTypes.Marker[] {
  return [
    {
      id: 'startTime',
      color: semanticColors.waveformIntervalBoundry,
      lineStyle: WeavessTypes.LineStyle.SOLID,
      timeSecs: startTimeSecs
    },
    {
      id: 'endTime',
      color: semanticColors.waveformIntervalBoundry,
      lineStyle: WeavessTypes.LineStyle.SOLID,
      timeSecs: endTimeSecs
    }
  ];
}

/**
 * If there are Signal Detections populate Weavess Channel Segment from the FK_BEAM
 * else use the default channel Weavess Channel Segment built
 *
 * @param signalDetections signal detections
 * @param defaultWaveformFilters default waveform filters
 * @param channelSegments channel segment map
 */
export function populateWavessChannelSegmentAndAddFilter(
  signalDetections: SignalDetectionTypes.SignalDetection[],
  defaultWaveformFilters: WaveformTypes.WaveformFilter[],
  channelSegments: Map<string, WeavessTypes.ChannelSegment>
) {
  if (signalDetections && signalDetections.length > 0) {
    // clone to add UNFILTERED
    const allFilters = [...defaultWaveformFilters, WaveformTypes.UNFILTERED_FILTER];
    allFilters.forEach(filter => {
      const signalDetectionChannelSegments = getSignalDetectionChannelSegments(
        signalDetections,
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
}

/**
 * Creates the selection window and markers for weavess
 *
 * @param signalDetections signal detections
 * @param currentOpenEvent the current open event
 * @param measurementMode measurement mode
 *
 * @returns a WeavessTypes.SelectionWindow[]
 */
export function generateSelectionWindows(
  signalDetections: SignalDetectionTypes.SignalDetection[],
  currentOpenEvent: EventTypes.Event,
  measurementMode: AnalystWorkspaceTypes.MeasurementMode
): WeavessTypes.SelectionWindow[] {
  const selectionWindows: WeavessTypes.SelectionWindow[] = flatMap(
    signalDetections.map(sd => {
      const associatedSignalDetectionHypothesisIds = currentOpenEvent
        ? currentOpenEvent.currentEventHypothesis.eventHypothesis.signalDetectionAssociations.map(
            association => association.signalDetectionHypothesis.id
          )
        : [];

      const arrivalTime: number = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
        sd.currentHypothesis.featureMeasurements
      ).value;

      const phase = SignalDetectionUtils.findPhaseFeatureMeasurementValue(
        sd.currentHypothesis.featureMeasurements
      ).phase;

      const isSdAssociatedToOpenEvent =
        includes(associatedSignalDetectionHypothesisIds, sd.currentHypothesis.id) &&
        // sd must have phase type that is contained in the measurement mode phase filter list
        includes(systemConfig.measurementMode.phases, phase);

      const isManualShow = [...measurementMode.entries.entries()]
        .filter(({ 1: v }) => v)
        .map(([k]) => k)
        .find(id => id === sd.id);

      const isManualHide = [...measurementMode.entries.entries()]
        .filter(({ 1: v }) => !v)
        .map(([k]) => k)
        .find(id => id === sd.id);

      const amplitudeMeasurementValue = SignalDetectionUtils.findAmplitudeFeatureMeasurementValue(
        sd.currentHypothesis.featureMeasurements,
        SignalDetectionTypes.FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2
      );

      const selectionStartOffset: number =
        systemConfig.measurementMode.selection.startTimeOffsetFromSignalDetection;
      const selectionEndOffset: number =
        systemConfig.measurementMode.selection.endTimeOffsetFromSignalDetection;

      // display the measurement selection windows if the sd is associated
      // to the open event and its phase is included in one of the measurement mode phases
      if (
        (measurementMode.mode === AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT &&
          isSdAssociatedToOpenEvent &&
          !isManualHide) ||
        isManualShow
      ) {
        const selections: WeavessTypes.SelectionWindow[] = [];
        selections.push({
          id: `${systemConfig.measurementMode.selection.id}${sd.id}`,
          startMarker: {
            id: 'start',
            color: systemConfig.measurementMode.selection.borderColor,
            lineStyle: systemConfig.measurementMode.selection.lineStyle,
            timeSecs: arrivalTime + selectionStartOffset
          },
          endMarker: {
            id: 'end',
            color: systemConfig.measurementMode.selection.borderColor,
            lineStyle: systemConfig.measurementMode.selection.lineStyle,
            timeSecs: arrivalTime + selectionEndOffset
          },
          isMoveable: systemConfig.measurementMode.selection.isMoveable,
          color: systemConfig.measurementMode.selection.color
        });

        if (amplitudeMeasurementValue) {
          const period = amplitudeMeasurementValue.period;
          const troughTime: number = amplitudeMeasurementValue.startTime;
          const peakTime = troughTime + period / 2; // display only period/2
          const isWarning = isPeakTroughInWarning(arrivalTime, period, troughTime, peakTime);

          const isMoveable =
            measurementMode.mode === AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT &&
            systemConfig.measurementMode.peakTroughSelection.isMoveable;
          selections.push({
            id: `${systemConfig.measurementMode.peakTroughSelection.id}${sd.id}`,
            startMarker: {
              id: 'start',
              color: !isWarning
                ? systemConfig.measurementMode.peakTroughSelection.borderColor
                : systemConfig.measurementMode.peakTroughSelection.warning.borderColor,
              lineStyle: isMoveable
                ? systemConfig.measurementMode.peakTroughSelection.lineStyle
                : systemConfig.measurementMode.peakTroughSelection.nonMoveableLineStyle,
              timeSecs: troughTime,
              minTimeSecsConstraint: arrivalTime + selectionStartOffset
            },
            endMarker: {
              id: 'end',
              color: !isWarning
                ? systemConfig.measurementMode.peakTroughSelection.borderColor
                : systemConfig.measurementMode.peakTroughSelection.warning.borderColor,
              lineStyle: isMoveable
                ? systemConfig.measurementMode.peakTroughSelection.lineStyle
                : systemConfig.measurementMode.peakTroughSelection.nonMoveableLineStyle,
              timeSecs: peakTime,
              maxTimeSecsConstraint: arrivalTime + selectionEndOffset
            },
            isMoveable,
            color: !isWarning
              ? systemConfig.measurementMode.peakTroughSelection.color
              : systemConfig.measurementMode.peakTroughSelection.warning.color
          });
        }
        return selections;
      }
      return [];
    })
  );
  return selectionWindows;
}

/**
 * Creates a station for weavess
 *
 * @param station station
 * @param defaultChannelName default channel name
 * @param segmentType segment type
 * @param selectedFilter selected filter
 * @param channelSegments channel segment map
 * @param signalDetections signal detections
 * @param params CreateWeavessStationsParameters the parameters required for
 * creating the weavess stations for the waveform display.
 *
 * @returns a WeavessTypes.Station
 */
export function createWeavessStation(
  station: ProcessingStationTypes.ProcessingStation,
  selectedFilter: WaveformTypes.WaveformFilter,
  channelSegments: Map<string, WeavessTypes.ChannelSegment>,
  signalDetections: SignalDetectionTypes.SignalDetection[],
  params: CreateWeavessStationsParameters
): WeavessTypes.Station {
  const distanceToEvent = params.distances
    ? params.distances.find(d => d.stationId === station.name)
    : undefined;

  const weavessStation: WeavessTypes.Station = {
    id: station.name,
    name: station.name,
    distance: distanceToEvent ? distanceToEvent.distance.degrees : 0,
    distanceUnits: userPreferences.distanceUnits,
    defaultChannel: createWeavessDefaultChannel(
      station,
      selectedFilter,
      channelSegments,
      signalDetections,
      params
    ),
    nonDefaultChannels: createWeavessNonDefaultChannels(station, params)
  };
  return weavessStation;
}

/**
 * Creates a default channel waveform for weavess
 *
 * @param station a processing station
 * @param selectedFilter the currently selected filter
 * @param channelSegments map of channel segments
 * @param signalDetections signal detections
 * @param params CreateWeavessStationsParameters the parameters required for
 * creating the weavess stations for the waveform display.
 *
 * @returns a WeavessTypes.Channel
 */
export function createWeavessDefaultChannel(
  station: ProcessingStationTypes.ProcessingStation,
  selectedFilter: WaveformTypes.WaveformFilter,
  channelSegments: Map<string, WeavessTypes.ChannelSegment>,
  signalDetections: SignalDetectionTypes.SignalDetection[],
  params: CreateWeavessStationsParameters
): WeavessTypes.Channel {
  // Build a default channel segment to use if no Signal Detections are found
  // The segment type is FK_BEAM since that is all that is drawn on the default channels
  const segmentType: ChannelSegmentTypes.ChannelSegmentType =
    ChannelSegmentTypes.ChannelSegmentType.FK_BEAM;
  const defaultChannelName = getChannelLabelAddition(segmentType, segmentType, false);
  // FIXME: What should the default channel be?
  const stationOffset = params.offsets.find(
    offset => offset.channelId === station.channels[0].name
  );
  const defaultChannel = {
    id: station.name,
    name: defaultChannelName ? `${station.name}${defaultChannelName}` : `${station.name}`,
    height: params.channelHeight,
    timeOffsetSeconds: stationOffset ? stationOffset.offset : 0,
    channelType: segmentType,
    waveform: createWeavessDefaultChannelWaveform(
      station,
      signalDetections,
      selectedFilter,
      channelSegments,
      params
    )
  };
  return defaultChannel;
}

/**
 * Creates a non default channel for weavess
 *
 * @param station a processing station
 * @param params CreateWeavessStationsParameters the parameters required for
 * creating the weavess stations for the waveform display.
 *
 * @returns a WeavessTypes.Channel[]
 */
export function createWeavessNonDefaultChannels(
  station: ProcessingStationTypes.ProcessingStation,
  params: CreateWeavessStationsParameters
): WeavessTypes.Channel[] {
  // sds are only displayed on the default channel;
  // hide all non-default channels in measurement mode

  // FIXME: What is the default channel?
  const stationOffset = params.offsets.find(
    offset => offset.channelId === station.channels[0].name
  );
  const nonDefaultChannels =
    AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT === params.measurementMode.mode
      ? []
      : station.channels.map(channel => {
          const nonDefaultChannel = getChannelSegments(
            params.measurementMode.mode,
            channel.name,
            channel.nominalSampleRateHz,
            params.channelFilters,
            params.waveformClient,
            params.defaultWaveformFilters,
            params.startTimeSecs
          );
          const nonDefaultChannelName = getChannelLabelAddition(
            nonDefaultChannel.segmentType,
            channel.displayName,
            true
          );
          const ndc: WeavessTypes.Channel = {
            id: channel.name,
            name: nonDefaultChannelName ? `${nonDefaultChannelName}` : `${channel.name}`,
            timeOffsetSeconds: stationOffset ? stationOffset.offset : 0,
            height: params.channelHeight,
            waveform: createWeavessNonDefaultChannelWaveform(nonDefaultChannel, channel, params)
          };
          return ndc;
        });
  return nonDefaultChannels;
}

/**
 * Creates a default channel waveform for weavess
 *
 * @param station a processing station
 * @param signalDetections signal detections
 * @param selectedFilter current selected filter
 * @param channelSegments map of channel segments
 * @param params CreateWeavessStationsParameters the parameters required for
 * creating the weavess stations for the waveform display.
 *
 * @returns a WeavessTypes.ChannelWaveformContent
 */
export function createWeavessDefaultChannelWaveform(
  station: ProcessingStationTypes.ProcessingStation,
  signalDetections: SignalDetectionTypes.SignalDetection[],
  selectedFilter: WaveformTypes.WaveformFilter,
  channelSegments: Map<string, WeavessTypes.ChannelSegment>,
  params: CreateWeavessStationsParameters
): WeavessTypes.ChannelWaveformContent {
  const waveform = {
    channelSegmentId: selectedFilter ? selectedFilter.id : '',
    channelSegments,
    predictedPhases: params.showPredictedPhases
      ? params.featurePredictions
          .filter(fp => fp.stationName === station.name)
          .map((fp, index) => ({
            timeSecs: isSdInstantMeasurementValue(fp.predictedValue)
              ? fp.predictedValue.value
              : undefined,
            label: fp.phase,
            id: `${index}`,
            color: semanticColors.analystOpenEvent,
            filter: 'opacity(0.5)',
            isConflicted: false
          }))
      : [],
    signalDetections:
      station && signalDetections
        ? signalDetections.map(detection => {
            const color = determineDetectionColor(
              detection,
              params.eventsInTimeRange,
              params.currentOpenEvent ? params.currentOpenEvent.id : undefined
            );
            const isAssociatedToOpenEvent = determineIfAssociated(
              detection,
              params.eventsInTimeRange,
              params.currentOpenEvent ? params.currentOpenEvent.id : undefined
            );
            const arrivalTimeFeatureMeasurementValue = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
              detection.currentHypothesis.featureMeasurements
            );
            const fmPhase = SignalDetectionUtils.findPhaseFeatureMeasurementValue(
              detection.currentHypothesis.featureMeasurements
            );
            return {
              timeSecs: arrivalTimeFeatureMeasurementValue
                ? arrivalTimeFeatureMeasurementValue.value
                : 0,
              label: fmPhase.phase.toString(),
              id: detection.id,
              color,
              isConflicted: detection.hasConflict,
              isDisabled: !isAssociatedToOpenEvent && detection.hasConflict
            };
          })
        : [],
    masks: undefined,
    markers: {
      verticalMarkers: getIntervalMarkers(params.startTimeSecs, params.endTimeSecs),
      selectionWindows: generateSelectionWindows(
        signalDetections,
        params.currentOpenEvent,
        params.measurementMode
      )
    }
  };
  return waveform;
}

/**
 * Creates a non default channel waveform for weavess
 *
 * @param nonDefaultChannel non default channel
 * @param channel processing channel
 * @param params CreateWeavessStationsParameters the parameters required for
 * creating the weavess stations for the waveform display.
 *
 * @returns a WeavessTypes.ChannelWaveformContent
 */
export function createWeavessNonDefaultChannelWaveform(
  nonDefaultChannel: {
    channelSegmentId: string;
    channelSegments: Map<string, WeavessTypes.ChannelSegment>;
    segmentType: ChannelSegmentTypes.ChannelSegmentType;
  },
  channel: ProcessingStationTypes.ProcessingChannel,
  params: CreateWeavessStationsParameters
): WeavessTypes.ChannelWaveformContent {
  const waveform = {
    channelSegmentId: nonDefaultChannel.channelSegmentId,
    channelSegments: nonDefaultChannel.channelSegments,
    // if the mask category matches the enabled masks then return the mask else skip it
    masks: channel
      ? params.qcMasksByChannelName
          .filter(m => m.channelName === channel.name)
          .filter(qcMask =>
            Object.keys(params.maskDisplayFilters).find(
              key =>
                qcMask.currentVersion.category === key && params.maskDisplayFilters[key].visible
            )
          )
          .map(qcMask => ({
            id: qcMask.id,
            startTimeSecs: qcMask.currentVersion.startTime,
            endTimeSecs: qcMask.currentVersion.endTime,
            color: userPreferences.colors.maskDisplayFilters[qcMask.currentVersion.category].color
          }))
      : undefined,
    markers: {
      verticalMarkers: getIntervalMarkers(params.startTimeSecs, params.endTimeSecs)
    }
  };
  return waveform;
}

/**
 * Creates the weavess stations for the waveform display.
 *
 * @param params CreateWeavessStationsParameters the parameters required for
 * creating the weavess stations for the waveform display.
 *
 * @returns a WeavessTypes.WeavessStation[]
 */
export function createWeavessStations(
  params: CreateWeavessStationsParameters
): WeavessTypes.Station[] {
  const weavessStations: WeavessTypes.Station[] = params.defaultStations
    // filter the stations based on the mode setting
    .filter(stationToFilterOnMode =>
      filterStationOnMode(
        params.measurementMode.mode,
        stationToFilterOnMode,
        params.currentOpenEvent,
        params.signalDetectionsByStation
      )
    )
    .map(station => {
      const selectedFilter: WaveformTypes.WaveformFilter = getSelectedWaveformFilter(
        params.measurementMode.mode,
        station.name,
        // FIXME: need nominal Hz rate for station
        station.channels[0].nominalSampleRateHz,
        params.channelFilters,
        params.defaultWaveformFilters
      );

      const signalDetections = params.signalDetectionsByStation
        ? filterSignalDetectionsByStationId(station.name, params.signalDetectionsByStation)
        : [];

      const channelSegments = new Map<string, WeavessTypes.ChannelSegment>();

      populateWavessChannelSegmentAndAddFilter(
        signalDetections,
        params.defaultWaveformFilters,
        channelSegments
      );
      const weavessStation = createWeavessStation(
        station,
        selectedFilter,
        channelSegments,
        signalDetections,
        params
      );

      // Sort non-default channels alphabetical
      weavessStation.nonDefaultChannels = orderBy(
        weavessStation.nonDefaultChannels,
        [chan => chan.name],
        ['asc']
      );
      return weavessStation;
    })
    .filter(weavessStation => weavessStation !== undefined);
  return weavessStations;
}

/**
 * Gets the appropriate channelSegments for the currently applied filter
 *
 * @param mode current mode
 * @param id Id of the channel
 * @param sampleRate the sample rate of the channel
 * @param channelFilters Mapping of ids to filters
 * @param waveformClient Reference to instantiated WaveformClient object
 * @param defaultWaveformFilters A list of filters retrieved from the gateway
 * @param startTimeSecs The start time of the channel Segments
 *
 * @returns an object containing a channelSegmentId, list of channel segments, and the type of segment
 */
export function getChannelSegments(
  mode: AnalystWorkspaceTypes.WaveformDisplayMode,
  id: string,
  sampleRate: number,
  channelFilters: Immutable.Map<string, WaveformTypes.WaveformFilter>,
  waveformClient: WaveformClient,
  defaultWaveformFilters: WaveformTypes.WaveformFilter[],
  startTimeSecs: number
) {
  const selectedFilter: WaveformTypes.WaveformFilter = getSelectedWaveformFilter(
    mode,
    id,
    sampleRate,
    channelFilters,
    defaultWaveformFilters
  );

  const channelSegments = new Map<string, WeavessTypes.ChannelSegment>();
  const cachedEntries = waveformClient.getWaveformEntriesForChannelId(id);

  let segmentType: ChannelSegmentTypes.ChannelSegmentType;
  if (cachedEntries) {
    cachedEntries.forEach((value, key) => {
      if (key === 'unfiltered') {
        segmentType = value[0].type;
      }
      let waveformFilter: WaveformTypes.WaveformFilter = createUnfilteredWaveformFilter();
      defaultWaveformFilters.forEach(f => {
        if (f.id === key) {
          waveformFilter = f;
        }
      });

      const result = getDataSegments(value, waveformFilter, id, startTimeSecs);
      const filterSampleRate =
        waveformFilter.sampleRate !== undefined ? ` ${waveformFilter.sampleRate} ` : '';
      const description = `${waveformFilter.name}${filterSampleRate}`;

      channelSegments.set(key, {
        description: result.showLabel ? description : '',
        descriptionLabelColor: result.isSampleRateOk
          ? gmsColors.gmsMain
          : gmsColors.gmsStrongWarning,
        dataSegments: result.dataSegments
      });
    });
  }
  return { channelSegmentId: selectedFilter.id, channelSegments, segmentType };
}

/**
 * Gets data segments based on what filter is being applied and the presence of raw data
 *
 * @param cachedData result of cache get based on channel and filter
 * @param filter filter being applied
 * @param channel the filter is being applied to
 * @param startTimeSecs start of data segment
 *
 * @returns object with list of dataSegments, isSampleRateOk (boolean), showLabel (boolean)
 */
function getDataSegments(
  cachedData: [
    | ChannelSegmentTypes.ChannelSegment<WaveformTypes.Waveform>
    | WaveformTypes.FilteredChannelSegment
  ],
  filter: WaveformTypes.WaveformFilter,
  channel: string,
  startTimeSecs: number
) {
  // If there was no raw data and no filtered data return empty data segments
  if (!cachedData || cachedData.length < 1 || !cachedData.filter(s => s.timeseries.length > 1)) {
    return {
      dataSegs: [{ startTimeSecs, data: [] }],
      sampleRateCheck: true,
      showLabel: false
    };
  }

  // This should be moved to waveform client fetch -- most likely
  const isSampleRateOk =
    filter.name === WaveformTypes.UNFILTERED_FILTER.name ||
    (filter.validForSampleRate &&
      sampleRateIsInTolerance(cachedData[0].timeseries[0].sampleRate, filter));

  const sampleRate =
    cachedData && cachedData[0].timeseries
      ? cachedData[0].timeseries[0].sampleRate
      : WaveformTypes.DEFAULT_SAMPLE_RATE;

  const dataSegments = flatten(
    cachedData.map(s =>
      s.timeseries.map((t: WaveformTypes.Waveform) => ({
        displayType: [WeavessTypes.DisplayType.LINE],
        color: semanticColors.waveformRaw,
        pointSize: 1,
        data: {
          startTimeSecs: t.startTime,
          sampleRate,
          values: t.values
        }
      }))
    )
  );

  return { dataSegments, isSampleRateOk, showLabel: true };
}

/**
 * Checks sample rate tolerance based on waveform filter
 *
 * @param sampleRate sample rate to check
 * @param wfFilter filter definition
 *
 * @returns boolean
 */
function sampleRateIsInTolerance(
  sampleRate: number,
  wfFilter: WaveformTypes.WaveformFilter
): boolean {
  return (
    sampleRate > wfFilter.sampleRate - wfFilter.sampleRateTolerance &&
    sampleRate < wfFilter.sampleRate + wfFilter.sampleRateTolerance
  );
}

/**
 * Helper function to return the correct channel label based on channel segment type
 * channel
 *
 * @param channelSegment Channel segment to get label for
 * @param channelName name of channel
 * @param isSubChannel whether or not its a sub channel
 *
 * @returns string representing the channel label
 */
function getChannelLabelAddition(
  channelSegment: ChannelSegmentTypes.ChannelSegmentType,
  channelName: string,
  isSubChannel: boolean
): string {
  // If channel segment is not defined return empty for default channel
  // or channel label for sub-channels (otherwise looks like repeated channels)
  if (!channelSegment) {
    if (isSubChannel) {
      return channelName;
    }
    return '';
  }

  if (channelSegment === ChannelSegmentTypes.ChannelSegmentType.FK_BEAM) {
    return ' fkb';
  }

  if (!channelName) {
    return '';
  }
  return channelName;
}

/**
 * Returns true if there is a graphql query loading; false otherwise.
 *
 * @param props WaveformDisplayProps
 * @param state WaveformDisplayState
 *
 * @returns boolean
 */
export function isLoading(props: WaveformDisplayProps, state: WaveformDisplayState): boolean {
  return (
    (props.defaultStationsQuery && props.defaultStationsQuery.loading) ||
    (props.uiConfigurationQuery && props.uiConfigurationQuery.loading) ||
    (props.eventsInTimeRangeQuery && props.eventsInTimeRangeQuery.loading) ||
    (props.signalDetectionsByStationQuery && props.signalDetectionsByStationQuery.loading) ||
    (props.qcMasksByChannelNameQuery && props.qcMasksByChannelNameQuery.loading) ||
    state.loadingWaveforms
  );
}

/**
 * Returns true if there is a graphql query error; false otherwise.
 *
 * @param props WaveformDisplayProps
 *
 * @returns boolean
 */
export function isError(props: WaveformDisplayProps): boolean {
  return (
    (props.defaultStationsQuery && props.defaultStationsQuery.error !== undefined) ||
    (props.uiConfigurationQuery && props.uiConfigurationQuery.error !== undefined) ||
    (props.eventsInTimeRangeQuery && props.eventsInTimeRangeQuery.error !== undefined) ||
    (props.signalDetectionsByStationQuery &&
      props.signalDetectionsByStationQuery.error !== undefined) ||
    (props.qcMasksByChannelNameQuery && props.qcMasksByChannelNameQuery.error !== undefined)
  );
}

/**
 * sort waveform list based on sort type
 *
 * @param props WaveformDisplayProps
 * @param state WaveformDisplayStates
 *
 * @returns sortedWaveformList
 */
export function sortWaveformList(
  stations: WeavessTypes.Station[],
  waveformSortType: AnalystWorkspaceTypes.WaveformSortType
): WeavessTypes.Station[] {
  // apply sort based on sort type
  let sortedStations: WeavessTypes.Station[] = [];
  // Sort by distance if in global scan
  if (waveformSortType === AnalystWorkspaceTypes.WaveformSortType.distance) {
    sortedStations = sortBy<WeavessTypes.Station>(stations, [station => station.distance]);
  } else {
    // For station name sort, order a-z by station config name
    if (waveformSortType === AnalystWorkspaceTypes.WaveformSortType.stationName) {
      sortedStations = orderBy<WeavessTypes.Station>(stations, [station => station.name], ['asc']);
    }
  }
  return sortedStations;
}

/**
 * sort waveform list based on sort type
 *
 * @param props WaveformDisplayProps
 * @param state WaveformDisplayStates
 *
 * @returns sortedWaveformList
 */
export function sortProcessingStations(
  stations: ProcessingStationTypes.ProcessingStation[],
  waveformSortType: AnalystWorkspaceTypes.WaveformSortType,
  distances: EventTypes.LocationToStationDistance[]
): ProcessingStationTypes.ProcessingStation[] {
  // apply sort based on sort type
  let sortedStations: ProcessingStationTypes.ProcessingStation[] = [];
  // Sort by distance if in global scan
  if (waveformSortType === AnalystWorkspaceTypes.WaveformSortType.distance) {
    sortedStations = sortBy<ProcessingStationTypes.ProcessingStation>(
      stations,
      station => distances.find(source => source.stationId === station.name).distance.degrees
    );
  } else {
    // For station name sort, order a-z by station config name
    if (waveformSortType === AnalystWorkspaceTypes.WaveformSortType.stationName) {
      sortedStations = orderBy<ProcessingStationTypes.ProcessingStation>(
        stations,
        [station => station.name],
        ['asc']
      );
    }
  }
  return sortedStations;
}

/**
 * Returns Feature Predictions if there is an open event
 * @param props current waveform display props
 */
export function getFeaturePredictionsForOpenEvent(
  props: WaveformDisplayProps
): EventTypes.FeaturePrediction[] {
  if (props.currentOpenEventId && props.eventsInTimeRangeQuery.eventsInTimeRange) {
    const openEvent = props.eventsInTimeRangeQuery.eventsInTimeRange.find(
      event => event.id === props.currentOpenEventId
    );
    if (openEvent) {
      return openEvent.currentEventHypothesis.eventHypothesis.preferredLocationSolution
        .locationSolution.featurePredictions;
    }
  }
  return [];
}

/**
 * Returns a list of phases that are present for FP alignment
 */
export function getAlignablePhases(props: WaveformDisplayProps): CommonTypes.PhaseType[] {
  return systemConfig.defaultSdPhases.filter(phase => {
    const fpList = getFeaturePredictionsForOpenEvent(props);
    return fpList.filter(fp => fp.phase === phase).length > 0;
  });
}
