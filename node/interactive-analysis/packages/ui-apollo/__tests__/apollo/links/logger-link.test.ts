import { Operation } from 'apollo-link';
import {
  gatewayLogger,
  loggerCallBack,
  LoggerLink
} from '../../../src/ts/apollo/links/logger-link';

// tslint:disable: no-console
// tslint:disable: no-unbound-method
window.alert = jest.fn();
window.open = jest.fn();

describe('logger link', () => {
  beforeAll(() => {
    // Create a spy on console (console.log in this case) and provide some mocked implementation
    // In mocking global objects it's usually better than simple `jest.fn()`
    // because you can `unmock` it in clean way doing `mockRestore`
    jest.spyOn(console, 'error').mockImplementation(msg => {
      expect(msg).toEqual('[GraphQL error]: Message: message, Location: locations, Path: path');
    });
    jest.spyOn(console, 'warn').mockImplementation(msg => {
      expect(msg).toEqual('[Network error]: network errors');
    });
  });
  // make sure the functions are defined
  test('should exist', () => {
    expect(LoggerLink).toBeDefined();
    expect(gatewayLogger).toBeDefined();
    expect(loggerCallBack).toBeDefined();
  });

  test('should create log messages', () => {
    const forward = jest.fn(() => ['op.operationName']);
    const operation: Operation = {
      query: undefined,
      operationName: 'operationName',
      variables: undefined,
      extensions: undefined,
      setContext: undefined,
      getContext: undefined,
      toKey: undefined
    };
    expect(loggerCallBack(operation, forward)).toEqual(['op.operationName']);
    expect(forward).toHaveBeenCalledTimes(1);
  });
});
