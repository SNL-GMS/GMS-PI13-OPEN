import { ConfigurationProcessor } from '../configuration/configuration-processor';
import { FkProcessor } from '../fk/fk-processor';
import { KafkaConsumer } from '../kafka/kafka-consumer';
import { KafkaProducer } from '../kafka/kafka-producer';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { SohProcessor } from '../soh/soh-processor';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { ReferenceStationProcessor } from '../station/reference-station/reference-station-processor';
import { SystemMessageProcessor } from '../system-message/system-message-processor';
import * as waveformProcessor from '../waveform/waveform-processor';
import { WorkflowProcessor } from '../workflow/workflow-processor';
import { schema } from './api-gateway-schema';
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

logger.info('Starting API Gateway Server...');

const userMap: ExpressUserMap = new Map<string, ExpressUser>();
const app = createExpressServer(userMap);
createHttpServer(app);
configureRouteAlive(app);
configureRouteReady(app, getProtocol());
configureRouteHealthCheck(app, getProtocol());

const initializeProcessors = async () => {
  // Initialize the API Gateway Processors
  logger.info(`==> initialize processors, data, and initial configuration`);

  // ! The initialization order matters
  // Configuration Processor which makes a network call to get the Analyst UI Configuration.
  await ConfigurationProcessor.Instance().fetchConfiguration();
  await ReferenceStationProcessor.Instance().getDefaultStations();
  await ProcessingStationProcessor.Instance().fetchStationData();
  await WorkflowProcessor.Instance().fetchWorkflowData();

  configureRouteCheckInitialized(app);
};

const initializeKafka = async () => {
  // Initialize the KAFKA Consumers and Producers
  logger.info(`==> initialize KAFKA configurations`);

  // Initialize the KAFKA consumer
  await KafkaConsumer.Instance().start();

  // Initialize the SOH Kafka producer
  await KafkaProducer.Instance().start();

  // register callbacks for kafka
  SohProcessor.Instance().registerKafkaConsumerCallbacks();
  SystemMessageProcessor.Instance().registerKafkaConsumerCallbacks();

  configureRouteCheckKafka(app);
};

const initializeApollo = () => {
  logger.info(`==> initialize Apollo Express server`);
  createApolloExpressServer(app, schema, userMap);
  configureRouteCheckApollo(app, getProtocol());
};

const initializeWebsocketServer = () => {
  logger.info(`==> initialize websocket server`);
  createWebSocketServer(schema, userMap);
  configureRouteCheckWebsocket(app);
};

initializeProcessors()
  .then(initializeKafka)
  .then(initializeApollo)
  .then(initializeWebsocketServer)
  .then(() => {
    // Calling because in mock the loading of the Fk Channel Segments
    // take long enough to timeout UI call to compute Fk
    FkProcessor.Instance(); // Testing fk loading

    logger.info('register /waveforms/raw');
    app.get('/waveforms/raw', waveformProcessor.waveformRawSegmentRequestHandler);

    logger.info('register /waveforms/filter');
    app.get('/waveforms/filtered', waveformProcessor.waveformFilteredSegmentRequestHandler);

    configureRouteAuthentication(app, userMap);
  })
  .catch(e => logger.error(e));
