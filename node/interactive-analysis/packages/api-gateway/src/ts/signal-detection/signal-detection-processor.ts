import { uuid4 } from '@gms/common-util';
import config from 'config';
import { produce } from 'immer';
import cloneDeep from 'lodash/cloneDeep';
import filter from 'lodash/filter';
import flatMap from 'lodash/flatMap';
import { CacheProcessor } from '../cache/cache-processor';
import { UserActionDescription, UserContext } from '../cache/model';
import { ChannelSegmentProcessor } from '../channel-segment/channel-segment-processor';
import { isWaveformChannelSegment } from '../channel-segment/model';
import { TimeRange } from '../common/model';
import { ConfigProcessor } from '../config/config-processor';
import { Event } from '../event/model-and-schema/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import { ProcessingStation } from '../station/processing-station/model';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { replaceByIdOrAddToList } from '../util/common-utils';
import { convertFeatureMeasurementFromOSD } from '../util/feature-measurement-convert-utils';
import {
  findArrivalTimeFeatureMeasurementValue,
  findAzimuthFeatureMeasurement,
  findPhaseFeatureMeasurementValue
} from '../util/feature-measurement-utils';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import {
  createFeatureMeasurement,
  createFmsFromUserInput,
  filterSdsByStationAndTime,
  getSignalDetectionsAssociatedToEvent
} from '../util/signal-detection-utils';
import { WaveformFilterProcessor } from '../waveform-filter/waveform-filter-processor';
import * as model from './model';
import {
  FeatureMeasurementOSD,
  SignalDetectionHypothesisOSD,
  SignalDetectionOSD
} from './model-osd';
import * as signalDetectionMockBackend from './signal-detection-mock-backend';
import {
  loadSignalDetectionsFromService,
  saveSignalDetectionsToService
} from './signal-detection-service-client';

logger.info('Initializing the Signal Detection Processor');

/** Handles sd data */
export class SignalDetectionProcessor {
  /** The singleton instance */
  private static instance: SignalDetectionProcessor;

  /** the settings */
  private readonly settings: any;

  /** the http wrapper */
  private readonly httpWrapper: HttpClientWrapper;

  private constructor() {
    // Load configuration settings
    this.settings = config.get('signalDetection');

    // Initialize an http client
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): SignalDetectionProcessor {
    if (SignalDetectionProcessor.instance === undefined) {
      SignalDetectionProcessor.instance = new SignalDetectionProcessor();
      SignalDetectionProcessor.instance.initialize();
    }
    return SignalDetectionProcessor.instance;
  }

  /**
   * Retrieve the signal detection hypothesis for the provided ID.
   * @param signalDetections signal detections to search
   * @param hypothesisId the ID of the signal detection hypothesis to retrieve
   *
   * @returns hypothesis that matches input id
   */
  public getSignalDetectionHypothesisById(
    signalDetections: model.SignalDetection[],
    hypothesisId: string
  ): model.SignalDetectionHypothesis {
    const hyps = this.getSignalDetectionHypothesisListById(signalDetections, [hypothesisId]);

    if (hyps && hyps.length > 0) {
      return hyps[0];
    }
    return undefined;
  }

  /**
   * Retrieve the signal detection hypotheses matching the provided IDs.
   * @param signalDetections signal detections to search
   * @param hypothesisIds the IDs of the signal detection hypotheses to retrieve
   *
   * @returns collection of signal detection hypotheses matching the input ids
   */
  public getSignalDetectionHypothesisListById(
    signalDetections: model.SignalDetection[],
    hypothesisIds: string[]
  ): model.SignalDetectionHypothesis[] {
    const hypsFound: model.SignalDetectionHypothesis[] = [];
    signalDetections.forEach(sd => {
      const hyps = sd.signalDetectionHypotheses.filter(
        sdHyp => hypothesisIds.indexOf(sdHyp.id) >= 0
      );
      if (hyps && hyps.length > 0) {
        hypsFound.push(...hyps);
      }
    });
    return Object.seal(hypsFound);
  }

  /**
   * Retrieve the signal detections associated with the provided list of station IDs, whose
   * arrival time feature measurements fall within the provided time range. Throws an error
   * if any of the hypotheses associated signal detections or arrival time measurements are missing.
   * @param userContext user context for current user
   * @param timeRange The time range for which to retrieve signal detections
   */
  public async getSignalDetectionsForDefaultStations(
    userContext: UserContext,
    timeRange: TimeRange
  ): Promise<model.SignalDetection[]> {
    const stations: ProcessingStation[] = ProcessingStationProcessor.Instance().getDefaultProcessingStations();
    const stationIds: string[] = stations.map(station => station.name);
    return Object.seal(await this.loadSignalDetections(userContext, timeRange, stationIds));
  }

  /**
   * Retrieve the signal detections associated with the provided list of station IDs, whose
   * arrival time feature measurements fall within the provided time range. Throws an error
   * if any of the hypotheses associated signal detections or arrival time measurements are missing.
   * @param stationIds The station IDs to find signal detections for
   * @param startTime The start of the time range for which to retrieve signal detections
   * @param endTime The end of the time range for which to retrieve signal detections
   */
  public getSignalDetectionsByStation(
    userContext: UserContext,
    stationIds: string[],
    timeRange: TimeRange
  ): model.SignalDetection[] {
    performanceLogger.performance('signalDetectionsByStation', 'enteringResolver');
    const returnedSds = userContext.userCache
      .getSignalDetections()
      .filter(detection => filterSdsByStationAndTime(detection, stationIds, timeRange));
    performanceLogger.performance('signalDetectionsByStation', 'leavingResolver');
    return Object.seal(returnedSds);
  }

  /**
   * Retrieve the signal detections matching the provided IDs.
   * @param detectionIds the IDs of the signal detections to retrieve
   */
  public getSignalDetectionsById(
    userContext: UserContext,
    detectionIds: string[]
  ): model.SignalDetection[] {
    const returning = filter(
      userContext.userCache.getSignalDetections(),
      detection => detectionIds.indexOf(detection.id) > -1
    );
    return Object.seal(returning);
  }

  /**
   * Retrieve the Signal Detection using the Signal Detection Hypo's parent id
   * @param sdHypoId the signal detection hypothesis id
   *
   * @returns Signal Detection
   */
  public getSignalDetectionByHypoId(
    userContext: UserContext,
    sdHypoId: string
  ): model.SignalDetection {
    const sdHypo = this.getSignalDetectionHypothesisById(
      userContext.userCache.getSignalDetections(),
      sdHypoId
    );
    if (sdHypo && sdHypo.parentSignalDetectionId) {
      return userContext.userCache.getSignalDetectionById(sdHypo.parentSignalDetectionId);
    }
    return undefined;
  }

  /**
   * Gets SDs based on an incoming list and filters out undefined sds not found
   * @param userContext user context for current user
   * @param sdHypIds hypothesis Ids to get sds for
   *
   * @returns signal detections corresponding to input sd hyp ids
   */
  public getSignalDetectionsByHypothesesId(
    userContext: UserContext,
    sdHypIds: string[]
  ): model.SignalDetection[] {
    return Object.seal(
      sdHypIds
        .map(sdHypId => this.getSignalDetectionByHypoId(userContext, sdHypId))
        .filter(sd => sd !== undefined)
    );
  }

  /**
   * Retrieve the signal detections matching the event ID using the SD Associations data structure.
   * @param eventId the ID of the
   */
  public async getSignalDetectionsByEventId(
    userContext: UserContext,
    eventId: string
  ): Promise<model.SignalDetection[]> {
    // Try to retrieve the event with the provided ID; throw an error if it is missing
    const event = userContext.userCache.getEventById(eventId);
    const allSds = userContext.userCache.getSignalDetections();
    if (!event) {
      throw new Error(`Failed to find event with ID ${eventId}`);
    }
    return Object.seal(getSignalDetectionsAssociatedToEvent(event, allSds));
  }

  /**
   * Gets all sd hypothesis ids in the system
   */
  public getAllValidSdHypIds(userContext: UserContext): string[] {
    return Object.seal(
      userContext.userCache
        .getSignalDetections()
        .flatMap(sd => sd.signalDetectionHypotheses)
        .map(sdh => sdh.id)
    );
  }

  /**
   * Loads signal detections.
   * @param userContext user context for current user
   * @param timeRange the time range to load
   * @param stationIds the station ids
   *
   * @returns signal detections in time range
   */
  public async loadSignalDetections(
    userContext: UserContext,
    timeRange: TimeRange,
    stationIds: string[]
  ): Promise<model.SignalDetection[]> {
    if (!timeRange) {
      throw new Error('Unable to retrieve Signal Detections for undefined time range');
    }
    const requestConfig = this.settings.backend.services.sdsByStation.requestConfig;
    // TODO Check cache then call backend if not found
    const endTimePadding: number = ConfigProcessor.Instance().getConfigByKey('extraLoadingTime');
    const dehydratedSds = await loadSignalDetectionsFromService(
      stationIds,
      timeRange,
      endTimePadding,
      this.httpWrapper,
      requestConfig
    );
    const signalDetectionPromises = dehydratedSds.map(async sd =>
      this.convertSDFromOSD(userContext, sd)
    );
    const signalDetections = await Promise.all(signalDetectionPromises);
    CacheProcessor.Instance().addLoadedSdsToGlobalCache(signalDetections);
    return Object.seal(userContext.userCache.getSignalDetections());
  }

  /**
   * Calls the Filter Waveform streaming service to calculate Filtered versions of the Arrival Time beam
   * @param arrivalTimeFM the arrival time feature measurement
   */
  public async populateFilterBeams(
    userContext: UserContext,
    arrivalTimeFM: model.FeatureMeasurement
  ): Promise<model.FeatureMeasurement[]> {
    const beamChannelSegment = await ChannelSegmentProcessor.Instance().getChannelSegmentUsingSegmentDescriptor(
      userContext,
      arrivalTimeFM.measuredChannelSegmentDescriptor,
      arrivalTimeFM.channel.name
    );
    const filteredFMs: model.FeatureMeasurement[] = [];
    if (!beamChannelSegment) {
      return [];
    }

    if (isWaveformChannelSegment(beamChannelSegment)) {
      try {
        const filteredCS = await WaveformFilterProcessor.Instance().getFilteredWaveformSegments(
          userContext,
          [beamChannelSegment]
        );
        filteredCS.forEach(cs => {
          const fm = createFeatureMeasurement(
            model.FeatureMeasurementTypeName.FILTERED_BEAM,
            {
              strValue: cs.wfFilterId
            },
            cs.channel,
            beamChannelSegment.startTime,
            beamChannelSegment.endTime
          );
          filteredFMs.push(fm);
        });
      } catch (e) {
        logger.warn(
          `Failed to compute filtered beams ${e} none will be added to Signal Detection.`
        );
      }
    }
    return Object.seal(filteredFMs);
  }

  /**
   * Saves the provided signal detections.
   *
   * @param userContext the user context
   * @param signalDetections the signal detections to save
   *
   * @returns the signal detections saved and response
   */
  public async saveSignalDetections(
    userContext: UserContext,
    signalDetections: model.SignalDetection[]
  ): Promise<{ signalDetections: model.SignalDetection[]; response: HttpResponse<string[]> }> {
    // determined the modified signal detections to save, no reason to save a non-modified signal detection
    const signalDetectionsToSave = signalDetections
      // filter out non-modified signal detections
      .filter(sd => sd.currentHypothesis.modified);

    if (signalDetectionsToSave.length !== signalDetections.length) {
      logger.info(
        `The following signal detections are not modified (skipping save): ` +
          `${String(
            signalDetections.filter(sd => !sd.currentHypothesis.modified).map(sd => sd.id)
          )}`
      );
    }

    if (signalDetectionsToSave.length > 0) {
      // Save channel segments
      const featureMeasurements = flatMap<model.FeatureMeasurement>(
        signalDetectionsToSave.map(sd => sd.currentHypothesis.featureMeasurements)
      ).filter(
        fm =>
          fm.featureMeasurementType ===
            model.FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH ||
          fm.featureMeasurementType === model.FeatureMeasurementTypeName.ARRIVAL_TIME
      );

      // Find all the Channel Segment for the Azimuth or ArrivalTime FM that requires saving
      const channelSegments = await Promise.all(
        featureMeasurements.map(async fm =>
          ChannelSegmentProcessor.Instance().getInCacheChannelSegmentBySegmentDescriptor(
            userContext,
            fm.measuredChannelSegmentDescriptor
          )
        )
      ).then(chanSegs => chanSegs.filter(c => c !== undefined && c.requiresSave));

      await ChannelSegmentProcessor.Instance().saveChannelSegments(userContext, channelSegments);

      // Call OSD endpoint to save signal detections
      logger.info(`Saving ${signalDetectionsToSave.length} signal detections`);
      const requestConfig = this.settings.backend.services.saveSds.requestConfig;
      const response = await saveSignalDetectionsToService(
        signalDetectionsToSave,
        this.httpWrapper,
        requestConfig
      );

      // determine which signal detections were saved successfully based on the response
      const httpOkay = 200;
      if (response.status === httpOkay) {
        // Save changes to Global Cache
        const resetSDs = this.resetModifiedFlags(
          userContext,
          signalDetectionsToSave.map(sd => sd.id)
        );
        userContext.userCache.commitSignalDetectionsWithIds(resetSDs.map(sd => sd.id));
        return { signalDetections: Object.seal(resetSDs), response };
      }
    }
    logger.info(`No modified signal detections to save`);
    return { signalDetections: [], response: undefined /* no response made */ };
  }

  /**
   * Resolves Hyp Ids to SDs based on association changes from the
   * event processor
   * @param sdHypIds hyp ids to resolve
   */
  public getSignalDetectionsByHypothesisIds(
    userContext: UserContext,
    sdHypIds: string[]
  ): model.SignalDetection[] {
    return Object.seal(sdHypIds.map(hypId => this.getSignalDetectionByHypoId(userContext, hypId)));
  }

  /**
   * Determine if any Signal Detections are in conflict.
   * @returns boolean
   */
  public areAnySDsInConflict(userContext: UserContext): boolean {
    return userContext.userCache.getSignalDetections().some(sd => sd.hasConflict);
  }

  /**
   * Returns the data structure required by the ui for conflict popups
   * @param userContext user context
   * @param event event to get conflicts for
   */
  public getConflictingSdHypotheses(
    userContext: UserContext,
    sd: model.SignalDetection
  ): model.ConflictingSdHypData[] {
    return Object.seal(
      [...sd.associations]
        .filter(a => !a.rejected)
        .map(association => {
          const hypId = association.signalDetectionHypothesisId;
          const hyp = this.getSignalDetectionHypothesisById(
            userContext.userCache.getSignalDetections(),
            hypId
          );
          return {
            eventId: association.eventId,
            arrivalTime: findArrivalTimeFeatureMeasurementValue(hyp.featureMeasurements).value,
            phase: findPhaseFeatureMeasurementValue(hyp.featureMeasurements).phase
          };
        })
    );
  }

  /**
   * Creates a new hyp if SD has not been modified, or updates the current hyp
   * @param detection detection to update or create hypothesis for
   * @param event event associated/open or undefined
   * @param reject reject the updated/created hypothesis? defaults to false
   * @param phase phase to updated hypothesis with
   * @param signalDetectionTiming timing information to update hypothesis with
   *
   * @returns collection of the updated signal detection and the updated or created hypothesis
   */
  public createOrUpdateSignalDetectionHypothesis(
    detection: model.SignalDetection,
    event: Event,
    reject: boolean = false,
    phase?: string,
    signalDetectionTiming?: model.SignalDetectionTimingInput
  ): { sd: model.SignalDetection; hypothesis: model.SignalDetectionHypothesis } {
    const hypothesis = detection.currentHypothesis;
    const eventId = event ? event.id : undefined;

    // New hypothesis should be created when:
    //  - the hypothesis hasn't been modified OR
    //  - a valid event is passed in AND the detection has at least one association
    //    AND the event is NOT already associated to the detection
    // The association length check is necessary because given a new detection with no
    // associations (created by the analyst), should not create a new hypothesis when associated
    return !hypothesis.modified ||
      (event &&
        detection.associations.length > 0 &&
        !detection.associations.find(association => association.eventId === eventId))
      ? this.createNewHypothesisForModifiedDetection(
          detection,
          reject,
          phase,
          signalDetectionTiming
        )
      : this.updateCurrentHypothesis(detection, reject, phase, signalDetectionTiming);
  }

  /**
   * Gets a feature measurement by id
   * @param userContext user context of the current user
   * @param fmId feature measurement id
   *
   * @returns feature measurement object matching input id
   */
  public getFeatureMeasurementById(
    userContext: UserContext,
    fmId: string
  ): model.FeatureMeasurement {
    const fms = [];
    const sds = userContext.userCache.getSignalDetections();
    sds.forEach(sd => {
      sd.signalDetectionHypotheses.forEach(hyp => {
        fms.push(...hyp.featureMeasurements);
      });
    });
    return fms.find(fm => fm.id === fmId);
  }

  /**
   * Gets a feature measurement by id
   * @param userContext user context of the current user
   * @param fmId feature measurement id
   *
   * @returns feature measurement object matching input id
   */
  public getFeatureMeasurementByIdAndType(
    userContext: UserContext,
    sdId: string,
    fmType: model.FeatureMeasurementTypeName
  ): model.FeatureMeasurement {
    const sd = userContext.userCache.getSignalDetectionById(sdId);
    if (!sd || !sd.currentHypothesis) {
      return undefined;
    }
    return sd.currentHypothesis.featureMeasurements.find(
      fm => fm.featureMeasurementType === fmType
    );
  }

  /**
   * Returns all sds that have been modified
   */
  public getModifiedSds(userContext: UserContext): model.SignalDetection[] {
    return Object.seal(
      userContext.userCache.getSignalDetections().filter(sd => sd.currentHypothesis.modified)
    );
  }

  /**
   * Resets all modified flags to false
   * @param userContext graphql user context
   * @param sdIds sd ids to reset
   */
  private resetModifiedFlags(userContext: UserContext, sdIds: string[]) {
    const sds = sdIds.map(sdId => userContext.userCache.getSignalDetectionById(sdId));
    return sds
      .map(sd => {
        if (sd.currentHypothesis.modified) {
          const updatedCurrentHypothesis = produce<model.SignalDetectionHypothesis>(
            sd.currentHypothesis,
            draftState => {
              draftState.modified = false;
            }
          );

          const updatedSd = produce<model.SignalDetection>(sd, draftState => {
            draftState.currentHypothesis = updatedCurrentHypothesis;
            draftState.signalDetectionHypotheses = replaceByIdOrAddToList<
              model.SignalDetectionHypothesis
            >(draftState.signalDetectionHypotheses, updatedCurrentHypothesis);
          });
          userContext.userCache.setSignalDetection(
            UserActionDescription.UPDATE_DETECTION,
            updatedSd
          );
          return updatedSd;
        }
      })
      .filter(sd => sd !== undefined);
  }

  /**
   * Hydrates a feature measurement
   * @param userContext user context for current user
   * @param fm feature measurement to hydrate
   * @param definingRules defining rules based on phase
   * @returns a Feature measurement[] as a promise
   */
  private async hydrateFeatureMeasurements(
    userContext: UserContext,
    fm: FeatureMeasurementOSD
  ): Promise<model.FeatureMeasurement[]> {
    // TODO: check if/how beams are being loaded
    let fmArray: model.FeatureMeasurement[] = [];

    const convertedFm = convertFeatureMeasurementFromOSD(fm);
    if (convertedFm.featureMeasurementType === model.FeatureMeasurementTypeName.ARRIVAL_TIME) {
      // Populate the Fk_Beam and Filter Fk_Beams in the channel segment processor cache
      const filteredFms = await this.populateFilterBeams(userContext, convertedFm);
      fmArray.push(convertedFm);
      filteredFms.forEach(filteredFm => fmArray.push(filteredFm));
    } else {
      fmArray = [convertedFm];
    }
    // Before returning set Id on all fms
    return fmArray;
  }

  /**
   * Initialize the processor.
   */
  private initialize(): void {
    logger.info(
      'Initializing the Signal Detection processor - Mock Enable: %s',
      this.settings.backend.mock.enable
    );
    logger.debug('');
    // If service mocking is enabled, initialize the mock backend
    const backendConfig = config.get('signalDetection.backend');

    if (backendConfig.mock.enable) {
      signalDetectionMockBackend.initialize(this.httpWrapper.createHttpMockWrapper());
    }
  }

  /**
   * Hydrates sd hypothesis with feature measurements
   * @param sdHypo hypothesis to hydrate
   *
   */
  private async hydrateSignalDetectionHypothesis(
    userContext: UserContext,
    sdHypothesisOSD: SignalDetectionHypothesisOSD
  ): Promise<model.SignalDetectionHypothesis> {
    // RESUME HERE
    const featureMeasurementPromises = flatMap(sdHypothesisOSD.featureMeasurements, async fmOSD =>
      this.hydrateFeatureMeasurements(userContext, fmOSD)
    );
    const fmMatrix = await Promise.all(featureMeasurementPromises);
    const featureMeasurements = flatMap(fmMatrix, fm => fm);

    return {
      id: sdHypothesisOSD.id,
      parentSignalDetectionId: sdHypothesisOSD.parentSignalDetectionId,
      monitoringOrganization: sdHypothesisOSD.monitoringOrganization,
      stationName: sdHypothesisOSD.stationName,
      parentSignalDetectionHypothesisId: sdHypothesisOSD.parentSignalDetectionHypothesisId,
      rejected: sdHypothesisOSD.rejected,
      featureMeasurements,
      modified: false,
      reviewed: {
        amplitudeMeasurement: false
      }
    };
  }

  /**
   * Hydrates sd hypothesis
   * @param sd Sd to hydrate
   */
  private async convertSDFromOSD(
    userContext: UserContext,
    sdOSD: SignalDetectionOSD
  ): Promise<model.SignalDetection> {
    try {
      // If the Signal Detection came from OSD then it is not modified
      // only local SD are modified by the Analyst
      const sdHypothesesPromises: Promise<model.SignalDetectionHypothesis>[] =
        sdOSD.signalDetectionHypotheses && sdOSD.signalDetectionHypotheses.length > 0
          ? sdOSD.signalDetectionHypotheses.map(async sdh =>
              this.hydrateSignalDetectionHypothesis(userContext, sdh)
            )
          : [];
      const hypotheses = await Promise.all(sdHypothesesPromises);

      const sd: model.SignalDetection = {
        id: sdOSD.id,
        monitoringOrganization: sdOSD.monitoringOrganization,
        signalDetectionHypotheses: hypotheses,
        stationName: sdOSD.stationName,
        currentHypothesis: hypotheses[hypotheses.length - 1],
        associations: [],
        hasConflict: false
      };

      const promises = hypotheses.map(async hyp => {
        // Populate the fk spectra from the channel segment
        const azimuthFeatureMeasurement = findAzimuthFeatureMeasurement(hyp.featureMeasurements);
        if (
          azimuthFeatureMeasurement &&
          azimuthFeatureMeasurement.measuredChannelSegmentDescriptor
        ) {
          // Retrieve the FK Spectra timeseries. The channel segment processor will cache after
          // OSD call
          await ChannelSegmentProcessor.Instance().getChannelSegmentUsingSegmentDescriptor(
            userContext,
            azimuthFeatureMeasurement.measuredChannelSegmentDescriptor,
            azimuthFeatureMeasurement.channel.name,
            sd
          );
        }
      });
      await Promise.all(promises);
      return sd;
    } catch (e) {
      logger.error(`Error adding SD ID: ${sdOSD.id} to data cache`);
      logger.error(e);
      logger.debug(`SD returned from OSD: ${JSON.stringify(sdOSD, undefined, 2)}`);
      return undefined;
    }
  }

  /**
   * Update Signal Detection's current Hypothesis time and or phase
   * @param detection detection to update hyp on
   * @param rejected rejected should be set
   * @param phase phase to update to
   * @param sdTiming timing information to update
   */
  private updateCurrentHypothesis(
    detection: model.SignalDetection,
    rejected: boolean,
    phase?: string,
    sdTiming?: model.SignalDetectionTimingInput
  ): { sd: model.SignalDetection; hypothesis: model.SignalDetectionHypothesis } {
    const hypToUse = detection.currentHypothesis;

    const stationDefaultChannel = ProcessingStationProcessor.Instance().getDefaultChannelForStation(
      detection.stationName
    );
    const newFeatureMeasurementList = createFmsFromUserInput(
      hypToUse.featureMeasurements,
      stationDefaultChannel,
      phase,
      sdTiming
    );
    const updatedHyp = produce<model.SignalDetectionHypothesis>(hypToUse, draftState => {
      draftState.featureMeasurements = newFeatureMeasurementList;
      draftState.rejected = rejected;
    });
    const updatedSd = produce<model.SignalDetection>(detection, draftState => {
      draftState.signalDetectionHypotheses = replaceByIdOrAddToList<
        model.SignalDetectionHypothesis
      >(draftState.signalDetectionHypotheses, updatedHyp);
    });

    return { sd: updatedSd, hypothesis: updatedHyp };
  }

  /**
   * Create a new Signal Detection Hypothesis and set current SD Hypothesis to it.
   * @param detection signal detection to update
   * @param rejected rejected flag should be set?
   * @param phase phase to update
   * @param sdTiming timing info to update
   *
   * @returns collection of updated sd and sd hypothesis
   */
  private createNewHypothesisForModifiedDetection(
    detection: model.SignalDetection,
    rejected: boolean,
    phase?: string,
    sdTiming?: model.SignalDetectionTimingInput
  ): { sd: model.SignalDetection; hypothesis: model.SignalDetectionHypothesis } {
    const hypToUse = detection.currentHypothesis;
    const stationDefaultChannel = ProcessingStationProcessor.Instance().getDefaultChannelForStation(
      detection.stationName
    );
    const newFeatureMeasurementList = createFmsFromUserInput(
      cloneDeep(hypToUse.featureMeasurements),
      stationDefaultChannel,
      phase,
      sdTiming
    );

    const newHypothesis: model.SignalDetectionHypothesis = {
      id: uuid4(),
      parentSignalDetectionId: detection.id,
      monitoringOrganization: hypToUse.monitoringOrganization,
      stationName: hypToUse.stationName,
      parentSignalDetectionHypothesisId: hypToUse.id,
      rejected,
      featureMeasurements: newFeatureMeasurementList,
      modified: true,
      reviewed: {
        amplitudeMeasurement: false
      }
    };

    const updatedDetection = produce<model.SignalDetection>(detection, draftState => {
      draftState.signalDetectionHypotheses = [
        ...detection.signalDetectionHypotheses,
        newHypothesis
      ];
    });

    return { sd: updatedDetection, hypothesis: newHypothesis };
  }
}
