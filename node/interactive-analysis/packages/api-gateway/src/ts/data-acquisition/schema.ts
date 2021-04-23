/**
 * GraphQL schema definition for Data Acquisition
 */
import { readFileSync } from 'fs';
import { resolve } from 'path';

// GraphQL schema definitions
export const schema = readFileSync(
  resolve(process.cwd(), 'resources/graphql/data-acquisition/schema.graphql')
).toString();
