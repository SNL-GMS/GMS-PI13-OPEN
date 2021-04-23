import { SohQueries, SohSubscriptions, SohTypes } from '@gms/common-graphql';
import { MILLISECONDS_IN_SECOND, setDecimalPrecision, toOSDTime } from '@gms/common-util';
import { UILogger } from '@gms/ui-apollo';
import {
  AppState,
  DataAcquisitionWorkspaceActions,
  DataAcquisitionWorkspaceTypes
} from '@gms/ui-state';
import ApolloClient, { ApolloError } from 'apollo-client';
import unionBy from 'lodash/unionBy';
import React from 'react';
import {
  OnSubscriptionDataOptions,
  useApolloClient,
  useQuery,
  useSubscription
} from 'react-apollo';
import * as ReactRedux from 'react-redux';
import * as Redux from 'redux';

interface ReduxProps {
  selectedStationIds: string[];
  sohStatus: DataAcquisitionWorkspaceTypes.SohStatus;
  setSohStatus(sohStatus: DataAcquisitionWorkspaceTypes.SohStatus): void;
}

/**
 * Mapping redux state to the properties of the component
 *
 * @param state App state, root level redux store
 */
const mapStateToProps = (state: AppState): Partial<ReduxProps> => ({
  selectedStationIds: state.dataAcquisitionWorkspaceState.selectedStationIds,
  sohStatus: state.dataAcquisitionWorkspaceState.data.sohStatus
});

/**
 * Mapping methods (actions and operations) to dispatch one or more updates to the redux store
 *
 * @param dispatch the redux dispatch event alerting the store has changed
 */
const mapDispatchToProps = (dispatch): Partial<ReduxProps> =>
  Redux.bindActionCreators(
    {
      setSohStatus: DataAcquisitionWorkspaceActions.setSohStatus
    } as any,
    dispatch
  );

/**
 * Wrap the provided component with the SOH Status Subscription.
 * @param Component the component to wrap
 * @param store the redux store
 */
export const wrapSohStatusSubscriptions = (
  Component: any,
  props: any,
  store: Redux.Store<AppState>
) =>
  Redux.compose(
    // connect the redux props
    ReactRedux.connect(
      mapStateToProps,
      mapDispatchToProps
    )(
      class<T extends ReduxProps> extends React.PureComponent<T> {
        public constructor(p: T) {
          super(p);
        }

        public render() {
          return (
            <>
              <this.SohQueryAndSubscription key="SohQueryAndSubscription" />
              <Component store={store} {...props} />
            </>
          );
        }

        /**
         * Query for the Channel SOH Station data
         */
        public readonly queryChannelSohForStationQuery = async (
          client: ApolloClient<object>
        ): Promise<SohTypes.ChannelSohForStation> => {
          const channelSohForStationQuery =
            this.props.selectedStationIds && this.props.selectedStationIds.length === 1
              ? await client.query<{ channelSohForStation: SohTypes.ChannelSohForStation }>({
                  query: SohQueries.channelSohForStationQuery,
                  variables: { stationName: this.props.selectedStationIds[0] },
                  fetchPolicy: 'no-cache'
                })
              : undefined;
          return channelSohForStationQuery
            ? channelSohForStationQuery.data.channelSohForStation
            : undefined;
        }

        /**
         * Updates the Redux store for the SOH Status
         *
         * @param lastUpdated the timestamp the data was last updated
         * @param loading the loading status of the query for the SOH Station data
         * @param error any error status on the query
         * @param stationAndStationGroupSoh the station and station group SOH data - does not include the channel data
         * @param channelSohForStation the channel SOH data for a given station
         * @param callback the callback to be executed after updating the store
         */
        public readonly updateReduxStore = (
          lastUpdated: number,
          loading: boolean,
          error: ApolloError,
          stationAndStationGroupSoh: SohTypes.StationAndStationGroupSoh,
          channelSohForStation: SohTypes.ChannelSohForStation = undefined,
          callback: () => void = undefined
        ) => {
          // update the redux store
          new Promise((resolve, reject) => {
            // merge station soh data
            const mergedStationAndStationGroupSoh: SohTypes.StationAndStationGroupSoh = {
              stationGroups: unionBy(
                stationAndStationGroupSoh.stationGroups,
                this.props.sohStatus.stationAndStationGroupSoh.stationGroups,
                'id'
              ),
              stationSoh: unionBy(
                stationAndStationGroupSoh.stationSoh,
                this.props.sohStatus.stationAndStationGroupSoh.stationSoh,
                'id'
              ),
              isUpdateResponse: stationAndStationGroupSoh.isUpdateResponse
            };

            // update the channel data for the station
            if (
              channelSohForStation &&
              channelSohForStation.stationName &&
              channelSohForStation.channelSohs
            ) {
              // sort the channel soh data
              channelSohForStation.channelSohs.sort(
                (a: SohTypes.ChannelSoh, b: SohTypes.ChannelSoh) =>
                  a.channelName.localeCompare(b.channelName)
              );

              mergedStationAndStationGroupSoh.stationSoh.find(
                s => s.stationName === channelSohForStation.stationName
              ).channelSohs = channelSohForStation.channelSohs;
            }

            // sort the station soh data by name
            mergedStationAndStationGroupSoh.stationSoh.sort(
              (a: SohTypes.UiStationSoh, b: SohTypes.UiStationSoh) =>
                a.stationName.localeCompare(b.stationName)
            );

            this.props.setSohStatus({
              lastUpdated,
              loading,
              error,
              stationAndStationGroupSoh: mergedStationAndStationGroupSoh
            });
            resolve();
          })
            .then(
              callback
                ? callback
                : () => {
                    /* no-op */
                  }
            )
            .catch(e =>
              UILogger.Instance().error(`Failed to update Redux state for SOH Status ${e}`)
            );
        }

        /**
         * The SOH Query and Subscription component
         */
        public readonly SohQueryAndSubscription: React.FunctionComponent<any> = p => {
          const client = useApolloClient();

          React.useEffect(() => {
            this.queryChannelSohForStationQuery(client)
              .then(channelSohForStation => {
                this.updateReduxStore(
                  this.props.sohStatus.lastUpdated,
                  this.props.sohStatus.loading,
                  this.props.sohStatus.error,
                  this.props.sohStatus.stationAndStationGroupSoh,
                  channelSohForStation
                );
              })
              .catch(error =>
                UILogger.Instance().error(`Failed to queryChannelSohForStationQuery ${error}`)
              );
          }, [this.props.selectedStationIds]);

          // setup the query for station and station group data
          const StationAndStationGroupSohQuery = useQuery<{
            stationAndStationGroupSoh: SohTypes.StationAndStationGroupSoh;
          }>(SohQueries.sohStationAndGroupStatusQuery, {
            client,
            fetchPolicy: 'no-cache',
            onCompleted: data => {
              // update the redux store from the station query
              this.updateReduxStore(
                Date.now(),
                StationAndStationGroupSohQuery.loading,
                StationAndStationGroupSohQuery.error,
                data.stationAndStationGroupSoh
              );
            }
          });

          // set up the subscriptions for SOH data
          useSubscription<{ sohStatus: SohTypes.StationAndStationGroupSoh }>(
            SohSubscriptions.sohStatusSubscription,
            {
              client,
              fetchPolicy: 'no-cache',
              onSubscriptionData: async (
                options: OnSubscriptionDataOptions<{
                  sohStatus: SohTypes.StationAndStationGroupSoh;
                }>
              ) => {
                const now = Date.now();

                // query the channel SOH data for the selected station
                const channelSohForStation = await this.queryChannelSohForStationQuery(client);

                // update the redux store
                this.updateReduxStore(
                  now,
                  StationAndStationGroupSohQuery.loading,
                  StationAndStationGroupSohQuery.error,
                  options.subscriptionData.data.sohStatus,
                  channelSohForStation,
                  () => {
                    new Promise((resolve, reject) => {
                      // log timing point messages
                      const updatedStationSoh = [
                        ...options.subscriptionData.data.sohStatus.stationSoh
                      ];

                      // Do not log timing point C for any Ack/Quiet responses
                      const isUpdateResponse =
                        options.subscriptionData.data.sohStatus.isUpdateResponse;
                      if (!isUpdateResponse) {
                        const timingPointMessages = updatedStationSoh.map(
                          stationSoh =>
                            `Timing point C: SOH object ${
                              stationSoh.uuid
                            } displayed in UI at ${toOSDTime(
                              now / MILLISECONDS_IN_SECOND
                            )} A->C ${setDecimalPrecision(
                              now / MILLISECONDS_IN_SECOND - stationSoh.time,
                              3
                            )} seconds`
                        );

                        // Call reporting timing points mutation to record in the UI Backend log
                        UILogger.Instance().timing(...timingPointMessages);
                      }
                      resolve();
                    });
                  }
                );
              }
            }
          );

          return <React.Fragment>{...p.children}</React.Fragment>;
        }
      }
    )
  );
