import { readFileSync } from 'fs';
import { resolve } from 'path';

// GraphQL schema definitions
export const schema = readFileSync(
  resolve(process.cwd(), 'resources/graphql/soh/schema.graphql')
).toString();
