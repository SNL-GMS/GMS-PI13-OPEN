import { SohQueries, SohTypes } from '@gms/common-graphql';
import { ApolloError } from 'apollo-client';
import React from 'react';
import { Query } from 'react-apollo';

/** The historical acei query props */
export interface HistoricalAceiQueryProps {
  stationName: string;
  startTime: Date;
  endTime: Date;
  type: SohTypes.AceiType;
}

/** The historical acei query data */
export interface HistoricalAceiQueryData {
  loading: boolean;
  error: ApolloError;
  data: SohTypes.UiHistoricalAcei[];
}

/** The historical acei query context - provides the historical acei query data to its consumers */
export const HistoricalAceiQueryContext: React.Context<HistoricalAceiQueryData> = React.createContext<
  HistoricalAceiQueryData
>(undefined);

/**
 * The historical acei query component
 * @param props the props
 */
export const HistoricalAceiQuery: React.FunctionComponent<HistoricalAceiQueryProps> = props => (
  <React.Fragment>
    <Query<
      { historicalAceiByStation: SohTypes.UiHistoricalAcei[] },
      { queryInput: SohTypes.UiHistoricalAceiInput }
    >
      query={SohQueries.historicalAceiByStationQuery}
      variables={{
        queryInput: {
          stationName: props.stationName,
          startTime: props.startTime.valueOf(),
          endTime: props.endTime.valueOf(),
          type: props.type
        }
      }}
      fetchPolicy={'no-cache'}
      // skip executing the query if any of these conditions are met
      skip={
        props.stationName === undefined ||
        props.startTime === undefined ||
        props.endTime === undefined ||
        props.type === undefined
      }
    >
      {({ loading, error, data }) => (
        <React.Fragment>
          <HistoricalAceiQueryContext.Provider
            // update and provide the historical data to the consumers
            value={{
              loading,
              error,
              data: data?.historicalAceiByStation
            }}
          >
            {props.children}
          </HistoricalAceiQueryContext.Provider>
        </React.Fragment>
      )}
    </Query>
  </React.Fragment>
);
