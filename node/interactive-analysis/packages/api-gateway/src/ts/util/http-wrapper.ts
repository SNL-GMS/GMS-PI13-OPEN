import { jsonPretty } from '@gms/common-util';
import { ApolloError } from 'apollo-server-core';
import Axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import clone from 'lodash/clone';
import toLower from 'lodash/toLower';
import msgpack from 'msgpack-lite';
import { gatewayLogger as logger } from '../log/gateway-logger';

/**
 * Http Response
 */
// tslint:disable-next-line: no-empty-interface
export interface HttpResponse<T = any> extends AxiosResponse<T> {}

/**
 * Shortens and formats a string with a ellipsis.
 *
 * @param str the string
 * @param length the desired maximum length of the string
 */
const ellipsis = (str: string, length: number = 500) =>
  length === undefined || str.length > length ? str.substr(0, length) + '...' : str;

/**
 * Returns a formated string from the HTTP Response status text.
 * @param response the http response
 * @param length (optional) the maximum length of the formatted string
 */
export const httpResponseFormattedStatusText = <T>(
  response: HttpResponse<T>,
  length: number = 500
): string => ellipsis(response.statusText, length);

/**
 * Returns a formated string from the HTTP Response.
 * @param response the http response
 */
export const httpResponseFormattedString = <T>(response: HttpResponse<T>): string =>
  `HttpResponse: ` +
  `call=${response.request.url || response.request.method || response.request.baseUrl} ` +
  `status=${response.status} `;

/**
 * Returns true if the response was successful; false otherwise
 * @param response the http response
 */
export const isHttpResponseError = <T>(response: HttpResponse<T>): boolean =>
  // tslint:disable-next-line: no-magic-numbers
  response.status !== 200;

/**
 * A lightweight wrapper around the Axios mock adapter library that provides convenience
 * wrapper functions to register mock handlers around simulated gms backend services.
 */
export class HttpMockWrapper {
  /** Mocks */
  private readonly mocks: Map<string, <I, T>(input: I) => Promise<T>> = new Map();

  /**
   * Initialize the axios mock wrapper with an instance of the axios mock adapter.
   * @param mockAdapter The axios mock adapter to wrap
   */
  public constructor() {
    this.mocks = new Map();
  }

  /**
   * Checks if mock url exists
   * @param url url
   * @returns a boolean
   */
  public has(url: string) {
    return this.mocks.has(url);
  }

  /**
   * Removes the mock handles
   * @param url url
   */
  public remove(url: string) {
    logger.debug(`Removing mock handlers : ${url}`);
    this.mocks.delete(url);
  }

  /**
   * Clears the mock handles
   */
  public clear(): void {
    logger.debug(`Removing all mock handlers.`);
    this.mocks.clear();
  }

  /**
   * Configure a mock service interface at the provided URL, with the provided handler function.
   * @param url The URL to mock a POST service interface for
   * @param handler The handler function to be called whenever a request is sent to the input URL.
   * This function should accept an input object representing the parsed JSON request body, and should
   * return an object representing the result to encode as the HTTP response body.
   */
  public onMock(url: string, handler: (input?: any) => any): void {
    logger.info(`Registering mock handler for url ${url}`);
    this.mocks.set(
      url,
      async (input?: any): Promise<any> =>
        // tslint:disable-next-line: no-inferred-empty-object-type
        new Promise((resolve: any) => {
          resolve(handler(input));
        }).catch(error => {
          logger.error(`Error in mock response url: ${url}  input: ${ellipsis(jsonPretty(input))}`);
          logger.error(`Error in mock response: ${error}`);
          throw new ApolloError(`Service error for url: "${url}" error: ${error}`);
        })
    );
  }

  /**
   * Http request helper method
   * @param url the url
   * @param data request body
   * @returns request response as promise
   */
  public async request<T>(url: string, data?: any): Promise<HttpResponse<T>> {
    if (this.has(url)) {
      const action = this.mocks.get(url);
      const methodReturn: Promise<HttpResponse<T>> = action(data).then((result: T) => ({
        data: result,
        status: 200,
        statusText: 'Mocked Successful',
        headers: {
          'content-type': 'application/json'
        },
        config: {
          url,
          data
        },
        request: {}
      }));
      return methodReturn;
    }
  }
}

/**
 * A lightweight wrapper around the Axios HTTP client providing convenience functions for
 * making HTTP GET and POST requests.
 */
export class HttpClientWrapper {
  /** The axios HTTP client instance */
  private readonly axiosInstance: AxiosInstance;

  /** Axios mock http wrapper */
  private mockAdaptor: HttpMockWrapper;

  /**
   * Initialize the axios wrapper with an instance of the axios http client.
   * @param config (optional) configuration settings used to initialize the axios client
   */
  public constructor(config?: AxiosRequestConfig) {
    // TODO consider passing config parameters to the axios.create() method (e.g. base url)
    // tslint:disable-next-line:no-magic-numbers
    this.axiosInstance = Axios.create({ maxContentLength: 100 * 1024 * 1024 });
  }

  /**
   * Handles real and mock http request
   * @param requestConfig request config
   * @param data request body data
   * @returns request result as promise
   */
  public async request<T>(requestConfig: AxiosRequestConfig, data?: any): Promise<HttpResponse<T>> {
    // Throw an error if the request configuration is undefined
    if (!requestConfig) {
      throw new Error('Cannot send HTTP service request with undefined request configuration');
    }
    // Throw an error if the request configuration does not include the url
    if (!requestConfig.url) {
      throw new Error('Cannot send HTTP service request with undefined URL');
    }

    if (this.mockAdaptor && this.mockAdaptor.has(requestConfig.url)) {
      return this.mockAdaptor.request<T>(requestConfig.url, data);
    }

    // Build the HTTP request object from the provided request config and data
    const request = clone(requestConfig);
    // If request data are provided, provide them as 'params' for GET requests or 'data'
    // for all other methods
    if (data) {
      // check if a post or a get
      if (!request.method || toLower(request.method) === 'get') {
        const paramsSerializer = params =>
          params
            ? Object.keys(params)
                .map(key => `${key}/${params[key]}`)
                .join('/')
            : '';
        // !TODO To be fixed in a CR
        // !This should be fixed once the CR for correcting how params are handled for the request
        // !for stations -> '/name/demo' should be something like `?name=demo` or `?name/demo`
        // !unable to use the params serializer, because axios adds a '?' between the url and params
        // GET / params and custom serializer for get parameters
        // request.params = data;
        // requestConfig.paramsSerializer = paramsSerializer;
        request.url = request.url.concat(paramsSerializer(data));
      } else {
        // POST / data
        // If request content-type header is set to msgpack,
        // encode the request body as msgpack
        const requestType = getHeaderValue(request, 'content-type');
        const encodedData =
          requestType && requestType === 'application/msgpack'
            ? msgpack.encode(data)
            : requestType && requestType === 'text/plain'
            ? `\"${data}\"`
            : data;

        if (requestType && requestType === 'application/msgpack') {
          logger.debug(
            // tslint:disable-next-line:no-magic-numbers
            `Encoding request as msgpack buffer length: ${encodedData.length / (1024 * 1024)}`
          );
        }
        request.data = encodedData;
      }
    }
    return this.axiosInstance(request)
      .then((response: AxiosResponse<any>) => {
        // If the request is configured to accept msgpack responses,
        // decode the response body from message pack; otherwise return the response
        // body as is (e.g. for JSON or plain text encodings)
        const contentType: string = getHeaderValue(response, 'content-type');
        const isMessagePack: boolean = contentType && contentType === 'application/msgpack';
        return {
          ...response,
          headers: {
            ...response.headers,
            'content-type': isMessagePack ? 'application/json' : contentType
          },
          data: isMessagePack ? (msgpack.decode(response.data) as T) : response.data
        };
      })
      .catch(error => {
        let httpResponse;
        if (error.response) {
          // The request was made and the server responded with a status code outside the range of 2xx

          // some services return a more detailed error response message
          // that has been set to the data field, append this to the statusText
          const statusText: string =
            error.response.data !== undefined &&
            error.response.data !== null &&
            (typeof error.response.data === 'string' || error.response.data instanceof String)
              ? `${error.response.statusText}: ${error.response.data.trim()}`
              : error.response.statusText;

          httpResponse = {
            data: undefined,
            status: error.response.status,
            statusText,
            headers: error.response.headers,
            config: error.response.config,
            request: error.response.request
          };
        } else {
          // The request was made but no response was received
          httpResponse = {
            data: undefined,
            status: 400, // Bad Request
            statusText: error.message,
            headers: {},
            config: error.config,
            request: error.request
          };
        }
        logger.error(
          `HTTP Request Error: `,
          `\n\turl: ${httpResponse.config.url}`,
          `\n\tstatus: ${httpResponse.status}`,
          // TODO update after upgrading to the latest version of winston: there is a method called isDebugEnabled
          `\n\tstatusText: ${
            logger.level === 'debug' ? httpResponse.statusText : ellipsis(httpResponse.statusText)
          }`,
          `\n\trequestData: ${
            logger.level === 'debug' ? jsonPretty(data) : ellipsis(jsonPretty(data))
          }`
        );
        const properties: Map<string, any> = new Map<string, any>();
        properties.set('httpResponse', httpResponse);
        // tslint:disable-next-line: max-line-length
        throw new ApolloError(
          `Service error for url: "${httpResponse.config.url}" error: ${ellipsis(
            httpResponse.statusText
          )}`,
          httpResponse.status,
          properties
        );
      });
  }

  /**
   * Create & return a new AxiosMockWrapper for this client
   */
  public createHttpMockWrapper(): HttpMockWrapper {
    return (this.mockAdaptor = new HttpMockWrapper());
  }
}

/**
 * Gets the header value from request
 * @param httpConfig http config
 * @param headerName header name
 * @returns a value from the header
 */
export function getHeaderValue(httpConfig: any, headerName: string): string {
  let value;
  if (httpConfig && httpConfig.headers) {
    Object.keys(httpConfig.headers).forEach(key => {
      if (key.toLowerCase() === headerName.toLowerCase()) {
        value = httpConfig.headers[key];
      }
    });
  }
  return value;
}
