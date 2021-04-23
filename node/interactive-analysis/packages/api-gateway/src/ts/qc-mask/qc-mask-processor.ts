import { toEpochSeconds, toOSDTime, uuid4 } from '@gms/common-util';
import config from 'config';
import cloneDeep from 'lodash/cloneDeep';
import { CacheProcessor } from '../cache/cache-processor';
import { UserContext } from '../cache/model';
import { TimeRange } from '../common/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import * as model from './model';
import * as qcMaskMockBackend from './qc-mask-mock-backend';

/**
 * API gateway processor for QC mask data APIs. This class supports:
 * - data fetching & caching from the backend service interfaces
 * - mocking of backend service interfaces based on test configuration
 * - session management
 * - GraphQL query resolution from the user interface client
 */

/** QC mask response from the service */
interface QcMaskServiceResponse {
  [key: string]: model.QcMask[];
}

/**
 * QC Mask Processor
 */
export class QcMaskProcessor {
  /** The singleton instance */
  private static instance: QcMaskProcessor;

  /** Local configuration settings */
  private readonly settings: any;

  /** HTTP client wrapper for communicating with backend services */
  private readonly httpWrapper: HttpClientWrapper;

  /**
   * Constructor - initialize the processor, loading settings and initializing
   * the HTTP client wrapper.
   */
  private constructor() {
    // Load configuration settings
    this.settings = config.get('qcMask');

    // Initialize an http client
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): QcMaskProcessor {
    if (QcMaskProcessor.instance === undefined) {
      QcMaskProcessor.instance = new QcMaskProcessor();
      QcMaskProcessor.instance.initialize();
    }
    return QcMaskProcessor.instance;
  }

  /**
   * Retrieve QC Masks from the cache, filtering the results
   * down to those masks overlapping the input time range and matching an entry in
   * the input list of channel IDs.
   *
   * @param timeRange The time range in which to retreive QC masks
   * @param channelNames The list of channel names for which to retrieve QC masks
   */
  public async getQcMasks(
    userContext: UserContext,
    timeRange: TimeRange,
    channelNames: string[]
  ): Promise<model.QcMask[]> {
    // Handle undefined input time range
    if (!timeRange) {
      throw new Error('Unable to retrieve cached QC masks for undefined time range');
    }

    // Handle undefined input channel ID list
    if (!channelNames || channelNames.length === 0) {
      throw new Error(`Unable to retrieve cached QC masks for undefined channel ID list`);
    }

    logger.debug(
      `Get QC Masks request for time range: ${JSON.stringify(timeRange)} and channels: ${String(
        channelNames
      )}`
    );

    // Retrieve the request configuration for the service call
    const requestConfig = this.settings.backend.services.masksByChannelIds.requestConfig;
    const qcMasks = [];

    // First call for cached masks then execute OSD queries for each channel id not found
    const filteredChannels = this.findCachedMasks(qcMasks, channelNames, timeRange);
    if (filteredChannels.length === 0) {
      if (qcMasks.length > 0) {
        logger.debug(`Returning qcMasks from Cache size ${qcMasks.length}`);
      }
      return qcMasks;
    }
    const query = {
      channelNames,
      startTime: toOSDTime(timeRange.startTime),
      endTime: toOSDTime(timeRange.endTime)
    };

    logger.debug(
      `Calling get QC Masks query: ${JSON.stringify(query)} request: ${JSON.stringify(
        requestConfig
      )}`
    );
    const response: HttpResponse<QcMaskServiceResponse> = await this.httpWrapper.request<
      QcMaskServiceResponse
    >(requestConfig, query);
    if (response && response.data) {
      // tslint:disable-next-line: no-for-in
      for (const key in response.data) {
        if (!response.data.hasOwnProperty(key)) {
          continue;
        }
        const masks: model.QcMask[] = response.data[key];
        masks.forEach(mask => {
          CacheProcessor.Instance().setQcMask(mask);
          // Add mask to list
          qcMasks.push(mask);
        });
      }
    }

    logger.info(`Returning qcMasks size ${qcMasks.length}`);
    return qcMasks;
  }

  /**
   * Creates QC Masks and adds them to the cache and OSD (when not mocked)
   *
   * @param userContext the user context
   * @param channelNames the channel names
   * @param input the input
   */
  public async createQcMasks(
    userContext: UserContext,
    channelNames: string[],
    input: model.QcMaskInput
  ): Promise<model.QcMask[]> {
    // Handle undefined channelNames
    if (!channelNames) {
      throw new Error('Unable to create QcMask with undefined channel ID list');
    }

    // Handle undefined input
    if (!input) {
      throw new Error('Unable to create QcMask with undefined input');
    }
    const qcMasks: model.QcMask[] = [];
    channelNames.forEach(channelId => {
      const qcMaskVersion: model.QcMaskVersion = {
        category: input.category,
        // TODO: Need CS Ids from UI input args
        channelSegmentIds: [uuid4()],
        startTime: toOSDTime(input.timeRange.startTime),
        endTime: toOSDTime(input.timeRange.endTime),
        parentQcMasks: [],
        rationale: input.rationale,
        type: input.type,
        version: 0
      };
      const newMask: model.QcMask = {
        id: uuid4(),
        channelName: channelId,
        qcMaskVersions: [qcMaskVersion]
      };
      // Add the mask to the collection to return in addition to the cache
      qcMasks.push(newMask);
      CacheProcessor.Instance().setQcMask(newMask);
    });

    // Save newly created masks
    if (await this.saveQCMasks(userContext, qcMasks)) {
      return qcMasks;
    }
    throw new Error('Save to OSD failed: Mask not created');
  }

  /**
   * Updates the QC Mask specified by the id parameter
   *
   * @param qcMaskId Id to update
   * @param input updated parameters
   */
  public async updateQcMask(
    userContext: UserContext,
    qcMaskId: string,
    input: model.QcMaskInput
  ): Promise<model.QcMask> {
    // Handle undefined mask id
    if (!qcMaskId) {
      throw new Error('Unable to update QcMask with undefined mask ID');
    }

    // Handle undefined input
    if (!input) {
      throw new Error('Unable to update QcMask with undefined input');
    }
    // Find the Mask
    const maskToUpdate: model.QcMask = CacheProcessor.Instance().getQcMask(qcMaskId);

    // If no mask was found, throw an error
    if (!maskToUpdate) {
      throw new Error('Mask not Found');
    }

    const currentMaskVersion = maskToUpdate.qcMaskVersions[maskToUpdate.qcMaskVersions.length - 1];
    const nextMaskVersion = currentMaskVersion.version + 1;
    // Create a new version of the mask
    const newMaskVersion: model.QcMaskVersion = {
      ...currentMaskVersion,
      category: input.category,
      startTime: toOSDTime(input.timeRange.startTime),
      endTime: toOSDTime(input.timeRange.endTime),
      parentQcMasks: [],
      rationale: input.rationale,
      type: input.type,
      version: nextMaskVersion
    };

    maskToUpdate.qcMaskVersions.push(newMaskVersion);

    // Save updated masks
    if (await this.saveQCMasks(userContext, [maskToUpdate])) {
      return maskToUpdate;
    }
    throw new Error('Save to OSD failed: Mask not updated');
  }

  /**
   * Rejects the specified mask for the specified reason
   *
   * @param qcMaskId Id to reject
   * @param rationale reason for the rejection
   */
  public async rejectQcMask(
    userContext: UserContext,
    qcMaskId: string,
    rationale: string
  ): Promise<model.QcMask> {
    // Handle undefined mask id
    if (!qcMaskId) {
      throw new Error('Unable to reject QcMask with undefined mask ID');
    }

    // Find the Mask
    const maskToUpdate: model.QcMask = CacheProcessor.Instance().getQcMask(qcMaskId);

    // If no mask was found, throw an error
    if (!maskToUpdate) {
      throw new Error('Mask not Found');
    }

    const currentMaskVersion = maskToUpdate.qcMaskVersions[maskToUpdate.qcMaskVersions.length - 1];
    const nextMaskVersion = currentMaskVersion.version + 1;
    // Create a new version of the mask
    const newMaskVersion: model.QcMaskVersion = {
      ...currentMaskVersion,
      category: model.QcMaskCategory.Rejected,
      rationale,
      version: nextMaskVersion
    };

    const osdMaskToReject = cloneDeep(maskToUpdate);
    const osdMaskToRejectNewVersion = cloneDeep(newMaskVersion);

    delete osdMaskToRejectNewVersion.type;
    delete osdMaskToRejectNewVersion.startTime;
    delete osdMaskToRejectNewVersion.endTime;

    maskToUpdate.qcMaskVersions.push(newMaskVersion);
    osdMaskToReject.qcMaskVersions.push(osdMaskToRejectNewVersion);

    // Save rejected mask
    if (await this.saveQCMasks(userContext, [osdMaskToReject])) {
      return maskToUpdate;
    }
    throw new Error('Save to OSD failed: Mask not rejected');
  }
  /**
   * Initialize the QC mask processor, setting up a mock backend if configured to do so.
   */
  private initialize(): void {
    logger.info(
      'Initializing the QcMask processor - Mock Enable: %s',
      this.settings.backend.mock.enable
    );

    // If service mocking is enabled, initialize the mock backend
    if (this.settings.backend.mock.enable) {
      qcMaskMockBackend.initialize(this.httpWrapper.createHttpMockWrapper());
    }
  }

  /**
   * Search the data cache to see if we already of the QcMask cached
   *
   * @param qcMask is empty on entry to be filled by method
   * @param channelNames list of channels to search
   * @returns list of channel ids we did not find masks for (this list will be list to OSD)
   */
  private readonly findCachedMasks = (
    qcMasks: model.QcMask[],
    channelNames: string[],
    timeRange: TimeRange
  ): string[] => {
    const osdChannelNames = [];
    if (CacheProcessor.Instance().getQcMasks().entrySeq.length === 0) {
      return channelNames;
    }

    // Walk thru each channel and find masks for the time range we are interested in
    channelNames.forEach(chanId => {
      let foundOne = false;
      CacheProcessor.Instance()
        .getQcMasks()
        .forEach(qcM => {
          const currentMaskVersion = qcM.qcMaskVersions[qcM.qcMaskVersions.length - 1];
          if (
            qcM.channelName === chanId &&
            toEpochSeconds(currentMaskVersion.startTime) >= timeRange.startTime &&
            toEpochSeconds(currentMaskVersion.endTime) <= timeRange.endTime
          ) {
            qcMasks.push(qcM);
            foundOne = true;
          }
        });

      // If found mask add it to list else add the chanId to OSD channel id query list
      if (!foundOne) {
        osdChannelNames.push(chanId);
      }
    });
    return osdChannelNames;
  }

  /**
   * Function to save the list of QcMasks in the OSD
   */
  private async saveQCMasks(userContext: UserContext, qcMasks: model.QcMask[]) {
    logger.info(
      `Save QC Masks request qc mask list size of:${qcMasks.length}. User: ${userContext.userName}`
    );

    // Create a deep copy and remove the currentVersion (UI construct) before saving
    const masksToSave: model.QcMask[] = cloneDeep(qcMasks);
    let status = true;
    // Retrieve the request configuration for the service call
    const requestConfig = this.settings.backend.services.saveMasks.requestConfig;
    logger.info(
      `Calling QC Masks save for: ${JSON.stringify(qcMasks)} request: ${JSON.stringify(
        requestConfig
      )}`
    );
    await this.httpWrapper
      .request<any>(requestConfig, masksToSave)
      .catch(error => (status = false));
    return status;
  }
}
