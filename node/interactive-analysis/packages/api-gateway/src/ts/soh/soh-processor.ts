import { SohTypes } from '@gms/common-graphql';
import {
  StationAndStationGroupSoh,
  StationGroupSohStatus,
  UiHistoricalAcei,
  UiHistoricalAceiInput,
  UiHistoricalSoh,
  UiHistoricalSohInput,
  UiStationSoh
} from '@gms/common-graphql/lib/graphql/soh/types';
import { MILLISECONDS_IN_SECOND, setDurationTime, toOSDTime } from '@gms/common-util';
import config from 'config';
import { PubSub } from 'graphql-subscriptions';
import * as Immutable from 'immutable';
import { UserContext } from '../cache/model';
import { ConfigurationProcessor } from '../configuration/configuration-processor';
import { KafkaConsumer } from '../kafka/kafka-consumer';
import { KafkaProducer } from '../kafka/kafka-producer';
import { gatewayLogger, gatewayLogger as logger } from '../log/gateway-logger';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import * as model from './model';
import * as sohMockBackend from './soh-mock-backend';
import {
  createAcknowledgedStatusChange,
  createEmptyStationSoh,
  createStationGroupSohStatus
} from './soh-util';

/** Delay between the initial connection to kafka and publishing the first set of SOH */
const QUARTER_SECOND_MS = 250;
const HALF_SECOND_MS = 500;

/**
 * The processor that handles and reformats SOH data. Keeps track of Acknowledge/quieting
 */
export class SohProcessor {
  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): SohProcessor {
    if (SohProcessor.instance === undefined) {
      SohProcessor.instance = new SohProcessor();
      SohProcessor.instance.initialize();
    }
    return SohProcessor.instance;
  }

  /** The singleton instance */
  private static instance: SohProcessor;

  /** Settings */
  private readonly settings: any;

  /** KAFKA settings */
  private readonly kafkaSettings: any;

  /* The kafka message consumed helps when to publish */
  private lastKafkaMessageTimestamp: number = 0;

  private kafkaQueuedSohData: Immutable.Map<string, UiStationSoh> = Immutable.Map<
    string,
    UiStationSoh
  >();

  private latestQueuedSohData: Immutable.Map<string, UiStationSoh> = Immutable.Map<
    string,
    UiStationSoh
  >();

  private stationGroups: StationGroupSohStatus[] = [];

  /** Pubsub for apollo graphql */
  public readonly pubsub: PubSub = new PubSub();

  /** Delay between publishing new SOH to the UI */
  /** Default is 20 seconds will be read from Configuration SOH Update Interval */
  public BATCHING_DELAY_MS: number = undefined;

  /** Quiet interval from Configuration in milliseconds */
  public DEFAULT_QUIET_INTERVAL_MS: number = undefined;

  /** Http wrapper for client */
  public httpWrapper: HttpClientWrapper;

  /** Map that tracks which stations have been acknowledged */
  public isStationAcknowledged: Map<string, boolean>;

  private constructor() {
    this.settings = config.get('performanceMonitoring');
    this.kafkaSettings = config.get('kafka');
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Returns Station and Station Group SOH from the provided station SOH data.
   * @param stationSohs the station SOH data
   * TODO - this is ugly
   */
  public getUiStationAndStationGroupSOH(
    stationSohs: UiStationSoh[],
    isUpdateResponse: boolean
  ): StationAndStationGroupSoh {
    return {
      stationGroups: this.stationGroups,
      stationSoh: this.stationGroups.length === 0 ? [] : stationSohs,
      isUpdateResponse
    };
  }

  /**
   * Gets the most recent (latest) SOH entry for all stations.
   */
  public getSohForAllStations(): UiStationSoh[] {
    return Array.from(this.kafkaQueuedSohData.values());
  }

  /**
   * Returns the most recent (latest) for all Station and StationGroup SOH.
   */
  public getStationAndGroupSohWithEmptyChannels(): StationAndStationGroupSoh {
    const uiStationSohs: UiStationSoh[] = this.getSohForAllStations();
    // Blank out the channelSohs for each station soh
    const updatedSohs = uiStationSohs.map(soh => ({
      ...soh,
      channelSohs: []
    }));
    const stationGroupAndStations: StationAndStationGroupSoh = {
      stationGroups: this.stationGroups,
      stationSoh: updatedSohs,
      isUpdateResponse: false
    };
    return stationGroupAndStations;
  }

  /**
   * Retrieve UiStationSoh for station name
   */
  public getSohForStation(stationName: string): UiStationSoh | undefined {
    if (stationName && this.kafkaQueuedSohData.has(stationName)) {
      return this.kafkaQueuedSohData.get(stationName);
    }
    return undefined;
  }

  /**
   * Check if need to send batched SOH Status Changes to UIs
   */
  public checkToSendSohStatusChanges(): void {
    // Check to see if we haven't had any data for 1/2 second or the update interval has expired
    // before sending the SOH Station data to UI subscribers
    const now = Date.now();
    if (!this.hasUiDataToSend() || now - this.lastKafkaMessageTimestamp < HALF_SECOND_MS) {
      return;
    }

    // Call method to publish the latest StationSoh entries
    this.publishUiStationAndStationSoh(this.getLatestStationAndGroupSohWithEmptyChannels());

    // Okay clear the latest StationSoh that were queued since sending
    this.clearLatestSohForAllStations();

    // Reset timestamps since just published
    this.lastKafkaMessageTimestamp = now;
  }

  /**
   * Quiet a given list of channel monitor pairs
   * @param userContext context for user
   * @param channelMonitorsToQuiet an array of ChannelMonitorInputs
   * @returns StationAndStationGroups
   */
  public publishQuietChannelMonitorStatuses(
    userContext: UserContext,
    channelMonitorsToQuiet: SohTypes.ChannelMonitorInput[]
  ): void {
    channelMonitorsToQuiet.forEach(channelMonitorToQuiet => {
      logger.info(
        `Publishing soh quiet for channel(s): ${channelMonitorToQuiet.channelMonitorPairs
          .map(c => `${c.channelName}/${c.monitorType}`)
          .join(',')}` +
          ` by ${userContext.userName}${
            channelMonitorToQuiet.comment !== undefined ? ` : ${channelMonitorToQuiet.comment}` : ''
          }`
      );

      channelMonitorToQuiet.channelMonitorPairs.forEach(sohStatusChange => {
        // If there is not a quite timer already in place for the channel monitor pair, then add one
        // Using the default quiet interval. If a pair is quieted for a week, don't want it to be overwritten.
        const quietedUntilTime =
          (Date.now() + Number(channelMonitorToQuiet.quietDurationMs)) / MILLISECONDS_IN_SECOND;
        const quietedSohStatusChange: model.QuietedSohStatusChange = {
          stationName: channelMonitorToQuiet.stationName,
          sohMonitorType: sohStatusChange.monitorType,
          channelName: sohStatusChange.channelName,
          comment: channelMonitorToQuiet.comment,
          quietUntil: toOSDTime(quietedUntilTime),
          quietDuration: setDurationTime(
            channelMonitorToQuiet.quietDurationMs / MILLISECONDS_IN_SECOND
          ),
          quietedBy: userContext.userName
        };

        // Publish quiet channel changes called from UI mutation
        this.publishQuietedChange(quietedSohStatusChange).catch(e =>
          logger.error(`Failed to publish quieted change with SOH producer`)
        );
      });
    });
  }

  /**
   * Acknowledges soh statuses for the provided station names.
   * @param userContext the user context
   * @param stationNames the stations names to acknowledge SOH status
   * @param comment (optional) the comment for the acknowledgement
   */
  public publishAcknowledgeSohStatus(
    userContext: UserContext,
    stationNames: string[],
    comment?: string
  ): boolean {
    logger.info(
      `Publishing soh acknowledgment for station(s): ${stationNames.join(',')}` +
        ` by ${userContext.userName}${comment !== undefined ? ` : ${comment}` : ''}`
    );

    stationNames.forEach(stationName => {
      const stationSoh = this.getSohForStation(stationName);
      // Build the list of unacknowledged monitor/status pairs
      const uiStationSohs: model.SohStatusChange[] = [];
      stationSoh.channelSohs.forEach(channelSoh => {
        channelSoh.allSohMonitorValueAndStatuses.forEach(mvs => {
          if (mvs.hasUnacknowledgedChanges) {
            uiStationSohs.push({
              firstChangeTime: stationSoh.time,
              sohMonitorType: mvs.monitorType,
              changedChannel: channelSoh.channelName
            });
          }
        });
      });

      // Acknowledged all unacknowledged changes for the station
      if (uiStationSohs.length > 0) {
        const newAcknowledged = createAcknowledgedStatusChange(
          userContext.userName,
          stationName,
          uiStationSohs,
          comment
        );
        this.publishAcknowledgedChange(newAcknowledged).catch(e =>
          logger.error(`Failed to publish acknowledged change with SOH producer`)
        );
      }
    });

    return true;
  }

  /**
   * Calls OSD Historical SOH endpoint for Missing and or Lag data.
   * @param historicalSohInput Input is station name, start time, end time
   *  and SohMonitorType list (MISSING, LAG)
   * @returns UiHistoricalSoh
   */
  public async getHistoricalSohData(
    historicalSohInput: UiHistoricalSohInput
  ): Promise<UiHistoricalSoh> | undefined {
    // Handle undefined input time range
    if (!historicalSohInput) {
      throw new Error('Unable to retrieve historical soh data due to input');
    }

    // Handle undefined input channel ID list
    if (!historicalSohInput.stationName) {
      throw new Error(`Unable to retrieve historical soh data due to stationName`);
    }

    // Retrieve the request configuration for the service call
    const requestConfig = this.settings.backend.services.getHistoricalSohData.requestConfig;

    logger.debug(
      `Calling get Historical Soh query: ${JSON.stringify(
        historicalSohInput
      )} request: ${JSON.stringify(requestConfig)}`
    );

    const response: HttpResponse<UiHistoricalSoh> = await this.httpWrapper.request<UiHistoricalSoh>(
      requestConfig,
      historicalSohInput
    );

    // Test if we got back a legit response
    if (response && response.data) {
      if (response.data.monitorValues) {
        logger.debug(`Returning historical soh data size ${response.data.monitorValues.length}`);
      }
      return response.data;
    }
    return undefined;
  }

  /**
   * Calls OSD Historical SOH endpoint for acei data.
   * @param historicalAceiInput Input is station name, start time, end time
   *  and ACEI type
   * @returns UiHistoricalAcei
   */
  public async getHistoricalAceiData(
    historicalAceiInput: UiHistoricalAceiInput
  ): Promise<UiHistoricalAcei[]> | undefined {
    // Handle undefined input
    if (!historicalAceiInput) {
      throw new Error('Unable to retrieve historical acei data due to input');
    }

    // Handle undefined input station name
    if (!historicalAceiInput.stationName) {
      throw new Error(`Unable to retrieve historical soh data due to missing stationName`);
    }
    // TODO: Request OSD Java to accept start and end time as numbers like what is done
    // TODO: with historical SOH
    // TODO: Also at the same time fix type input field to match monitorType return definition
    const historicalAceiInputOSD: any = {
      stationName: historicalAceiInput.stationName,
      startTime: toOSDTime(historicalAceiInput.startTime / 1000),
      endTime: toOSDTime(historicalAceiInput.endTime / 1000),
      type: historicalAceiInput.type
    };
    // Retrieve the request configuration for the service call
    const requestConfig = this.settings.backend.services.getHistoricalAceiData.requestConfig;

    logger.debug(
      `Calling get Historical acei query: ${JSON.stringify(
        historicalAceiInputOSD
      )} request: ${JSON.stringify(requestConfig)}`
    );

    const response: HttpResponse<UiHistoricalAcei[]> = await this.httpWrapper.request<
      UiHistoricalAcei[]
    >(requestConfig, historicalAceiInputOSD);

    // Test if we got back a legit response
    if (response && response.data) {
      if (response.data.length > 0) {
        logger.debug(`Returning historical acei issues data size ${response.data.length}`);
      }
      return response.data;
    }
    return [];
  }

  /**
   * Handle and consume messages for the Ui soh KAFKA Topics
   * @param topic the topic
   * @param messages the messages
   */
  public consumeUiStationSohKafkaMessages(
    topic: string,
    messages: Immutable.List<StationAndStationGroupSoh>
  ) {
    new Promise((resolve, reject) => {
      if (topic !== undefined || messages !== undefined) {
        logger.debug(`Consuming messages ${messages.size} for topic '${topic}'`);
        if (topic === this.kafkaSettings.consumerTopics.uiStationSoh) {
          // Last time got messages from Kafka consumer, helps when to publish
          this.lastKafkaMessageTimestamp = Date.now();

          // Build a list of timing point B messages and send them to the Kafka producer
          const now = Date.now();
          messages.forEach(stationAndStationGroupSoh => {
            const isUpdateResponse = stationAndStationGroupSoh.isUpdateResponse;
            // See if this is a response to an ack or quiet notification
            if (stationAndStationGroupSoh.isUpdateResponse) {
              logger.info(
                `processing update response message for station ` +
                  `${stationAndStationGroupSoh.stationSoh[0].stationName}`
              );
            }

            this.stationGroups = stationAndStationGroupSoh.stationGroups;
            stationAndStationGroupSoh.stationSoh.forEach(s => {
              // Add the UiStationSoh to the queue to be sent to the UI.
              // If this is a new UiStationSoh entry based on the UUID.
              // If it is a duplicate or an update response to Ack/Quiet don't log the Timing Point B
              if (this.addSohForStation(s, isUpdateResponse) && !isUpdateResponse) {
                logger.timing(
                  `Timing point B: SOH object ${s.uuid} received in UI Backend at ` +
                    `${toOSDTime(now / MILLISECONDS_IN_SECOND)} SSAM creation time ${toOSDTime(
                      s.time / 1000
                    )}`
                );
              }
            });
            // If this is an update response message send it immediately to be more responsive.
            // Also sending only the update response message allows the UI to filter it and not
            // log Timing Pt C messages. If not filtered will skew the results.
            if (isUpdateResponse) {
              const responseMessage: StationAndStationGroupSoh = {
                stationGroups: stationAndStationGroupSoh.stationGroups,
                stationSoh: this.clearChannelSohs(stationAndStationGroupSoh.stationSoh),
                isUpdateResponse: true
              };
              this.publishUiStationAndStationSoh(responseMessage);
            }
          });

          // Log the timestamp of each StationGroup message
          if (messages && messages.size > 0) {
            const sg = messages.get(0).stationGroups[0];
            logger.debug(
              `StationGroup ${sg.stationGroupName}  creation time ${toOSDTime(sg.time / 1000)}`
            );
          }
        } else {
          logger.warn(`Received data for unknown topic: ${topic} ${messages.size}`);
        }
        resolve();
      }
    });
  }

  /**
   * Sends (produces) the message to acknowledged an SOH change to KAFKA.
   */
  public readonly publishAcknowledgedChange = async (
    acknowledge: model.AcknowledgedSohStatusChange
  ): Promise<void> => {
    // Publish on acknowledgement on topic
    await KafkaProducer.Instance().send(this.kafkaSettings.producerTopics.acknowledgedTopic, [
      { value: JSON.stringify(acknowledge) }
    ]);
    return;
  }

  /**
   * Sends (produces) the message to quiet an SOH change to KAFKA.
   */
  public readonly publishQuietedChange = async (
    quieted: model.QuietedSohStatusChange
  ): Promise<void> => {
    // Publish on quiet channel on topic
    await KafkaProducer.Instance().send(this.kafkaSettings.producerTopics.quietedTopic, [
      { value: JSON.stringify(quieted) }
    ]);
    return;
  }

  /**
   * Register the KAFKA consumer callbacks for topics.
   */
  public registerKafkaConsumerCallbacks() {
    // register the UI station SOH callbacks for the topics
    KafkaConsumer.Instance().registerKafkaConsumerCallbackForTopics<StationAndStationGroupSoh>(
      [this.kafkaSettings.consumerTopics.uiStationSoh],
      (topic, messages) => this.consumeUiStationSohKafkaMessages(topic, messages)
    );
  }

  /**
   * Is there any UiStationSoh entries queued to send
   */
  private hasUiDataToSend(): boolean {
    return this.latestQueuedSohData.size > 0;
  }
  /**
   * Publishing the StationAndStationGroupSoh to the UI via the subscription
   * @param stationAndStationGroup StationAndStationGroupSoh
   */
  private publishUiStationAndStationSoh(stationAndStationGroup: StationAndStationGroupSoh): void {
    // Publish changes (executes a promise)
    new Promise((resolve, reject) => {
      const settings = config.get('performanceMonitoring');
      this.pubsub
        .publish(settings.subscriptions.channels.sohStatus, {
          sohStatus: stationAndStationGroup
        })
        .catch(e => logger.error('Failed to publish SOH data: ', e));
      resolve();
    });
  }
  /**
   * Returns the most recent (latest) Station and StationGroup SOH.
   */
  private getLatestStationAndGroupSohWithEmptyChannels(): StationAndStationGroupSoh {
    const uiStationSohs: UiStationSoh[] = this.getLatestSohForAllStations();
    // Blank out the channelSohs for each station soh
    const updatedSohs = this.clearChannelSohs(uiStationSohs);
    return {
      stationGroups: this.stationGroups,
      stationSoh: updatedSohs,
      isUpdateResponse: false
    };
  }

  /**
   * Returns UiStationSoh entries with the channel soh cleared out
   */
  private clearChannelSohs(uiStationSohs: UiStationSoh[]): UiStationSoh[] {
    // Blank out the channelSohs for each station soh
    return uiStationSohs.map(soh => ({
      ...soh,
      channelSohs: []
    }));
  }

  /**
   * Gets the most recent (latest) SOH entry for all stations.
   */
  private getLatestSohForAllStations(): UiStationSoh[] {
    return Array.from(this.latestQueuedSohData.values());
  }

  /**
   * Clear the most recent (latest) SOH entry for all stations.
   */
  private clearLatestSohForAllStations(): void {
    this.latestQueuedSohData = Immutable.Map<string, UiStationSoh>();
  }
  /**
   * Add UiStationSoh to the map
   * @param uiStationSoh latest UiStationSoh to add to queue
   * @param isUpdateResponse need to update the cache and queue
   *                         if this is an update from an ack or quiet
   * @return boolean if added to queue and map. If UiStationSoh UUID is already
   * in the map do not add it.
   */
  private addSohForStation(uiStationSoh: UiStationSoh, isUpdateResponse: boolean): boolean {
    if (uiStationSoh) {
      // Check if this is a repeated UiStationSoh based on the UUID
      const mapUiStationSoh = this.kafkaQueuedSohData.get(uiStationSoh.stationName);
      if (isUpdateResponse || !mapUiStationSoh || mapUiStationSoh.uuid !== uiStationSoh.uuid) {
        this.kafkaQueuedSohData = this.kafkaQueuedSohData.set(
          uiStationSoh.stationName,
          uiStationSoh
        );
        // Add the UiStationSoh to the queue if not an update response from Ack or Quiet.
        // If it is an update response only replace an older UiStationSoh entry.
        if (!isUpdateResponse || this.latestQueuedSohData.has(uiStationSoh.stationName)) {
          this.latestQueuedSohData = this.latestQueuedSohData.set(
            uiStationSoh.stationName,
            uiStationSoh
          );
        }
        // Return true if not an update response
        return !isUpdateResponse;
      }
      // If we have already seen the UiStaionSoh entry log a warning and return false.
      logger.warn(`Duplicate UiStationSoh UUID ${uiStationSoh.uuid} found, dropping entry!`);
    }
    return false;
  }

  /**
   * Loads seed data from the backend
   */
  private initializeBackend() {
    sohMockBackend.initialize(this.httpWrapper.createHttpMockWrapper());
    sohMockBackend.getStationSohData().forEach(s => this.addSohForStation(s, false));
  }

  /**
   * Pushes new SOH data to client at the configured interval
   */
  private initializePublishingSohToClient() {
    setTimeout(() => {
      this.checkToSendSohStatusChanges();
    }, QUARTER_SECOND_MS);

    setInterval(() => {
      this.checkToSendSohStatusChanges();
    }, QUARTER_SECOND_MS);
  }

  /**
   * Initializes the processor.
   */
  private initialize() {
    try {
      logger.info(
        'Initializing the Data Acquisition SOH processor - Mock Enable: %s',
        this.settings.backend.mock.enable
      );

      // Load the Analyst Config intervals;
      this.BATCHING_DELAY_MS = ConfigurationProcessor.Instance().getSohUpdateIntervalForUser();
      this.DEFAULT_QUIET_INTERVAL_MS = ConfigurationProcessor.Instance().getSohDefaultQuietInterval();

      this.stationGroups = createStationGroupSohStatus(
        ConfigurationProcessor.Instance().getStationGroupNamesWithPriorities()
      );

      // populate map with empties
      const stations = ProcessingStationProcessor.Instance().getSohProcessingStations();
      stations.forEach(station => {
        this.addSohForStation(createEmptyStationSoh(station.name), false);
      });

      if (this.settings.backend.mock.enable) {
        this.initializeBackend();
      }
      // TODO - initialize blank/empty stations with no real SOH data
      this.initializePublishingSohToClient();
    } catch (error) {
      gatewayLogger.error(`Failed to initialize the SOH processor: ${error}`);
      throw error;
    }
  }
}
