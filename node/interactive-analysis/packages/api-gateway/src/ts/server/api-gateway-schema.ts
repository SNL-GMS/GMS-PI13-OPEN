import { makeExecutableSchema } from 'apollo-server-express';
import merge from 'lodash/merge';
import path from 'path';
import { resolvers as cacheResolvers } from '../cache/resolvers';
import { schema as cacheSchema } from '../cache/schema';
import { resolvers as channelSegmentResolvers } from '../channel-segment/resolvers';
import { schema as channelSegmentSchema } from '../channel-segment/schema';
import {
  extendedResolvers as commonExtendedResolvers,
  resolvers as commonResolvers
} from '../common/resolvers';
import { extendedSchema as commonExtendedSchema, schema as commonSchema } from '../common/schema';
import { resolvers as configurationResolvers } from '../configuration/resolvers';
import { schema as configurationSchema } from '../configuration/schema';
import { resolvers as dataAcquisitionStatusResolvers } from '../data-acquisition/resolvers';
import { schema as dataAcquisitionStatusSchema } from '../data-acquisition/schema';
import { schema as eventSchema } from '../event/model-and-schema/schema';
import { resolvers as eventResolvers } from '../event/resolvers';
import { resolvers as fkResolvers } from '../fk/resolvers';
import { schema as fkSchema } from '../fk/schema';
import { resolvers as qcMaskResolvers } from '../qc-mask/resolvers';
import { schema as qcMaskSchema } from '../qc-mask/schema';
import { resolvers as signalDetectionResolvers } from '../signal-detection/resolvers';
import { schema as signalDetectionSchema } from '../signal-detection/schema';
import { resolvers as sohStatusResolvers } from '../soh/resolvers';
import { schema as sohStatusSchema } from '../soh/schema';
import { resolvers as processingStationResolvers } from '../station/processing-station/resolvers';
import { schema as processingStationSchema } from '../station/processing-station/schema';
import { resolvers as referenceStationResolvers } from '../station/reference-station/resolvers';
import { schema as referenceStationSchema } from '../station/reference-station/schema';
import { resolvers as systemMessageResolvers } from '../system-message/resolvers';
import { schema as systemMessageSchema } from '../system-message/schema';
import { resolvers as userProfileResolvers } from '../user-profile/resolvers';
import { schema as userProfileSchema } from '../user-profile/schema';
import { resolvers as waveformResolvers } from '../waveform/resolvers';
import { schema as waveformSchema } from '../waveform/schema';
import { resolvers as workflowResolvers } from '../workflow/resolvers';
import { schema as workflowSchema } from '../workflow/schema';

/**
 * Global GraphQL schema definition for the entire API gateway
 */

// using the gateway logger
import { gatewayLogger as logger } from '../log/gateway-logger';
const objectPath = path.relative(process.cwd(), __filename);

// GraphQL schema definitions
logger.info('Creating graphql schema...', { module: objectPath });

const typeDefs = [
  commonSchema,
  commonExtendedSchema,
  systemMessageSchema,
  workflowSchema,
  processingStationSchema,
  referenceStationSchema,
  waveformSchema,
  signalDetectionSchema,
  eventSchema,
  qcMaskSchema,
  userProfileSchema,
  channelSegmentSchema,
  fkSchema,
  dataAcquisitionStatusSchema,
  sohStatusSchema,
  cacheSchema,
  configurationSchema
];

// Merge GraphQL resolvers from the schema domains
const resolvers = merge(
  commonResolvers,
  commonExtendedResolvers,
  systemMessageResolvers,
  workflowResolvers,
  processingStationResolvers,
  referenceStationResolvers,
  waveformResolvers,
  signalDetectionResolvers,
  eventResolvers,
  userProfileResolvers,
  qcMaskResolvers,
  channelSegmentResolvers,
  fkResolvers,
  dataAcquisitionStatusResolvers,
  sohStatusResolvers,
  cacheResolvers,
  configurationResolvers
);

// Build the GraphQL schema
export const schema = makeExecutableSchema({
  typeDefs,
  resolvers
});
