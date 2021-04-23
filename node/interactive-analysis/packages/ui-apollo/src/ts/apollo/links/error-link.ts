import { Toaster } from '@gms/ui-util';
import { ErrorHandler, onError } from 'apollo-link-error';
import { UILogger } from '../../ui-logger';

const toaster: Toaster = new Toaster();

/**
 * error handler callback for the onError call
 * @param param0 error handling
 */
export const onErrorCallBack: ErrorHandler = ({ graphQLErrors, networkError }) => {
  if (graphQLErrors) {
    graphQLErrors.map(({ message, locations, path }) => {
      toaster.toastError(message);
      UILogger.Instance().error(
        `[GraphQL error]: Message: ${message}, Location: ${String(locations)}, Path: ${String(
          path
        )}`
      );
    });
  }
  if (networkError) {
    UILogger.Instance().warn(`[Network error]: ${String(networkError)}`);
  }
};

// pass on error callback as the error handler to onError so its more testable
export const ErrorLink = onError(onErrorCallBack);
