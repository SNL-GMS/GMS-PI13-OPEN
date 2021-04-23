import { compose } from '@gms/common-util';
import { WithNonIdealStates } from '@gms/ui-core-components';
import { AppState, SystemMessageOperations } from '@gms/ui-state';
import { withApollo } from 'react-apollo';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import {
  ReactApolloMutations,
  ReactApolloQueries
} from '~components/common-ui/react-apollo-components';
import { CommonNonIdealStateDefs } from '../non-ideal-states';
import { SystemMessage } from './system-message-component';
import { SystemMessageProps, SystemMessageReduxProps } from './types';
/**
 * Mapping redux state to the properties of the component
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<SystemMessageReduxProps> => ({
  systemMessagesState: state.systemMessageState
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<SystemMessageReduxProps> =>
  bindActionCreators(
    {
      addSystemMessages: SystemMessageOperations.addSystemMessages,
      clearExpiredSystemMessages: SystemMessageOperations.clearExpiredSystemMessages,
      clearSystemMessages: SystemMessageOperations.clearSystemMessages,
      clearAllSystemMessages: SystemMessageOperations.clearAllSystemMessages
    } as any,
    dispatch
  );

/**
 * Renders the system message display, or a non-ideal state from the provided list of
 * non ideal state definitions
 */
const SystemMessageComponentOrNonIdealState = WithNonIdealStates<SystemMessageProps>(
  [
    ...CommonNonIdealStateDefs.baseNonIdealStateDefinitions,
    ...CommonNonIdealStateDefs.systemMessageNonIdealStateDefinitions
  ],
  SystemMessage
);

/**
 * A new redux apollo component, that's wrapping the SystemMessage component and injecting in the redux state
 * and apollo graphQL queries and mutations.
 */
export const ReduxApolloSystemMessageContainer = compose(
  withApollo,
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  ReactApolloQueries.graphqlSystemMessageDefinitionsQuery(),
  ReactApolloQueries.graphqlUserProfileQuery(),
  ReactApolloMutations.graphqlSetAudibleNotificationsMutation()
)(SystemMessageComponentOrNonIdealState);
