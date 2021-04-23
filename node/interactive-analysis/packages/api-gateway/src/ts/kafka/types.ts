import { KeyValue } from '@gms/common-util';
import * as Immutable from 'immutable';

/**
 * Consumer Callback Function Type
 */
export type ConsumerCallback<T> = (topic: string, messages: Immutable.List<T>) => void;

/**
 * The KAFKA Status States
 */
export enum KafkaStatus {
  /** KAFKA has not been initialized */
  NOT_INITIALIZED = 'Not Initialized',
  /** KAFKA is connecting */
  CONNECTING = 'Connecting',
  /** KAFKA failed to connect and is reconnecting */
  RECONNECTING = 'Reconnecting',
  /** KAFKA has started */
  STARTED = 'Started',
  /** KAFKA is connected */
  CONNECTED = 'Connected',
  /** KAFKA has stopped */
  STOPPED = 'Stopped',
  /** KAFKA has disconnected */
  DISCONNECTED = 'Disconnected',
  /** KAFKA has as an error */
  ERROR = 'Error',
  /** KAFKA has crashed */
  CRASHED = 'Crashed'
}

/**
 * Defines the base interface for a KAFKA Consumer or Producer.
 */
// tslint:disable-next-line: interface-name
export interface IKafka {
  /**
   * Returns the KAFKA status.
   */
  getStatus(): KafkaStatus;

  /**
   * Returns the KAFKA status history information.
   */
  getStatusHistoryInformation(): Immutable.List<KeyValue<string, string | Error>>;

  /**
   * Returns the KAFKA status history information as an object
   */
  getStatusHistoryInformationAsObject(): {};

  /**
   * Returns the up time. The time in which KAFKA last established a good connection.
   */
  getUpTime(): Date;

  /**
   * Returns the number of seconds the KAFKA instance has been up and running (connected)
   */
  getUpTimeSeconds(): number;

  /**
   * Returns true if connected; false otherwise.
   */
  connected(): boolean;

  /**
   * Start the KAFKA consumer/producer.
   */
  start(): Promise<void>;

  /**
   * Stop the KAFKA consumer/producer.
   */
  stop(): Promise<void>;
}

/** Defines the KAFKA settings for the KAFKA Consumer */
export interface KafkaSettings {
  clientId: string;
  groupId: string;
  brokers: string[];
  connectionTimeout: number;
  maxWaitTimeInMs: number;
  heartbeatInterval: number;
  consumerTopics: {
    systemMessagesTopic: string;
    uiStationSoh: string;
  };
  producerTopics: {
    acknowledgedTopic: string;
    quietedTopic: string;
  };
}
