import { SohQueries, SohTypes } from '@gms/common-graphql';
import { ApolloError } from 'apollo-client';
import React from 'react';
import { Query } from 'react-apollo';
import { validateNonIdealState } from '../components/missing-lag-history/non-ideal-states';
import { MISSING_LAG } from '../components/missing-lag-history/types';

/** The historical SOH query props */
export interface HistoricalSohQueryProps {
  stationName: string;
  startTime: Date;
  endTime: Date;
  sohMonitorType: MISSING_LAG;
}

/** The historical SOH query data */
export interface HistoricalSohQueryData {
  loading: boolean;
  error: ApolloError;
  data: SohTypes.UiHistoricalSoh;
}

/** The historical SOH query context - provides the historical SOH query data to its consumers */
export const HistoricalSohQueryContext: React.Context<HistoricalSohQueryData> = React.createContext<
  HistoricalSohQueryData
>(undefined);

/**
 * The historical SOH query component
 * @param props the props
 */
export const HistoricalSohQuery: React.FunctionComponent<HistoricalSohQueryProps> = props => {
  if (props.endTime?.valueOf() < props.startTime?.valueOf()) {
    throw new Error(`Invalid start and end times provided for historical SOH query`);
  }

  const isSkipped = () =>
    props.stationName === undefined ||
    props.startTime === undefined ||
    props.endTime === undefined ||
    props.sohMonitorType === undefined ||
    props.sohMonitorType.length < 1;

  return (
    <React.Fragment>
      <Query<
        { historicalSohByStation: SohTypes.UiHistoricalSoh },
        { queryInput: SohTypes.UiHistoricalSohInput }
      >
        query={SohQueries.historicalSohByStationQuery}
        variables={{
          queryInput: {
            stationName: props.stationName,
            startTime: props.startTime.valueOf(),
            endTime: props.endTime.valueOf(),
            sohMonitorTypes: [props.sohMonitorType]
          }
        }}
        fetchPolicy={'no-cache'}
        // skip executing the query if any of these conditions are met
        skip={isSkipped()}
      >
        {({ loading, error, data }) => {
          const nonIdealState = validateNonIdealState(
            props.sohMonitorType,
            loading,
            data?.historicalSohByStation,
            props.startTime,
            props.endTime
          );
          return (
            <React.Fragment>
              <HistoricalSohQueryContext.Provider
                // update and provide the historical data to the consumers
                value={{
                  loading: loading && !isSkipped(),
                  error,
                  data: data?.historicalSohByStation
                }}
              >
                {nonIdealState ? nonIdealState : props.children}
              </HistoricalSohQueryContext.Provider>
            </React.Fragment>
          );
        }}
      </Query>
    </React.Fragment>
  );
};
