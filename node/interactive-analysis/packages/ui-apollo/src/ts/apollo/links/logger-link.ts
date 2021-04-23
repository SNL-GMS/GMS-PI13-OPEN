import { CommonTypes } from '@gms/common-graphql';
import { IS_NODE_ENV_DEVELOPMENT } from '@gms/common-util';
import { ApolloLink, Operation } from 'apollo-link';
import apolloLogger from 'apollo-link-logger';
import { UILogger } from '../../ui-logger';

/**
 * Returns true if the operation is `clientLog`; false otherwise.
 * @param operation the apollo link operation
 */
const isClientLog = (operation: Operation) =>
  operation?.operationName === CommonTypes.ClientLogOperationMutationName;

// logger callback
export const loggerCallBack = (operation: Operation, forward: any) => {
  const startTime = new Date().getTime();
  const operationType = (operation?.query?.definitions[0] as any)?.operation;
  if (operation && !isClientLog(operation)) {
    UILogger.Instance().performance(operation.operationName, 'request');
  }
  return forward(operation).map(result => {
    if (operation && !isClientLog(operation)) {
      const elapsed = new Date().getTime() - startTime;
      UILogger.Instance().performance(operation.operationName, 'returned');
      UILogger.Instance().data(`${operationType} ${operation.operationName} (in ${elapsed} ms)`);
    }
    return result;
  });
};

// this logger catches a apollo operation before the ajax call and after the ajax call
// and logs the type, name, and time elapsed of the call
export const gatewayLogger = new ApolloLink(loggerCallBack);

// orphaned function that takes an operation and returns it for the chaining of ApolloLinks
export const passThrough = new ApolloLink((operation: any, forward: any) => forward(operation));

// ApolloLink concatenator function that turns multiple ApolloLinks into 1
export const LoggerLink = ApolloLink.from(
  IS_NODE_ENV_DEVELOPMENT ? [gatewayLogger, apolloLogger] : [passThrough]
);
