import { readJsonData, toEpochSeconds, toOSDTime, uuid4 } from '@gms/common-util';
import config from 'config';
import cloneDeep from 'lodash/cloneDeep';
import path from 'path';
import { getFkPowerSpectraSegments, getNextFk } from '../fk/fk-mock-backend';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { HttpMockWrapper } from '../util/http-wrapper';
import { resolveTestDataPaths } from '../util/test-data-util';
import {
  isMockWaveformChannelSegment,
  MockWaveform,
  OSDWaveform,
  WaveformFileInfo
} from '../waveform/model';
import { getWaveformSegmentsByChannelSegments } from '../waveform/waveform-mock-backend';
import {
  BeamFormingInput,
  ChannelSegmentByIdInput,
  ChannelSegmentInput,
  ChannelSegmentType,
  OSDChannelSegment,
  OSDTimeSeries,
  TimeSeriesType
} from './model';
/**
 * Data Structure to return mapping of FilterId and Channel Segment Id
 * Used by Waveform Filter processor to call (for now) for derived Filtered
 * Channel Segments for FK_BEAM and ACQUIRED.
 */
export interface DerivedFilterChannelSegmentId {
  csId: string;
  wfFilterId: string;
}

/**
 * The data store for channel segment data where channel segments
 * is the complete list, and fkPowerSpectraSegments are fk and the beam
 */
interface ChannelSegmentDataStore {
  channelSegments: OSDChannelSegment<OSDTimeSeries>[];
  beamChannelSegmentsIds: string[];
  currentBeamIdx: number;
  filterMappings: FilterMapping[];
}

/**
 * Filter mapping interface for mapping filter id to filter name
 */
interface FilterMapping {
  filterId: string;
  filterName: string;
}
let isInitialized = false;
let dataStore: ChannelSegmentDataStore;
let channelSegmentConfig: any;

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

  logger.info('Initializing mock backend for channel segment data');

  if (!httpMockWrapper) {
    throw new Error(
      'Cannot initialize mock channel segment services with undefined HTTP mock wrapper'
    );
  }

  // Load test data from the configured data set
  dataStore = loadTestData();

  // Load the channel segment backend service config settings
  channelSegmentConfig = config.get('channelSegment');
  httpMockWrapper.onMock(
    channelSegmentConfig.backend.services.channelSegmentsById.requestConfig.url,
    // Get channel segments by id
    getChannelSegmentById
  );
  httpMockWrapper.onMock(
    channelSegmentConfig.backend.services.channelSegmentsByTimeRange.requestConfig.url,
    // Get segments by channel id and time range
    getChannelSegmentsByTimeRange
  );
  httpMockWrapper.onMock(
    channelSegmentConfig.backend.services.computeBeam.requestConfig.url,
    computeBeam
  );

  httpMockWrapper.onMock(channelSegmentConfig.backend.services.saveFks.requestConfig.url, saveFks);

  httpMockWrapper.onMock(
    channelSegmentConfig.backend.services.saveWaveforms.requestConfig.url,
    saveWaveforms
  );

  // Set flag
  isInitialized = true;
}

/**
 * Reads in and sets the test data used for mocking
 */
function loadTestData(): ChannelSegmentDataStore {
  logger.info('Initializing the channel segment service');
  const paths = resolveTestDataPaths();

  // STDS test data path (not part of the deployment this is where the large files exist)
  const testDataSTDSConfig = config.get('testData.standardTestDataSet');

  const chanSegIdWFilename = testDataSTDSConfig.channelSegment.channelSegmentIdToW;
  const chanSegFullPath = paths.channelsHome.concat(path.sep).concat(chanSegIdWFilename);

  logger.info(`Standard test data home ${paths.dataHome}`);
  logger.info(`Loading channel segments id file from ${chanSegFullPath}`);

  let chanSegFileInfoArray: WaveformFileInfo[] = [];
  try {
    chanSegFileInfoArray = readJsonData(chanSegFullPath);
  } catch (e) {
    logger.error(`Failed to read chan seg to w from file: ${chanSegFullPath}`);
    throw new Error(`File not found ${chanSegIdWFilename}`);
  }

  const channelSegmentMap: Map<string, OSDChannelSegment<OSDTimeSeries>> = new Map();
  // Need parallel fkBeam Channel Segment list to rotate thru to support computeBeam
  const beamChannelSegmentsIds: string[] = [];

  // convert cs from file
  chanSegFileInfoArray.forEach(cs => {
    const channelSegment: OSDChannelSegment<OSDTimeSeries> = createChannelSegmentEntry(cs);
    channelSegmentMap.set(channelSegment.id, channelSegment);
    if (
      cs.segmentType === ChannelSegmentType.FK_BEAM ||
      cs.segmentType === ChannelSegmentType.DETECTION_BEAM
    ) {
      beamChannelSegmentsIds.push(cs.segmentId);
    }
  });

  const filterMappingsString = 'filterMappings';
  const filterMapPath = config.get('testData.standardTestDataSet.filterMappings');
  const filterMappings = readJsonData(filterMapPath)[filterMappingsString];
  return {
    channelSegments: Array.from(channelSegmentMap.values()),
    beamChannelSegmentsIds,
    currentBeamIdx: beamChannelSegmentsIds.length - 1,
    filterMappings
  };
}

/**
 * Private helper function to convert claim check into a Channel Segment
 */
export function createChannelSegmentEntry(cs: WaveformFileInfo): OSDChannelSegment<OSDTimeSeries> {
  const timeseriesObj = [
    {
      type: TimeSeriesType.WAVEFORM,
      startTime: cs.startTime,
      waveformFile: cs.waveformFile,
      sampleRate: cs.sampleRate,
      sampleCount: cs.sampleCount,
      fOff: cs.fOff,
      dataType: cs.dataType,
      values: []
    }
  ];
  const startTime: number = toEpochSeconds(cs.startTime);
  const seconds: number = cs.sampleCount / cs.sampleRate;
  const endTime: string = toOSDTime(startTime + seconds);
  const channelSegment: OSDChannelSegment<OSDTimeSeries> = {
    id: cs.segmentId,
    channel: cs.channel,
    name: cs.segmentName,
    type: cs.segmentType,
    timeseriesType: TimeSeriesType.WAVEFORM,
    startTime: cs.startTime,
    endTime,
    timeseries: timeseriesObj
  };
  return channelSegment;
}

/**
 * Returns the next Beam Channel Segment to be returned by the computeBeam call.
 * @param sampleRate match the sample rate of the Beam being replaced
 */
export function getNextBeam(sampleRate: number): string {
  // Get next index and update the dataStore limited to the length of the beam channel length
  let beamIdx = (dataStore.currentBeamIdx += 1) % dataStore.beamChannelSegmentsIds.length;
  // Starting at next position walk through list to find next beam that match the rate
  let nextBeamId = dataStore.beamChannelSegmentsIds[beamIdx];
  let foundCandidate = false;
  while (!foundCandidate) {
    // Lookup beam in store
    const nextBeam = dataStore.channelSegments.find(cs => cs.id === nextBeamId);
    // If rate match found the candidate else keep looking
    if (nextBeam && nextBeam.timeseries[0].sampleRate === sampleRate) {
      foundCandidate = true;
    } else {
      beamIdx += 1;
      beamIdx %= dataStore.beamChannelSegmentsIds.length;
      nextBeamId = dataStore.beamChannelSegmentsIds[beamIdx];
    }
  }
  dataStore.currentBeamIdx = beamIdx;
  return nextBeamId;
}

/**
 * Mock to simulate COI streaming call to compute a new Beam (part of Fk)
 * @param input the beam forming input
 */
function computeBeam(input: BeamFormingInput): OSDChannelSegment<OSDTimeSeries>[] {
  performanceLogger.performance(
    'beamChannelSegment',
    'enteringService',
    `${input.waveforms[0].channel.name}`
  );
  if (!input) {
    return undefined;
  }

  // Got the beam but it still needs waveforms populated
  const nextBeamCSId = getNextBeam(input.waveforms[0].timeseries[0].sampleRate);
  if (!nextBeamCSId) {
    return [];
  }

  // build the query
  const query: ChannelSegmentByIdInput = {
    channelSegmentIds: [nextBeamCSId],
    withTimeseries: true
  };
  const channelSegmentOSDMap = getChannelSegmentById(query);

  // The channel seg returns a Map, so get the channel segment out of it and return as part of a list
  const channelSegments: OSDChannelSegment<OSDTimeSeries>[] = [];
  if (channelSegmentOSDMap && Object.keys(channelSegmentOSDMap).length > 0) {
    // Clone and then set the times for the beam returning
    const channelSegment: OSDChannelSegment<OSDTimeSeries> = cloneDeep(
      channelSegmentOSDMap[Object.keys(channelSegmentOSDMap)[0]]
    );
    channelSegment.id = uuid4();
    channelSegment.startTime = input.waveforms[0].startTime;
    channelSegment.endTime = input.waveforms[0].endTime;
    channelSegment.timeseries[0].startTime = input.waveforms[0].startTime;
    channelSegments.push(channelSegment);
  }
  performanceLogger.performance(
    'beamChannelSegment',
    'returningFromService',
    `${input.waveforms[0].channel.name}`
  );
  return channelSegments;
}

/**
 * Retrieve Channel Segment based on the Channel Segment Id
 * This could be an FK Spectrum or Waveform data structure
 */
function getChannelSegmentById(input: ChannelSegmentByIdInput): any | undefined {
  // ALWAYS assumes that the array will be of size one
  const channelSegmentId = input.channelSegmentIds[0];
  const channelSegment = dataStore.channelSegments.find(segment => segment.id === channelSegmentId);
  // Call the waveform mock backend to populate the waveform value
  // TODO add support for the various types and checks for single segment
  if (channelSegment) {
    if (isMockWaveformChannelSegment(channelSegment)) {
      const channelSegments: OSDChannelSegment<
        OSDWaveform
      >[] = getWaveformSegmentsByChannelSegments(
        toEpochSeconds(channelSegment.startTime),
        toEpochSeconds(channelSegment.endTime),
        [channelSegment]
      );
      const id: string = channelSegments[0].id;
      const toReturn = {};
      toReturn[id] = channelSegments[0];
      return toReturn;
    }
    // This is a FkSpectra Channel Segment
    return channelSegment;
  }
  const fk = getFkPowerSpectraSegments(channelSegmentId);
  if (fk) {
    return getNextFk(fk);
  }
}

/**
 * Gets the channel segments by time range.
 * @params input as ChannelSegmentInput
 */
export function getChannelSegmentsByTimeRange(
  input: ChannelSegmentInput
): OSDChannelSegment<OSDTimeSeries>[] {
  return getChannelSegments(
    input.channelNames,
    toEpochSeconds(input.startTime),
    toEpochSeconds(input.endTime)
  );
}

/**
 * Gets channel segments by
 * @param channelIds channel IDs
 * @param startTime start time as epoch seconds
 * @param endTime end time as epoch seconds
 * @returns a OSD channel segment[]
 */
export function getChannelSegments(
  channelIds: string[],
  startTime: number,
  endTime: number
): OSDChannelSegment<OSDTimeSeries>[] {
  const mins30 = 1800;
  const allChannelSegments: OSDChannelSegment<MockWaveform>[] = dataStore.channelSegments
    .filter(segment => {
      const segStartTime = toEpochSeconds(segment.startTime);
      const segEndTime = toEpochSeconds(segment.endTime);

      if (isMockWaveformChannelSegment(segment)) {
        // If not the channel id then this is not the one.
        // Else need to look at the length of the waveform (time length)
        // are we looking for a full raw segment something > 2 hours
        // or are we looking for raw data the length of fk-beam for compute beam
        if (channelIds.indexOf(segment.channel.name) === -1) {
          return false;
        }

        if (endTime - startTime < mins30) {
          // if less than 30 mins not full interval
          return segStartTime <= startTime && segEndTime >= endTime;
        }
        return Math.min(segEndTime, endTime) - Math.max(segStartTime, startTime) >= 0;
      }
    })
    .filter(isMockWaveformChannelSegment);
  if (allChannelSegments.length > 0) {
    return getWaveformSegmentsByChannelSegments(startTime, endTime, allChannelSegments);
  }
  return [];
}

/**
 * Function used in backend only to find Channel Segments matching on the channel name to
 * channel segment name (At least start with channel name). This is due to the Filtered Channel
 * Segment channelId is a derived channel id and not the reference channel id.
 * @param channelNames List of reference channel ids
 * @param startTime Start of interval searching
 * @param endTime End of interval searching
 */
export function getAcquiredFilteredChannelSegments(
  channelNames: string[],
  startTime: number,
  endTime: number,
  filterId: string
): OSDChannelSegment<OSDTimeSeries>[] {
  // list to return
  let allChannelSegments: OSDChannelSegment<MockWaveform>[] = [];
  // Loop through channel find each CS that matching on name and time range
  channelNames.forEach(channelName => {
    const channel = ProcessingStationProcessor.Instance().getChannelByName(channelName);
    if (channel && channel.name) {
      // tslint:disable-next-line:arrow-return-shorthand
      allChannelSegments = allChannelSegments.concat(
        dataStore.channelSegments
          .filter(segment => {
            // const segStartTime = toEpochSeconds(segment.startTime);
            // const segEndTime = toEpochSeconds(segment.endTime);
            let pushSegment = false;
            if (
              segment.channel.name.startsWith(channel.name) &&
              segment.type === ChannelSegmentType.FILTER
            ) {
              const filterToUse: FilterMapping = dataStore.filterMappings.find(
                fm => fm.filterId === filterId
              );
              if (segment.name.includes(filterToUse.filterName)) {
                pushSegment = true;
              }
            }
            return pushSegment;
            // && Math.min(segEndTime, endTime) - Math.max(segStartTime, startTime) >= 0;
          })
          .filter(isMockWaveformChannelSegment)
      );
    }
  });
  if (allChannelSegments.length > 0) {
    allChannelSegments = [allChannelSegments[0]];
    return getWaveformSegmentsByChannelSegments(startTime, endTime, allChannelSegments);
  }
  return [];
}

/**
 * Function used in backend only to find Channel Segments matching on the channel name to
 * channel segment name (At least start with channel name). This is due to the Filtered Channel
 * Segment channelId is a derived channel id and not the reference channel id.
 * @param channelIds List of reference channel ids
 * @param startTime Start of interval searching
 * @param endTime End of interval searching
 */
export function getBeamFilteredChannelSegments(
  startTime: number,
  endTime: number,
  filterId: string,
  csName: string
): OSDChannelSegment<OSDTimeSeries>[] {
  const splitValues = csName.split('.fkb');
  const chanName = splitValues[0];
  const filterToUse: FilterMapping = dataStore.filterMappings.find(fm => fm.filterId === filterId);
  // list to return
  let allChannelSegments: OSDChannelSegment<MockWaveform>[] = [];
  // Loop through channel find each CS that matching on name and time range
  dataStore.channelSegments.forEach(cs => {
    const csStartTime = toEpochSeconds(cs.startTime);
    const csEndTime = toEpochSeconds(cs.endTime);
    if (
      cs.name.startsWith(chanName) &&
      cs.name.includes(filterToUse.filterName) &&
      csStartTime <= startTime &&
      csEndTime >= endTime &&
      isMockWaveformChannelSegment(cs)
    ) {
      allChannelSegments.push(cs);
    }
  });
  if (allChannelSegments.length > 0) {
    allChannelSegments = [allChannelSegments[0]];
    return getWaveformSegmentsByChannelSegments(startTime, endTime, allChannelSegments);
  }
  return [];
}
/**
 * Handle cases where the data store has not been initialized.
 */
export function handleUninitializedDataStore() {
  // If the data store is uninitialized, throw an error
  if (!dataStore) {
    throw new Error('Mock backend channel segment processing data store has not been initialized');
  }
}

/**
 * channel segments to save
 * @param channelSegments segments to save
 */
export function saveWaveforms(channelSegments: OSDChannelSegment<OSDTimeSeries>[]): void {
  channelSegments.forEach(cs => {
    dataStore.channelSegments.push(cs);
  });
}

/**
 * fk channel segments to save
 * @param channelSegments segments to save
 */
export function saveFks(input: any): void {
  const channelSegments = input.channelSegments;
  channelSegments.forEach(cs => {
    dataStore.channelSegments.push(cs);
  });
}

/**
 * Function to return the channel segment ids related to reference channel.
 * These channel segments use a derived channel id so cannot be found via getChannelSegment
 * using an input with channel id and time range
 * @params input (reference channel id, time range)
 * @params timeRange
 * @returns ChannelSegments (for now only filtered Raw or Beam)
 */
export function getDerivedChannelSegments(
  csId: string,
  filterIds: string[]
): DerivedFilterChannelSegmentId[] {
  // Loop thru the the filterIds
  const input: ChannelSegmentByIdInput = {
    channelSegmentIds: [csId],
    withTimeseries: false
  };
  const channelSegmentOSDMap: OSDChannelSegment<OSDTimeSeries> = getChannelSegmentById(input);
  if (!channelSegmentOSDMap || Object.keys(channelSegmentOSDMap).length === 0) {
    logger.warn(`Failed to find Channel Segment for Id: ${csId}`);
    return [];
  }
  const fkCS: OSDChannelSegment<OSDTimeSeries> =
    channelSegmentOSDMap[Object.keys(channelSegmentOSDMap)[0]];
  const csList: DerivedFilterChannelSegmentId[] = [];
  filterIds.forEach(fid => {
    const startTime = toEpochSeconds(fkCS.startTime);
    const endTime = toEpochSeconds(fkCS.endTime);
    if (fkCS.type === ChannelSegmentType.FK_BEAM) {
      const res = getBeamFilteredChannelSegments(startTime, endTime, fid, fkCS.name);
      csList.push({ wfFilterId: fid, csId: res[0].id });
    } else if (fkCS.type === ChannelSegmentType.ACQUIRED) {
      const res = getAcquiredFilteredChannelSegments([fkCS.channel.name], startTime, endTime, fid);
      csList.push({ wfFilterId: fid, csId: res[0].id });
    }
  });
  return csList;
}
