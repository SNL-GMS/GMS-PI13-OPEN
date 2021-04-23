import { ApolloClient } from 'apollo-client';
import * as ClientLogger from '../../src/ts/apollo/client-logger';
import { Client } from '../../src/ts/apollo/types';

window.alert = jest.fn();
window.open = jest.fn();
describe('apollo client Logger', () => {
  test('should exist', () => {
    expect(ClientLogger.createApolloClientLogger).toBeDefined();
  });

  test('UI Logger can be created with the create function', () => {
    const logger: Client = ClientLogger.createApolloClientLogger();
    // make sure we can make a client if we call the client creation function
    expect(logger).toBeInstanceOf(ApolloClient);
  });

  test('UI Logger returns undefined if given a bad window', () => {
    (ClientLogger as any).windowIsDefined = false;
    const logger: Client = ClientLogger.createApolloClientLogger();
    // make sure we can make a client if we call the client creation function
    expect(logger).toBe(undefined);
  });
});
