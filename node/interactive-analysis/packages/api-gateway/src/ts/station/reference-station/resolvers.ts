import { UserContext } from '../../cache/model';
import { gatewayLogger as logger } from '../../log/gateway-logger';
import * as referenceModel from './model';
import { ReferenceStationProcessor } from './reference-station-processor';

/**
 * Resolvers for the reference station API gateway
 */

// GraphQL Resolvers
logger.info('Creating GraphQL resolvers for the processing channel API...');
export const resolvers = {
  // Query resolvers
  Query: {
    // Retrieve the default set of stations configured to be included in the waveform display
    defaultReferenceStations: async (_, __, userContext: UserContext) => {
      logger.info(`Getting default reference stations. User: ${userContext.userName}`);
      return ReferenceStationProcessor.Instance().getDefaultStations();
    }
  },

  // Field resolvers for Channel
  ReferenceChannel: {
    site: async (channel: referenceModel.ReferenceChannel, _, userContext: UserContext) =>
      ReferenceStationProcessor.Instance().getSiteById(channel.siteId),
    channelType: async (channel: referenceModel.ReferenceChannel, _, userContext: UserContext) =>
      channel.dataType
  },

  // Field resolvers for Site
  ReferenceSite: {
    station: async (site: referenceModel.ReferenceSite, _, userContext: UserContext) =>
      ReferenceStationProcessor.Instance().getStationById(site.stationId),
    channels: async (site: referenceModel.ReferenceSite, _, userContext: UserContext) =>
      ReferenceStationProcessor.Instance().getChannelsBySite(site.id),
    defaultChannel: async (site: referenceModel.ReferenceSite, _, userContext: UserContext) =>
      ReferenceStationProcessor.Instance().getDefaultChannelForStation(site.stationId)
  },

  // Field resolvers for Station
  ReferenceStation: {
    sites: async (station: referenceModel.ReferenceStation, _, userContext: UserContext) =>
      ReferenceStationProcessor.Instance().getSitesByStation(station.id),
    defaultChannel: async (station: referenceModel.ReferenceStation, _, userContext: UserContext) =>
      ReferenceStationProcessor.Instance().getDefaultChannelForStation(station.id),
    networks: async (station: referenceModel.ReferenceStation, _, userContext: UserContext) =>
      ReferenceStationProcessor.Instance().getNetworksByIdList(station.networkIds)
  },

  // Field resolvers for Network
  ReferenceNetwork: {
    stations: async (network: referenceModel.ReferenceNetwork, _, userContext: UserContext) =>
      ReferenceStationProcessor.Instance().getStationsByNetworkId(network.id)
  }
};
