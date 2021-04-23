import { readFileSync } from 'fs';
import { resolve } from 'path';

/**
 * GraphQL schema definition for the signal detection API gateway
 */

// GraphQL schema definitions
export const schema = readFileSync(
  resolve(process.cwd(), 'resources/graphql/signal-detection/schema.graphql')
).toString();
