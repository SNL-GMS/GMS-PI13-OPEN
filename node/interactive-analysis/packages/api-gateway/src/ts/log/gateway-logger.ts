import { CommonTypes } from '@gms/common-graphql';
import config from 'config';
import fs from 'fs';
import logform from 'logform';
import * as Winston from 'winston';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const winston = require('winston');

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
require('winston-daily-rotate-file');

/** The log directory */
const logDir = 'logs';

/** The daily log file name */
const logFile = 'gateway';

/** The daily client log file name */
const clientLogFile = `${logFile}-client`;

/** The daily timing log file name */
const timingLogFile = `${logFile}-timing`;

/** The daily error log file name */
const errorLogFile = `${logFile}-error`;

/** The defined log level value and color for each defined level. */
const levelsAndColors = {
  levels: {
    error: 0,
    warn: 1,
    client: 2,
    info: 3,
    timing: 4,
    data: 5,
    debug: 6
  },
  colors: {
    // capitalized, because string output will be capitalized for each level
    ERROR: 'red',
    WARN: 'yellow',
    CLIENT: 'brightBlue',
    INFO: 'green',
    TIMING: 'brightCyan',
    DATA: 'magenta',
    DEBUG: 'blue'
  }
};

/** The daily max file size */
const dailyMaxFileSize = '50m';

/** The max number of days to keep daily files */
const dailyMaxFiles = '14d';

/** Color formatter */
const colorFormatter: logform.Colorizer = Winston.format.colorize({
  colors: levelsAndColors.colors,
  all: true
});

/** Regular expression used to colorize the client log levels */
const colorizeClientLevelRegExp = new RegExp(
  `(${Object.values(CommonTypes.LogLevel)
    .join('|')
    .toUpperCase()}):`
);

/**
 * Pretty print the log output
 * @param colorize (false) true if the pretty printed message levels should be colorized
 * @param isClientLogFile (false) true if formatting the message for the client log file
 */
const prettyPrint = (colorize: boolean, isClientLogFile: boolean = false) => info => {
  const level = info.level.toUpperCase();
  return `${new Date().toISOString()} - ${
    !isClientLogFile ? (colorize ? `${colorFormatter.colorize(level, level)}: ` : `${level}: `) : ''
  }${
    colorize && info.level === CommonTypes.LogLevel.client
      ? info.message.replace(
          colorizeClientLevelRegExp,
          (match, capture) => `${colorFormatter.colorize(capture, capture)}:`
        )
      : info.message
  }`;
};

/**
 * Winston log formatter
 * @param colorize (false) true if the pretty printed message levels should be colorized
 * @param isClientLogFile (false) true if formatting the message for the client log file
 */
const formatter = (colorize: boolean = false, isClientLogFile: boolean = false): logform.Format =>
  Winston.format.combine(Winston.format.printf(prettyPrint(colorize, isClientLogFile)));

export const getLogLevel = (): CommonTypes.LogLevel => {
  /* determine the log level setting */

  // First lookup process specific log level environment variable else
  // use the gms wide config log level
  const envLogLevel = (process.env.GMS_CONFIG_INTERACTIVE_ANALYSIS_API_GATEWAY__LOG_LEVEL
    ? process.env.GMS_CONFIG_INTERACTIVE_ANALYSIS_API_GATEWAY__LOG_LEVEL || ''
    : process.env.GMS_CONFIG_LOG_LEVEL || ''
  ).toLowerCase();

  // If envLogLevel is undefined or not legit value use the config 'logLevel'
  const logLevel: CommonTypes.LogLevel =
    envLogLevel && levelsAndColors.levels[envLogLevel] >= 0
      ? envLogLevel
      : config.get('logLevel').toLowerCase();
  return logLevel;
};

/** Filter logs based on the log level configured */
const filterLogsBasedOnLogLevel = Winston.format((log, opts) => {
  const logLevel = getLogLevel();
  return levelsAndColors.levels[log.level] > levelsAndColors.levels[logLevel] ? false : log;
});

/** Filter only client logs */
const filterOnlyClientLogs = Winston.format((log, opts) =>
  log.level !== CommonTypes.LogLevel.client ? false : log
);

/** Filter out all client logs */
const filterNoClientLogs = Winston.format((log, opts) =>
  log.level === CommonTypes.LogLevel.client ? false : log
);

/** Filter only timing logs */
const filterOnlyTimingLogs = Winston.format((log, opts) =>
  log.level !== CommonTypes.LogLevel.timing ? false : log
);

/** Filter out all timing logs */
const filterNoTimingLogs = Winston.format((log, opts) =>
  log.level === CommonTypes.LogLevel.timing ? false : log
);

/** Winston logger configuration and options */
const options: Winston.LoggerOptions = {
  // define the available log levels
  levels: levelsAndColors.levels,
  // define default (common) log format for all logs (executes once regardless of the number of transports)
  format: Winston.format.combine(
    // filter out logs based on the set log level
    filterLogsBasedOnLogLevel(),
    Winston.format((info: any) => {
      // retrieve the additional meta data for the log
      const meta = info[Symbol.for('splat')] ? [...info[Symbol.for('splat')]] : [];
      info.meta = meta;

      // format client log messages
      if (info.level === CommonTypes.LogLevel.client) {
        if (meta && meta.length === 2) {
          const clientLevel = meta[0].toUpperCase();
          const username = meta[1];
          if (clientLevel && username) {
            info.message = `${clientLevel}: [${username}] - ${info.message}`;
          }
        }
      }

      // format timing log messages
      if (info.level === CommonTypes.LogLevel.timing) {
        if (meta && meta.length === 1) {
          const username = meta[0];
          if (username) {
            info.message = `[${username}] - ${info.message}`;
          }
        }
      }
      return info;
    })(),
    Winston.format.simple(),
    Winston.format.prettyPrint(),
    Winston.format.splat(),
    Winston.format.timestamp({
      format: new Date().toISOString()
    })
  ),
  exitOnError: false,
  transports: [
    new Winston.transports.Console({
      level: CommonTypes.LogLevel.debug,
      // colorize the output to the console
      format: Winston.format.combine(formatter(true))
    }),
    new winston.transports.DailyRotateFile({
      level: CommonTypes.LogLevel.debug,
      format: Winston.format.combine(filterNoClientLogs(), filterNoTimingLogs(), formatter()),
      name: 'daily',
      filename: `${logDir}/${logFile}`,
      extension: '.log',
      datePattern: 'YYYY-MM-DD',
      zippedArchive: true,
      maxSize: dailyMaxFileSize,
      maxFiles: dailyMaxFiles
    }),
    new winston.transports.DailyRotateFile({
      level: CommonTypes.LogLevel.client,
      format: Winston.format.combine(filterOnlyClientLogs(), formatter(false, true)),
      name: 'daily',
      filename: `${logDir}/${clientLogFile}`,
      extension: '.log',
      datePattern: 'YYYY-MM-DD',
      zippedArchive: true,
      maxSize: dailyMaxFileSize,
      maxFiles: dailyMaxFiles
    }),
    new winston.transports.DailyRotateFile({
      level: CommonTypes.LogLevel.timing,
      format: Winston.format.combine(filterOnlyTimingLogs(), formatter()),
      name: 'daily',
      filename: `${logDir}/${timingLogFile}`,
      extension: '.log',
      datePattern: 'YYYY-MM-DD',
      zippedArchive: true,
      maxSize: dailyMaxFileSize,
      maxFiles: dailyMaxFiles
    }),
    new winston.transports.DailyRotateFile({
      level: CommonTypes.LogLevel.warn,
      format: Winston.format.combine(formatter()),
      name: 'daily',
      filename: `${logDir}/${errorLogFile}`,
      extension: '.log',
      datePattern: 'YYYY-MM-DD',
      zippedArchive: true,
      maxSize: dailyMaxFileSize,
      maxFiles: dailyMaxFiles
    })
  ]
};

// set the custom colors
winston.addColors(levelsAndColors.colors);

// Create the log directory if it does not exist
if (!fs.existsSync(logDir)) {
  try {
    fs.mkdirSync(logDir);
  } catch (e) {
    // Do nothing if error caught
    // Log directory already exists and that's ok
    // When running tests - race conditions cause us to enter this state
  }
}

/** Gateway Winston Logger  */
export interface GatewayLogger extends Winston.Logger {
  // define logging methods for custom logging levels
  timing(message: string, username?: string): Winston.Logger;
  client(
    message: string,
    level: string | CommonTypes.LogLevel.debug,
    username: string
  ): Winston.Logger;
}

// create our logger instance
export const gatewayLogger: GatewayLogger = Winston.createLogger(options) as GatewayLogger;

// sample log types
gatewayLogger.debug(`sample debug log`);
gatewayLogger.data(`sample data log`);
gatewayLogger.timing(`sample timing log`, 'username');
gatewayLogger.info(`sample info log`);
gatewayLogger.client(`sample client debug log`, CommonTypes.LogLevel.debug, 'username');
gatewayLogger.client(`sample client data log`, CommonTypes.LogLevel.data, 'username');
gatewayLogger.client(`sample client timing log`, CommonTypes.LogLevel.timing, 'username');
gatewayLogger.client(`sample client info log`, CommonTypes.LogLevel.info, 'username');
gatewayLogger.client(`sample client warn log`, CommonTypes.LogLevel.warn, 'username');
gatewayLogger.client(`sample client error log`, CommonTypes.LogLevel.error, 'username');
gatewayLogger.warn(`sample warn log`);
gatewayLogger.error(`sample error log`);

// log system configuration
gatewayLogger.info('Logging to %s/%s', logDir, logFile);
gatewayLogger.info('process.env.NODE_ENV set to %s', JSON.stringify(process.env.NODE_ENV));
gatewayLogger.info('GIT_VERSION set to %s', process.env.GIT_VERSION);
gatewayLogger.info('GIT_COMMITHASH set to %s', process.env.GIT_COMMITHASH);
gatewayLogger.info('GIT_BRANCH set to %s', process.env.GIT_BRANCH);
gatewayLogger.info('NODE_ENV set to %s', process.env.NODE_ENV);
gatewayLogger.info('NODE_CONFIG_ENV set to %s', process.env.NODE_CONFIG_ENV);
gatewayLogger.info('CI_COMMIT_REF_NAME set to %s', process.env.CI_COMMIT_REF_NAME);
gatewayLogger.info('Config name %s', config.get('configName'));
