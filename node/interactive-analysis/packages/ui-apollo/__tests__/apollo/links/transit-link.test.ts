import { HttpLink } from 'apollo-link-http';
import { fetcher, TransitLink } from '../../../src/ts/apollo/links/transit-link';

// tslint:disable: no-console
// tslint:disable: no-unbound-method
window.alert = jest.fn();
window.open = jest.fn();

describe('transit links', () => {
  // make sure the functions are defined
  test('should exist', () => {
    expect(fetcher).toBeDefined();
    expect(TransitLink).toBeDefined();
  });

  // make sure the functions return links
  test('should have a custom fetch', () => {
    const mockSuccessResponse = {
      headers: { get: () => null },
      text: () => 'text'
    };
    const mockFetchPromise = Promise.resolve(mockSuccessResponse);
    jest.spyOn(global as any, 'fetch').mockImplementation(async () => mockFetchPromise); // 4
    const uri = 'uri';
    const options = {};
    expect(fetcher(uri, options)).toBeInstanceOf(Promise);
    expect((global as any).fetch).toHaveBeenCalledTimes(1);
    (global as any).fetch.mockClear();
  });

  // // make sure the functions handles bad links
  test('should return an http link', () => {
    const uri = 'uri';
    expect(TransitLink(uri)).toBeInstanceOf(HttpLink);
  });
});
