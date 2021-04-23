import { PubSub } from 'graphql-subscriptions';
import flatMap from 'lodash/flatMap';
import { UserContext } from '../../cache/model';
import { gatewayLogger as logger } from '../../log/gateway-logger';
import * as processingModel from './model';
import { ProcessingStationProcessor } from './processing-station-processor';

/**
 * Resolvers for the waveform API gateway
 */

// Create the publish/subscribe API for GraphQL subscriptions
export const pubsub = new PubSub();

// GraphQL Resolvers
logger.info('Creating GraphQL resolvers for the processing channel API...');
export const resolvers = {
  // Query resolvers
  Query: {
    // Retrieve the default set of stations configured to be included in the waveform display
    defaultProcessingStations: async (_, __, userContext: UserContext) => {
      logger.info(`Getting default processing stations. User: ${userContext.userName}`);
      return ProcessingStationProcessor.Instance().getDefaultProcessingStations();
    }
  },

  // Field resolver for Processing Station
  ProcessingStation: {
    // Returns only acquired (raw) channels. The station.channels contains derived channels as well as acquired
    channels: async (station: processingModel.ProcessingStation, _, userContext: UserContext) =>
      flatMap(station.channelGroups.map(cg => cg.channels))
  },

  // Field resolver for Processing Station
  ProcessingChannel: {
    // Returns displayable name for channel
    displayName: async (
      channel: processingModel.ProcessingChannel,
      _,
      userContext: UserContext
    ) => {
      // channel name is stationName.siteName.channelName (i.e PDAR.PD01.SHZ)
      // strip it down and return a display name of 'PD01 SHZ' (station is usually independent in UI displays)
      const names: string[] = channel.name.split('.');
      if (names && names.length === 3) {
        return `${names[1]} ${names[2]}`;
      }
      return channel.name;
    }
  }
};
