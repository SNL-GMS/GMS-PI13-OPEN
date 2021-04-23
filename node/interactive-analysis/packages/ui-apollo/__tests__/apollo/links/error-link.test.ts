import { ErrorResponse } from 'apollo-link-error';
import { GraphQLError } from 'graphql';
import { ErrorLink, onErrorCallBack } from '../../../src/ts/apollo/links/error-link';

// tslint:disable: no-console
// tslint:disable: no-unbound-method
window.alert = jest.fn();
window.open = jest.fn();

describe('error link', () => {
  beforeAll(() => {
    // Create a spy on console (console.log in this case) and provide some mocked implementation
    // In mocking global objects it's usually better than simple `jest.fn()`
    // because you can `unmock` it in clean way doing `mockRestore`
    jest.spyOn(console, 'error').mockImplementation(msg => {
      expect(msg).toEqual(
        '[GraphQL error]: Message: error message, Location: undefined, Path: undefined'
      );
    });
    jest.spyOn(console, 'warn').mockImplementation(msg => {
      expect(msg).toEqual('[Network error]: Error: network errors');
    });
  });
  // make sure the functions are defined
  test('should exist', () => {
    expect(ErrorLink).toBeDefined();
    expect(onErrorCallBack).toBeDefined();
  });
  // make sure the functions return batch links
  test('should not create error toast and message on good calls', () => {
    const errorsToSend: ErrorResponse = {
      operation: undefined,
      forward: undefined,
      graphQLErrors: undefined,
      networkError: undefined
    };
    expect(onErrorCallBack(errorsToSend)).toBe(undefined);
    expect(console.error).toHaveBeenCalledTimes(0);
    expect(console.warn).toHaveBeenCalledTimes(0);
  });
  // make sure the functions return batch links
  test('should create error toast and message', () => {
    const errorsToSend: ErrorResponse = {
      operation: undefined,
      forward: undefined,
      graphQLErrors: [new GraphQLError('error message')],
      networkError: new Error('network errors')
    };
    expect(onErrorCallBack(errorsToSend)).toBe(undefined);
    expect(console.error).toHaveBeenCalledTimes(1);
    expect(console.warn).toHaveBeenCalledTimes(1);
  });
});
