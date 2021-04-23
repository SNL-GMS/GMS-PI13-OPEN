/**
 * GraphQL schema definition for the waveform API gateway
 */
import { readFileSync } from 'fs';
import { resolve } from 'path';

// GraphQL schema definitions
export const schema = readFileSync(
  resolve(process.cwd(), 'resources/graphql/fk/schema.graphql')
).toString();
