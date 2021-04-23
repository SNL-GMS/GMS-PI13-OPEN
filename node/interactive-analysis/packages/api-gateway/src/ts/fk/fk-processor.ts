import { setDurationTime, toOSDTime } from '@gms/common-util';
import config from 'config';
import { produce } from 'immer';
import { DataPayload, UserContext } from '../cache/model';
import { ChannelSegmentProcessor } from '../channel-segment/channel-segment-processor';
import {
  ChannelSegment,
  isFkSpectraChannelSegment,
  OSDChannelSegment
} from '../channel-segment/model';
import { FrequencyBand } from '../common/model';
import { ConfigProcessor } from '../config/config-processor';
import { EventProcessor } from '../event/event-processor';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import {
  FeatureMeasurementTypeName,
  InstantMeasurementValue,
  SignalDetection,
  SignalDetectionHypothesis
} from '../signal-detection/model';
import { SignalDetectionProcessor } from '../signal-detection/signal-detection-processor';
import {
  createDataPayload,
  createEmptyDataPayload,
  replaceByIdOrAddToList
} from '../util/common-utils';
import {
  findArrivalTimeFeatureMeasurement,
  findAzimuthFeatureMeasurement,
  findSlownessFeatureMeasurement
} from '../util/feature-measurement-utils';
import {
  calculateStartTimeForFk,
  convertFkFromOSDtoAPI,
  isEmptyReturnFromFkService,
  kmToDegreesApproximate
} from '../util/fk-utils';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import * as fkMockBackend from './fk-mock-backend';
import { updateFMChannelSegmentDescriptor } from './fk-produce';
import {
  ComputeFkInput,
  FkConfiguration,
  FkFrequencyThumbnail,
  FkFrequencyThumbnailBySDId,
  FkInput,
  FkPowerSpectra,
  FkPowerSpectraOSD,
  MarkFksReviewedInput
} from './model';

/**
 * Fk Processor handles requests for all things fk
 */
export class FkProcessor {
  /** The singleton instance */
  private static instance: FkProcessor;

  /**
   * Returns the singleton instance of the waveform filter processor.
   */
  public static Instance(): FkProcessor {
    if (FkProcessor.instance === undefined) {
      FkProcessor.instance = new FkProcessor();
      FkProcessor.instance.initialize();
    }
    return FkProcessor.instance;
  }

  /** Local configuration settings for URLs, mocking, and other values. */
  private readonly settings: any;

  /**
   * HTTP client wrapper for communicating with the OSD - used for easy mocking of URLs
   * when using mock backend.
   */
  private readonly httpWrapper: HttpClientWrapper;

  private constructor() {
    this.settings = config.get('fk');
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Add to cache the new FKPowerSpectra
   * @param userContext the user context
   * @param newFkPowerSpectra the new fk power spectra
   */
  public updateFkDataById(
    userContext: UserContext,
    newFkPowerSpectra: ChannelSegment<FkPowerSpectra>
  ) {
    if (newFkPowerSpectra && isFkSpectraChannelSegment(newFkPowerSpectra)) {
      ChannelSegmentProcessor.Instance().addOrUpdateToCache(userContext, newFkPowerSpectra);
    }
  }

  /**
   * Update each FK power spectra channel segments with reviewed status
   * @returns List of Signal Detections successfully updated
   */
  public updateFkReviewedStatuses(
    userContext: UserContext,
    markFksReviewedInput: MarkFksReviewedInput,
    reviewed: boolean
  ): SignalDetection[] {
    // Go thru each input
    const updatedSignalDetections: SignalDetection[] = markFksReviewedInput.signalDetectionIds
      .map(sdId => {
        // Lookup the Azimuth feature measurement and get the fkDataId (channel segment id)
        const signalDetection = userContext.userCache.getSignalDetectionById(sdId);
        // Lookup the Channel Segment FK Power Spectra from the Az or Slow feature measurement
        const azimuthMeasurement = findAzimuthFeatureMeasurement(
          signalDetection.currentHypothesis.featureMeasurements
        );
        if (!azimuthMeasurement || !azimuthMeasurement.measuredChannelSegmentDescriptor) {
          logger.warn(`Failed to Update an Fk Azimuth Feature Measurement is not defined.`);
          return undefined;
        }
        const azimuthChannelSegment = ChannelSegmentProcessor.Instance().getInCacheChannelSegmentBySegmentDescriptor(
          userContext,
          azimuthMeasurement.measuredChannelSegmentDescriptor
        );
        if (azimuthChannelSegment && isFkSpectraChannelSegment(azimuthChannelSegment)) {
          azimuthChannelSegment.timeseries[0].reviewed = reviewed;
        } else {
          logger.warn(`Failed to Update Fk associated channel segment is not defined.`);
          return undefined;
        }
        return signalDetection;
      })
      .filter(sd => sd !== undefined);
    return updatedSignalDetections;
  }

  /**
   * Build a new FkData object, using the input parameters
   * @param input parameters
   * @returns SignalDetection that was modified
   */
  public async computeFk(userContext: UserContext, input: FkInput): Promise<DataPayload> {
    // Lookup the Azimuth feature measurement and get the fkDataId (channel segment id)
    const { signalDetection, event } = userContext.userCache.getSignalDetectionAndEventBySdId(
      input.signalDetectionId
    );
    const openEventId = userContext.userCache.getOpenEventId();

    if (!event && openEventId && signalDetection.hasConflict) {
      // tslint:disable-next-line: max-line-length
      throw new Error(
        'Cannot compute fk on a detection that is in conflict and not associated to the currently open event'
      );
    }

    const originalHypothesis = signalDetection.currentHypothesis;

    const {
      sd,
      hypothesis
    } = SignalDetectionProcessor.Instance().createOrUpdateSignalDetectionHypothesis(
      signalDetection,
      event
    );
    const updatedHypothesis = hypothesis;

    // Creates the input argument for computeFk arg false means sample rate won't be for the Thumbnail Fks
    const computeFkInput: ComputeFkInput = this.createComputeFkInput(
      userContext,
      input,
      updatedHypothesis,
      false
    );

    // Call the computeFk endpoint
    const fkChannelSegment = await this.callComputeFk(
      userContext,
      computeFkInput,
      input,
      updatedHypothesis,
      sd.stationName
    );

    if (fkChannelSegment) {
      // Update the Az and Slow FM channel segment id with new Channel Segment
      // Lookup the Channel Segment FK Power Spectra from the Az or Slow feature measurement
      const azimuthMeasurement = findAzimuthFeatureMeasurement(
        updatedHypothesis.featureMeasurements
      );
      if (!azimuthMeasurement || !azimuthMeasurement.measuredChannelSegmentDescriptor) {
        logger.warn(`Failed to Create or Update an Fk either the Arrival Time or " +
                "Azimuth Feature Measurements is not defined.`);
        return undefined;
      }

      const updatedAzimuthMeasurement = updateFMChannelSegmentDescriptor(
        azimuthMeasurement,
        fkChannelSegment
      );

      const slownessMeasurement = findSlownessFeatureMeasurement(
        updatedHypothesis.featureMeasurements
      );
      if (!slownessMeasurement || !slownessMeasurement.measuredChannelSegmentDescriptor) {
        logger.warn(`Failed to Create or Update an Fk " +
                "Azimuth Feature Measurements is not defined.`);
        return undefined;
      }

      const updatedSlownessMeasurement = updateFMChannelSegmentDescriptor(
        slownessMeasurement,
        fkChannelSegment
      );

      const updatedHypothesisWithFMs = produce<SignalDetectionHypothesis>(
        updatedHypothesis,
        draftState => {
          draftState.featureMeasurements = Object.seal([
            ...draftState.featureMeasurements.filter(
              fm =>
                fm.featureMeasurementType !==
                  FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH &&
                fm.featureMeasurementType !== FeatureMeasurementTypeName.SLOWNESS
            ),
            updatedAzimuthMeasurement,
            updatedSlownessMeasurement
          ]);
        }
      );

      const updatedDetection = produce<SignalDetection>(sd, draftState => {
        draftState.signalDetectionHypotheses = replaceByIdOrAddToList<SignalDetectionHypothesis>(
          draftState.signalDetectionHypotheses,
          updatedHypothesisWithFMs
        );
      });

      // Set the channel segment to be saved now that the channelSegmentId is associated to the FM
      fkChannelSegment.requiresSave = true;

      // Add changed/new channel segment back to cache. This needs to be done
      // before the compute beam call which uses the beam channel segment
      ChannelSegmentProcessor.Instance().addOrUpdateToCache(userContext, fkChannelSegment);

      const events = EventProcessor.Instance().updateSignalDetectionAssociation(
        userContext,
        event,
        updatedHypothesisWithFMs.id,
        originalHypothesis.id
      );

      // Before returning Fk compute a new Fk beam
      const updatedDetectionWithBeam = await ChannelSegmentProcessor.Instance().computeBeam(
        userContext,
        input,
        updatedDetection,
        updatedHypothesisWithFMs,
        computeFkInput.channelNames
      );

      return createDataPayload(events, [updatedDetectionWithBeam], []);
    }
    return createEmptyDataPayload();
  }

  /**
   * Compute a list of Fk Thumbnails for the new Fk
   *
   * @returns List of FkFrequencyThumbnails
   */
  public async computeFkFrequencyThumbnails(
    userContext: UserContext,
    input: FkInput
  ): Promise<FkFrequencyThumbnailBySDId> {
    const frequencyBands: FrequencyBand[] = ConfigProcessor.Instance().getConfigByKey(
      'defaultFrequencyBands'
    );

    const sd = userContext.userCache.getSignalDetectionById(input.signalDetectionId);
    // Creates the input argument for computeFk value true means the step size
    // is set to only compute one Spectrum for the thumbnail to display
    const computeFkInput: ComputeFkInput = this.createComputeFkInput(
      userContext,
      input,
      sd.currentHypothesis,
      true
    );
    const promises = frequencyBands.map(async fb => {
      computeFkInput.highFrequency = fb.maxFrequencyHz;
      computeFkInput.lowFrequency = fb.minFrequencyHz;

      const fkCS = await this.callComputeFk(
        userContext,
        computeFkInput,
        input,
        sd.currentHypothesis,
        sd.stationName
      );
      if (fkCS) {
        const thumbnail: FkFrequencyThumbnail = {
          frequencyBand: fb,
          fkSpectra: fkCS.timeseries[0]
        };
        return thumbnail;
      }
      return undefined;
    });
    // filter out any thumbnails that were returned as undefined
    const thumbnails = (await Promise.all(promises)).filter(tb => tb !== undefined);
    return {
      signalDetectionId: sd.id,
      fkFrequencyThumbnails: thumbnails
    };
  }

  /**
   * Helper function that builds the ComputeFk Input object. Shared by computeFk and computeFkFrequencyThumbnails
   * @param userContext user context for current user
   * @param input FkInput sent by UI
   * @param sdHyp signal detection hypothesis for fk
   * @param areThumbnails (Modifies sample rate so Thumbnails only returns one spectrum in fk)
   *
   * @returns fk input
   */
  private createComputeFkInput(
    userContext: UserContext,
    input: FkInput,
    sdHyp: SignalDetectionHypothesis,
    areThumbnails: boolean
  ): ComputeFkInput {
    const ONE_MINUTE = 60;
    const FOUR_MINUTES = 240;
    // Get arrivalTime segment to figure out length in secs
    // TODO: Add null pointer exception guards
    // Lookup the Azimuth feature measurement and get the fkDataId (channel segment id)
    const arrivalFM = findArrivalTimeFeatureMeasurement(sdHyp.featureMeasurements);
    const arrivalFMV = (arrivalFM.measurementValue as InstantMeasurementValue).value;
    const arrivalSegment = ChannelSegmentProcessor.Instance().getInCacheChannelSegmentBySegmentDescriptor(
      userContext,
      arrivalFM.measuredChannelSegmentDescriptor
    );

    // TODO we are only allowing SHZ contrib channels to be sent to FK service. This seems wrong
    // TODO add BHZ to filter
    // Either all SHZ or BHZ confirm what the heck do the channel names to pass in
    // TODO: contrib channel should only selectable and displayed in UI Azimuth...
    const shz = input.configuration.contributingChannelsConfiguration.filter(
      ccc => ccc.name.includes('SHZ') && ccc.enabled
    );
    const shzIds = shz.map(cet => cet.id);
    // TODO: Replace with real conversion
    // tslint:disable-next-line: no-magic-numbers
    const maximumSlownessInSPerKm = kmToDegreesApproximate(input.configuration.maximumSlowness);
    // Set start and end time based on arrival segment if it exists,
    // else default to one minute before and 4 minutes after arrival time
    const startTime = arrivalSegment ? arrivalSegment.startTime : arrivalFMV - ONE_MINUTE;
    const endTime = arrivalSegment ? arrivalSegment.endTime : arrivalFMV + FOUR_MINUTES;
    // For thumbnail with sample count of 1 just use arrival start time
    const offsetStartTime = areThumbnails
      ? startTime
      : calculateStartTimeForFk(
          startTime,
          arrivalFMV,
          input.windowParams.leadSeconds,
          input.windowParams.stepSize
        );
    // const offsetStartTime = arrivalFMV - input.windowParams.leadSeconds;
    // Sample rate inverse of step size. If thumbnail set rate so we only get one spectrum back from service
    const sampleRate = areThumbnails
      ? 1 / (endTime - offsetStartTime)
      : 1 / input.windowParams.stepSize;

    // const endTime = arrivalSegment.startTime + (arrivalSegment.timeseries[0].sampleCount / sampleRate);
    // Compute sample count if thumbnail only want one spectrum
    const timeSpanAvailable = endTime - startTime;
    const sampleCount = areThumbnails
      ? 1
      : Math.floor(timeSpanAvailable / input.windowParams.stepSize);
    return {
      startTime: toOSDTime(offsetStartTime),
      sampleRate,
      sampleCount,
      channelNames: shzIds,
      windowLead: setDurationTime(input.windowParams.leadSeconds),
      windowLength: setDurationTime(input.windowParams.lengthSeconds),
      lowFrequency: input.frequencyBand.minFrequencyHz,
      highFrequency: input.frequencyBand.maxFrequencyHz,
      useChannelVerticalOffset: input.configuration.useChannelVerticalOffset,
      phaseType: input.phase,
      normalizeWaveforms: input.configuration.normalizeWaveforms,
      slowCountX: Math.floor(input.configuration.numberOfPoints),
      slowCountY: Math.floor(input.configuration.numberOfPoints),
      slowStartX: -maximumSlownessInSPerKm,
      slowStartY: -maximumSlownessInSPerKm,
      slowDeltaX: (maximumSlownessInSPerKm * 2) / input.configuration.numberOfPoints,
      slowDeltaY: (maximumSlownessInSPerKm * 2) / input.configuration.numberOfPoints
    };
  }

  /**
   * Call compute Fk endpoint service with Compute Fk Input
   * @param userContext user context for current user
   * @param inputToService created fk input
   * @param inputFromClient fk input from ui
   * @param sdHyp signal detection hypothesis for sd for fk
   * @param stationId station id for the sd's station
   *
   * @returns fk power spectra channel segment
   */
  private async callComputeFk(
    userContext: UserContext,
    inputToService: ComputeFkInput,
    inputFromClient: FkInput,
    sdHyp: SignalDetectionHypothesis,
    stationId: string
  ): Promise<ChannelSegment<FkPowerSpectra>> {
    const requestConfig = this.settings.backend.services.computeFk.requestConfig;
    const query = {
      ...inputToService
    };

    // tslint:disable-next-line:max-line-length
    logger.debug(
      `Sending service request: ${JSON.stringify(
        requestConfig,
        undefined,
        2
      )} query: ${JSON.stringify(query, undefined, 2)}`
    );
    performanceLogger.performance(
      'fkChannelSegment',
      'requestedFromService',
      inputToService.startTime
    );
    const fkOSD: HttpResponse<OSDChannelSegment<
      FkPowerSpectraOSD
    >[]> = await this.httpWrapper.request<OSDChannelSegment<FkPowerSpectraOSD>[]>(
      requestConfig,
      query
    );
    performanceLogger.performance(
      'fkChannelSegment',
      'returnedFromService',
      inputToService.startTime
    );
    if (isEmptyReturnFromFkService(fkOSD.data)) {
      logger.error(
        `Compute FK: Failed to compute FK for signal detection id: ${sdHyp.parentSignalDetectionId}`
      );
      return;
    }
    const fkChannelSegment = convertFkFromOSDtoAPI(userContext, fkOSD.data[0], sdHyp, stationId);

    fkChannelSegment.timeseries[0].reviewed = false;
    fkChannelSegment.timeseries[0].lowFrequency = inputFromClient.frequencyBand.minFrequencyHz;
    fkChannelSegment.timeseries[0].highFrequency = inputFromClient.frequencyBand.maxFrequencyHz;
    fkChannelSegment.timeseries[0].windowLead = inputFromClient.windowParams.leadSeconds;
    fkChannelSegment.timeseries[0].windowLength = inputFromClient.windowParams.lengthSeconds;
    fkChannelSegment.timeseries[0].stepSize = inputFromClient.windowParams.stepSize;

    fkChannelSegment.timeseries[0].slowCountX = inputFromClient.configuration.numberOfPoints;
    fkChannelSegment.timeseries[0].slowCountY = inputFromClient.configuration.numberOfPoints;
    const shz = inputFromClient.configuration.contributingChannelsConfiguration.filter(
      ccc => ccc.name.includes('SHZ') && ccc.enabled
    );
    const shzIds = shz.map(cet => cet.id);

    const newConfig: FkConfiguration = {
      contributingChannelsConfiguration: inputFromClient.configuration.contributingChannelsConfiguration.map(
        ccc => ({
          id: ccc.id,
          name: ccc.name,
          enabled: ccc.enabled && shzIds.indexOf(ccc.id) > -1
        })
      ),
      leadFkSpectrumSeconds: fkChannelSegment.timeseries[0].windowLead,
      maximumSlowness: inputFromClient.configuration.maximumSlowness,
      mediumVelocity: inputFromClient.configuration.mediumVelocity,
      normalizeWaveforms: inputFromClient.configuration.normalizeWaveforms,
      numberOfPoints: inputFromClient.configuration.numberOfPoints,
      useChannelVerticalOffset: inputFromClient.configuration.useChannelVerticalOffset
    };
    fkChannelSegment.timeseries[0].configuration = newConfig;
    return fkChannelSegment;
  }

  /**
   * Initialize the channel segment processor, setting up the mock backend if enabled in configuration.
   */
  private initialize(): void {
    logger.info(
      'Initializing the Fk processor - Mock Enable: %s',
      this.settings.backend.mock.enable
    );

    const mockWrapper = this.httpWrapper.createHttpMockWrapper();

    // If service mocking is enabled, initialize the mock backend
    const backendConfig = config.get('fk.backend');

    if (backendConfig.mock.enable) {
      fkMockBackend.initialize(mockWrapper);
    }
  }
}
