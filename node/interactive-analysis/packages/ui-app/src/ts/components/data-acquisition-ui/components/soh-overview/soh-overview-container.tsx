import { compose } from '@gms/common-util';
import { WithNonIdealStates } from '@gms/ui-core-components';
import { AppState, DataAcquisitionWorkspaceOperations } from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { CommonNonIdealStateDefs } from '~components/common-ui/components/non-ideal-states';
import { DataAcquisitionNonIdealStateDefs } from '~components/data-acquisition-ui/shared/non-ideal-states';
import { SohReduxProps } from '~components/data-acquisition-ui/shared/types';
import { ReactApolloQueries } from '~components/react-apollo-components';
import { ReactApolloMutations as ReactApolloSohMutations } from '~data-acquisition-ui/react-apollo-components';
import { SohOverviewComponent } from './soh-overview-component';
import { SohOverviewProps } from './types';

/**
 * Mapping redux state to the properties of the component
 *
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<SohReduxProps> => ({
  selectedStationIds: state.dataAcquisitionWorkspaceState.selectedStationIds,
  sohStatus: state.dataAcquisitionWorkspaceState.data.sohStatus
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 *
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<SohReduxProps> =>
  bindActionCreators(
    {
      setSelectedStationIds: DataAcquisitionWorkspaceOperations.setSelectedStationIds
    } as any,
    dispatch
  );

/**
 * Renders the Overview component, or a non-ideal state
 */
const OverviewComponentOrNonIdealState = WithNonIdealStates<SohOverviewProps>(
  [
    ...CommonNonIdealStateDefs.baseNonIdealStateDefinitions,
    ...DataAcquisitionNonIdealStateDefs.generalSohNonIdealStateDefinitions
  ],
  SohOverviewComponent
);

/**
 * A new apollo component that's wrapping the SohOverview component and injecting
 * apollo graphQL queries and mutations.
 */
export const ApolloSohOverviewContainer = compose(
  ReactRedux.connect(mapStateToProps, mapDispatchToProps),
  ReactApolloQueries.graphqlUIConfigurationQuery(),
  ReactApolloSohMutations.graphqlAcknowledgeSohStatusMutation()
)(OverviewComponentOrNonIdealState);
