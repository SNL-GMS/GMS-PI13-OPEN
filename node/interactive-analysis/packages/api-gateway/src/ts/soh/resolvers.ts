// GraphQL Resolvers
import {
  ChannelSoh,
  StationAndStationGroupSoh,
  StationGroupSohStatus,
  UiStationSoh
} from '@gms/common-graphql/lib/graphql/soh/types';
import config from 'config';
import { UserContext } from '../cache/model';
import { SohProcessor } from './soh-processor';

// Load configuration settings
const settings = config.get('performanceMonitoring');

export const resolvers = {
  // Query Resolvers
  Query: {
    // Returns the station and station Group SOH information by time range
    stationAndStationGroupSoh: (): StationAndStationGroupSoh =>
      SohProcessor.Instance().getStationAndGroupSohWithEmptyChannels(),

    // Returns the historical lag and missing information by station and time range
    historicalSohByStation: async (_, { queryInput }) =>
      SohProcessor.Instance().getHistoricalSohData(queryInput),

    // Returns the historical acei information by station and time range
    historicalAceiByStation: async (_, { queryInput }) =>
      SohProcessor.Instance().getHistoricalAceiData(queryInput),

    // Returns the populated UiChannelSoh for a station
    channelSohForStation: (
      _,
      { stationName }
    ): { channelSohs: ChannelSoh[]; stationName: string; uuid: string } => {
      const stationSoh = SohProcessor.Instance().getSohForStation(stationName);
      if (!stationSoh) {
        return {
          channelSohs: [],
          stationName,
          uuid: undefined
        };
      }
      return {
        channelSohs: stationSoh.channelSohs,
        stationName: stationSoh.stationName,
        uuid: stationSoh.id
      };
    }
  },

  // Mutation Resolvers
  Mutation: {
    // Acknowledges the SOH status for the provided station names
    acknowledgeSohStatus: (_, { stationNames, comment }, userContext: UserContext) => {
      SohProcessor.Instance().publishAcknowledgeSohStatus(userContext, stationNames, comment);
      return true;
    },
    quietChannelMonitorStatuses: async (
      _,
      { channelMonitorsToQuiet },
      userContext: UserContext
    ) => {
      SohProcessor.Instance().publishQuietChannelMonitorStatuses(
        userContext,
        channelMonitorsToQuiet
      );
      return { stationGroups: [], stationSoh: [], isUpdateResponse: false };
    }
  },

  // Subscription Resolvers
  Subscription: {
    // SOH Status subscription - returns the latest updated SOH data
    sohStatus: {
      subscribe: () =>
        SohProcessor.Instance().pubsub.asyncIterator(settings.subscriptions.channels.sohStatus)
    }
  },

  // Field Resolvers
  // TODO: Fix the time field names so not needed to convert to epoch seconds to theUI
  UiStationSoh: {
    time: (uiStationSoh: UiStationSoh) => uiStationSoh.time / 1000
  },
  StationGroupSohStatus: {
    time: (stationGroup: StationGroupSohStatus): number => stationGroup.time / 1000
  }
};
