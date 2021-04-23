import { SOHStationGroupNameWithPriority } from '@gms/common-graphql/lib/graphql/ui-configuration/types';
import { sleep } from '@gms/common-util';
import config from 'config';
import * as geolib from 'geolib';
import { uniqBy } from 'lodash';
import { CacheProcessor } from '../../cache/cache-processor';
import { UserContext } from '../../cache/model';
import {
  Distance,
  DistanceSourceType,
  DistanceToSource,
  DistanceToSourceInput,
  Location
} from '../../common/model';
import { ConfigurationProcessor } from '../../configuration/configuration-processor';
import { Event } from '../../event/model-and-schema/model';
import { gatewayLogger, gatewayLogger as logger } from '../../log/gateway-logger';
import { HttpClientWrapper, HttpResponse } from '../../util/http-wrapper';
import * as model from './model';
import * as processingStationMockBackend from './processing-station-mock-backend';

/**
 * API gateway processor for station-related data APIs. This class supports:
 * - data fetching & caching from the backend service interfaces
 * - mocking of backend service interfaces based on test configuration
 * - session management
 * - GraphQL query resolution from the user interface client
 */
export class ProcessingStationProcessor {
  /** The singleton instance */
  private static instance: ProcessingStationProcessor;

  /** Conversion km to degrees. 1 degree = 6371*pi/180 km = 111.1949266 km. */
  private readonly KM_TO_DEGREES: number = 111.1949266;

  /** Local configuration settings for reference stations */
  private readonly settings: any;

  /** HTTP client wrapper for communicating with backend services */
  private readonly httpWrapper: HttpClientWrapper;

  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): ProcessingStationProcessor {
    if (ProcessingStationProcessor.instance === undefined) {
      ProcessingStationProcessor.instance = new ProcessingStationProcessor();
      ProcessingStationProcessor.instance.initialize();
    }
    return ProcessingStationProcessor.instance;
  }

  /**
   * Constructor - initialize the processor, loading settings and initializing the HTTP client wrapper.
   */
  private constructor() {
    // Load configuration settings
    this.settings = config.get('processingStation');

    // Initialize an http client
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Retrieve a list of SOH StationGroup Names the SOH Station is associated to
   * @param stationName SOH Station Name
   * @returns Station Group Names
   */
  public getSohStationGroupNames(stationName: string): SOHStationGroupNameWithPriority[] {
    if (
      CacheProcessor.Instance()
        .getProcessingStationData()
        .sohStationGroupNameMap.has(stationName)
    ) {
      return CacheProcessor.Instance()
        .getProcessingStationData()
        .sohStationGroupNameMap.get(stationName);
    }
    return [];
  }

  /**
   * Retrieve a collection of processing channels for the provided processing
   * station ID. If the provided station ID is undefined or does not match any
   * processing channel entries, this function returns empty list.
   * @param stationId the ID of the processing station to retrieve processing channels for
   * @returns ProcessingChannel[]
   */
  public getChannelsByStation(name: string): model.ProcessingChannel[] {
    // Get the sites associated to the station
    const station = this.getStationByName(name);

    if (station && station.channels) {
      return station.channels;
    }
    return [];
  }

  /**
   * Retrieve a collection of processing channels for the provided processing
   * ChannelGroup name. If the provided ChannelGroup name is undefined or does not match any
   * processing channel entries, this function returns an empty list.
   * @param channelGroupName the name of the processing name to retrieve processing channels for
   * @returns ProcessingChannel[]
   */
  public getChannelsByChannelGroupName(channelGroupName: string): model.ProcessingChannel[] {
    const channelGroup = CacheProcessor.Instance()
      .getProcessingStationData()
      .channelGroupMap.get(channelGroupName);
    if (channelGroup && channelGroup.channels) {
      return channelGroup.channels;
    }
    return [];
  }

  /**
   * Get channels by names or returns empty list
   * @param names channel names
   * @returns a processing channel[] as a promise
   */
  public async getChannelsByNames(names: string[]): Promise<model.ProcessingChannel[]> {
    if (!names) {
      return [];
    }
    return names.map(name => this.getChannelByName(name)).filter(chan => chan !== undefined);
  }

  /**
   * Retrieve the processing channel with the provided name.
   * If the provided name is undefined or does not match any processing
   * channel entries, the function returns undefined.
   * @param name The name of the processing channel to retrieve
   * @returns ProcessingChannel
   */
  public getChannelByName(name: string): model.ProcessingChannel {
    if (!name) {
      return undefined;
    }
    return CacheProcessor.Instance()
      .getProcessingStationData()
      .channelMap.get(name);
  }

  /**
   * Retrieve the processing station with the provided channel name.
   * If the provided name is undefined or does not match any processing
   * channel entries, the function returns undefined.
   * @param channelName The name of the processing channel to retrieve
   * @returns ProcessingStation
   */
  public getStationByChannelName(channelName: string): model.ProcessingStation {
    if (!channelName) {
      return undefined;
    }
    const channel = this.getChannelByName(channelName);
    if (!channel) {
      return undefined;
    }
    return this.getStationByName(channel.station);
  }

  /**
   * Retrieve the configured default list of processing stations to display
   * on the interactive analysis displays. If the default station configuration
   * is uninitialized, this function returns empty list.
   * @returns ProcessingStation[]
   */
  public getDefaultProcessingStations(): model.ProcessingStation[] {
    const defaultStationMap = CacheProcessor.Instance().getProcessingStationData().stationMap;
    if (!defaultStationMap) {
      logger.warn(`Default processing station map not found`);
      return [];
    }
    const stations: model.ProcessingStation[] = Array.from(defaultStationMap.values());
    logger.info(`Found ${stations.length} default processing stations`);
    return stations;
  }

  /**
   * Retrieve the configured soh station groups and all stations contained
   * @returns ProcessingStation[] of unique stations
   */
  public getSohProcessingStations(): model.ProcessingStation[] {
    let stations = [];
    try {
      const sohStationGroups = ConfigurationProcessor.Instance().getSohConfiguration()
        .stationSohControlConfiguration.displayedStationGroups;

      sohStationGroups.forEach(stationGroupName => {
        const stationGroup = CacheProcessor.Instance()
          .getProcessingStationData()
          .stationGroupMap.get(stationGroupName);
        stations.push(...stationGroup.stations);
      });
      stations = uniqBy(stations, s => s.name);

      gatewayLogger.info(`Stations by group name has ${stations.length}`);
    } catch (error) {
      gatewayLogger.error(`Failed to get the stations by group name ${error}`);
    }

    const allStations = [
      ...CacheProcessor.Instance()
        .getProcessingStationData()
        .stationMap.values()
    ];

    gatewayLogger.info(`All stations has ${allStations.length}`);

    return allStations;
  }

  /**
   * Creates a list of the first channel of each station in defaultNetwork
   * @returns ProcessingChannel[] loaded from default stations network
   */
  public getDefaultChannels(): model.ProcessingChannel[] {
    const defaultStations: model.ProcessingStation[] = this.getDefaultProcessingStations();
    if (!defaultStations) {
      return [];
    }
    // Return first channel in each station channels list
    return defaultStations.map(sta => sta.channels[0]);
  }

  /**
   * Retrieve the processing station with the provided name.
   * If the provided name is undefined or does not match any processing
   * station entries, the function returns undefined.
   * @param name The name of the processing station to retrieve
   * @returns ProcessingStation
   */
  public getStationByName(name: string): model.ProcessingStation {
    return CacheProcessor.Instance()
      .getProcessingStationData()
      .stationMap.get(name);
  }

  /**
   * Retrieve the processing ChannelGroup for the provided ID.
   * If the provided ID is undefined or does not match any processing
   * ChannelGroup entries, the function returns undefined.
   * @param name The name of the processing ChannelGroup to retrieve
   * @returns ProcessingChannelGroup
   */
  public getChannelGroupByName(name: string): model.ProcessingChannelGroup {
    return CacheProcessor.Instance()
      .getProcessingStationData()
      .channelGroupMap.get(name);
  }

  /**
   * Retrieve the processing channel groups for the provided processing station name.
   * If the provided name is undefined or does not match any processing station,
   * the function returns undefined.
   * @param name The processing station ID to retrieve processing sites for
   * @returns ProcessingChannelGroup[]
   */
  public getChannelGroupsByStation(name: string): model.ProcessingChannelGroup[] {
    if (!name) {
      return undefined;
    }
    const station = this.getStationByName(name);
    // If station is good return channel groups
    if (station) {
      return station.channelGroups;
    }
    return undefined;
  }

  /**
   * Retrieve the default (first) processing channel for the processing station with the
   * provided name. If the provided processing station name is undefined or does not
   * match any default channel entries, the function returns undefined.
   * @param name The name of the processing station to retrieve the default processing channel for
   * @returns ProcessingChannel
   */
  public getDefaultChannelForStation(name: string): model.ProcessingChannel {
    if (!name) {
      return undefined;
    }

    const station = this.getStationByName(name);
    if (station && station.channels) {
      return station.channels[0];
    }
    return undefined;
  }

  /**
   * Retrieve the processing station groups for the provided list of names.
   * If the provided list of names is undefined or does not match any processing
   * network entries, the function returns an empty array.
   * @param names The list of names of the station groups
   * @returns ProcessingNetwork[]
   */
  public getStationGroupsByNames(names: string[]): model.ProcessingStationGroup[] {
    return names.map(name =>
      CacheProcessor.Instance()
        .getProcessingStationData()
        .stationGroupMap.get(name)
    );
  }

  /**
   * Retrieve the processing stations for the provided processing station group name.
   * If the provided station group name is undefined or does not match any processing
   * station entries, the function returns empty list.
   * @param name The name of the processing station group to retrieve processing stations for
   * @returns ProcessingStation[]
   */
  public getStationsByStationGroupName(name: string): model.ProcessingStation[] {
    if (!name) {
      return [];
    }
    const stationGroup = CacheProcessor.Instance()
      .getProcessingStationData()
      .stationGroupMap.get(name);
    if (stationGroup && stationGroup.stations) {
      return stationGroup.stations;
    }

    return [];
  }

  /**
   * Populate the distance to source list using the default stations
   * @param dTSInput distance to source input
   * @returns DistanceToSource[] as Promise
   */
  public getDTSForDefaultStations(
    userContext: UserContext,
    dTSInput: DistanceToSourceInput
  ): DistanceToSource[] {
    const distanceToSourceList: DistanceToSource[] = [];
    const stations = this.getDefaultProcessingStations();
    stations.forEach(station => {
      this.getDTSWithDistance(userContext, dTSInput, station)
        .then(distanceToSource => {
          if (distanceToSource) {
            distanceToSourceList.push(distanceToSource);
          }
        })
        .catch(e => logger.warn(e));
    });
    return distanceToSourceList;
  }

  /**
   * Populate the distance to source object with the distance
   * @param dTSInput distance to source input
   * @param station ProcessingStation
   * @returns DistanceToSource as Promise
   */
  public async getDTSWithDistance(
    userContext: UserContext,
    dTSInput: DistanceToSourceInput,
    station: model.ProcessingStation
  ): Promise<DistanceToSource> {
    // If no source location set and this is an source type of event
    // with the sourceId (eventHypId) set then look up the source loc
    // TODO: this will return a hypothesis and not an event. Will need to get the hypothesis that matches id
    // TODO: by getting all events, then get the hypothesis' eventId field, then search for the
    // TODO: target event by id for this logic to work
    const event: Event = userContext.userCache.getEventById(dTSInput.sourceId);
    const eventHypothesis =
      event && event.hypotheses ? event.hypotheses[event.hypotheses.length - 1] : undefined;

    const location =
      eventHypothesis && dTSInput.sourceType === DistanceSourceType.Event && dTSInput.sourceId
        ? {
            latitudeDegrees:
              eventHypothesis.preferredLocationSolution.locationSolution.location.latitudeDegrees,
            longitudeDegrees:
              eventHypothesis.preferredLocationSolution.locationSolution.location.longitudeDegrees,
            elevationKm: eventHypothesis.preferredLocationSolution.locationSolution.location.depthKm
          }
        : undefined;

    if (!location) {
      if (dTSInput.sourceId) {
        logger.warn(
          `For source id ${dTSInput.sourceId}` +
            `source location is not set cannot compute distance for station ${station.name}.`
        );
      }
      return undefined;
    }

    const dTSReturn: DistanceToSource = {
      ...dTSInput,
      distance: this.getDistanceToSource(location, station),
      azimuth: this.getAzimuthToSource(location, station),
      sourceLocation: location,
      stationId: station.name
    };

    if (!dTSReturn.distance) {
      logger.warn(
        `For source id ${dTSReturn.sourceId} Distance is undefined for station ${station.name}.`
      );
    }

    return dTSReturn;
  }

  /**
   * Calculate the azimuth in degrees between the provided source location and processing station
   * @param sourceLocation The source location for which to calculate azimuth to the provided station
   * @param station The station for which to calculate azimuth to the provided source location
   * @returns calculated azimuth in degrees
   */
  public getAzimuthToSource(sourceLocation: Location, station: model.ProcessingStation): number {
    return geolib.getRhumbLineBearing(
      { latitude: sourceLocation.latitudeDegrees, longitude: sourceLocation.longitudeDegrees },
      { latitude: station.location.latitudeDegrees, longitude: station.location.longitudeDegrees }
    );
  }

  /**
   * Calculate the distance in kilometers between a provided source location and processing station.
   * @param sourceLocation The source location for which to calculate distance to the provided station
   * @param station The station for which to calculate distance to the provided source location
   * @returns calculated distance in kilometers
   */
  public getDistanceToSource(sourceLocation: Location, station: model.ProcessingStation): Distance {
    const accuracy = 1000;
    const degreePrecision = 1000;
    const KM = 1000;
    const dist: number = geolib.getDistance(
      { latitude: station.location.latitudeDegrees, longitude: station.location.longitudeDegrees },
      { latitude: sourceLocation.latitudeDegrees, longitude: sourceLocation.longitudeDegrees },
      accuracy
    );
    const km = dist / KM;

    // return distance as degrees and km
    return {
      degrees: Math.round((km / this.KM_TO_DEGREES) * degreePrecision) / degreePrecision,
      km
    };
  }

  /**
   * Fetch station-related data from backend services for the provided network name.
   * This is an asynchronous function.
   * This function propagates errors from the underlying HTTP call.
   * Fetched data include processing networks, stations, sites, and channels.
   */
  public async fetchStationData() {
    // Build list of station group names to load. The names will be the SOH Station Groups and
    // the UI Analyst default station group
    let stationGroupNames: string[] = [];
    // Load the Analyst Config station group values
    const sohStationNamesPriorities = ConfigurationProcessor.Instance().getStationGroupNamesWithPriorities();
    if (sohStationNamesPriorities) {
      stationGroupNames = stationGroupNames.concat(
        sohStationNamesPriorities.map(groupName => groupName.name)
      );
    }

    // Lookup the analyst stationGroup name if in analyst mode (not SOH mode)
    const analysisStationGroupName = ConfigurationProcessor.Instance().getNetworkForUser();
    if (stationGroupNames.find(groupName => groupName === analysisStationGroupName) === undefined) {
      stationGroupNames.push(analysisStationGroupName);
    }
    logger.info(
      `Fetching processing station data for network with name: ${String(stationGroupNames)}`
    );

    // Retrieve the request configuration for the service call
    const requestConfig = this.settings.backend.services.stationGroupByName.requestConfig;

    // Call the service and process the response data
    logger.debug(
      `Calling for processing station group ${JSON.stringify(requestConfig, undefined, 2)} ` +
        `query ${JSON.stringify(stationGroupNames, undefined, 2)}`
    );
    await this.httpWrapper
      .request<model.ProcessingStationGroup[]>(requestConfig, stationGroupNames)
      .then((response: HttpResponse<model.ProcessingStationGroup[]>) => {
        const stationGroups: model.ProcessingStationGroup[] = response.data;

        stationGroups.forEach(stationGroup => {
          // Add it to the processingStationData cache. Then process the station group
          // to add the stations and channels to the cache maps. If any entry is already in the
          // map replace it with the new definition
          CacheProcessor.Instance()
            .getProcessingStationData()
            .stationGroupMap.set(stationGroup.name, stationGroup);
          logger.info(
            `Added processing station group ${stationGroup.name} with stations ${stationGroup?.stations.length}`
          );

          // Add to the station and channel maps
          stationGroup.stations.forEach(station => {
            // Add/replace the station if not already added (It can be added from a different station group)
            CacheProcessor.Instance()
              .getProcessingStationData()
              .stationMap.set(station.name, station);

            // Add each channel group if already added update with the latest
            station.channelGroups.forEach(channelGroup => {
              CacheProcessor.Instance()
                .getProcessingStationData()
                .channelGroupMap.set(channelGroup.name, channelGroup);
            });

            // Add each channel if already added update with the latest
            station.channels.forEach(channel => {
              CacheProcessor.Instance()
                .getProcessingStationData()
                .channelMap.set(channel.name, channel);
            });
          });
        });
      })
      .then(() => {
        const noDataString = CacheProcessor.Instance()
          .getProcessingStationData()
          .stationGroupMap.get(stationGroupNames[0])
          ? `- Loaded ${
              CacheProcessor.Instance().getProcessingStationData().stationGroupMap.size
            } station groups and ${
              CacheProcessor.Instance().getProcessingStationData().stationMap.size
            } stations`
          : '- No data loaded';
        logger.info(
          `Processing station data ${this.settings.backend.mock.enable ? 'mock' : 'OSD'} ` +
            `fetch complete ${noDataString}`
        );
      })
      .then(() => {
        // Walk the stations for the SOH Station Groups
        // and add the station group names to the soh station group name map
        if (sohStationNamesPriorities) {
          const sohStationGroupNamesMap = CacheProcessor.Instance().getProcessingStationData()
            .sohStationGroupNameMap;
          sohStationNamesPriorities.forEach(groupName => {
            const stations = this.getStationsByStationGroupName(groupName.name);
            stations.forEach(station => {
              if (sohStationGroupNamesMap.has(station.name)) {
                sohStationGroupNamesMap.get(station.name).push(groupName);
              } else {
                sohStationGroupNamesMap.set(station.name, [groupName]);
              }
            });
          });
        }
      })
      .catch(async e => {
        logger.warn(`Error failed to connect to processing station endpoint: ${e}`);
        const timeoutMS = 2000;

        await sleep(timeoutMS).then(async () => {
          logger.info(`Retrying to connect to processing station endpoint`);
          await this.fetchStationData();
        });
      });
  }

  /**
   * Initialize the station processor, fetching station data from the backend.
   * This function sets up a mock backend if configured to do so.
   */
  private initialize() {
    try {
      logger.info(
        'Initializing the processing station processor - Mock Enable: %s',
        this.settings.backend.mock.enable
      );

      // If service mocking is enabled, initialize the mock backend
      if (this.settings.backend.mock.enable) {
        processingStationMockBackend.initialize(this.httpWrapper.createHttpMockWrapper());
      }
    } catch (error) {
      gatewayLogger.error(`Failed to initialize the Processing Station processor: ${error}`);
      throw error;
    }
  }
}
