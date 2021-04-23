import { readFileSync } from 'fs';
import { resolve } from 'path';

/**
 * GraphQL schema definition for the waveform API gateway
 */

// GraphQL schema definitions
export const schema = readFileSync(
  resolve(process.cwd(), 'resources/graphql/processing-station/schema.graphql')
).toString();
