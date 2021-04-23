import { fragmentSchema, fragmentSchemaSOH } from '@gms/common-graphql';
import { IS_INTERACTIVE_ANALYSIS_MODE_SOH } from '@gms/common-util';
import { Hermes } from 'apollo-cache-hermes';
import {
  defaultDataIdFromObject,
  InMemoryCache,
  IntrospectionFragmentMatcher
} from 'apollo-cache-inmemory';

// determine which fragment schema to use based on the mode
const schema = IS_INTERACTIVE_ANALYSIS_MODE_SOH ? fragmentSchemaSOH : fragmentSchema;

// construct the introspection fragment matcher
const fragmentMatcher = new IntrospectionFragmentMatcher({
  introspectionQueryResultData: {
    ...schema
  }
});

// function that determines how to retrieve the id from an object
const retrieveId = (object: any) => {
  if (object.__typename) {
    if (object.id || object._id) {
      return defaultDataIdFromObject(object); // fall back to default id handling
    }
    // TODO: consider handling special cases here
    // no id exists on this type
    // create a unique id by hashing the objects
    // return `${object.__typename}:${hash(object, {respectType: false})}`;
  }
  return null;
};

// a in-memory cache configuration
export const inMemoryCacheConfiguration = new InMemoryCache({
  addTypename: true,
  fragmentMatcher,
  dataIdFromObject: retrieveId,
  cacheRedirects: {
    Query: {}
  },
  freezeResults: true,
  resultCaching: false
});

// a hermes cache configuration
export const cacheHermesConfiguration: Hermes = new Hermes({
  addTypename: true,
  entityIdForNode: retrieveId
});
