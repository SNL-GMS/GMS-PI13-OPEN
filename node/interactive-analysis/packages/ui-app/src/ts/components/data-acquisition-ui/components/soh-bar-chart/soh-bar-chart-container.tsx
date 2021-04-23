import { SohTypes } from '@gms/common-graphql';
import { compose } from '@gms/common-util';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import { WithNonIdealStates } from '@gms/ui-core-components';
import { AppState, DataAcquisitionWorkspaceOperations } from '@gms/ui-state';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { CommonNonIdealStateDefs } from '~components/common-ui/components/non-ideal-states';
import { DataAcquisitionNonIdealStateDefs } from '~components/data-acquisition-ui/shared/non-ideal-states';
import { graphqlUIConfigurationQuery } from '~components/react-apollo-components/queries';
import { ReactApolloMutations } from '~data-acquisition-ui/react-apollo-components';
import { SohBarChart } from './soh-bar-chart-component';
import { SohBarChartProps, Type } from './types';

/**
 * Mapping redux state to the properties of the component
 * @param state App state, root level redux store
 */
const mapStateToProps = (type: Type, valueType: ValueType) => (
  state: AppState
): Partial<SohBarChartProps> => ({
  type,
  valueType,
  selectedStationIds: state.dataAcquisitionWorkspaceState.selectedStationIds,
  sohStatus: state.dataAcquisitionWorkspaceState.data.sohStatus
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<SohBarChartProps> =>
  bindActionCreators(
    {
      setSelectedStationIds: DataAcquisitionWorkspaceOperations.setSelectedStationIds
    } as any,
    dispatch
  );

/**
 * Renders the SohBarChart component, or a non-ideal state from the provided list of
 * non ideal state definitions
 */
const SohBarChartComponentOrNonIdealState = WithNonIdealStates<SohBarChartProps>(
  [
    ...CommonNonIdealStateDefs.baseNonIdealStateDefinitions,
    ...DataAcquisitionNonIdealStateDefs.generalSohNonIdealStateDefinitions,
    ...DataAcquisitionNonIdealStateDefs.stationSelectedSohNonIdealStateDefinitions,
    ...DataAcquisitionNonIdealStateDefs.channelSohNonIdealStateDefinitions
  ],
  SohBarChart
);

const SohBarChartComponent = (type: Type, valueType: ValueType) =>
  compose(
    ReactRedux.connect(mapStateToProps(type, valueType), mapDispatchToProps),
    graphqlUIConfigurationQuery(),
    ReactApolloMutations.graphqlQuietChannelMonitorStatusesMutation()
  )(SohBarChartComponentOrNonIdealState);

export const SohLag = SohBarChartComponent(SohTypes.SohMonitorType.LAG, ValueType.INTEGER);
export const SohMissing = SohBarChartComponent(
  SohTypes.SohMonitorType.MISSING,
  ValueType.PERCENTAGE
);
export const SohTimeliness = SohBarChartComponent(
  SohTypes.SohMonitorType.TIMELINESS,
  ValueType.INTEGER
);
