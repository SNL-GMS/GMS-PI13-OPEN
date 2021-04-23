import {
  CommonMutations,
  CommonTypes,
  ConfigurationQueries,
  ConfigurationTypes
} from '@gms/common-graphql';
import { ApolloQueryResult } from 'apollo-client';
import { FetchResult } from 'apollo-link';
import log4javascript from 'log4javascript';
import { Client } from './apollo/types';

/**
 * Filters out `undefined` and empty messages
 * @param messages the messages to filter
 * @return the filtered messages
 */
const filterEmptyMessages = (...messages: string[]) =>
  // filter out undefined and empty messages
  messages ? messages.filter(msg => msg && msg.trim && msg.trim().length > 0) : [];

/**
 * Logger class, used throughout the UI for logging
 * Logs are sent to the API gateway.
 */
export class UILogger {
  /** The singleton instance of the logger */
  private static INSTANCE: UILogger;

  /** The pop up appender  */
  private readonly popUpAppender: log4javascript.PopUpAppender;

  /** The apollo client for sending logs to the gateway */
  private client: Client | undefined;

  /** The log4javascript logger */
  private readonly logger: log4javascript.Logger;

  /** Number of max messages stored before purge begins */
  private readonly maxMessages: number = 1000;

  /** The configured log level -> determines the log levels to log */
  private logLevel: CommonTypes.LogLevel = CommonTypes.LogLevel.info;

  /** returns the logger instance */
  public static Instance() {
    return UILogger.INSTANCE || (UILogger.INSTANCE = new UILogger());
  }

  private constructor() {
    // create a Pop Up Appender with default options
    this.popUpAppender = new log4javascript.PopUpAppender();

    // change the desired configuration options
    this.popUpAppender.setNewestMessageAtTop(true);
    this.popUpAppender.setComplainAboutPopUpBlocking(true);
    this.popUpAppender.setUseOldPopUp(true);
    this.popUpAppender.setReopenWhenClosed(true);
    this.popUpAppender.setScrollToLatestMessage(true);
    this.popUpAppender.setFocusPopUp(false);
    this.popUpAppender.setInitiallyMinimized(true);
    this.popUpAppender.setMaxMessages(this.maxMessages);
    this.popUpAppender.hide();

    // initialize the logger
    this.logger = log4javascript.getLogger('logger');
    this.logger.addAppender(this.popUpAppender);
    this.logger.setLevel(log4javascript.Level.ALL);
  }
  /**
   * Sets the apollo client, which is used to send logs to the gateway.
   *
   * @param client the apollo client
   */
  public setClient(client: Client | undefined): void {
    this.client = client;
    // TODO Currently the log level configuration will only be retrieved when the client is set
    // TODO Update to check the configuration at runtime once it can be changed on fly
    this.getLogLevelConfiguration(client)
      .then(level => {
        this.logLevel = level;
      })
      .catch();
  }

  /**
   * Shows the log pop out window.
   */
  public showLogPopUpWindow(): void {
    this.popUpAppender.show();
  }

  /**
   * General log
   * @param message type string message to be logged
   */
  public log(...messages: string[]): void {
    this.info(...messages);
  }

  /**
   * Debug log
   * @param message type string message to be logged
   */
  public debug(...messages: string[]): void {
    new Promise(async (resolve, reject) => {
      await this.logToClient(log4javascript.Level.DEBUG, ...messages);
      await this.logToServer(CommonTypes.LogLevel.debug, ...messages);
      resolve();
    });
  }

  /**
   * Info log
   * @param message type string message to be logged
   */
  public info(...messages: string[]): void {
    new Promise(async (resolve, reject) => {
      await this.logToClient(log4javascript.Level.INFO, ...messages);
      await this.logToServer(CommonTypes.LogLevel.info, ...messages);
      resolve();
    });
  }

  /**
   * Timing point log
   * @param message type string message to be logged
   */
  public timing(...messages: string[]): void {
    new Promise(async (resolve, reject) => {
      await this.logToServer(CommonTypes.LogLevel.timing, ...messages);
      resolve();
    });
  }

  /**
   * Warning log
   * @param message type string message to be logged
   */
  public warn(...messages: string[]): void {
    new Promise(async (resolve, reject) => {
      const messagesToConsole = filterEmptyMessages(...messages);
      if (messagesToConsole && messagesToConsole.length > 0) {
        // tslint:disable-next-line: no-console
        console.warn(messagesToConsole.join('\n'));
      }
      await this.logToClient(log4javascript.Level.WARN, ...messages);
      await this.logToServer(CommonTypes.LogLevel.warn, ...messages);
      resolve();
    });
  }

  /**
   * Error log
   * @param message type string message to be logged
   */
  public error(...messages: string[]): void {
    new Promise(async (resolve, reject) => {
      const messagesToConsole = filterEmptyMessages(...messages);
      if (messagesToConsole && messagesToConsole.length > 0) {
        // tslint:disable-next-line: no-console
        console.error(messagesToConsole.join('\n'));
      }
      await this.logToClient(log4javascript.Level.ERROR, ...messages);
      await this.logToServer(CommonTypes.LogLevel.error, ...messages);
      resolve();
    });
  }

  /**
   * Data log
   * @param message type string message to be logged
   */
  public data(...messages: string[]): void {
    new Promise(async (resolve, reject) => {
      await this.logToServer(CommonTypes.LogLevel.data, ...messages);
      resolve();
    });
  }

  /**
   * Performance logger formatter
   * @param action top level action (signalDetectionsByStation, filterChannelSegment, etc)
   * @param step stop in action (enteringResolver, returningFromServer, etc)
   * @param identifier unique id for object being worked (sd id, event id, channel segment with filter id, etc)
   */
  public performance(action: string, step: string, identifier?: string): void {
    new Promise(async (resolve, reject) => {
      const message = identifier
        ? `Action:${action} Step:${step} ID:${identifier}`
        : `Action:${action} Step:${step}`;
      await this.logToServer(CommonTypes.LogLevel.data, message);
      resolve();
    });
  }

  /**
   * Determine if a message should be logged based on it's log level and the configured
   * log level. Only log the message if the configured log level is greater than or equal to
   * the message's log level. Returns true if the message should be logged; false otherwise.
   *
   * @param level the log level to check
   */
  private shouldLogMessageForLogLevel(level: log4javascript.Level | CommonTypes.LogLevel): boolean {
    return (
      // checking the index is sufficient because the log levels are in ascending order where
      // the most important starts at zero (ex. error=0 and debug=6)
      Object.keys(CommonTypes.LogLevel).indexOf(this.logLevel) >=
      Object.keys(CommonTypes.LogLevel).indexOf(level.toString().toLowerCase())
    );
  }

  /**
   * Logs to the log4javascript logger
   *
   * @param level the log level
   * @param messages the messages to log
   */
  private async logToClient(level: log4javascript.Level, ...messages: string[]): Promise<void> {
    await new Promise((resolve, reject) => {
      // only log to the client if the configured log level is greater than or equal to the message to be logged
      if (this.shouldLogMessageForLogLevel(level)) {
        const messagesToLog = filterEmptyMessages(...messages);
        if (messagesToLog && messagesToLog.length > 0) {
          // filter out any undefined of empty messages
          this.logger.log(level, messagesToLog);
        }
      }
      resolve();
    });
  }

  /**
   * Performs a query to fetch the configuration for the log level.
   *
   * @param client apollo client
   * @param variables mutation variables
   * @returns the configured log level
   */
  private readonly getLogLevelConfiguration = async (
    client: Client
  ): Promise<CommonTypes.LogLevel> => {
    if (this.client) {
      const level = await client
        .query<{ uiAnalystConfiguration: ConfigurationTypes.AnalystConfiguration }>({
          query: ConfigurationQueries.uiConfigurationQuery
        })
        .then(
          (
            result: ApolloQueryResult<{
              uiAnalystConfiguration: ConfigurationTypes.AnalystConfiguration;
            }>
          ) =>
            // return the log level retrieved from the gateway
            result.data.uiAnalystConfiguration.logLevel
        )
        .catch(e => {
          // tslint:disable-next-line: no-console
          console.error(
            `Failed to fetch log level configuration; setting log level to 'info' by default`
          );
          return CommonTypes.LogLevel.info;
        });
      const msg = `Configured log level = ${level}`;
      console.info(msg); // tslint:disable-line: no-console
      this.logger.log(log4javascript.Level.INFO, [msg]);
      return level;
    }
    this.logger.warn(
      `Apollo client has not been initialized; setting log level to 'info' by default`
    );
    return CommonTypes.LogLevel.info;
  }

  /**
   * Performs a clientLog mutation.
   *
   * @param client apollo client
   * @param variables mutation variables
   */
  private readonly clientLogMutation = async (
    client: Client,
    variables: CommonTypes.ClientLogMutationArgs
  ): Promise<FetchResult<string>> => {
    if (this.client) {
      return client.mutate<string>({
        variables,
        fetchPolicy: 'no-cache',
        mutation: CommonMutations.clientLogMutation
      });
    }
    return undefined;
  }

  /**
   * Handles the mutation to the GraphQL Server
   * @param logLevel the log level
   * @param messages messages to log
   */
  private async logToServer(logLevel: CommonTypes.LogLevel, ...messages: string[]): Promise<void> {
    await new Promise(async (resolve, reject) => {
      if (this.client) {
        // only log to the server if the configured log level is greater than or equal to the message to be logged
        if (this.shouldLogMessageForLogLevel(logLevel)) {
          const messagesToLog = filterEmptyMessages(...messages);
          if (messagesToLog && messagesToLog.length > 0) {
            const time = new Date().toISOString();
            const variables: CommonTypes.ClientLogMutationArgs = {
              logs: messagesToLog.map(message => ({
                logLevel,
                message,
                time
              }))
            };
            await this.clientLogMutation(this.client, variables).catch(e => {
              this.logger.warn(`Failed to log to server: ${e.message}`);
            });
          }
        }
      } else {
        this.logger.warn('Apollo client has not been initialized; unable to send logs to server');
      }
      resolve();
    });
  }
}
