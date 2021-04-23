import { KeyValue, MILLISECONDS_IN_SECOND, toOSDTime } from '@gms/common-util';
import * as Immutable from 'immutable';
import {
  CompressionCodecs,
  CompressionTypes,
  Consumer,
  Kafka,
  LoggerEntryContent,
  logLevel as LogLevel,
  Producer
} from 'kafkajs';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { IKafka, KafkaStatus } from './types';

/** Delay between retrying connections to the kafka broker if a connection attempt fails */
export const RETRY_BROKER_DELAY = 5000;

/**
 * Configure SNAPPY Compression for KAFKA JS.
 */
export const configureSnappyCompression = () => {
  // tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
  const SnappyCodec = require('kafkajs-snappy');

  // Enable the Snappy Compression Codec
  CompressionCodecs[CompressionTypes.Snappy] = SnappyCodec;
};

/**
 * Convert KAFKA JS log level to Winston log level.
 *
 * @param level the KAFKA JS log level
 */
export const toWinstonLogLevel = (level: LogLevel): string => {
  switch (level) {
    case LogLevel.ERROR:
      return 'error';
    case LogLevel.NOTHING:
      return 'error';
    case LogLevel.WARN:
      return 'warn';
    case LogLevel.INFO:
      return 'info';
    default:
      return 'debug';
  }
};

/**
 * Creates a KAFKA JS Log Creator
 *
 * @param namespace the namespace
 * @param level the log level
 * @param label the label
 * @param log the log entry
 */
export const kafkaLogCreator = (
  namespace: string,
  level: LogLevel,
  label: string,
  log: LoggerEntryContent
) => {
  const { message, ...extra } = log;
  logger.log({
    level: toWinstonLogLevel(level),
    message,
    extra
  });
};

/**
 * Create the KAFKA instance.
 *
 * @param clientId the unique client id
 * @param brokers the registered brokers
 * @param connectionTimeout the connection timeout (default 1000)
 * @param logLevel the log level for KAFKA (default WARN)
 */
export const createKafka = (
  clientId: string,
  brokers: string[],
  connectionTimeout: number = 1000,
  logLevel: LogLevel = LogLevel.WARN
): Kafka =>
  new Kafka({
    clientId,
    brokers,
    connectionTimeout,
    logLevel,
    logCreator: () => ({ namespace, level, label, log }) => {
      kafkaLogCreator(namespace, level, label, log);
    }
  });

/**
 * Create the KAFKA consumer.
 *
 * @param kafka the kafka instance
 * @param groupId the unique group id
 */
export const createConsumer = (
  kafka: Kafka,
  groupId: string,
  maxWaitTimeInMs: number,
  heartbeatInterval: number
): Consumer => kafka.consumer({ groupId, maxWaitTimeInMs, heartbeatInterval });

/**
 * Create the KAFKA producer.
 *
 * @param kafka the kafka instance
 */
export const createProducer = (kafka: Kafka): Producer => kafka.producer({});

/**
 * Abstract class that defines the common implementation
 * for a KAFKA Consumer or Producer.
 */
export abstract class AbstractKafka implements IKafka {
  /** The amount of history entries to keep */
  protected static historySize: number = 20;

  /** The KAFKA instance */
  protected readonly kafka: Kafka;

  /** The KAFKA topics */
  protected readonly topics: string[];

  /** The date since the KAFKA instance last established a connection */
  private upTime: Date | undefined;

  /** The KAFKA status */
  private status: KafkaStatus;

  /** The KAFKA status history information */
  private statusHistoryInformation: Immutable.List<KeyValue<string, string | Error>>;

  /** The unique timeout id */
  protected reconnectTimeoutId: NodeJS.Timeout;

  /**
   * Constructor.
   *
   * @param kafka the kafka instance
   * @param topics the unique topics to subscribe too
   */
  public constructor(kafka: Kafka, topics: string[]) {
    this.kafka = kafka;
    this.topics = topics;

    this.status = KafkaStatus.NOT_INITIALIZED;
    this.statusHistoryInformation = Immutable.List();
    this.updateStatusHistoryInformation('not.initialized', KafkaStatus.NOT_INITIALIZED);
    this.reconnectTimeoutId = undefined;
  }

  /**
   * Returns the KAFKA status.
   */
  public readonly getStatus = (): KafkaStatus => this.status;

  /**
   * Returns the KAFKA status history information.
   */
  public readonly getStatusHistoryInformation = (): Immutable.List<
    KeyValue<string, string | Error>
  > => this.statusHistoryInformation.reverse()

  /**
   * Returns the KAFKA status history information (information about the events that have been issued) as an object.
   * Only keeps the latest information.
   */
  public readonly getStatusHistoryInformationAsObject = (): Object => {
    // convert to an Object so it can be converted to JSON
    const mapToObj = <V>(list: Immutable.List<KeyValue<string, V>>) => {
      const obj = {};
      list.forEach((value, key) => {
        obj[key] = `${value.id} ${String(value.value)}`;
      });
      return obj;
    };
    return mapToObj<string | Error>(this.getStatusHistoryInformation());
  }

  /**
   * Returns the Date the KAFKA instance has been up and running (connected) since
   */
  public readonly getUpTime = (): Date => this.upTime;

  /**
   * Returns the number of seconds the KAFKA instance has been up and running (connected)
   */
  public readonly getUpTimeSeconds = (): number => {
    if (this.status === KafkaStatus.CONNECTED && this.upTime) {
      const now = new Date();
      // calculate the number of seconds
      return (now.getTime() - this.upTime.getTime()) / 1000;
    }
    return 0;
  }

  /**
   * Returns true if connected; false otherwise.
   */
  public readonly connected = (): boolean => this.status === KafkaStatus.CONNECTED;

  /**
   * Start the KAFKA consumer/producer.
   */
  public abstract start(): Promise<void>;

  /**
   * Stop the KAFKA consumer/producer.
   */
  public abstract stop(): Promise<void>;

  /**
   * Convert KAFKA timestamp to a human readable format
   */
  protected readonly convertTimestamp = (timestampMs: number): string =>
    toOSDTime(timestampMs / MILLISECONDS_IN_SECOND)

  /**
   * Updates the status of the KAFKA instance.
   */
  protected readonly updateStatus = (status: KafkaStatus): void => {
    this.upTime =
      status === KafkaStatus.CONNECTED && this.status !== KafkaStatus.CONNECTED
        ? new Date()
        : this.status === KafkaStatus.CONNECTED
        ? this.upTime
        : undefined;
    this.status = status;
  }

  /**
   * Updates the status history information.
   */
  protected readonly updateStatusHistoryInformation = (id: string, value: string | Error): void => {
    this.statusHistoryInformation = this.statusHistoryInformation.push({ id, value });
    // only keep a limited amount of history
    while (this.statusHistoryInformation.size > AbstractKafka.historySize) {
      this.statusHistoryInformation = this.statusHistoryInformation.delete(0);
    }
  }
}
