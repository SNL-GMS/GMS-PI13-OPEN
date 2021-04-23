import { getSecureRandomNumber } from '@gms/common-util';
import config from 'config';
import filter from 'lodash/filter';
import find from 'lodash/find';
import merge from 'lodash/merge';
import { CacheProcessor } from '../../cache/cache-processor';
import { ConfigurationProcessor } from '../../configuration/configuration-processor';
import { gatewayLogger as logger } from '../../log/gateway-logger';
import { HttpClientWrapper, HttpResponse } from '../../util/http-wrapper';
import * as model from './model';
import * as referenceStationMockBackend from './reference-station-mock-backend';

/**
 * API gateway processor for station-related data APIs. This class supports:
 * - data fetching & caching from the backend service interfaces
 * - mocking of backend service interfaces based on test configuration
 * - session management
 * - GraphQL query resolution from the user interface client
 */
export class ReferenceStationProcessor {
  /** The singleton instance */
  private static instance: ReferenceStationProcessor;

  /** The default network that is set and configured in the configuration settings */
  private readonly defaultNetwork: string;

  /** Local configuration settings for reference stations */
  private readonly settings: any;

  /** HTTP client wrapper for communicating with backend services */
  private readonly httpWrapper: HttpClientWrapper;

  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): ReferenceStationProcessor {
    if (ReferenceStationProcessor.instance === undefined) {
      ReferenceStationProcessor.instance = new ReferenceStationProcessor();
      ReferenceStationProcessor.instance.initialize();
    }
    return ReferenceStationProcessor.instance;
  }
  /**
   * Constructor - initialize the processor, loading settings and initializing the HTTP client wrapper.
   */
  private constructor() {
    // todo configuration-service get this from the service
    this.defaultNetwork = ConfigurationProcessor.Instance().getNetworkForUser();

    // Load configuration settings
    this.settings = config.get('referenceStation');

    // Initialize an http client
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Retrieve a collection of reference channels for the provided reference
   * station ID. If the provided station ID is undefined or does not match any
   * reference channel entries, this function returns empty list.
   * @param stationId the ID of the reference station to retrieve reference channels for
   * @returns ReferenceChannel[]
   */
  public getChannelsByStation(stationId: string): model.ReferenceChannel[] {
    // Get the sites associated to the station
    const station = this.getStationById(stationId);
    // If nothing there return empty list
    if (!station || !station.siteIds || station.siteIds.length === 0) {
      return [];
    }

    // Loop through sites adding channels
    let referenceChannels: model.ReferenceChannel[] = [];
    const referenceChannelLists = station.siteIds.map(siteId => this.getChannelsBySite(siteId));
    referenceChannelLists.forEach(
      channelList => (referenceChannels = referenceChannels.concat(channelList))
    );
    return referenceChannels;
  }

  /**
   * Retrieve a collection of reference channels for the provided reference
   * site ID. If the provided site ID is undefined or does not match any
   * reference channel entries, this function returns undefined.
   * @param siteId the ID of the reference name to retrieve reference channels for
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceChannel[]
   */
  public getChannelsBySite(
    siteId: string,
    networkName: string = this.defaultNetwork
  ): model.ReferenceChannel[] {
    return filter(CacheProcessor.Instance().getReferenceStationData(networkName).channels, {
      siteId
    });
  }

  /**
   * Retrieve a reference channel= for the provided site and channel name.
   * If the provided site and channel names do not match any reference
   * channel entries, this function returns undefined.
   * @param siteName the name of the site associated with the reference channel to retrieve
   * @param channelName the name of the reference channel to retrieve
   * @returns ReferenceChannel
   */
  public getChannelBySiteAndChannelName(
    siteName: string,
    channelName: string,
    networkName: string = this.defaultNetwork
  ): model.ReferenceChannel {
    return find(
      CacheProcessor.Instance().getReferenceStationData(networkName).channels,
      channel =>
        channel.siteName.toLowerCase() === siteName.toLowerCase() &&
        channel.name.toLowerCase() === channelName.toLowerCase()
    );
  }

  /**
   * Get channels by Ids
   * @param ids channel ids
   * @returns a reference channel[] as a promise
   */
  public async getChannelsByIds(ids: string[]): Promise<model.ReferenceChannel[]> {
    if (!ids) {
      return undefined;
    }

    return ids.map(id => this.getChannelById(id));
  }

  /**
   * Retrieve the reference channel with the provided ID.
   * If the provided ID is undefined or does not match any reference
   * channel entries, the function returns undefined.
   * @param id The ID of the reference channel to retrieve
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceChannel
   */
  public getChannelById(
    id: string,
    networkName: string = this.defaultNetwork
  ): model.ReferenceChannel {
    const requestConfig = this.settings.backend.services.channelsByIds.requestConfig;
    const channel = find(CacheProcessor.Instance().getReferenceStationData(networkName).channels, {
      id
    });

    if (channel) {
      return channel;
    }
    const query = {
      id
    };
    logger.debug(`Calling get channels by ids query: ${JSON.stringify(query)}
                      request: ${JSON.stringify(requestConfig)}`);
  }

  /**
   * Retrieve the reference channel with the provided ID.
   * If the provided ID is undefined or does not match any reference
   * channel entries, the function returns undefined.
   * @param id The ID of the reference channel to retrieve
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceChannel as a Promise
   */
  public async getChannelByVersionId(
    id: string,
    networkName: string = this.defaultNetwork
  ): Promise<model.ReferenceChannel> {
    const requestConfig = this.settings.backend.services.channelsByIds.requestConfig;
    const query = {
      versionIds: [id]
    };
    logger.info(`Calling get channels by ids query: ${JSON.stringify(query)}
                        request: ${JSON.stringify(requestConfig)}`);

    const channel = CacheProcessor.Instance().hasReferenceStationData(networkName)
      ? find(CacheProcessor.Instance().getReferenceStationData(networkName).channels, { id })
      : undefined;
    if (channel) {
      return channel;
    }
    // Call the service and process the response data
    return this.httpWrapper
      .request<model.ReferenceChannel[]>(requestConfig, query)
      .then(response => (response.data ? this.processChannelData(response.data[0]) : undefined))
      .catch(e => {
        logger.error(`processChannelData error: ${e}`);
        return undefined;
      });
  }

  /**
   * Retrieve the reference station with the provided channel ID.
   * If the provided ID is undefined or does not match any reference
   * channel entries, the function returns undefined.
   * @param channelId The ID of the reference channel to retrieve
   * @returns ReferenceStation
   */
  public getStationByChannelId(channelId: string): model.ReferenceStation {
    if (!channelId) {
      return undefined;
    }
    const channel = this.getChannelById(channelId);
    if (!channel) {
      return undefined;
    }
    const site: model.ReferenceSite = this.getSiteById(channel.siteId);
    if (!site) {
      return undefined;
    }
    return this.getStationById(site.stationId);
  }

  /**
   * Retrieve the reference channels matching the provided list of IDs.
   * If the provided list of IDs is undefined or does not match any reference
   * channel entries, the function returns undefined.
   * @param ids The list of IDs to retrieve reference channels for
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceChannel[]
   */
  public getChannelsById(
    ids: string[],
    networkName: string = this.defaultNetwork
  ): model.ReferenceChannel[] {
    return ids.map(id =>
      find(CacheProcessor.Instance().getReferenceStationData(networkName).channels, { id })
    );
  }

  /**
   * Retrieve the configured default list of reference stations to display
   * on the interactive analysis displays. If the default station configuration
   * is uninitialized, this function returns undefined.
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceStation[] as Promise
   */
  public async getDefaultStations(
    networkName: string = this.defaultNetwork
  ): Promise<model.ReferenceStation[]> {
    if (!CacheProcessor.Instance().hasReferenceStationData(networkName)) {
      await this.fetchStationData(this.defaultNetwork).then(stationDataCache => {
        if (stationDataCache && stationDataCache.stations) {
          stationDataCache.stations.forEach(station => {
            const site =
              station.siteIds.length > 0 ? this.getSiteById(station.siteIds[0]) : undefined;
            const channel =
              site && site.channelIds.length > 0
                ? this.getChannelById(site.channelIds[0])
                : undefined;
            if (station && channel) {
              CacheProcessor.Instance()
                .getReferenceStationData(networkName)
                .defaultStationInfo.push({
                  stationId: station.id,
                  channelId: channel.id
                });
            }
          });
        }
      });
    }

    // Filter the cached station data based on the default station ID list
    const dataCacheEntryForNetwork = CacheProcessor.Instance().getReferenceStationData(networkName);
    let stations = [];
    if (dataCacheEntryForNetwork && dataCacheEntryForNetwork.stations) {
      stations = filter(
        dataCacheEntryForNetwork.stations,
        station =>
          CacheProcessor.Instance()
            .getReferenceStationData(networkName)
            .defaultStationInfo.map(defaultStation => defaultStation.stationId)
            .indexOf(station.id) > -1
      );
    }
    return stations;
  }

  /**
   * Creates a list of ReferenceChannels using the default channel for each
   * station in the network
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceChannel[] loaded from default stations network
   */
  public getDefaultChannels(networkName: string = this.defaultNetwork): model.ReferenceChannel[] {
    return this.getChannelsById(
      CacheProcessor.Instance()
        .getReferenceStationData(networkName)
        .defaultStationInfo.map(info => info.channelId)
    );
  }

  /**
   * Retrieve the reference station with the provided ID.
   * If the provided ID is undefined or does not match any reference
   * station entries, the function returns undefined.
   * @param id The ID of the reference station to retrieve
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceStation
   */
  public getStationById(
    id: string,
    networkName: string = this.defaultNetwork
  ): model.ReferenceStation {
    const requestConfig = this.settings.backend.services.stationsByIds.requestConfig;
    const station = find(CacheProcessor.Instance().getReferenceStationData(networkName).stations, {
      id
    });

    if (station) {
      return station;
    }
    const query = {
      id
    };
    logger.debug(`Calling get stations by ids query: ${JSON.stringify(query)}
                      request: ${JSON.stringify(requestConfig)}`);
  }

  /**
   * Retrieve the reference station with the provided ID.
   * If the provided ID is undefined or does not match any reference
   * station entries, the function returns undefined.
   * @param id The ID of the reference station to retrieve
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceStation as a promise
   */
  public async getStationByVersionId(
    id: string,
    networkName: string = this.defaultNetwork
  ): Promise<model.ReferenceStation> {
    const requestConfig = this.settings.backend.services.stationsByIds.requestConfig;
    logger.debug(`Calling get stations by ids query: ${JSON.stringify([id])}
                        request: ${JSON.stringify(requestConfig)}`);
    const station = CacheProcessor.Instance().hasReferenceStationData(networkName)
      ? find(CacheProcessor.Instance().getReferenceStationData(networkName).stations, { id })
      : undefined;
    if (station) {
      return station;
    }
    // Call the service and process the response data
    return this.httpWrapper
      .request<model.ReferenceStation>(requestConfig, [id])
      .then(response =>
        response.data ? this.processStationData(response.data[0]).station : undefined
      )
      .catch(e => {
        logger.error(`processStationData error: ${e}`);
        return undefined;
      });
  }

  /**
   * Retrieve the reference station with the provided name.
   * If the provided name is undefined or does not match any reference
   * station entries, the function returns undefined.
   * @param name The name of the reference station to retrieve
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceStation
   */
  public getStationByName(
    name: string,
    networkName: string = this.defaultNetwork
  ): model.ReferenceStation {
    return find(CacheProcessor.Instance().getReferenceStationData(networkName).stations, { name });
  }

  /**
   * Retrieve the reference site for the provided ID.
   * If the provided ID is undefined or does not match any reference
   * site entries, the function returns undefined.
   * @param Id The id of the reference site to retrieve
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceSite
   */
  public getSiteById(id: string, networkName: string = this.defaultNetwork): model.ReferenceSite {
    return find(CacheProcessor.Instance().getReferenceStationData(networkName).sites, { id });
  }

  /**
   * Retrieve the reference sites for the provided reference station ID.
   * If the provided ID is undefined or does not match any reference
   * site entries, the function returns undefined.
   * @param stationId The reference station ID to retrieve reference sites for
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceSite[]
   */
  public getSitesByStation(
    stationId: string,
    networkName: string = this.defaultNetwork
  ): model.ReferenceSite[] {
    return filter(CacheProcessor.Instance().getReferenceStationData(networkName).sites, {
      stationId
    });
  }

  /**
   * Retrieve the default reference channel for the reference station with the
   * provided ID. If the provided reference station ID is undefined or does not
   * match any default channel entries, the function returns undefined.
   * @param stationId The ID of the reference station to retrieve the default reference channel for
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceChannel
   */
  public getDefaultChannelForStation(
    stationId: string,
    networkName: string = this.defaultNetwork
  ): model.ReferenceChannel {
    const defaultInfo = find(
      CacheProcessor.Instance().getReferenceStationData(networkName).defaultStationInfo,
      { stationId }
    );

    if (!defaultInfo) {
      throw new Error(`No default station info found for station with ID: ${stationId}`);
    }

    return find(CacheProcessor.Instance().getReferenceStationData(networkName).channels, {
      id: defaultInfo.channelId
    });
  }

  /**
   * Retrieve the reference networks for the provided list of IDs.
   * If the provided list of IDs is undefined or does not match any reference
   * network entries, the function returns undefined.
   * @param ids The list of IDs to retrieve reference networks for
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceNetwork[]
   */
  public getNetworksByIdList(
    ids: string[],
    networkName: string = this.defaultNetwork
  ): model.ReferenceNetwork[] {
    const networks = ids
      .map(id => CacheProcessor.Instance().getReferenceStationData(id))
      .filter(stationData => stationData !== undefined)
      .map(stationData => stationData.network);
    return networks;
  }

  /**
   * Retrieve the processing stations for the provided processing network ID.
   * If the provided network ID is undefined or does not match any processing
   * station entries, the function returns undefined.
   * @param networkId The ID of the reference network to retrieve reference stations for
   * @param networkName optional will use default network if none is provided
   * @returns ReferenceStation[]
   */
  public getStationsByNetworkId(
    networkId: string,
    networkName: string = this.defaultNetwork
  ): model.ReferenceStation[] {
    return filter(
      CacheProcessor.Instance().getReferenceStationData(networkName).stations,
      station => station.networkIds.indexOf(networkId) > -1
    );
  }

  /**
   * Retrieve the reference stations for the provided reference network name.
   * If the provided network name is undefined or does not match any reference
   * station entries, the function returns undefined.
   * @param networkName The name of the reference network to retrieve reference stations for
   * @returns ReferenceStation[]
   */
  public getStationsByNetworkName(networkName: string): model.ReferenceStation[] {
    const network: model.ReferenceNetwork = CacheProcessor.Instance().getReferenceStationData(
      networkName
    ).network;

    if (!network) {
      return undefined;
    }

    return filter(
      CacheProcessor.Instance().getReferenceStationData(networkName).stations,
      station => network.stationIds.indexOf(station.id) > -1
    );
  }

  /**
   * Transforms the Station response from the OSD to a ReferenceStation with sites and channels
   * @param stationResponse response from the endpoint
   * @param networkId optional networkId
   * @returns ReferenceStation, ReferenceSite[], ReferenceChannel[]
   */
  public processStationData(
    stationResponse: any,
    networkId?: string
  ): {
    station: model.ReferenceStation;
    sites: model.ReferenceSite[];
    channels: model.ReferenceChannel[];
  } {
    if (!stationResponse) return undefined;
    const stationId: string = stationResponse.id;
    const sites: model.ReferenceSite[] = [];
    const channels: model.ReferenceChannel[] = [];
    const station: model.ReferenceStation = {
      id: stationId,
      name: stationResponse.name,
      description: stationResponse.description,
      stationType: stationResponse.stationType,
      latitude: stationResponse.latitude,
      longitude: stationResponse.longitude,
      elevation: stationResponse.elevation,
      location: {
        latitudeDegrees: stationResponse.latitude,
        longitudeDegrees: stationResponse.longitude,
        elevationKm: stationResponse.elevation
      },
      siteIds: [],
      networkIds: [networkId ? networkId : undefined],
      dataAcquisition: {
        dataAcquisition: this.randomEnumSelector('dataAcquisition'),
        interactiveProcessing: this.randomEnumSelector('interactiveProcessing'),
        automaticProcessing: this.randomEnumSelector('automaticProcessing')
      }
    };

    if (stationResponse.sites) {
      // For each station, parse the list of sites from the response
      stationResponse.sites.forEach(siteResponse => {
        const siteData = this.processSiteData(siteResponse, station);
        channels.push(...siteData.channels);
        sites.push(siteData.site);
        station.siteIds.push(siteResponse.id);
      });
    }

    return { station, sites, channels };
  }

  /**
   * Initialize the station processor, fetching station data from the backend.
   * This function sets up a mock backend if configured to do so.
   */
  private initialize(): void {
    logger.info(
      'Initializing the station processor - Mock Enable: %s',
      this.settings.backend.mock.enable
    );

    // If service mocking is enabled, initialize the mock backend
    if (this.settings.backend.mock.enable) {
      referenceStationMockBackend.initialize(this.httpWrapper.createHttpMockWrapper());
    }
  }

  /**
   * Fetch station-related data from backend services for the provided network name.
   * This is an asynchronous function.
   * This function propagates errors from the underlying HTTP call.
   * Fetched data include reference networks, stations, sites, and channels.
   * @param networkName The name of the network to retrieve station-related data for
   * optional will use default network if none is provided
   * @returns StationDataCacheEntry as Promise
   */
  private async fetchStationData(
    networkName: string = this.defaultNetwork
  ): Promise<model.ReferenceStationData> {
    if (CacheProcessor.Instance().hasReferenceStationData(networkName)) {
      return CacheProcessor.Instance().getReferenceStationData(networkName);
    }
    logger.info(`Fetching reference station data for network with name: ${networkName}`);

    // Build the query to be encoded as query string parameters
    const query = [networkName];

    // Retrieve the request configuration for the service call
    const requestConfig = this.settings.backend.services.networkByName.requestConfig;

    logger.info(
      `Calling for reference network ${JSON.stringify(requestConfig, undefined, 2)} ` +
        `query ${JSON.stringify(query, undefined, 2)}`
    );
    // Call the service and process the response data
    await this.httpWrapper
      .request<model.OSDReferenceNetwork[]>(requestConfig, query)
      .then((response: HttpResponse<model.OSDReferenceNetwork[]>) => {
        // Walk the list of networks adding them to cache
        if (response && response.data) {
          response.data.forEach(network => {
            const networkData: model.ReferenceStationData = this.processNetworkData(network);
            // If the member station data cache is uninitialized, set it to the parsed response data;
            // otherwise merge the parsed response data into the existing member cache instance
            CacheProcessor.Instance().setReferenceStationData(
              CacheProcessor.Instance().hasReferenceStationData(networkName)
                ? merge(CacheProcessor.Instance().getReferenceStationData(networkName), networkData)
                : networkData
            );
          });
        }
      })
      .then(() => {
        const noDataString = CacheProcessor.Instance().getReferenceStationData(networkName)
          ? ''
          : '- No data loaded';
        logger.info(
          `Reference station data ${this.settings.backend.mock.enable ? 'mock' : 'OSD'} ` +
            `fetch complete ${noDataString}`
        );
      })
      .catch(error => logger.error(error));
    return CacheProcessor.Instance().getReferenceStationData(networkName);
  }

  /**
   * Transforms site response from the OSD to a ReferenceSite with channels
   * @param siteResponse response from endpoint
   * @param station ReferenceStation used to get station id to link site to station
   * @returns ReferenceSite, ReferenceChannel[]
   */
  private processSiteData(
    siteResponse: any,
    station: model.ReferenceStation
  ): {
    site: model.ReferenceSite;
    channels: model.ReferenceChannel[];
  } {
    if (!siteResponse) return undefined;
    const channels: model.ReferenceChannel[] = [];
    const site: model.ReferenceSite = {
      id: siteResponse.id,
      name: siteResponse.name,
      location: {
        latitudeDegrees: siteResponse.latitude,
        longitudeDegrees: siteResponse.longitude,
        elevationKm: siteResponse.elevation
      },
      stationId: station.id,
      channelIds: []
    };

    if (siteResponse.channels) {
      // For each site, parse the list of channels from the response
      siteResponse.channels.forEach(channelResponse => {
        const channel = this.processChannelData(channelResponse, site);
        site.channelIds.push(channel.id);
        channels.push(channel);
      });
    }
    return { site, channels };
  }

  /**
   * Transforms channel response from OSD to a ReferenceChannel
   * @param channelResponse response from endpoint
   * @param site optional ReferenceSite used to link channel to site and provide site name
   * @returns ReferenceChannel
   */
  private processChannelData(
    channelResponse: model.OSDReferenceChannel,
    site?: model.ReferenceSite
  ): model.ReferenceChannel {
    if (!channelResponse) return undefined;
    const channelId: string = channelResponse.id;
    const channel = {
      id: channelId,
      name: channelResponse.name,
      // TODO backend channel has a location, rather than a location code;
      // update the model and populate
      locationCode: channelResponse.locationCode,
      siteId: site ? site.id : undefined,
      siteName: site ? site.name : undefined,
      dataType: channelResponse.dataType,
      latitude: channelResponse.latitude,
      longitude: channelResponse.longitude,
      elevation: channelResponse.elevation,
      verticalAngle: channelResponse.verticalAngle,
      horizontalAngle: channelResponse.horizontalAngle,
      position: channelResponse.position,
      actualTime: channelResponse.actualTime,
      systemTime: channelResponse.systemTime,
      sampleRate: channelResponse.sampleRate,
      depth: channelResponse.depth
    };
    // TODO: Remove this check when the OSD start returning the channel position
    if (channel && !channel.position) {
      channel.position = {
        eastDisplacementKm: 0,
        northDisplacementKm: 0,
        verticalDisplacementKm: 0
      };
    }

    // Return processed channel
    return channel;
  }

  /**
   * Process station-related data response from the backend service call. Specifically,
   * parse the response JSON into model entities (reference networks, stations, sites & channels),
   * and store the parsed data in the cache
   * @param networkResponse The JSON station data response received from a backend service to be processed
   * @returns StationDataCacheEntry
   */
  private processNetworkData(
    networkResponse: model.OSDReferenceNetwork
  ): model.ReferenceStationData {
    const stationDataCache: model.ReferenceStationData = {
      network: undefined,
      stations: [],
      defaultStationInfo: [],
      sites: [],
      channels: []
    };

    // If the response data is valid, parse it into model entities and merge into the member cache
    if (networkResponse && networkResponse.stations && networkResponse.stations.length > 0) {
      const stations: model.ReferenceStation[] = [];
      const sites: model.ReferenceSite[] = [];
      const channels: model.ReferenceChannel[] = [];
      // Parse the network from the response
      const network: model.ReferenceNetwork = {
        id: networkResponse.id,
        name: networkResponse.name,
        monitoringOrganization: networkResponse.organization,
        stationIds: []
      };

      networkResponse.stations.forEach(stationResponse => {
        const stationData = this.processStationData(stationResponse, network.id);
        stationData.sites.forEach(site => sites.push(site));
        stationData.channels.forEach(channel => channels.push(channel));
        network.stationIds.push(stationData.station.id);
        stations.push(stationData.station);
      });

      stationDataCache.network = network;
      stationDataCache.stations = stations;
      stationDataCache.sites = sites;
      stationDataCache.channels = channels;

      logger.debug(
        'Updated station data cache following fetch from the backend:',
        `\n${JSON.stringify(stationDataCache, undefined, 2)}`
      );
    }
    return stationDataCache;
  }

  /**
   * Tool to generate more realistic mock data, selects a random enum attribute
   * @param enumType enum type identifier (dataAcquisition, interactiveProcessing, automaticProcessing)
   * @returns a random selection of the enumType passed in
   */
  private readonly randomEnumSelector = (enumType: string): any => {
    const min = 1;
    const max = 5;
    const base = 10;
    const bound = 4;
    let randomNumber = min + getSecureRandomNumber() * (max - min);
    let enumTypeValue: string;
    randomNumber = parseInt(randomNumber.toFixed(), base);

    if (enumType === 'dataAcquisition') {
      randomNumber % 2 ? (enumTypeValue = 'enabled') : (enumTypeValue = 'disabled');
    } else if (enumType === 'interactiveProcessing') {
      randomNumber % 2 ? (enumTypeValue = 'default') : (enumTypeValue = 'request');
    } else {
      if (randomNumber >= bound) {
        enumTypeValue = 'network';
      } else {
        randomNumber % 2 ? (enumTypeValue = 'station') : (enumTypeValue = 'disabled');
      }
    }

    return enumTypeValue;
  }
}
