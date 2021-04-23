import {
  ConfigurationTypes,
  SystemMessageSubscriptions,
  SystemMessageTypes
} from '@gms/common-graphql/lib/graphql';
import { compose } from '@gms/common-util';
import { UILogger } from '@gms/ui-apollo';
import { AppState, SystemMessageOperations } from '@gms/ui-state';
import includes from 'lodash/includes';
import React from 'react';
import { OnSubscriptionDataOptions, useSubscription } from 'react-apollo';
import * as ReactRedux from 'react-redux';
import * as Redux from 'redux';
import { graphqlUIConfigurationQuery } from '~components/react-apollo-components/queries';
import { SystemMessageReduxProps } from './types';

/**
 * Mapping redux state to the properties of the component
 *
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<SystemMessageReduxProps> => ({
  systemMessagesState: state.systemMessageState
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 *
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<SystemMessageReduxProps> =>
  Redux.bindActionCreators(
    {
      addSystemMessages: SystemMessageOperations.addSystemMessages,
      clearExpiredSystemMessages: SystemMessageOperations.clearExpiredSystemMessages,
      clearSystemMessages: SystemMessageOperations.clearSystemMessages,
      clearAllSystemMessages: SystemMessageOperations.clearAllSystemMessages
    } as any,
    dispatch
  );

/**
 * The system message subscription component.
 */
class SystemMessageSubscriptionComponent<
  T extends SystemMessageReduxProps & ConfigurationTypes.UIConfigurationQueryProps
> extends React.PureComponent<T> {
  /** constructor */
  public constructor(p: T) {
    super(p);
  }

  /** React render lifecycle method  */
  public render() {
    return (
      <this.SystemMessageSubscription key="SystemMessageSubscription">
        {this.props.children}
      </this.SystemMessageSubscription>
    );
  }

  /**
   * Updates the Redux store.
   *
   * @param systemMessages the system messages to be added to the redux store
   */
  public readonly updateReduxStore = (systemMessages: SystemMessageTypes.SystemMessage[]) => {
    // update the redux store
    new Promise((resolve, reject) => {
      const systemMessagesToUpdate = systemMessages.filter(sysMsg => {
        if (
          includes(
            this.props.systemMessagesState.systemMessages?.map(s => s.id),
            sysMsg.id
          )
        ) {
          UILogger.Instance().warn(
            `Duplicated system message received; dropping message ${sysMsg.id}`
          );
          return false;
        }
        return true;
      });

      const numberOfMessagesToDelete =
        this.props.uiConfigurationQuery.uiAnalystConfiguration.systemMessageLimit / 2;
      this.props.addSystemMessages(
        systemMessagesToUpdate,
        this.props.uiConfigurationQuery.uiAnalystConfiguration.systemMessageLimit,
        numberOfMessagesToDelete
      );
      resolve();
    }).catch(e =>
      UILogger.Instance().error(`Failed to update Redux state for system messages ${e}`)
    );
  }

  /**
   * The system message subscription component
   */
  public readonly SystemMessageSubscription: React.FunctionComponent<any> = props => {
    // set up the subscriptions for the system message data
    useSubscription<{ systemMessages: SystemMessageTypes.SystemMessage[] }>(
      SystemMessageSubscriptions.systemMessageSubscription,
      {
        fetchPolicy: 'no-cache',
        skip:
          !this.props.uiConfigurationQuery ||
          this.props.uiConfigurationQuery.loading ||
          this.props.uiConfigurationQuery.error !== undefined ||
          !this.props.uiConfigurationQuery.uiAnalystConfiguration,
        onSubscriptionData: (
          options: OnSubscriptionDataOptions<{
            systemMessages: SystemMessageTypes.SystemMessage[];
          }>
        ) => {
          this.updateReduxStore(options.subscriptionData.data.systemMessages);
        }
      }
    );

    return (
      <React.Fragment>
        {
          // provide the system message context to the children components
        }
        {...props.children}
      </React.Fragment>
    );
  }
}

/**
 * Wrap the provided component with the system message subscription and context.
 * @param Component the component to wrap
 * @param store the redux store
 */
const SystemMessageSubscription = compose(
  // connect the redux props
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  graphqlUIConfigurationQuery()
)(SystemMessageSubscriptionComponent);

/**
 * Wrap the provided component with the SOH Status Subscription.
 * @param Component the component to wrap
 * @param store the redux store
 */
export const wrapSystemMessageSubscription = (Component: any, props: any) =>
  Redux.compose()(
    class<T> extends React.PureComponent<T> {
      public render() {
        return (
          <>
            <SystemMessageSubscription />
            <Component {...props} />
          </>
        );
      }
    }
  );
