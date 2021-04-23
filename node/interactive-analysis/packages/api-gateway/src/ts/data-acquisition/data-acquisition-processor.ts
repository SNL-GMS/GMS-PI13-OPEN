import { MILLISECONDS_IN_SECOND, toOSDTime } from '@gms/common-util';
import config from 'config';
import cloneDeep from 'lodash/cloneDeep';
import filter from 'lodash/filter';
import { TimeRange } from '../common/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import * as dataAcquisitionStatusMockBackend from './data-acquisition-mock-backend';
import * as model from './model';

/**
 * Data acquisition processor which handles the transfer file gaps
 */
export class DataAcquisitionProcessor {
  /** The singleton instance */
  private static instance: DataAcquisitionProcessor;

  /** Settings */
  private readonly settings: any;

  /** Http wrapper for client */
  private readonly httpWrapper: HttpClientWrapper;

  private constructor() {
    this.settings = config.get('dataAcquisition');
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): DataAcquisitionProcessor {
    if (DataAcquisitionProcessor.instance === undefined) {
      DataAcquisitionProcessor.instance = new DataAcquisitionProcessor();
      DataAcquisitionProcessor.instance.initialize();
    }
    return DataAcquisitionProcessor.instance;
  }

  /**
   * Gets transferred files by a time range
   * @param timeRange time range for transferred files
   * @returns a promise TransferredFile[]
   */
  public async getTransferredFilesByTimeRange(
    timeRange: TimeRange
  ): Promise<model.TransferredFile[]> {
    const requestConfig = this.settings.backend.services.transferredFilesByTimeRange.requestConfig;
    const query = {
      startTime: toOSDTime(timeRange.startTime),
      endTime: toOSDTime(timeRange.endTime)
    };
    logger.debug(`Calling get data acquisition status query: ${JSON.stringify(query)}
                request: ${JSON.stringify(requestConfig)}`);

    // Call the service and process the response data
    const response: HttpResponse<model.TransferredFile[]> = await this.httpWrapper.request<
      model.TransferredFile[]
    >(requestConfig, query);
    return this.processTransferredFileData(response.data);
  }

  /**
   * Calculates the duration of the gap based on the reception and transfer time
   * @param receptionTime reception time
   * @param transferTime transfer time
   * @returns gap duration as string
   */
  public processGapDuration(receptionTime: string, transferTime: string): string {
    let gapLength = new Date(receptionTime).getTime() - new Date(transferTime).getTime();

    gapLength = gapLength / MILLISECONDS_IN_SECOND;

    return gapLength.toString();
  }

  /**
   * Location doesn't exist yet, so this is a place holder
   * @param transferredFile place holder
   * @returns location as string
   */
  public getLocation(transferredFile: model.TransferredFile): string {
    let location = 'Bad Location';
    if (transferredFile) {
      location = 'location';
    }
    return location;
  }

  /**
   * Function to upload a Reference Station to the OSD
   */
  public async saveReferenceStation(
    referenceStation: model.DataAcqReferenceStation
  ): Promise<boolean> {
    logger.info(`Saving station: ${referenceStation.name}`);

    // Create a deep copy and remove the currentVersion (UI construct) before saving
    const referenceStationToSave: model.DataAcqReferenceStation = cloneDeep(referenceStation);
    let status = true;
    // Retrieve the request configuration for the service call
    const requestConfig = this.settings.backend.services.saveReferenceStation.requestConfig;
    logger.debug(`Calling reference station save for: ${JSON.stringify(referenceStation)}
      request: ${JSON.stringify(requestConfig)}`);
    await this.httpWrapper
      .request<any>(requestConfig, [referenceStationToSave])
      .catch(error => (status = false));

    return status;
  }

  /**
   * Initialize the data acquisition status processor, setting up a mock backend if configured to do so.
   */
  private initialize(): void {
    logger.info(
      'Initializing the Data Acquisition Status processor - Mock Enable: %s',
      this.settings.backend.mock.enable
    );

    // TODO: Comment guard back in when transfer gaps is refactored to not return all gaps
    // If service mocking is enabled, initialize the mock backend
    if (this.settings.backend.mock.enable) {
      dataAcquisitionStatusMockBackend.initialize(this.httpWrapper.createHttpMockWrapper());
    }
  }

  /**
   * Filters response to based on gaps
   * @param transferredFileResponseData response from service to be filtered based on Gaps
   * @returns gaps as TransferredFile[]
   */
  private processTransferredFileData(
    transferredFileResponseData: model.TransferredFile[]
  ): model.TransferredFile[] {
    let gaps: model.TransferredFile[] = [];
    if (transferredFileResponseData) {
      // Filter the transferred file objects from the response to Raw_station_data_frame
      gaps = filter(
        transferredFileResponseData,
        transferredFile =>
          transferredFile.metadataType === model.MetadataType.RAW_STATION_DATA_FRAME &&
          transferredFile.status === model.Status.SENT
      );
    }
    return gaps;
  }
}
