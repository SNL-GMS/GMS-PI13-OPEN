// tslint:disable: no-magic-numbers

import { ConfigurationProcessor } from '../configuration/configuration-processor';
import { KafkaConsumer } from '../kafka/kafka-consumer';
import { KafkaProducer } from '../kafka/kafka-producer';
import { gatewayLogger, gatewayLogger as logger } from '../log/gateway-logger';
import { SohProcessor } from '../soh/soh-processor';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { SystemMessageProcessor } from '../system-message/system-message-processor';
import { schema } from './api-soh-gateway-schema';
import { createApolloExpressServer } from './apollo-server';
import { createExpressServer, getProtocol } from './express-server';
import { ExpressUser, ExpressUserMap } from './express-user';
import { createHttpServer } from './http-server';
import {
  configureRouteAlive,
  configureRouteAuthentication,
  configureRouteCheckApollo,
  configureRouteCheckInitialized,
  configureRouteCheckKafka,
  configureRouteCheckWebsocket,
  configureRouteHealthCheck,
  configureRouteReady
} from './routes';
import { createWebSocketServer } from './websocket-server';

logger.info('Starting API SOH Gateway Server...');

const userMap: ExpressUserMap = new Map<string, ExpressUser>();
const app = createExpressServer(userMap);
createHttpServer(app);
configureRouteAlive(app);
configureRouteReady(app, getProtocol());
configureRouteHealthCheck(app, getProtocol());

const initializeProcessors = async () => {
  try {
    // Initialize the API Gateway Processors
    logger.info(`==> initialize processors, data, and initial configuration`);

    // ! The initialization order matters
    // Configuration Processor which makes a network call to get the Analyst UI Configuration.
    await ConfigurationProcessor.Instance().fetchConfiguration();
    await ProcessingStationProcessor.Instance().fetchStationData();
    configureRouteCheckInitialized(app);
  } catch (error) {
    gatewayLogger.error(`Failed to initialize processors: ${error}`);
  }
};

const initializeKafka = async () => {
  try {
    // Initialize the KAFKA Consumers and Producers
    logger.info(`==> initialize KAFKA configurations`);

    // Initialize the system message Kafka consumer
    await KafkaConsumer.Instance().start();

    // Initialize the SOH Kafka producer
    await KafkaProducer.Instance().start();

    // register callbacks for kafka
    SohProcessor.Instance().registerKafkaConsumerCallbacks();
    SystemMessageProcessor.Instance().registerKafkaConsumerCallbacks();

    configureRouteCheckKafka(app);
  } catch (error) {
    gatewayLogger.error(`Failed to initialize KAFKA: ${error}`);
  }
};

const initializeApollo = () => {
  try {
    logger.info(`==> initialize Apollo Express server`);
    createApolloExpressServer(app, schema, userMap);
    configureRouteCheckApollo(app, getProtocol());
  } catch (error) {
    gatewayLogger.error(`Failed to initialize Apollo: ${error}`);
  }
};

const initializeWebsocketServer = () => {
  try {
    logger.info(`==> initialize websocket server`);
    createWebSocketServer(schema, userMap);
    configureRouteCheckWebsocket(app);
  } catch (error) {
    gatewayLogger.error(`Failed to initialize Websocket Server: ${error}`);
  }
};

initializeProcessors()
  .then(initializeKafka)
  .then(initializeApollo)
  .then(initializeWebsocketServer)
  .then(() => {
    configureRouteAuthentication(app, userMap);
  })
  .catch(e => logger.error(e));
