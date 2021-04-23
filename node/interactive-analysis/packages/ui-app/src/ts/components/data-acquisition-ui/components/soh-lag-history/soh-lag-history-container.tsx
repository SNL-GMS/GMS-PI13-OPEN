import { SohTypes } from '@gms/common-graphql';
import { compose } from '@gms/common-util';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import { WithNonIdealStates } from '@gms/ui-core-components';
import { AppState } from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { CommonNonIdealStateDefs } from '~components/common-ui/components/non-ideal-states';
import { DataAcquisitionNonIdealStateDefs } from '~components/data-acquisition-ui/shared/non-ideal-states';
import { graphqlUIConfigurationQuery } from '~components/react-apollo-components/queries';
import { SohReduxProps } from '../../shared/types';
import { buildSohMissingLagHistoryComponent } from '../missing-lag-history/missing-lag-history-component';
import { SohMissingLagHistoryComponentProps } from '../missing-lag-history/types';

/**
 * Create a lag history component using the shared missing/lag history component
 */
const LagHistoryComponent = buildSohMissingLagHistoryComponent(
  SohTypes.SohMonitorType.LAG,
  ValueType.INTEGER
);

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
 * Renders the Lag History display, or a non-ideal state from the provided list of
 * non ideal state definitions
 */
const LagHistoryComponentOrNonIdealState = WithNonIdealStates<SohMissingLagHistoryComponentProps>(
  [
    ...CommonNonIdealStateDefs.baseNonIdealStateDefinitions,
    ...DataAcquisitionNonIdealStateDefs.generalSohNonIdealStateDefinitions,
    ...DataAcquisitionNonIdealStateDefs.stationSelectedSohNonIdealStateDefinitions,
    ...DataAcquisitionNonIdealStateDefs.channelSohNonIdealStateDefinitions
  ],
  LagHistoryComponent
);

/**
 * A new apollo component that's wrapping the SohOverview component and injecting
 * apollo graphQL queries and mutations.
 */
export const ApolloSohLagHistoryContainer = compose(
  ReactRedux.connect(mapStateToProps),
  graphqlUIConfigurationQuery()
)(LagHistoryComponentOrNonIdealState);
