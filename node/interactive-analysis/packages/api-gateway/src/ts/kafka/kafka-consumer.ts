import { dateToISOString, epochSecondsNow, toOSDTime } from '@gms/common-util';
import config from 'config';
import * as Immutable from 'immutable';
import { Consumer, InstrumentationEvent, Kafka, logLevel as LogLevel } from 'kafkajs';
import includes from 'lodash/includes';
import join from 'lodash/join';
import { gatewayLogger as logger } from '../log/gateway-logger';
import {
  AbstractKafka,
  configureSnappyCompression,
  createConsumer,
  createKafka,
  RETRY_BROKER_DELAY
} from './kafka';
import { ConsumerCallback, KafkaSettings, KafkaStatus } from './types';

// configure snappy compression
configureSnappyCompression();

/**
 * KAFKA Consumer implementation using KAFKA JS
 */
export class KafkaConsumer extends AbstractKafka {
  /** The singleton instance */
  private static instance: KafkaConsumer;

  /**
   * Creates an KAFKA Consumer instance.
   *
   * @param clientId the unique client id
   * @param brokers the registered brokers
   * @param groupId the unique group id
   * @param topics the unique topics to subscribe too
   * @param connectionTimeout the connection timeout (default 1000)
   * @param logLevel the log level for KAFKA (default WARN)
   */
  public static readonly createKafkaConsumer = (
    kafkaSettings: KafkaSettings,
    topics: string[],
    logLevel: LogLevel = LogLevel.WARN
  ): KafkaConsumer => {
    const kafka = createKafka(
      kafkaSettings.clientId,
      kafkaSettings.brokers,
      kafkaSettings.connectionTimeout ? kafkaSettings.connectionTimeout : 1000,
      logLevel
    );
    const consumer = createConsumer(
      kafka,
      kafkaSettings.groupId,
      kafkaSettings.maxWaitTimeInMs,
      kafkaSettings.heartbeatInterval
    );
    return new KafkaConsumer(kafka, consumer, topics);
  }

  /**
   * Returns the singleton instance of the KAFKA consumer.
   * @returns the instance of the KAFKA consumer
   */
  public static Instance(): KafkaConsumer {
    if (KafkaConsumer.instance === undefined) {
      logger.debug(`Instantiating the KAFKA consumer`);

      // Load configuration settings
      const kafkaSettings: KafkaSettings = config.get('kafka');
      logger.info(`Configured KAFKA consumer clientId ${kafkaSettings.clientId}`);
      logger.info(`Configured KAFKA consumer brokers ${join(kafkaSettings.brokers, ', ')}`);
      logger.info(`Configured KAFKA consumer groupId ${kafkaSettings.groupId}`);
      logger.info(`Configured KAFKA Consumer connectionTimeout ${kafkaSettings.connectionTimeout}`);
      logger.info(`Configured KAFKA Consumer maxWaitTimeInMs ${kafkaSettings.maxWaitTimeInMs}`);

      // create the consumer
      const topics: string[] = [
        kafkaSettings.consumerTopics.systemMessagesTopic,
        kafkaSettings.consumerTopics.uiStationSoh
      ];
      logger.info(`Configured KAFKA consumer topics ${join(topics, ', ')}`);

      KafkaConsumer.instance = KafkaConsumer.createKafkaConsumer(kafkaSettings, topics);
    }
    return KafkaConsumer.instance;
  }

  /** The KAFKA consumer instance */
  private readonly consumer: Consumer;

  /**
   * The registered consumer callbacks
   */
  private kafkaConsumerCallbacks: Immutable.Map<string, ConsumerCallback<any>>;

  /**
   * Constructor.
   *
   * @param kafka the kafka instance
   * @param consumer the kafka consumer
   * @param topics the unique topics to subscribe too
   */
  private constructor(kafka: Kafka, consumer: Consumer, topics: string[]) {
    super(kafka, topics);
    this.consumer = consumer;
    this.kafkaConsumerCallbacks = Immutable.Map();
  }

  /**
   * Registers a consumer callback for a topic.
   *
   * @param topic the topic
   * @param callback the callback
   */
  public readonly registerKafkaConsumerCallbackForTopic = <T>(
    topic: string,
    callback: ConsumerCallback<T>
  ): void => {
    if (topic === undefined || topic === null) {
      logger.error(`Invalid topic, failed to register consumer callback for topic`);
      return;
    }

    if (callback === undefined || callback === null) {
      logger.error(`Invalid callback, failed to register consumer callback for topic`);
      return;
    }

    if (!includes(this.topics, topic)) {
      logger.error(
        `Registering KAFKA consumer callback for topic that has not been configured with the consumer: ${topic}`
      );
    }

    if (this.kafkaConsumerCallbacks.has(topic)) {
      logger.warn(`Overwriting an existing registered KAFKA consumer callback for topic: ${topic}`);
    }

    logger.info(`Registering KAFKA consumer callback for topic: ${topic}`);
    this.kafkaConsumerCallbacks = this.kafkaConsumerCallbacks.set(topic, callback);
  }

  /**
   * Registers a consumer callback for multiple topic.
   *
   * @param topic the topic
   * @param callback the callback
   */
  public readonly registerKafkaConsumerCallbackForTopics = <T>(
    topics: string[],
    callback: ConsumerCallback<T>
  ): void => {
    topics.forEach(t => this.registerKafkaConsumerCallbackForTopic(t, callback));
  }

  /**
   * Un-registers a consumer callback for a given topic.
   *
   * @param topic the topic
   */
  public readonly unregisterKafkaConsumerCallbackForTopic = (topic: string): void => {
    if (this.kafkaConsumerCallbacks.has(topic)) {
      logger.info(`Un-registering KAFKA consumer callback for topic: ${topic}`);
      this.kafkaConsumerCallbacks = this.kafkaConsumerCallbacks.remove(topic);
    }
  }

  /**
   * Start and initializes the KAFKA consumer.
   */
  public readonly start = async (): Promise<void> => {
    if (this.getStatus() === KafkaStatus.NOT_INITIALIZED) {
      logger.info('Starting and initializing KAFKA consumer');
      this.updateStatus(KafkaStatus.CONNECTING);
      this.updateStatusHistoryInformation('consumer.connecting', KafkaStatus.CONNECTING);
      await this.initializeAndConnectKafkaConsumer();
    } else {
      logger.warn(`KAFKA consumer has already been started`);
    }
  }

  /**
   * Stop the KAFKA consumer.
   */
  public readonly stop = async () => {
    if (this.consumer) {
      logger.info('Stopping KAFKA consumer');

      clearTimeout(this.reconnectTimeoutId);
      this.reconnectTimeoutId = undefined;

      this.updateStatus(KafkaStatus.STOPPED);
      this.updateStatusHistoryInformation('consumer.stopped', KafkaStatus.STOPPED);

      await this.consumer
        .disconnect()
        .catch(e => logger.error(`Failed to disconnect KAFKA consumer ${e}`));

      await this.consumer.stop().catch(e => logger.error(`Failed to stop KAFKA consumer ${e}`));
    }
  }

  /**
   * Initialize and connect the KAFKA consumer.
   */
  private readonly initializeAndConnectKafkaConsumer = async (): Promise<void> => {
    if (this.getStatus() !== KafkaStatus.STARTED) {
      await this.run()
        .then(() => {
          clearTimeout(this.reconnectTimeoutId);
          this.reconnectTimeoutId = undefined;
          this.updateStatus(KafkaStatus.STARTED);
          this.updateStatusHistoryInformation('consumer.started', KafkaStatus.STARTED);
        })
        .then(() => {
          this.updateStatus(KafkaStatus.CONNECTED);
          this.updateStatusHistoryInformation('consumer.connected', KafkaStatus.CONNECTED);
        })
        .catch(e => {
          logger.warn(`Connection to kafka broker for consumer failed, retrying...`);

          this.updateStatus(KafkaStatus.RECONNECTING);
          this.updateStatusHistoryInformation('consumer.error', e);

          clearTimeout(this.reconnectTimeoutId);
          this.reconnectTimeoutId = undefined;

          setTimeout(async () => {
            await this.initializeAndConnectKafkaConsumer();
          }, RETRY_BROKER_DELAY);
        });
    } else {
      logger.info(`KAFKA consumer is already initialized and connected`);
    }
  }

  /**
   * Configures the event listeners for the consumer.
   */
  private readonly configureEventListeners = () => {
    this.consumer.on(this.consumer.events.CONNECT, (e: InstrumentationEvent<any>) => {
      const info = `${this.convertTimestamp(e.timestamp)} Consumer connected: ${JSON.stringify(
        e.payload
      )}`;
      this.updateStatus(KafkaStatus.CONNECTED);
      this.updateStatusHistoryInformation(this.consumer.events.CONNECT, info);
      logger.info(info);
    });

    this.consumer.on(
      this.consumer.events.HEARTBEAT,
      (
        e: InstrumentationEvent<{ groupId: string; memberId: string; groupGenerationId: string }>
      ) => {
        const info = `${this.convertTimestamp(e.timestamp)} Consumer heartbeat: ${JSON.stringify(
          e.payload
        )}`;
        this.updateStatusHistoryInformation(this.consumer.events.HEARTBEAT, info);
      }
    );

    this.consumer.on(
      this.consumer.events.REQUEST,
      (
        e: InstrumentationEvent<{
          broker: string;
          clientId: string;
          correlationId: string;
          size: number;
          createdAt: number;
          sentAt: number;
          pendingDuration: number;
          duration: number;
          apiName: string;
          apiKey: string;
          apiVersion: string;
        }>
      ) => {
        /*
        const info = `${this.convertTimestamp(e.timestamp)} Consumer request: ${JSON.stringify(
          e.payload
        )}`;
        this.updateStatusHistoryInformation(this.consumer.events.REQUEST, info);
        */
      }
    );

    this.consumer.on(
      this.consumer.events.REQUEST_TIMEOUT,
      (
        e: InstrumentationEvent<{
          broker: string;
          clientId: string;
          correlationId: string;
          createdAt: number;
          sentAt: number;
          pendingDuration: number;
          apiName: string;
          apiKey: string;
          apiVersion: string;
        }>
      ) => {
        const info = `${this.convertTimestamp(
          e.timestamp
        )} Consumer request timeout: ${JSON.stringify(e.payload)}`;
        this.updateStatusHistoryInformation(this.consumer.events.REQUEST_TIMEOUT, info);
        logger.warn(info);
      }
    );

    this.consumer.on(
      this.consumer.events.REQUEST_QUEUE_SIZE,
      (e: InstrumentationEvent<{ broker: string; clientId: string; queueSize: number }>) => {
        const info = `${this.convertTimestamp(
          e.timestamp * 1000
        )} Consumer request queue size: ${JSON.stringify(e.payload)}`;
        this.updateStatusHistoryInformation(this.consumer.events.REQUEST_QUEUE_SIZE, info);
      }
    );

    this.consumer.on(this.consumer.events.DISCONNECT, (e: InstrumentationEvent<any>) => {
      const info = `${this.convertTimestamp(e.timestamp)} Consumer disconnected: ${JSON.stringify(
        e.payload
      )}`;
      this.updateStatus(KafkaStatus.DISCONNECTED);
      this.updateStatusHistoryInformation(this.consumer.events.DISCONNECT, info);
      logger.warn(info);
    });

    this.consumer.on(
      this.consumer.events.CRASH,
      (e: InstrumentationEvent<{ error: any; groupId: string }>) => {
        const info = `${this.convertTimestamp(e.timestamp)} Consumer crashed: ${JSON.stringify(
          e.payload
        )}`;
        this.updateStatus(KafkaStatus.DISCONNECTED);
        this.updateStatusHistoryInformation(this.consumer.events.CRASH, info);
        logger.error(info);
      }
    );

    this.consumer.on(this.consumer.events.STOP, (e: InstrumentationEvent<any>) => {
      const info = `${this.convertTimestamp(e.timestamp)} Consumer stopped: ${JSON.stringify(
        e.payload
      )}`;
      this.updateStatus(KafkaStatus.STOPPED);
      this.updateStatusHistoryInformation(this.consumer.events.STOP, info);
      logger.info(info);
    });
  }

  /**
   * Runs the KAFKA consumer
   */
  private readonly run = async (): Promise<void> => {
    // connect the consumer
    await this.consumer.connect();

    // subscribe topics
    this.topics.forEach(async topic => {
      await this.consumer.subscribe({ topic, fromBeginning: false });
    });

    /**
     * Handled the consumed messages for a given topic
     * @param topic the topic
     * @param messages the messages
     */
    const handleMessages = async (topic: string, messages: Immutable.List<string>): Promise<void> =>
      new Promise((resolve, reject) => {
        if (
          this.kafkaConsumerCallbacks.has(topic) &&
          this.kafkaConsumerCallbacks.get(topic) !== undefined
        ) {
          this.kafkaConsumerCallbacks.get(topic)(topic, messages);
        } else {
          logger.warn(`No consumer callback for KAFKA consumer configured for topic: ${topic}`);
        }
        resolve();
      });

    /* configure event listeners */
    this.configureEventListeners();

    await this.consumer.run({
      eachBatch: async ({
        batch,
        resolveOffset,
        heartbeat,
        uncommittedOffsets,
        isRunning,
        isStale
      }) => {
        // Check if there is no messages to process
        if (!batch || !batch.messages || batch.messages.length === 0) {
          return;
        }

        let messages: Immutable.List<string> = Immutable.List();
        try {
          for (const message of batch.messages) {
            resolveOffset(message.offset);
            messages = messages.push(JSON.parse(message.value.toString()));
          }
          const info = `Last consumed ${messages.size} message(s) at ${toOSDTime(
            epochSecondsNow()
          )}`;
          this.updateStatusHistoryInformation('consumer.received', info);
          await handleMessages(batch.topic, messages);
        } catch (e) {
          const info = `Failed to consume ${messages.size} message(s) at ${dateToISOString(
            new Date()
          )} ${e}`;
          this.updateStatus(KafkaStatus.ERROR);
          this.updateStatusHistoryInformation('consumer.error', info);
          logger.error(`Failed to consume KAFKA message(s) ${e}`);
        }
      }
    });
  }
}
