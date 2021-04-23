import { UserContext } from '../cache/model';
import * as model from './model';

/**
 * Resolvers for the signal detection API gateway
 */

// GraphQL Resolvers
export const resolvers = {
  // Field resolvers for Channel Segment
  ChannelSegment: {
    channelId: async (
      channelSegment: model.ChannelSegment<model.TimeSeries>,
      _,
      userContext: UserContext
    ) => channelSegment.channel.name
  },

  FkPowerSpectra: {
    contribChannels: async () => []
  },

  // Field resolvers for Timeseries
  Timeseries: {
    /**
     * Special interface resolver to determine the implementing type based on field content
     */
    __resolveType(obj) {
      if (obj) {
        if (obj.spectrums !== undefined) {
          return 'FkPowerSpectra';
        }
        if (obj.values !== undefined) {
          return 'Waveform';
        }
      }
      return undefined;
    }
  }
};
