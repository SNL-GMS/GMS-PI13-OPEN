import { HttpLink } from 'apollo-link-http';
import * as msgpack from 'msgpack-lite';
import {
  batchMsgPackFetcher,
  msgPackFetcher,
  MsgPackLink
} from '../../../src/ts/apollo/links/msgpack-link';

window.alert = jest.fn();
window.open = jest.fn();

describe('batch links', () => {
  beforeAll(() => {
    // Create a spy on console (console.log in this case) and provide some mocked implementation
    // In mocking global objects it's usually better than simple `jest.fn()`
    // because you can `unmock` it in clean way doing `mockRestore`
    jest.spyOn(console, 'error').mockImplementation(() => {
      /* empty */
    });
  });
  // make sure the functions are defined
  test('should exist', () => {
    expect(msgPackFetcher).toBeDefined();
    expect(batchMsgPackFetcher).toBeDefined();
    expect(MsgPackLink).toBeDefined();
  });

  // make sure the functions return batch links
  test('should create links', () => {
    const mockSuccessResponse = { data: { data: msgpack.encode('data') } };
    const mockJsonPromise = Promise.resolve(mockSuccessResponse); // 2
    const mockFetchPromise = Promise.resolve({
      // 3
      json: async () => mockJsonPromise
    });
    jest.spyOn(global as any, 'fetch').mockImplementation(async () => mockFetchPromise); // 4

    const uri = 'uri';
    const options = {};
    expect(msgPackFetcher(uri, options)).toBeInstanceOf(Promise);
    expect((global as any).fetch).toHaveBeenCalledTimes(1);
    (global as any).fetch.mockClear();
  });

  // make sure the functions handles bad links
  test('should handle error links', () => {
    const mockSuccessResponse = { badData: { data: msgpack.encode('data') } };
    const mockJsonPromise = Promise.resolve(mockSuccessResponse); // 2
    const mockFetchPromise = Promise.resolve({
      // 3
      json: async () => mockJsonPromise,
      text: () => Promise.resolve('bad')
    });
    jest.spyOn(global as any, 'fetch').mockImplementation(async () => mockFetchPromise); // 4
    const uri = 'uri';
    const options = {};
    expect(msgPackFetcher(uri, options)).toBeInstanceOf(Promise);
  });

  // make sure the functions return batch links
  test('should create batch links', () => {
    (global as any).fetch.mockClear();
    const mockSuccessResponse = [{ data: { data: msgpack.encode('data') } }];
    const mockJsonPromise = Promise.resolve(mockSuccessResponse); // 2
    const mockFetchPromise = Promise.resolve({
      // 3
      json: async () => mockJsonPromise
    });
    jest.spyOn(global as any, 'fetch').mockImplementation(async () => mockFetchPromise); // 4

    const uri = 'uri';
    const options = {};
    expect(batchMsgPackFetcher(uri, options)).toBeInstanceOf(Promise);
    expect((global as any).fetch).toHaveBeenCalledTimes(1);
  });

  // make sure the functions handles bad batch links
  test('should handle batch error links', () => {
    const mockSuccessResponse = [{ badData: { data: msgpack.encode('data') } }];
    const mockJsonPromise = Promise.resolve(mockSuccessResponse); // 2
    const mockFetchPromise = Promise.resolve({
      // 3
      json: async () => mockJsonPromise,
      text: () => Promise.resolve('bad')
    });
    jest.spyOn(global as any, 'fetch').mockImplementation(async () => mockFetchPromise); // 4
    const uri = 'uri';
    const options = {};
    expect(batchMsgPackFetcher(uri, options)).toBeInstanceOf(Promise);
    // tslint:disable-next-line
    expect(console.error).toHaveBeenCalledTimes(1);
  });

  // make sure the functions return batch links
  test('should create msg pack  http link', () => {
    const uri = 'uri';
    expect(MsgPackLink(uri)).toBeInstanceOf(HttpLink);
  });
});
