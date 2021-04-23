import { readJsonData } from '@gms/common-util';
import config from 'config';
import cloneDeep from 'lodash/cloneDeep';
import path from 'path';
import { isFkSpectraChannelSegmentOSD, OSDChannelSegment } from '../channel-segment/model';
import { ComputeFkInput, FkPowerSpectraOSD, FkPowerSpectrumOSD } from '../fk/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import { HttpMockWrapper } from '../util/http-wrapper';
import { resolveTestDataPaths } from '../util/test-data-util';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const fs = require('file-system');

/**
 * The data store for channel segment data where channel segments
 * is the complete list, and fkPowerSpectraSegments are fk and the beam
 */
interface FkDataStore {
  fkPowerSpectraSegments: OSDChannelSegment<FkPowerSpectraOSD>[];
  currentFkIdx: number;
}

let isInitialized = false;
let fkConfig: any;
let dataStore: FkDataStore;

/**
 * Initialize for the Channel Segment mock processor that sets the mock enable on
 * the service calls, and intercepts them with internal function calls.
 * @param httpMockWrapper axios mock wrapper
 */
export function initialize(httpMockWrapper: HttpMockWrapper): void {
  // If already initialized...
  if (isInitialized) {
    return;
  }

  logger.info('Initializing mock backend for fk data');

  if (!httpMockWrapper) {
    throw new Error('Cannot initialize mock fk services with undefined HTTP mock wrapper');
  }

  // Load test data from the configured data set
  dataStore = loadTestData();

  // Load the channel segment backend service config settings
  fkConfig = config.get('fk');
  httpMockWrapper.onMock(fkConfig.backend.services.computeFk.requestConfig.url, computeFk);

  // Set flag
  isInitialized = true;
}

/**
 * Reads in and sets the test data used for mocking
 */
function loadTestData(): FkDataStore {
  logger.info('Initializing the fk service');
  const paths = resolveTestDataPaths();

  // STDS test data path (not part of the deployment this is where the large files exist)
  const testDataSTDSConfig = config.get('testData.standardTestDataSet');
  const testDataSTDSConfigFkSpectraDefinition: string = testDataSTDSConfig.fk.fkSpectraDefinition;

  logger.info(`Loading Fk spectrum from ${paths.fkHome}`);
  const fkPaths = [];
  try {
    fs.recurseSync(paths.fkHome, ['*.ChanSeg.json'], (filepath, relative, filename: string) => {
      if (filename) {
        fkPaths.push(paths.fkHome + path.sep + filename);
      }
    });
  } catch (e) {
    logger.error(`Failed to read fk paths from ${paths.fkHome} error: ${e}`);
  }

  // Lookup the Fk Definition path
  const fkDefinitionPath = paths.dataHome + path.sep + testDataSTDSConfigFkSpectraDefinition;
  const fkSpectraDefinitionString = 'FkSpectraDefinition';
  let fkSpectraDefinitions = [];
  try {
    fkSpectraDefinitions = readJsonData(fkDefinitionPath)[fkSpectraDefinitionString];
  } catch (e) {
    logger.error(`Failed to read fk spectra definitions`);
  }
  let stdsFkDataList: OSDChannelSegment<FkPowerSpectraOSD>[][] = [[]];
  stdsFkDataList = fkPaths.map(file => {
    try {
      const seg: any = readJsonData(file);
      const fkSegments: OSDChannelSegment<FkPowerSpectraOSD>[] = [];
      const fkDefinition = fkSpectraDefinitions.find(def => def.station === seg.channel.station);
      const fkSpectra: OSDChannelSegment<FkPowerSpectraOSD> = {
        id: seg.id,
        channel: seg.channel,
        name: seg.name,
        type: seg.type,
        timeseriesType: seg.timeseriesType,
        startTime: seg.timeseries[0].startTime,
        endTime: '',
        timeseries: seg.timeseries.map(series => ({
          startTime: seg.timeseries[0].startTime,
          sampleRate: series.sampleRate,
          sampleCount: series.sampleCount,
          windowLead: fkDefinition.windowLead,
          windowLength: fkDefinition.windowLength,
          lowFrequency: fkDefinition.lowFrequency,
          highFrequency: fkDefinition.highFrequency,
          type: seg.type,
          slowCountX: fkDefinition.slowCountX,
          slowCountY: fkDefinition.slowCountY,
          metadata: {
            slowStartX: fkDefinition.slowStartX,
            slowDeltaX: fkDefinition.slowDeltaX,
            slowStartY: fkDefinition.slowStartY,
            slowDeltaY: fkDefinition.slowDeltaY,
            phaseType: fkDefinition.phaseType
          },
          values: series.values
        }))
      };
      fkSegments.push(fkSpectra);
      return fkSegments;
    } catch (e) {
      logger.error(`Failed to read / process fk at ${file}`);
      return [];
    }
  });

  const stdsFks: OSDChannelSegment<FkPowerSpectraOSD>[] = [];
  stdsFkDataList.forEach(list => list.forEach(li => stdsFks.push(li)));
  logger.info(`Fk Mock Backend loaded ${stdsFks.length} FkSpectra entries`);
  return {
    fkPowerSpectraSegments: stdsFks,
    currentFkIdx: stdsFkDataList.length - 1
  };
}

/**
 * Update or add changed FkPowerSpectra Channel Segment to the data store
 */
export function updateFkPowerSpectra(newFkPowerSpectra: OSDChannelSegment<FkPowerSpectraOSD>) {
  if (newFkPowerSpectra && isFkSpectraChannelSegmentOSD(newFkPowerSpectra)) {
    const index = dataStore.fkPowerSpectraSegments.findIndex(
      fkp => fkp.id === newFkPowerSpectra.id
    );
    if (index >= 0) {
      dataStore.fkPowerSpectraSegments[index] = newFkPowerSpectra;
    } else {
      dataStore.fkPowerSpectraSegments.push(newFkPowerSpectra);
    }
  }
}

/**
 * Returns the next FK in the test data set, incrementing a static index.
 * This method provides a basic capability for mock FK data on demand
 * (e.g. to simulate calculating a new FK for an updated set of inputs specified
 * in the UI). If signal detection is set will try and lookup next Fk via that
 * specific signal detection (list fkToSdhIds).
 */
export function getNextFk(sdHypId?: string): OSDChannelSegment<FkPowerSpectraOSD> {
  dataStore.currentFkIdx = (dataStore.currentFkIdx + 1) % dataStore.fkPowerSpectraSegments.length;
  return cloneDeep(dataStore.fkPowerSpectraSegments[dataStore.currentFkIdx]);
}

/**
 * Gets new fk for given input - more or less a dummy function
 * @param createFkOsdInput window and frequency parameters for new fk
 */
export function computeFk(
  createFkOsdInput: ComputeFkInput
): OSDChannelSegment<FkPowerSpectraOSD>[] {
  performanceLogger.performance(
    'fkChannelSegment',
    'enteringService',
    `${createFkOsdInput.startTime}`
  );
  const nextFk: OSDChannelSegment<FkPowerSpectraOSD> = getNextFk();

  // Clone the channel segment since we are going to change the spectrum list, start and end times below.
  // blank out the reference to the values (spectrums list)
  const timeseries: FkPowerSpectraOSD[] = nextFk.timeseries.map(series => ({
    ...series,
    values: []
  }));
  const fkChannelSegment = {
    ...nextFk,
    timeseries
  };
  const sampleCount = createFkOsdInput.sampleCount;
  const valuesLength = nextFk.timeseries[0].values.length;
  // If timeseries[0] values is more than sample count down sample else add more to timeseries values
  // TODO: Fix if sample count is more than double the available values length
  let newValues: FkPowerSpectrumOSD[] = [];
  if (sampleCount < valuesLength) {
    newValues = nextFk.timeseries[0].values.slice(0, sampleCount);
  } else if (sampleCount > valuesLength) {
    let additionalAddedSamples = sampleCount;
    while (additionalAddedSamples > 0) {
      const currentRequiredSamples =
        additionalAddedSamples < 0 ? additionalAddedSamples : valuesLength;

      const moreSamples = nextFk.timeseries[0].values.slice(0, currentRequiredSamples);
      newValues = newValues.concat(moreSamples);
      additionalAddedSamples -= currentRequiredSamples;
    }
  }

  // Set the list of spectrums on the returning fk
  fkChannelSegment.timeseries[0].values = newValues;

  // Fix timeseries time, sample count, sample rate
  fkChannelSegment.timeseries[0].startTime = createFkOsdInput.startTime;
  fkChannelSegment.timeseries[0].sampleCount = fkChannelSegment.timeseries[0].values.length;
  fkChannelSegment.timeseries[0].sampleRate = createFkOsdInput.sampleRate;
  fkChannelSegment.timeseries[0].windowLead = createFkOsdInput.windowLead;
  fkChannelSegment.timeseries[0].windowLength = createFkOsdInput.windowLength;
  fkChannelSegment.timeseries[0].lowFrequency = createFkOsdInput.lowFrequency;
  fkChannelSegment.timeseries[0].highFrequency = createFkOsdInput.highFrequency;

  performanceLogger.performance(
    'fkChannelSegment',
    'returningFromService',
    `${createFkOsdInput.startTime}`
  );
  return [fkChannelSegment];
}

/**
 * Returns an fk spectra with the given channel segment id
 * @param channelSegmentId id to retrieve
 */
export function getFkPowerSpectraSegments(channelSegmentId: string): any {
  return dataStore.fkPowerSpectraSegments.find(fkC => fkC.id === channelSegmentId);
}
