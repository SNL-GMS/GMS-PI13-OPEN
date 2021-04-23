import {
  ApolloClientConfiguration,
  Client,
  createApolloClientConfiguration,
  createApolloClientLogger,
  UILogger
} from '@gms/ui-apollo';
import { AppState } from '@gms/ui-state';
import { Actions } from '@gms/ui-state/lib/state/user-session/actions';
import React from 'react';
import { ApolloProvider } from 'react-apollo';
import { Provider } from 'react-redux';
import * as Redux from 'redux';

/**
 * Wraps the provided component with Redux Provider and ApolloProvider.
 *
 * @param Component the component
 * @param store the redux store
 */
export const withApolloReduxProvider = (
  Component: React.ComponentClass | React.FunctionComponent,
  store: Redux.Store<AppState>
): React.ComponentClass =>
  class WithReduxApolloProvider extends React.PureComponent<any, any> {
    private static readonly apolloClientConfiguration: ApolloClientConfiguration = createApolloClientConfiguration();

    private static readonly apolloClientLogger: Client = createApolloClientLogger();

    public constructor(props) {
      super(props);
      UILogger.Instance().setClient(WithReduxApolloProvider.apolloClientLogger);

      this.registerWsClientEvents();
    }

    /**
     * Wrap the component in an apollo and redux providers
     */
    public render() {
      return (
        <Provider store={store as any}>
          <ApolloProvider client={WithReduxApolloProvider.apolloClientConfiguration.client}>
            <Component {...this.props} />
          </ApolloProvider>
        </Provider>
      );
    }

    /**
     * Register websocket event handlers.
     */
    private readonly registerWsClientEvents = () => {
      // on disconnect; update the Redux connected status not connected
      WithReduxApolloProvider.apolloClientConfiguration.wsClient.on('disconnected', () => {
        store.dispatch(Actions.setConnected(false));
      });

      // on reconnected; update the Redux connected status to connected
      WithReduxApolloProvider.apolloClientConfiguration.wsClient.on('reconnected', () => {
        store.dispatch(Actions.setConnected(true));
      });
    }
  };
