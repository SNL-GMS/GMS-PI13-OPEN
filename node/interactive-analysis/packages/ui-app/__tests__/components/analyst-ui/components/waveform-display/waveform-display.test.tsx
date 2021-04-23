// tslint:disable:max-line-length
import { WaveformTypes } from '@gms/common-graphql';
import { readJsonData } from '@gms/common-util';
import { Client, createApolloClientConfiguration } from '@gms/ui-apollo';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import Immutable from 'immutable';
import path from 'path';
import { WaveformClient } from '../../../../../src/ts/components/analyst-ui/components/waveform-display/waveform-client';
import {
  createWeavessStations,
  CreateWeavessStationsParameters
} from '../../../../../src/ts/components/analyst-ui/components/waveform-display/weavess-stations-util';
import { eventData } from '../../../../__data__/event-data';

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

const apolloClient: Client | undefined = createApolloClientConfiguration().client;

describe('Waveform display unit tests', () => {
  it('When switching to measurement mode, should show only waveforms/channels with associated SD', () => {
    const basePath = path.resolve(__dirname, './__data__');
    const currentOpenEvent = eventData[0]; // readJsonData(path.resolve(basePath, 'currentOpenEvent.json'))[0];
    const defaultStations = readJsonData(path.resolve(basePath, 'defaultStations.json'));
    const defaultWaveformFilters = readJsonData(
      path.resolve(basePath, 'defaultWaveformFilters.json')
    );
    const eventsInTimeRange = [eventData];
    const featurePredictions = readJsonData(path.resolve(basePath, 'featurePredictions.json'))[0];
    const maskDisplayFilters = readJsonData(path.resolve(basePath, 'maskDisplayFilters.json'))[0];
    const measurementMode: AnalystWorkspaceTypes.MeasurementMode = {
      mode: AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT,
      entries: Immutable.Map<string, boolean>()
    };
    const defaultMode: AnalystWorkspaceTypes.MeasurementMode = {
      mode: AnalystWorkspaceTypes.WaveformDisplayMode.DEFAULT,
      entries: Immutable.Map()
    };
    const qcMasksByChannelId = readJsonData(path.resolve(basePath, 'qcMasksByChannelId.json'));
    const signalDetectionsByStation = readJsonData(
      path.resolve(basePath, 'signalDetectionsByStation.json')
    );
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

    let result = createWeavessStations(waveformUtilParams);
    result.forEach(station => {
      delete station.defaultChannel.waveform.channelSegments;
      station.nonDefaultChannels.forEach(channel => delete channel.waveform.channelSegments);
    });

    expect(result).toMatchSnapshot();

    waveformUtilParams.measurementMode = measurementMode;

    result = createWeavessStations(waveformUtilParams);
    result.forEach(station => {
      delete station.defaultChannel.waveform.channelSegments;
      station.nonDefaultChannels.forEach(channel => delete channel.waveform.channelSegments);
    });
    expect(result).toMatchSnapshot();
  });
});
