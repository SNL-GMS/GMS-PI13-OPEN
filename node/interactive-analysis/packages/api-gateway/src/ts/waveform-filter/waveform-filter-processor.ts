import config from 'config';
import { UserContext } from '../cache/model';
import { ChannelSegmentProcessor } from '../channel-segment/channel-segment-processor';
import { ChannelSegment, OSDChannelSegment, OSDTimeSeries } from '../channel-segment/model';
import { ConfigurationProcessor } from '../configuration/configuration-processor';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import {
  convertChannelSegmentFromAPIToOSD,
  convertChannelSegmentFromOSDToAPI
} from '../util/channel-segment-utils';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import { WaveformFilterDefinition } from '../waveform-filter/model';
import { OSDWaveform, Waveform } from '../waveform/model';
import { CalculateWaveformSegmentInput, FilteredWaveformChannelSegment } from './model';
import * as waveformFilterMockBackend from './waveform-filter-mock-backend';

/**
 * Waveform filter processor, which handles a filtered waveform segments
 */
export class WaveformFilterProcessor {
  /** The singleton instance */
  private static instance: WaveformFilterProcessor;

  /**
   * Returns the singleton instance of the waveform filter processor.
   */
  public static Instance(): WaveformFilterProcessor {
    if (WaveformFilterProcessor.instance === undefined) {
      WaveformFilterProcessor.instance = new WaveformFilterProcessor();
      WaveformFilterProcessor.instance.initialize();
    }
    return WaveformFilterProcessor.instance;
  }

  /** Local configuration settings */
  private readonly settings: any;

  /** HTTP client wrapper for communicating with backend services */
  private readonly httpWrapper: HttpClientWrapper;

  /**
   * Constructor - initialize the processor, loading settings and initializing
   * the HTTP client wrapper.
   */
  private constructor() {
    // Load configuration settings
    this.settings = config.get('filterWaveform');

    // Initialize an http client
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Helper function to lookup default filters before calling calculateFilteredWaveformSegment
   * rawChannelSegments
   */
  public async getFilteredWaveformSegments(
    userContext: UserContext,
    rawChannelSegments: ChannelSegment<Waveform>[]
  ): Promise<FilteredWaveformChannelSegment[]> {
    // Get the default set of waveform filter definitions for filtering
    const defaultFilters: WaveformFilterDefinition[] = ConfigurationProcessor.Instance().getWaveformFiltersForUser();
    return this.calculateFilteredWaveformSegments(userContext, rawChannelSegments, defaultFilters);
  }

  /**
   * Retrieve waveform filters from the cache, filtering the results
   * down to those default filter names loaded from the config
   */
  public async calculateFilteredWaveformSegments(
    userContext: UserContext,
    rawChannelSegments: ChannelSegment<Waveform>[],
    filters: WaveformFilterDefinition[]
  ): Promise<FilteredWaveformChannelSegment[]> {
    // Call for the derived Channel Segments. Uses the ChannelSegmentMockBackend find
    // the Filtered ChannelSegment IDs instead of calling Filter streaming service to
    // calculate new Filtered Channel Segments
    logger.debug(`Waveform processor entered filtered waveforms`);

    // Construct the parameters for the OSD call for each filter
    const filterCallParameters = filters.map(filter => {
      // Segments are only valid when all timeseries have the same sample rate as the filter sample rate
      // tslint:disable-next-line:arrow-return-shorthand
      const validChannelSegments: ChannelSegment<
        Waveform
      >[] = rawChannelSegments.filter(channelSegment =>
        channelSegment.timeseries.every(waveform => waveform.sampleRate === filter.sampleRate)
      );

      return {
        filter,
        channelSegments: validChannelSegments
      };
    });

    // Segments are only valid when all timeseries have the same sample rate as the filter sample rate
    // Call OSD for filtered channel segments then associate filtered channel segments with raw
    const promises = filterCallParameters.map(async parameters =>
      this.calculateWaveformSegments(userContext, parameters.filter, parameters.channelSegments)
    );
    const channelSegmentsArray = await Promise.all(promises);
    const filteredWaveforms: FilteredWaveformChannelSegment[][] = channelSegmentsArray.map(
      (channelSegments, index) =>
        channelSegments.map((channelSegment: ChannelSegment<Waveform>) => {
          if (!channelSegment || channelSegment === undefined) {
            logger.error(`Problem with filtered channel segment returned undefined!!!!`);
          }

          // Store the Filtered Channel Segment in the ChannelSegmentProcessor for future retrieval
          ChannelSegmentProcessor.Instance().addOrUpdateToCache(userContext, channelSegment);

          // Find the raw channel segment channel name to populate the sourceChannelId
          const rawChannelSegment = filterCallParameters[index].channelSegments.find(rcs =>
            channelSegment.channel.name.startsWith(rcs.channel.name)
          );
          // Associate filter id value and raw channel segment channel id for filtered channel segment
          return {
            ...channelSegment,
            wfFilterId: filterCallParameters[index].filter.id,
            sourceChannelId: rawChannelSegment
              ? rawChannelSegment.channel.name
              : channelSegment.channel.name
          };
        })
    );
    const toReturn = filteredWaveforms.reduce(
      (prev: FilteredWaveformChannelSegment[], curr: FilteredWaveformChannelSegment[]) => [
        ...prev,
        ...curr
      ],
      [] // Initial empty value of prev for first curr
    );
    // Concatenate all filtered channel segment lists together when promises complete
    return toReturn;
  }

  /**
   * Retrieve waveform filters from the cache, filtering the results
   * down to those default filter names loaded from the config
   * @param userContext the user context
   * @param filter the filter
   * @param channelSegments the channel segments
   */
  public async calculateWaveformSegments(
    userContext: UserContext,
    filter: WaveformFilterDefinition,
    channelSegments: ChannelSegment<Waveform>[]
  ): Promise<ChannelSegment<Waveform>[]> {
    // If the list is empty then immediately return
    if (!channelSegments || channelSegments.length === 0) {
      return [];
    }

    // Retrieve the request configuration for the service call
    const requestConfig = this.settings.backend.services.calculateWaveformSegments.requestConfig;

    // Truncate the channel segment waveforms first then convert to OSD format
    const osdChannelSegments: OSDChannelSegment<OSDTimeSeries>[] = channelSegments
      .map(channelSegment => ({
        ...channelSegment,
        timeseries: this.truncateWaveforms(channelSegment.timeseries)
      }))
      .map(convertChannelSegmentFromAPIToOSD);

    const query: CalculateWaveformSegmentInput = {
      channelSegments: osdChannelSegments as OSDChannelSegment<OSDWaveform>[],
      filterDefinition: filter
    };

    logger.debug(`Sending service request ${JSON.stringify(requestConfig, undefined, 2)}`);
    performanceLogger.performance(
      'filterChannelSegment',
      'requestedFromService',
      osdChannelSegments[0].id
    );
    return this.httpWrapper
      .request<OSDChannelSegment<OSDWaveform>[]>(requestConfig, query)
      .then<ChannelSegment<Waveform>[]>(
        (response: HttpResponse<OSDChannelSegment<OSDWaveform>[]>) => {
          performanceLogger.performance(
            'filterChannelSegment',
            'returnedFromService',
            osdChannelSegments[0].id
          );
          // Cache filters after request
          if (response && response.data && response.data.length > 0) {
            return response.data.map(res =>
              convertChannelSegmentFromOSDToAPI(userContext, res)
            ) as ChannelSegment<Waveform>[];
          }

          // No data, pass through
          return [];
        }
      )
      .catch(e => {
        logger.warn(`Error computing filtered waveform channel segments error: ${e}`);
        return [];
      });
  }

  /**
   * Initialize the waveform filter processor, setting up a mock backend if configured to do so.
   */
  private initialize(): void {
    logger.info(
      'Initializing the waveform filter processor - Mock Enable: %s',
      this.settings.backend.mock.enable
    );

    // If service mocking is enabled, initialize the mock backend
    if (this.settings.backend.mock.enable) {
      waveformFilterMockBackend.initialize(this.httpWrapper.createHttpMockWrapper());
    }
  }

  /**
   * Shortens waveforms
   * @param waveforms waveforms[]
   * @returns a Waveform[]
   */
  private truncateWaveforms(waveforms: Waveform[]): Waveform[] {
    return waveforms.map((waveform: Waveform) => {
      // If sample size is less than length use it, otherwise short-circuit the boolean if below
      const waveformSampleSize: number =
        this.settings.numberFilterSamples < waveform.values.length
          ? this.settings.numberFilterSamples
          : waveform.values.length;
      const numSecs = waveformSampleSize / waveform.sampleRate;

      const startTimeNum = waveform.startTime ? waveform.startTime : 0;
      const endTimeNum = waveform.startTime ? waveform.startTime + numSecs : numSecs;

      // Truncate the waveform if enabled and valid otherwise return same waveform
      if (waveformSampleSize !== waveform.values.length && !this.settings.backend.mock.enable) {
        const newWaveform = {
          ...waveform,
          sampleRate: waveformSampleSize / numSecs,
          values: waveform.values.slice(0, waveformSampleSize),
          sampleCount: waveformSampleSize,
          startTime: startTimeNum,
          endTime: endTimeNum
        };
        return newWaveform;
      }
      return waveform;
    });
  }
}
