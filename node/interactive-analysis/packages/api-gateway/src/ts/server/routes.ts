import { toOSDTime } from '@gms/common-util';
import { Express } from 'express';
import { CacheProcessor } from '../cache/cache-processor';
import { KafkaConsumer } from '../kafka/kafka-consumer';
import { KafkaProducer } from '../kafka/kafka-producer';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { ExpressUserMap } from './express-user';
import { healthChecks, HealthStatus } from './health-checks';

/**
 * Defines the available routes.
 */
export interface Routes {
  /** route to check alive status */
  readonly alive: string;
  /** route to check the ready status */
  readonly ready: string;
  /** route to check all health status */
  readonly healthCheck: string;
  /** health check routes */
  readonly healthChecks: {
    /** initialization health check */
    readonly initialized: string;
    /** kafka health check */
    readonly kafka: string;
    /** apollo server health check */
    readonly apollo: string;
    /** apollo server (internal) health check */
    readonly apolloInternal: string;
    /** websocket server health check */
    readonly websocket: string;
  };
  /** authentication routes */
  authentication: {
    /** route to login a user */
    login: string;
    /** route to logout a user */
    logout: string;
    /** route to check authentication status */
    check: string;
  };
}

/**
 * The routes
 */
const routes: Routes = {
  alive: '/alive',
  ready: '/ready',
  healthCheck: '/health-check',
  healthChecks: {
    initialized: '/health-check/initialized',
    kafka: '/health-check/kafka',
    apollo: '/health-check/apollo',
    apolloInternal: '/.well-known/apollo/server-health',
    websocket: '/health-check/websocket'
  },
  authentication: {
    login: '/auth/logInUser',
    logout: '/auth/logOutUser',
    check: '/auth/checkLogIn'
  }
};

/**
 * Configures the `alive` route.
 * This route returns a timestamp indicating the the gateway server is alive.
 *
 * @param app the express server app
 */
export const configureRouteAlive = (app: Express): void => {
  const handler = async (req, res) => res.send(Date.now().toString());
  logger.info(`register ${routes.alive}`);
  app.get(routes.alive, handler);
};

/**
 * Configures the `ready` route.
 * Performs simple health checks and verifies that the gateway is up and ready.
 *
 * @param app the express server app
 * @param protocol the http protocol
 */
export const configureRouteReady = (app: Express, protocol: string) => {
  const handler = async (req, res) => {
    const checks = await healthChecks([
      { id: routes.alive, path: `${protocol}${req.headers.host}${routes.alive}` },
      {
        id: routes.healthChecks.initialized,
        path: `${protocol}${req.headers.host}${routes.healthChecks.initialized}`
      },
      {
        id: routes.healthChecks.apollo,
        path: `${protocol}${req.headers.host}${routes.healthChecks.apollo}`
      },
      {
        id: routes.healthChecks.websocket,
        path: `${protocol}${req.headers.host}${routes.healthChecks.websocket}`
      }
    ]);
    const status = checks.map<HealthStatus>(c => c.status).every(s => s === HealthStatus.OK)
      ? HealthStatus.OK
      : HealthStatus.FAILED;
    // tslint:disable-next-line: no-magic-numbers
    return res.status(status === HealthStatus.OK ? 200 : 500).send(status);
  };

  logger.info(`register ${routes.ready}`);
  app.get(routes.ready, handler);
};

/**
 * Configures the `health-check` route.
 * Performs all of the health checks and returns each status.
 *
 * @param app the express server app
 * @param protocol the http protocol
 */
export const configureRouteHealthCheck = (app: Express, protocol: string) => {
  const handler = async (req, res) => {
    const checks = await healthChecks([
      { id: routes.alive, path: `${protocol}${req.headers.host}${routes.alive}` },
      {
        id: routes.healthChecks.initialized,
        path: `${protocol}${req.headers.host}${routes.healthChecks.initialized}`
      },
      {
        id: routes.healthChecks.kafka,
        path: `${protocol}${req.headers.host}${routes.healthChecks.kafka}`
      },
      {
        id: routes.healthChecks.apollo,
        path: `${protocol}${req.headers.host}${routes.healthChecks.apollo}`
      },
      {
        id: routes.healthChecks.websocket,
        path: `${protocol}${req.headers.host}${routes.healthChecks.websocket}`
      }
    ]);
    return res.send(JSON.stringify(checks));
  };

  logger.info(`register ${routes.healthCheck}`);
  app.get(routes.healthCheck, handler);
};

/**
 * Configures the `health-check/initialized` route.
 * Performs a simple health check to see if the gateway is initialized.
 *
 * @param app the express server app
 */
export const configureRouteCheckInitialized = (app: Express) => {
  // TODO determine what this health check should check and return
  const handler = async (req, res) => res.send(HealthStatus.OK);
  logger.info(`register ${routes.healthChecks.initialized}`);
  app.get(routes.healthChecks.initialized, handler);
};

/**
 * Configures the `health-check/kafka` route.
 * Performs a simple health check to see if the KAFKA connections are ok.
 *
 * @param app the express server app
 */
export const configureRouteCheckKafka = (app: Express) => {
  const handler = async (req, res) =>
    res
      .status(
        KafkaConsumer.Instance().connected() && KafkaProducer.Instance().connected()
          ? // tslint:disable-next-line: no-magic-numbers
            200
          : // tslint:disable-next-line: no-magic-numbers
            500
      )
      .send({
        'KAFKA Consumer': {
          Status: KafkaConsumer.Instance().getStatus(),
          'Up Time': KafkaConsumer.Instance().getUpTime()
            ? `${String(
                toOSDTime(
                  KafkaConsumer.Instance()
                    .getUpTime()
                    .getTime() / 1000
                )
              )} (${KafkaConsumer.Instance().getUpTimeSeconds()}s)`
            : 'N/A',
          'Status History': KafkaConsumer.Instance().getStatusHistoryInformationAsObject()
        },
        'KAFKA Producer': {
          Status: KafkaProducer.Instance().getStatus(),
          'Up Time': KafkaProducer.Instance().getUpTime()
            ? `${String(
                toOSDTime(
                  KafkaProducer.Instance()
                    .getUpTime()
                    .getTime() / 1000
                )
              )} (${KafkaProducer.Instance().getUpTimeSeconds()}s)`
            : `N/A`,
          'Status History': KafkaProducer.Instance().getStatusHistoryInformationAsObject()
        }
      });
  logger.info(`register ${routes.healthChecks.kafka}`);
  app.get(routes.healthChecks.kafka, handler);
};

/**
 * Configures the `health-checks/apollo` route.
 * Performs simple health checks and verifies that the apollo server is ready
 *
 * @param app the express server app
 * @param protocol the http protocol
 */
export const configureRouteCheckApollo = (app: Express, protocol: string) => {
  const handler = async (req, res) => {
    // check the built-in apollo server health check
    const check = (
      await healthChecks([
        {
          id: routes.healthChecks.apolloInternal,
          path: `${protocol}${req.headers.host}${routes.healthChecks.apolloInternal}`
        }
      ])
    )[0];
    return res
      .status(
        check.status === HealthStatus.OK
          ? // tslint:disable-next-line: no-magic-numbers
            200
          : // tslint:disable-next-line: no-magic-numbers
            500
      )
      .send(check.status === HealthStatus.OK ? HealthStatus.OK : HealthStatus.FAILED);
  };
  logger.info(`register ${routes.healthChecks.apollo}`);
  app.get(routes.healthChecks.apollo, handler);
};

/**
 * Configures the `health-checks/websocket` route.
 * Performs simple health checks and verifies that the websocket server is ready
 *
 * @param app the express server app
 */
export const configureRouteCheckWebsocket = (app: Express) => {
  // TODO determine what this health check should check and return
  const handler = async (req, res) => res.send(HealthStatus.OK);
  logger.info(`register ${routes.healthChecks.websocket}`);
  app.get(routes.healthChecks.websocket, handler);
};

/**
 * Configures the `authentication` routes.
 *
 * @param app the express server app
 * @param userMap the user map
 */
export const configureRouteAuthentication = (app: Express, userMap: ExpressUserMap): void => {
  // tslint:disable-next-line: no-require-imports
  const xssFilters = require('xss-filters');
  /** Checks if a user is logged in */
  const checkIsLoggedIn = async (req, res) => {
    logger.info(`Checking login for ${req.sessionID}`);
    if (userMap.has(req.sessionID)) {
      return res.send({
        authenticated: true,
        userName: xssFilters.inHTMLData(userMap.get(req.sessionID).userName)
      });
    }
    return res.send({ authenticated: false, userName: '' });
  };

  /** Login a user */
  const loginUser = (req, res) => {
    logger.info(`Logging in user ${req.query.userName}`);
    const safeUserName = xssFilters.inHTMLData(req.query.userName);
    if (req.query.userName !== safeUserName) {
      logger.warn(
        `User name - ${req.query.userName} - contained invalid characters - failed authentication`
      );
      res.send({ userName: safeUserName, authenticated: false });
    } else {
      userMap.set(req.sessionID, {
        userName: safeUserName
      });
      res.send({ userName: safeUserName, authenticated: true });
    }
  };

  /** Logouts a user */
  const logOutUser = (req, res) => {
    if (userMap.has(req.sessionID)) {
      logger.info(`Logging out user ${userMap.get(req.sessionID).userName}`);
      userMap.delete(req.sessionID);
      CacheProcessor.Instance().deleteUserCache(req.sessionID);
    } else {
      logger.info(`no session found to log out user session ${req.sessionID}`);
    }
    res.send({ userName: '', authenticated: false });
  };

  logger.info(`register ${routes.authentication.login}`);
  app.get(routes.authentication.login, loginUser);

  logger.info(`register ${routes.authentication.logout}`);
  app.get(routes.authentication.logout, logOutUser);

  logger.info(`register ${routes.authentication.check}`);
  app.get(routes.authentication.check, checkIsLoggedIn);
};
