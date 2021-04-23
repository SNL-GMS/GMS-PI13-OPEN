import { makeExecutableSchema } from 'apollo-server-express';
import merge from 'lodash/merge';
import path from 'path';
import { resolvers as commonResolvers } from '../common/resolvers';
import { schema as commonSchema } from '../common/schema';
import { resolvers as configurationResolvers } from '../configuration/resolvers';
import { schema as configurationSchema } from '../configuration/schema';
import { resolvers as sohStatusResolvers } from '../soh/resolvers';
import { schema as sohStatusSchema } from '../soh/schema';
import { resolvers as systemMessageResolvers } from '../system-message/resolvers';
import { schema as systemMessageSchema } from '../system-message/schema';
import { resolvers as userProfileResolvers } from '../user-profile/resolvers';
import { schema as userProfileSchema } from '../user-profile/schema';

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
  userProfileSchema,
  systemMessageSchema,
  sohStatusSchema,
  configurationSchema
];

// Merge GraphQL resolvers from the schema domains
const resolvers = merge(
  commonResolvers,
  userProfileResolvers,
  systemMessageResolvers,
  sohStatusResolvers,
  configurationResolvers
);

// Build the GraphQL schema
export const schema = makeExecutableSchema({
  typeDefs,
  resolvers
});
