/**
 * GraphQL schema definition for the qc mask API gateway
 */
import { readFileSync } from 'fs';
import { resolve } from 'path';

// GraphQL schema definitions
export const schema = readFileSync(
  resolve(process.cwd(), 'resources/graphql/qc-mask/schema.graphql')
).toString();
