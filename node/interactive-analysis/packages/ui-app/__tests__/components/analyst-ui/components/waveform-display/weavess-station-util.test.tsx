import { CommonTypes, EventTypes, WaveformTypes } from '@gms/common-graphql';
import { readJsonData } from '@gms/common-util';
import { Client, createApolloClientConfiguration } from '@gms/ui-apollo';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { WeavessTypes } from '@gms/weavess';
import Immutable from 'immutable';
import path from 'path';
// tslint:disable-next-line: max-line-length
import { WaveformClient } from '../../../../../src/ts/components/analyst-ui/components/waveform-display/waveform-client';
import {
  createWeavessDefaultChannel,
  createWeavessDefaultChannelWaveform,
  createWeavessNonDefaultChannels,
  createWeavessNonDefaultChannelWaveform,
  createWeavessStation,
  createWeavessStations,
  CreateWeavessStationsParameters,
  generateSelectionWindows,
  getAlignablePhases,
  getChannelSegments,
  getFeaturePredictionsForOpenEvent,
  isError,
  isLoading,
  populateCreateWeavessStationsParameters,
  populateWavessChannelSegmentAndAddFilter,
  sortProcessingStations,
  sortWaveformList
} from '../../../../../src/ts/components/analyst-ui/components/waveform-display/weavess-stations-util';
import { eventData } from '../../../../__data__/event-data';

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

const apolloClient: Client | undefined = createApolloClientConfiguration().client;

describe('Waveform display util', () => {
  it('should have proper exports', () => {
    expect(populateCreateWeavessStationsParameters).toBeDefined();
    expect(populateWavessChannelSegmentAndAddFilter).toBeDefined();
    expect(generateSelectionWindows).toBeDefined();
    expect(createWeavessStation).toBeDefined();
    expect(createWeavessDefaultChannel).toBeDefined();
    expect(createWeavessNonDefaultChannels).toBeDefined();
    expect(createWeavessDefaultChannelWaveform).toBeDefined();
    expect(createWeavessNonDefaultChannelWaveform).toBeDefined();
    expect(createWeavessStations).toBeDefined();
    expect(isLoading).toBeDefined();
    expect(isError).toBeDefined();
    expect(sortWaveformList).toBeDefined();
    expect(sortProcessingStations).toBeDefined();
    expect(getFeaturePredictionsForOpenEvent).toBeDefined();
    expect(getAlignablePhases).toBeDefined();
  });
  /** set up all the necessary data for most of these function tests */
  const basePath = path.resolve(__dirname, './__data__');
  const currentOpenEvent: EventTypes.Event = eventData;
  const eventsInTimeRange = [eventData];
  const signalDetectionsByStation = readJsonData(
    path.resolve(basePath, 'signalDetectionsByStation.json')
  );
  const defaultStations = readJsonData(path.resolve(basePath, 'defaultStations.json'));
  const featurePredictions = readJsonData(path.resolve(basePath, 'featurePredictions.json'))[0];
  const defaultWaveformFilters = readJsonData(
    path.resolve(basePath, 'defaultWaveformFilters.json')
  );
  const defaultMode: AnalystWorkspaceTypes.MeasurementMode = {
    mode: AnalystWorkspaceTypes.WaveformDisplayMode.DEFAULT,
    entries: Immutable.Map()
  };
  const maskDisplayFilters = readJsonData(path.resolve(basePath, 'maskDisplayFilters.json'))[0];
  const qcMasksByChannelId = readJsonData(path.resolve(basePath, 'qcMasksByChannelId.json'));
  const measurementMode: AnalystWorkspaceTypes.MeasurementMode = {
    mode: AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT,
    entries: Immutable.Map<string, boolean>()
  };
  const defaultFilter: WaveformTypes.WaveformFilter = {
    id: '48fd578e-e428-43ff-9f9e-62598e7e6ce6',
    name: 'HAM FIR BP 0.70-2.00 Hz',
    description: 'Hamming FIR Filter Band Pass, 0.70-2.00 Hz',
    filterType: 'FIR_HAMMING',
    filterPassBandType: 'BAND_PASS',
    lowFrequencyHz: 0.7,
    highFrequencyHz: 2,
    order: 48,
    filterSource: 'SYSTEM',
    filterCausality: 'CAUSAL',
    zeroPhase: false,
    sampleRate: 20,
    sampleRateTolerance: 0.05,
    groupDelaySecs: 1.2,
    validForSampleRate: true
  };
  const selectedPreferredLocationSolutionId =
    currentOpenEvent.currentEventHypothesis.eventHypothesis.locationSolutionSets[0]
      .locationSolutions[0].id;
  const startTime = 1274392801;
  const endTime = 1274400000;
  const props = {
    featurePredictions,
    currentTimeInterval: {
      startTime,
      endTime
    },
    uiConfigurationQuery: {
      uiAnalystConfiguration: {
        logLevel: CommonTypes.LogLevel.info,
        defaultFilters: defaultWaveformFilters
      }
    },
    location: {
      selectedPreferredLocationSolutionId
    },
    measurementMode,
    defaultStationsQuery: {
      defaultProcessingStations: defaultStations
    },
    signalDetectionsByStationQuery: {
      signalDetectionsByStation
    },
    eventsInTimeRangeQuery: {
      eventsInTimeRange
    }
  } as any;
  const state = {
    currentOpenEventId: currentOpenEvent.id
  } as any;
  const channelHeight = 100;
  const waveformClient = new WaveformClient(apolloClient);
  const channelFilters = Immutable.Map<string, WaveformTypes.WaveformFilter>();
  const waveformUtilParams: CreateWeavessStationsParameters = {
    channelFilters,
    channelHeight: 24.8,
    currentOpenEvent,
    defaultStations,
    defaultWaveformFilters,
    endTimeSecs: 1274400000,
    eventsInTimeRange,
    featurePredictions,
    maskDisplayFilters,
    measurementMode: defaultMode,
    offsets: [],
    qcMasksByChannelName: qcMasksByChannelId,
    showPredictedPhases: false,
    signalDetectionsByStation,
    startTimeSecs: 1274392801,
    distances: [],
    waveformClient
  };
  // let result = createWeavessStations(waveformUtilParams);
  // result.forEach(station => {
  //     delete station.defaultChannel.waveform.channelSegments;
  //     station.nonDefaultChannels.forEach(channel => delete channel.waveform.channelSegments);
  // });
  /** end of test data settup */
  const a5FeatureMessurement = {
    id: '5550e3f1-edf2-4fa0-b7b7-1231b1230af2',
    featureMeasurementType: 'AMPLITUDE_A5_OVER_2',
    measurementValue: {
      startTime: 1274392955.4,
      period: 0.79,
      amplitude: {
        value: 3.6,
        standardDeviation: 0,
        units: 'UNITLESS',
        __typename: 'DoubleValue'
      },
      __typename: 'AmplitudeMeasurementValue'
    },
    creationInfo: {
      id: '5550e3f1-edf2-4fa0-b7b7-1231b1230af2',
      creationTime: 1553709593.995,
      creatorId: 'creatorId',
      creatorType: 'Analyst',
      creatorName: 'Matthew Carrasco',
      __typename: 'CreationInfo'
    },
    definingRules: [
      {
        operationType: 'Location',
        isDefining: true,
        __typename: 'DefiningRule'
      }
    ],
    __typename: 'FeatureMeasurement'
  };

  it('should have function populateCreateWeavessStationsParameters', () => {
    const params = populateCreateWeavessStationsParameters(
      props,
      state,
      channelHeight,
      waveformClient
    );
    params.waveformClient = undefined;
    expect(params).toMatchSnapshot();
  });

  it('should have function populateWavessChannelSegmentAndAddFilter', () => {
    signalDetectionsByStation.forEach(element => {
      element.currentHypothesis.featureMeasurements
        .filter(fm => fm.featureMeasurementType === 'ARRIVAL_TIME')
        .forEach(fm => {
          fm.channelSegment = {
            timeseries: [{ startTime, values: [], sampleRate: 10 }]
          };
        });
    });
    const populatedChannelSegments = new Map<string, WeavessTypes.ChannelSegment>();
    populateWavessChannelSegmentAndAddFilter(
      signalDetectionsByStation,
      defaultWaveformFilters,
      populatedChannelSegments
    );
    expect(populatedChannelSegments).toMatchSnapshot();
  });

  it('should have function generateSelectionWindows', () => {
    const oldId =
      currentOpenEvent.currentEventHypothesis.eventHypothesis.signalDetectionAssociations[0]
        .signalDetectionHypothesis.id;
    // tslint:disable-next-line: max-line-length
    currentOpenEvent.currentEventHypothesis.eventHypothesis.signalDetectionAssociations[0].signalDetectionHypothesis.id =
      '45db56ed-4393-3234-9ffe-8378e5d7be57';
    const fmOld = signalDetectionsByStation[0].currentHypothesis.featureMeasurements[0];
    signalDetectionsByStation[0].currentHypothesis.featureMeasurements[0] = a5FeatureMessurement;
    expect(
      generateSelectionWindows(signalDetectionsByStation, currentOpenEvent, measurementMode)
    ).toMatchSnapshot();
    // tslint:disable-next-line: max-line-length
    currentOpenEvent.currentEventHypothesis.eventHypothesis.signalDetectionAssociations[0].signalDetectionHypothesis.id = oldId;
    signalDetectionsByStation[0].currentHypothesis.featureMeasurements[0] = fmOld;
  });

  it('should have function createWeavessStation', () => {
    const populatedChannelSegments = new Map<string, WeavessTypes.ChannelSegment>();
    expect(
      createWeavessStation(
        defaultStations[0],
        defaultFilter,
        populatedChannelSegments,
        signalDetectionsByStation,
        waveformUtilParams
      )
    ).toMatchSnapshot();
  });

  it('should have function createWeavessDefaultChannel', () => {
    const populatedChannelSegments = new Map<string, WeavessTypes.ChannelSegment>();
    expect(
      createWeavessDefaultChannel(
        defaultStations[0],
        defaultFilter,
        populatedChannelSegments,
        signalDetectionsByStation,
        waveformUtilParams
      )
    ).toMatchSnapshot();
  });

  it('should have function createWeavessNonDefaultChannels', () => {
    expect(
      createWeavessNonDefaultChannels(defaultStations[0], waveformUtilParams)
    ).toMatchSnapshot();
  });

  it('should have function createWeavessDefaultChannelWaveform', () => {
    const populatedChannelSegments = new Map<string, WeavessTypes.ChannelSegment>();
    expect(
      createWeavessDefaultChannelWaveform(
        defaultStations[0],
        signalDetectionsByStation,
        defaultFilter,
        populatedChannelSegments,
        waveformUtilParams
      )
    ).toMatchSnapshot();
  });

  it('should have function createWeavessNonDefaultChannelWaveform', () => {
    const nonDefaultChannel = getChannelSegments(
      waveformUtilParams.measurementMode.mode,
      defaultStations[0].channels[0].name,
      defaultStations[0].channels[0].nominalSampleRateHz,
      waveformUtilParams.channelFilters,
      waveformUtilParams.waveformClient,
      waveformUtilParams.defaultWaveformFilters,
      waveformUtilParams.startTimeSecs
    );
    expect(
      createWeavessNonDefaultChannelWaveform(
        nonDefaultChannel,
        defaultStations[0].channels[0],
        waveformUtilParams
      )
    ).toMatchSnapshot();
  });

  it('should have function createWeavessStations', () => {
    expect(createWeavessStations(waveformUtilParams)).toMatchSnapshot();
  });

  it('should have function isLoading', () => {
    const inputProps: any = {
      defaultStationsQuery: { loading: false },
      uiConfigurationQuery: { loading: false },
      signalDetectionsByStationQuery: { loading: false },
      eventsInTimeRangeQuery: { loading: false },
      qcMasksByChannelNameQuery: { loading: true }
    };
    const inputState: any = { loadingWaveforms: true };
    expect(isLoading(inputProps, inputState)).toEqual(true);
    inputState.loadingWaveforms = false;
    expect(isLoading(inputProps, inputState)).toEqual(true);
    inputProps.qcMasksByChannelNameQuery.loading = false;
    expect(isLoading(inputProps, inputState)).toEqual(false);
    inputProps.defaultStationsQuery.loading = true;
    expect(isLoading(inputProps, inputState)).toEqual(true);
    inputProps.defaultStationsQuery.loading = false;
    inputProps.uiConfigurationQuery.loading = true;
    expect(isLoading(inputProps, inputState)).toEqual(true);
    inputProps.uiConfigurationQuery.loading = false;
    inputProps.signalDetectionsByStationQuery.loading = true;
    expect(isLoading(inputProps, inputState)).toEqual(true);
    inputProps.signalDetectionsByStationQuery.loading = false;
    inputProps.eventsInTimeRangeQuery.loading = true;
    expect(isLoading(inputProps, inputState)).toEqual(true);
  });

  it('should have function isError', () => {
    const inputProps: any = {
      defaultStationsQuery: { error: undefined },
      uiConfigurationQuery: { error: undefined },
      signalDetectionsByStationQuery: { error: undefined },
      eventsInTimeRangeQuery: { error: undefined },
      qcMasksByChannelNameQuery: { error: undefined }
    };
    expect(isError(inputProps)).toEqual(false);
    inputProps.defaultStationsQuery.error = 'error';
    expect(isError(inputProps)).toEqual(true);
    inputProps.defaultStationsQuery.error = undefined;
    inputProps.uiConfigurationQuery.error = 'error';
    expect(isError(inputProps)).toEqual(true);
    inputProps.uiConfigurationQuery.error = undefined;
    inputProps.signalDetectionsByStationQuery.error = 'error';
    expect(isError(inputProps)).toEqual(true);
    inputProps.signalDetectionsByStationQuery.error = undefined;
    inputProps.eventsInTimeRangeQuery.error = 'error';
    expect(isError(inputProps)).toEqual(true);
    inputProps.eventsInTimeRangeQuery.error = undefined;
    inputProps.qcMasksByChannelNameQuery.error = 'error';
    expect(isError(inputProps)).toEqual(true);
  });

  it('should have function sortWaveformList', () => {
    const distanceSort: any = sortWaveformList(defaultStations, 'Distance' as any);
    const stationSort: any = sortWaveformList(defaultStations, 'Station Name' as any);
    expect(distanceSort).toMatchSnapshot();
    expect(stationSort).toMatchSnapshot();
    expect(distanceSort !== stationSort).toBeTruthy();
  });

  it('should have function sortProcessingStations', () => {
    const distances: any = [{ stationId: 'PDAR', distance: { degrees: '1100' } }];
    const sorted: any = sortProcessingStations(defaultStations, 'Station Name' as any, distances);
    expect(sorted).toMatchSnapshot();
  });

  it('should have function getFeaturePredictionsForOpenEvent', () => {
    const fp: any = getFeaturePredictionsForOpenEvent(props);
    expect(fp).toMatchSnapshot();
  });

  it('should have function getAlignablePhases', () => {
    const phases: any = getAlignablePhases(props);
    expect(phases).toMatchSnapshot();
  });
});
