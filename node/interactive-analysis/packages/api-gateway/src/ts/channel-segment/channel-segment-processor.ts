import { toOSDTime } from '@gms/common-util';
import config from 'config';
import { produce } from 'immer';
import { CacheProcessor } from '../cache/cache-processor';
import { UserContext } from '../cache/model';
import { ConfigProcessor } from '../config/config-processor';
import { FkInput } from '../fk/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import {
  FeatureMeasurement,
  MeasuredChannelSegmentDescriptor,
  SignalDetection,
  SignalDetectionHypothesis
} from '../signal-detection/model';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import {
  convertChannelSegmentFromAPIToOSD,
  convertChannelSegmentFromOSDToAPI,
  truncateChannelSegmentTimeseries
} from '../util/channel-segment-utils';
import { replaceByIdOrAddToList } from '../util/common-utils';
import {
  findArrivalTimeFeatureMeasurement,
  findArrivalTimeFeatureMeasurementValue,
  findAzimuthFeatureMeasurement,
  findPhaseFeatureMeasurementValue
} from '../util/feature-measurement-utils';
import { convertFkFromOSDtoAPI } from '../util/fk-utils';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import { getChannelSegmentDescriptor } from '../util/signal-detection-utils';
import { Waveform } from '../waveform/model';
import * as waveformMockBackend from '../waveform/waveform-mock-backend';
import * as channelSegmentMockBackend from './channel-segment-mock-backend';
import {
  saveFkChannelSegmentsToService,
  saveWaveformChannelSegmentsToService
} from './channel-segment-services-client';
import {
  BeamFormingInput,
  ChannelSegment,
  ChannelSegmentInput,
  ChannelSegmentType,
  isFkSpectraChannelSegment,
  isFkSpectraChannelSegmentOSD,
  isWaveformChannelSegment,
  OSDChannelSegment,
  OSDTimeSeries,
  TimeSeries,
  TimeSeriesType
} from './model';

/**
 * Interface for reporting the found and missed channel segments in a cache lookup. The
 * interface is used in multiple contexts where misses could be channel ids or channel segment ids.
 */
interface CacheResults {
  hits: ChannelSegment<TimeSeries>[];
  misses: string[];
}

/** OSDChannelSegment response from the service */
interface OSDChannelSegmentServiceResponse {
  [key: string]: OSDChannelSegment<OSDTimeSeries>;
}

/**
 * The channel segment processor which fetches channel segments for feature measurements
 * associated to signal detections which, use the channel segments ID as a claim check
 * Also returns raw channel segments associated to a reference channel for a time interval.
 */
export class ChannelSegmentProcessor {
  /** The singleton instance */
  private static instance: ChannelSegmentProcessor;

  /**
   * Returns the singleton instance of the waveform filter processor.
   */
  public static Instance(): ChannelSegmentProcessor {
    if (ChannelSegmentProcessor.instance === undefined) {
      ChannelSegmentProcessor.instance = new ChannelSegmentProcessor();
      ChannelSegmentProcessor.instance.initialize();
    }
    return ChannelSegmentProcessor.instance;
  }

  /** Local configuration settings for URLs, mocking, and other values. */
  private readonly settings: any;

  /**
   * HTTP client wrapper for communicating with the OSD - used for easy mocking of URLs
   * when using mock backend.
   */
  private readonly httpWrapper: HttpClientWrapper;

  private constructor() {
    this.settings = config.get('channelSegment');
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Retrieve the channel segment data matching the channel segment channel name start and
   * end time.
   * @param userContext the user context
   * @param startTime the start time
   * @param endTime the end time
   * @param channelName the channel name
   * @param sd optional signal detection used to populate default fk
   * configuration correctly if getting az slow channel segment
   */
  public async getChannelSegmentUsingSegmentDescriptor(
    userContext: UserContext,
    fmCSDescriptor: MeasuredChannelSegmentDescriptor,
    channelName: string,
    sd?: SignalDetection
  ): Promise<ChannelSegment<TimeSeries>> | undefined {
    if (!fmCSDescriptor || !channelName) {
      return undefined;
    }
    return this.getChannelSegment(
      userContext,
      fmCSDescriptor.measuredChannelSegmentStartTime,
      fmCSDescriptor.measuredChannelSegmentEndTime,
      fmCSDescriptor.channelName,
      sd
    );
  }
  /**
   * Retrieve the channel segment data matching the channel segment channel name start and
   * end time.
   * @param userContext the user context
   * @param startTime the start time
   * @param endTime the end time
   * @param channelName the channel name
   * @param sd optional signal detection,
   *  used to populate default fk configuration correctly if getting az slow channel segment
   */
  public async getChannelSegment(
    userContext: UserContext,
    startTime: number,
    endTime: number,
    channelName: string,
    sd?: SignalDetection
  ): Promise<ChannelSegment<TimeSeries>> | undefined {
    if (!channelName || !startTime || !endTime) {
      return undefined;
    }

    // Query for channel segment based on time and channel name
    const channelSegments: ChannelSegment<TimeSeries>[] = await this.getChannelSegmentsByChannels(
      userContext,
      startTime,
      endTime,
      [channelName],
      sd
    );
    if (!channelSegments || channelSegments.length === 0) {
      logger.warn(`Could not retrieve channel segment for channel name ${channelName}`);
      return undefined;
    }

    if (channelSegments.length > 1) {
      logger.warn(
        `Query for channel name ${channelName} ` +
          `returned multiple channel segments returning first entry`
      );
    }
    return channelSegments[0];
  }

  /**
   * Retrieve all channel segments for each given channel id that are within the requested time range.
   * If the optional type of the channel segments is passed in the cache will not be checked
   * for other types, so leave it blank if uncertain.
   *
   * @param timeRange the time range the channel segments should be in
   * @param channelNames list of channel ids to request channel segments for
   * @param type optional type of the channel segment for quicker cache lookup
   */
  public async getChannelSegmentsByChannels(
    userContext: UserContext,
    startTime: number,
    endTime: number,
    channelNames: string[],
    sd?: SignalDetection
  ): Promise<ChannelSegment<TimeSeries>[]> {
    // Retrieve the request configuration for the service call
    const requestConfig = this.settings.backend.services.channelSegmentsByTimeRange.requestConfig;

    // Check cache for channel segments
    const cacheResults: CacheResults = this.checkCacheByChannels(
      userContext,
      startTime,
      endTime,
      channelNames
    );

    // tslint:disable-next-line:max-line-length
    logger.debug(
      `Channel segment cache results(${channelNames.length}): hits=${cacheResults.hits.length} misses=${cacheResults.misses.length}`
    );
    if (cacheResults.misses.length === 0) {
      return cacheResults.hits;
    }

    // Cached segments not all fulfilled, query OSD
    const query: ChannelSegmentInput = {
      channelNames: cacheResults.misses,
      startTime: toOSDTime(startTime),
      endTime: toOSDTime(endTime),
      withWaveforms: true
    };
    // tslint:disable-next-line:max-line-length
    logger.debug(
      `Sending service request: ${JSON.stringify(
        requestConfig,
        undefined,
        2
      )} query: ${JSON.stringify(query, undefined, 2)}`
    );
    return this.httpWrapper
      .request<OSDChannelSegmentServiceResponse>(requestConfig, query)
      .then(async (response: HttpResponse<OSDChannelSegmentServiceResponse>) => {
        // Cache filters after request
        const channelSegmentsToReturn: ChannelSegment<TimeSeries>[] = cacheResults.hits;
        if (response && response.data) {
          // tslint:disable-next-line: no-for-in
          for (const key in response.data) {
            if (!response.data.hasOwnProperty(key)) {
              continue;
            }
            // Convert OSD channel segment to API
            const channelSegmentOSD: OSDChannelSegment<OSDTimeSeries> = response.data[key];

            let channelSegment: ChannelSegment<TimeSeries> | undefined;
            if (isFkSpectraChannelSegmentOSD(channelSegmentOSD)) {
              // Raw from osd probably shouldn't include a real fk configuration...
              if (sd) {
                channelSegment = convertFkFromOSDtoAPI(
                  userContext,
                  channelSegmentOSD,
                  sd.currentHypothesis,
                  sd.stationName
                );
              } else {
                logger.error(
                  `Must include signal detections part of call for ` +
                    `Fk Spectra Channel segment conversion from OSD.`
                );
              }
            } else {
              channelSegment = convertChannelSegmentFromOSDToAPI(userContext, channelSegmentOSD);
              // Lookup the calibration factor for the channel
              // For RAW (ACQUIRED) waveforms lookup the calibration factor
              // for filtered waveforms or beams it has already been applied
              let channelCalibrationFactor = 0.02;
              if (
                channelSegment.type === ChannelSegmentType.ACQUIRED ||
                channelSegment.type === ChannelSegmentType.RAW
              ) {
                channelCalibrationFactor = waveformMockBackend.getCalibrationFactor(
                  channelSegment.channel.name,
                  channelSegment.startTime
                );
              }

              // Sometimes the OSD sends back undefined or NaN, so we hack fix
              channelSegment.timeseries = channelSegment.timeseries.map((timeseries: Waveform) => ({
                // Dangerous cast, might explode
                ...timeseries,
                values: timeseries.values.map(value =>
                  value !== undefined ? value * channelCalibrationFactor : 0
                )
              }));
            }
            // Add to processor cache
            this.addOrUpdateToCache(userContext, channelSegment);
            // Add it to list to return, since the channel name indicates
            // the type of Channel Segment we are looking for we don't have to filter
            channelSegmentsToReturn.push(channelSegment);
          }
        }
        return channelSegmentsToReturn;
      })
      .catch(error => {
        logger.error(`Failed to request/fetch channel segments: ${error}`);
        return [];
      });
  }

  /**
   * Adds or updates the cache for the given channel segment.
   *
   * @param channelSegment the channel segments to add to the cache
   */
  public addOrUpdateToCache(userContext: UserContext, channelSegment: ChannelSegment<TimeSeries>) {
    if (channelSegment && channelSegment.id) {
      CacheProcessor.Instance().setChannelSegment(channelSegment);
    }
  }

  /**
   * Checks the cache for a channel segment matching the channel segment id.
   *
   * @param  the channel segment id to check cache for
   */
  public getInCacheChannelSegmentBySegmentDescriptor(
    userContext: UserContext,
    fmCSDescriptor: MeasuredChannelSegmentDescriptor
  ): ChannelSegment<TimeSeries> | undefined {
    const results = this.checkCacheByChannels(
      userContext,
      fmCSDescriptor.measuredChannelSegmentStartTime,
      fmCSDescriptor.measuredChannelSegmentEndTime,
      [fmCSDescriptor.channelName]
    );
    if (results && results.hits && results.hits.length > 0) {
      if (results.hits.length > 1) {
        logger.warn(
          `Query for channel name ${fmCSDescriptor.channelName} ` +
            `returned multiple channel segments using segment descriptor returning first entry`
        );
      }
      return results.hits[0];
    }
    return undefined;
  }

  /**
   * Checks the cache for a channel segment matching the channel segment id.
   *
   * @param channelSegmentId the channel segment id to check cache for
   */
  public getInCacheChannelSegmentById(
    userContext: UserContext,
    channelSegmentId: string
  ): ChannelSegment<TimeSeries> | undefined {
    return CacheProcessor.Instance()
      .getChannelSegments()
      .get(channelSegmentId);
  }

  /**
   * Computes new beam segment
   * @param userContext user context object
   * @param input fk input object
   * @param signalDetection signal detection to add beam to
   * @param sdHyp signal detection hypothesis to user
   * @param contributingChannelNames channels used to compute Fk
   *
   * @returns DataPayload populated with updated SignalDetection
   */
  public async computeBeam(
    userContext: UserContext,
    input: FkInput,
    signalDetection: SignalDetection,
    sdHyp: SignalDetectionHypothesis,
    contributingChannelNames: string[]
  ): Promise<SignalDetection> {
    // Push the SD that will be updated with new beam
    if (!signalDetection) {
      logger.warn(
        `Failed to find Signal Detection: ${input.signalDetectionId} no new beam could be calculated!`
      );
      return signalDetection;
    }
    const requestConfig = this.settings.backend.services.computeBeam.requestConfig;

    // Call method to build the Beam Input parameters
    const beamInput = await this.buildBeamInput(
      userContext,
      signalDetection,
      sdHyp,
      contributingChannelNames
    );

    // Check beam input was create
    if (!beamInput || !signalDetection) {
      return signalDetection;
    }
    // tslint:disable-next-line: max-line-length
    logger.debug(
      `Sending service request: ${JSON.stringify(
        requestConfig,
        undefined,
        2
      )} query: ${JSON.stringify(beamInput, undefined, 2)}`
    );
    performanceLogger.performance(
      'beamChannelSegment',
      'requestedFromService',
      beamInput.waveforms[0].channel.name
    );
    const response: HttpResponse<OSDChannelSegment<
      OSDTimeSeries
    >[]> = await this.httpWrapper.request<OSDChannelSegment<OSDTimeSeries>[]>(
      requestConfig,
      beamInput
    );
    performanceLogger.performance(
      'beamChannelSegment',
      'returnedFromService',
      beamInput.waveforms[0].channel.name
    );
    // See if we get a legit response
    if (response && response.data && response.data.length > 0) {
      // Convert and push the returned channel segment into the cache
      const newBeamChannelSegment = convertChannelSegmentFromOSDToAPI(
        userContext,
        response.data[0]
      );
      // Add it to the Channel Segment cache
      this.addOrUpdateToCache(userContext, newBeamChannelSegment);

      // create a the new arrival time feature measurement
      const arrivalTimeFM = produce<FeatureMeasurement>(
        findArrivalTimeFeatureMeasurement(sdHyp.featureMeasurements),
        draftState => {
          // Replace the arrival time channel segment with new one
          draftState.measuredChannelSegmentDescriptor = getChannelSegmentDescriptor(
            newBeamChannelSegment
          );
        }
      );

      // updated the signal detection hypothesis
      const updatedSignalDetectionHyp = produce<SignalDetectionHypothesis>(sdHyp, draftState => {
        draftState.featureMeasurements = [
          ...draftState.featureMeasurements.filter(
            fm => fm.featureMeasurementType !== arrivalTimeFM.featureMeasurementType
          ),
          arrivalTimeFM
        ];
      });

      // Update the signal detection and save
      const updatedSignalDetection = produce<SignalDetection>(signalDetection, draftState => {
        draftState.signalDetectionHypotheses = replaceByIdOrAddToList<SignalDetectionHypothesis>(
          draftState.signalDetectionHypotheses,
          updatedSignalDetectionHyp
        );
      });

      // Set the channel segment to be saved now that the channelSegmentId is associated to the FM
      newBeamChannelSegment.requiresSave = true;

      // ! TODO why is this call being made, we are not using the array of filter beams being returned?
      // Recalculate the filtered beams using the newly computed beam
      // await signalDetectionProcessor.populateFilterBeams(userContext, arrivalTimeFM);

      return updatedSignalDetection;
    }
    logger.warn(`Compute Beam service call returned undefined result. No new beam was computed!`);
    return signalDetection;
  }

  /**
   * Build the Beam Input parameters for computeBeam streaming call
   * @param userContext user context for the current user
   * @param sd SignalDetection
   * @param sdHyp signal detection hypothesis to use
   * @param contributingChannelNames channels used to compute Fk
   *
   * @returns BeamFormInput populated based on the Signal Detection
   */
  public async buildBeamInput(
    userContext: UserContext,
    sd: SignalDetection,
    sdHyp: SignalDetectionHypothesis,
    contributingChannelNames: string[]
  ): Promise<BeamFormingInput> {
    // TODO: Get rid of ConfigUI beam input when know how to populate
    // Get the default configured BeamFormingInput from ConfigUi.json
    // Get the Locate Event Parameters used by locateEvent call (Event Definition Type)
    const beamInput: BeamFormingInput = ConfigProcessor.Instance().getConfigByKey('computeBeamArg');
    const beamDef = beamInput.beamDefinition;

    // Get Peak Spectrum to get Az/Slow values
    // TODO: Maybe should look at both Az CS and Slow CS for FkPowerSpectra Channel Segment
    const arrivalFMV = findArrivalTimeFeatureMeasurementValue(sdHyp.featureMeasurements);
    const azFM = findAzimuthFeatureMeasurement(sdHyp.featureMeasurements);
    const azFkSpectraCS = await this.getChannelSegmentUsingSegmentDescriptor(
      userContext,
      azFM.measuredChannelSegmentDescriptor,
      azFM.channel.name,
      sd
    );

    const azFkSpectra =
      azFkSpectraCS &&
      isFkSpectraChannelSegment(azFkSpectraCS) &&
      azFkSpectraCS.timeseries &&
      azFkSpectraCS.timeseries.length > 0
        ? azFkSpectraCS.timeseries[0]
        : undefined;
    if (azFkSpectra) {
      beamDef.slowness = 1 / beamDef.slowness;
    }
    const phaseFMV = findPhaseFeatureMeasurementValue(sdHyp.featureMeasurements);
    if (phaseFMV && phaseFMV.phase) beamDef.phaseType = phaseFMV.phase;

    // Waveforms for each channel
    let waveforms: ChannelSegment<TimeSeries>[] = [];
    // Build the relative positions of the channels
    if (contributingChannelNames && contributingChannelNames.length > 0) {
      // Find first channel contributing channel list for sample rate
      const firstChannel = ProcessingStationProcessor.Instance().getChannelByName(
        contributingChannelNames[0]
      );

      // Set the Waveform Sample Rate based on first channel
      // TODO: figure out if they are all the same as the first and if this is right
      beamDef.nominalWaveformSampleRate = firstChannel.nominalSampleRateHz;

      // Using the arrival time, set the start and end time of the waveform to retrieve
      const leadBeamSeconds: number = ConfigProcessor.Instance().getConfigByKey('leadBeamSeconds');
      const lagBeamSeconds: number = ConfigProcessor.Instance().getConfigByKey('lagBeamSeconds');
      const startTime = arrivalFMV.value - leadBeamSeconds;
      const endTime = arrivalFMV.value + lagBeamSeconds;

      // Also populate waveforms for all the Station channels
      waveforms = await this.getChannelSegmentsByChannels(
        userContext,
        startTime,
        endTime,
        contributingChannelNames
      );

      // Truncate them down to only be in the amount requested
      beamInput.waveforms = waveforms.map(wf =>
        convertChannelSegmentFromAPIToOSD(truncateChannelSegmentTimeseries(wf, startTime, endTime))
      );
      // Check if waveform were found as part of the input to compute beam
      if (!waveforms || waveforms.length === 0) {
        logger.warn('No waveforms were found to compute beam a new beam will not be computed.');
        return undefined;
      }
      return beamInput;
    }
    logger.warn(
      `No channels were found for station ${sd.stationName} a new beam will not be computed.`
    );
    return undefined;
  }

  /**
   * Saves the provided channel segments.
   *
   * @param userContext the user context
   * @param channelSegments the channel segments to save
   */
  public async saveChannelSegments(
    userContext: UserContext,
    channelSegments: ChannelSegment<TimeSeries>[]
  ) {
    await this.saveFkChannelSegments(userContext, channelSegments);
    await this.saveWaveformChannelSegments(userContext, channelSegments);
  }

  /**
   * Saves the provided fk channel segments.
   *
   * NOTE: Will ONLY save channel segments of type FK_SPECTRA
   *
   * @param userContext the user context
   * @param channelSegments the channel segments to save
   */
  public async saveFkChannelSegments(
    userContext: UserContext,
    channelSegments: ChannelSegment<TimeSeries>[]
  ): Promise<{ channelSegments: ChannelSegment<TimeSeries>[]; response: HttpResponse<string[]> }> {
    // determined the modified channel segments to save, no reason to save a non-modified channel segments
    const channelSegmentsToSave = channelSegments.filter(
      cs => cs.timeseriesType === TimeSeriesType.FK_SPECTRA
    );
    if (channelSegmentsToSave.length > 0) {
      // Call OSD endpoint to save events
      logger.info(`Saving ${channelSegmentsToSave.length} fk channel segments`);
      const saveFkRequestConfig = this.settings.backend.services.saveFks.requestConfig;
      const response = await saveFkChannelSegmentsToService(
        userContext,
        channelSegmentsToSave,
        this.httpWrapper,
        saveFkRequestConfig
      );

      // determine which signal detections were saved successfully based on the response
      const httpOkay = 200;
      if (response.status !== httpOkay) {
        logger.warn(`Failed to save FK Channel Segments.`);
      }
    }
    return { channelSegments: [], response: undefined /* no response made */ };
  }

  /**
   * Saves the provided wave channel segments.
   *
   * NOTE: Will ONLY save channel segments of type WAVEFORM
   *
   * @param userContext the user context
   * @param channelSegments the channel segments to save
   */
  public async saveWaveformChannelSegments(
    userContext: UserContext,
    channelSegments: ChannelSegment<TimeSeries>[]
  ) {
    // determined the modified channel segments to save, no reason to save a non-modified channel segments
    const channelSegmentsToSave = channelSegments.filter(
      cs => cs.timeseriesType === TimeSeriesType.WAVEFORM
    );

    if (channelSegmentsToSave.length > 0) {
      // Call OSD endpoint to save events
      logger.info(`Saving ${channelSegmentsToSave.length} waveform channel segments`);
      const saveFkRequestConfig = this.settings.backend.services.saveWaveforms.requestConfig;
      await saveWaveformChannelSegmentsToService(
        userContext,
        channelSegmentsToSave,
        this.httpWrapper,
        saveFkRequestConfig
      );
    }
  }

  /**
   * Initialize the channel segment processor, setting up the mock backend if enabled in configuration.
   */
  private initialize(): void {
    logger.info(
      'Initializing the ChannelSegment processor - Mock Enable: %s',
      this.settings.backend.mock.enable
    );

    const mockWrapper = this.httpWrapper.createHttpMockWrapper();

    // If service mocking is enabled, initialize the mock backend
    const backendConfig = config.get('channelSegment.backend');

    if (backendConfig.mock.enable) {
      channelSegmentMockBackend.initialize(mockWrapper);
    }

    // TODO: waveformMockBackend should be removed when calibration lookup
    // comes from the responses and not mock backend
    waveformMockBackend.initialize(mockWrapper);
  }

  /**
   * Checks the cache for channel segments that have the given channel id and
   * the requested time range in their segment.
   * See isHit(...) for definition of a hit.
   *
   * @param timeRange the time range the channel segments should be in
   * @param channelNames list of channel ids to request channel segments for
   */
  private checkCacheByChannels(
    userContext: UserContext,
    startTime: number,
    endTime: number,
    channelNames: string[]
  ): CacheResults {
    const channelSegments: ChannelSegment<TimeSeries>[] = [];
    CacheProcessor.Instance()
      .getChannelSegments()
      .forEach(channelSegment => {
        if (this.isHit(channelSegment, startTime, endTime, channelNames)) {
          channelSegments.push(channelSegment);
        }
      });

    const cachedWindowedChannelSegments = channelSegments.map(
      (channelSegment: ChannelSegment<TimeSeries>) => {
        if (isWaveformChannelSegment(channelSegment)) {
          return truncateChannelSegmentTimeseries(channelSegment, startTime, endTime);
        }
        return channelSegment;
      }
    );

    // Compute misses as channels that have no matching channel segments
    return {
      hits: cachedWindowedChannelSegments,
      misses: channelNames.filter((channelName: string) =>
        cachedWindowedChannelSegments.every(
          channelSegment => channelSegment.channel.name !== channelName
        )
      )
    };
  }

  /**
   * Checks if the channel segment is a hit for the requested channel ids and time range.
   * A hit for time range is calculated differently for different ChannelSegmentType's.
   * For a beam type (FK_BEAM, DETECTION_BEAM) the beam is considered a hit if any part
   * of the beam is _within_ the time range. For any other type (RAW, ACQUIRED, FILTER)
   * the waveform is considered a hit if the requested time frame is completely within a
   * single cached waveform.
   *
   * Beams that are partially in the time range are windowed to be completely in the
   * time range. The other type may need to be split up when further
   * ChannelSegmentType's are supported.
   *
   * Any hit, regardless of type, means the cache sufficed for the requested channel which
   * means that the OSD _will not_ be called for that channel.
   *
   * Caching strategy does not support partial misses or stitching of any form for the
   * other type (RAW, ACQUIRED, FILTER) - this may be implemented in the future.
   *
   * @param channelSegment channel segment to check
   * @param channelIds list of channel ids to check channel segment is in
   * @param timeRange the time range the channel segments should be in
   */
  private isHit(
    channelSegment: ChannelSegment<TimeSeries>,
    startTime: number,
    endTime: number,
    channelNames: string[]
  ): boolean {
    return (
      startTime >= channelSegment.startTime &&
      endTime <= channelSegment.endTime &&
      channelNames.findIndex(csName => csName === channelSegment.channel.name) >= 0
    );
  }
}
