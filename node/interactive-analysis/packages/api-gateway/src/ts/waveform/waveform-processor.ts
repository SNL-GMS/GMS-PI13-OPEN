import config from 'config';
import includes from 'lodash/includes';
import { UserContext } from '../cache/model';
import { ChannelSegmentProcessor } from '../channel-segment/channel-segment-processor';
import { ChannelSegment, isWaveformChannelSegment } from '../channel-segment/model';
import { ConfigurationProcessor } from '../configuration/configuration-processor';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { FilteredWaveformChannelSegment, WaveformFilterDefinition } from '../waveform-filter/model';
import { WaveformFilterProcessor } from '../waveform-filter/waveform-filter-processor';
import { Waveform } from './model';

/**
 * Waveform Processor that handles channel segments and time series
 */
export class WaveformProcessor {
  /** The singleton instance */
  private static instance: WaveformProcessor;

  /**
   * Returns the singleton instance of the waveform filter processor.
   */
  public static Instance(): WaveformProcessor {
    if (WaveformProcessor.instance === undefined) {
      WaveformProcessor.instance = new WaveformProcessor();
      WaveformProcessor.instance.initialize();
    }
    return WaveformProcessor.instance;
  }

  /** Local configuration settings */
  private readonly settings: any;

  /**
   * Constructor - initialize the processor, loading settings and initializing
   * the HTTP client wrapper.
   */
  private constructor() {
    // Load configuration settings
    this.settings = config.get('waveform');
  }

  /**
   * Retrieve raw waveform from the cache.
   */
  public async getRawWaveformSegmentsByChannels(
    userContext: UserContext,
    startTime: number,
    endTime: number,
    channelNames: string[]
  ): Promise<ChannelSegment<Waveform>[]> {
    // Retrieve the raw channel segments for channel ids from channel segment processor
    // !TODO: FK_BEAM will be removed once these are retrieved from the signal detection
    // ! Beams are only associated with the station id
    const defaultStationIds = ProcessingStationProcessor.Instance()
      .getDefaultProcessingStations()
      .map(s => s.name);

    // FKBeam
    if (channelNames.every(id => includes(defaultStationIds, id))) {
      logger.warn('Warning FK_BEAM Channel Segments is depreciated and should not be called!');
      return [];
    }
    // Return Raw Channel Segments found
    return (
      (
        await ChannelSegmentProcessor.Instance()
          // tslint:disable-next-line: max-line-length
          .getChannelSegmentsByChannels(userContext, startTime, endTime, channelNames)
      ).filter(isWaveformChannelSegment)
    );
  }

  /**
   * Retrieve filtered waveform from the cache.
   */
  public async getFilteredWaveformSegmentsByChannels(
    userContext: UserContext,
    startTime: number,
    endTime: number,
    channelIds: string[],
    filterIds?: string[]
  ): Promise<FilteredWaveformChannelSegment[]> {
    const rawChannelSegments: ChannelSegment<
      Waveform
    >[] = await this.getRawWaveformSegmentsByChannels(userContext, startTime, endTime, channelIds);

    // Get the default set of waveform filter definitions for filtering
    const defaultFilters: WaveformFilterDefinition[] = ConfigurationProcessor.Instance().getWaveformFiltersForUser();

    const filters = filterIds
      ? defaultFilters.filter(wf => includes(filterIds, wf.id))
      : defaultFilters;

    // Filter the raw waveforms using the default filter definitions
    const filteredChannelSegments = await WaveformFilterProcessor.Instance()
      .calculateFilteredWaveformSegments(userContext, rawChannelSegments, filters)
      .catch(error => {
        logger.error(
          `Error getFilteredWaveformSegmentsByChannels failed ` +
            `returning empty filters: ${error}`
        );
        return [];
      });
    return filteredChannelSegments;
  }

  /**
   * Initialize the waveform filter processor, setting up a mock backend if configured to do so.
   */
  private initialize(): void {
    logger.info(
      'Initializing the waveform processor - Mock Enable: %s',
      this.settings.backend.mock.enable
    );
  }
}

/**
 * express request handler,
 * @param req - should have query parameters start: number, end: number, channels: comma-separated list e.g. 1,2,3
 * @param res - a message pack encoded set of model.Waveforms
 */
export const waveformRawSegmentRequestHandler = (req, res) => {
  const startTime = Number(req.query.startTime);
  const endTime = Number(req.query.endTime);
  const channelIds: string[] | null = req.query.channelIds && req.query.channelIds.split(',');
  const mockUserContext: UserContext = {
    userName: 'waveformRawSegmentRequestHandler',
    sessionId: req.sessionID,
    userCache: undefined,
    userRole: 'DEFAULT'
  };
  // If channels is specified, return those channels
  WaveformProcessor.Instance()
    .getRawWaveformSegmentsByChannels(mockUserContext, startTime, endTime, channelIds)
    .then((waveforms: ChannelSegment<Waveform>[]) => {
      res.send(waveforms);
    })
    .catch(e => logger.warn(e));
};

/**
 * express request handler,
 * @param req - should have query parameters start: number, end: number, channels: comma-separated list e.g. 1,2,3
 * @param res - a message pack encoded set of model.Waveforms
 */
export const waveformFilteredSegmentRequestHandler = (req, res) => {
  const startTime = Number(req.query.startTime);
  const endTime = Number(req.query.endTime);
  const channelIds: string[] | null = req.query.channelIds && req.query.channelIds.split(',');
  const filterIds: string[] | null = req.query.filterIds && req.query.filterIds.split(',');
  const mockUserContext: UserContext = {
    userName: 'waveformFilteredSegmentRequestHandler',
    sessionId: req.sessionID,
    userCache: undefined,
    userRole: 'DEFAULT'
  };

  // If channels is specified, return those channels
  WaveformProcessor.Instance()
    .getFilteredWaveformSegmentsByChannels(
      mockUserContext,
      startTime,
      endTime,
      channelIds,
      filterIds
    )
    .then((waveforms: FilteredWaveformChannelSegment[]) => {
      res.send(waveforms);
    })
    .catch(e => logger.warn(e));
};
