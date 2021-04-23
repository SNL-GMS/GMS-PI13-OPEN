import bodyParser from 'body-parser';
import config from 'config';
import { Express } from 'express';
import session, { MemoryStore } from 'express-session';
import { v4 as uuid } from 'uuid';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { ExpressUserMap } from './express-user';

/** Represents 24 hours */
export const TWENTY_FOUR_HOURS = 86400000; // 24 * 60 * 60 * 1000

/** The number of days to allow the session to stay active */
export const NUMBER_DAYS = 7;

/** The session lifetime */
export const SESSION_LIFETIME = TWENTY_FOUR_HOURS * NUMBER_DAYS;

/** The session name */
export const SESSION_NAME = 'InteractiveAnalysis';

/** True if running as ssl; false otherwise */
export const SSL = process.env.SSL === 'true';

/** Returns the protocol */
export const getProtocol = () => (SSL ? 'https://' : 'http://');

/**
 * Creates the Express Server
 * @param userMap the user map
 */
export const createExpressServer = (userMap: ExpressUserMap): Express => {
  logger.info(`Creating the express server...`);

  // tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
  const express = require('express');
  const app: Express = express();

  app.set('trust proxy', true);

  const useInMemory: boolean = config.get('inMemorySession');

  app.use(
    session({
      name: SESSION_NAME,
      secret: 'upside down hedgehog',
      genid: req => uuid(), // use UUIDs for unique session IDs
      resave: true,
      store: useInMemory
        ? new MemoryStore()
        : // tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
          new (require('connect-pg-simple')(session))({
            conString:
              'postgres://gms_session_application:gmsdb:postgres:gms_session_application:thawed-mortals-rebounds@postgresql-gms:5432/gms',
            tableName: 'session',
            schemaName: 'gms_session'
          }),

      saveUninitialized: true,
      proxy: true,
      cookie: {
        expires: new Date(Date.now() + SESSION_LIFETIME),
        maxAge: SESSION_LIFETIME,
        sameSite: true,
        secure: SSL
      }
    })
  );

  app.use(bodyParser.json({ limit: '50mb' }));
  app.use(bodyParser.urlencoded({ limit: '50mb', extended: true }));

  return app;
};
