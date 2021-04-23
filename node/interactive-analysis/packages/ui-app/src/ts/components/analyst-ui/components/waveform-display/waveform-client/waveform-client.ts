import { ChannelSegmentTypes, WaveformTypes } from '@gms/common-graphql';
import { WAVEFORMS_PROXY_URI } from '@gms/common-util';
import { Client, UILogger } from '@gms/ui-apollo';
import Axios, { CancelTokenSource } from 'axios';
import includes from 'lodash/includes';
import uniq from 'lodash/uniq';
import { getFilteredWaveformSegmentsByChannels } from '~analyst-ui/react-apollo-components/get-filtered-waveform-segments';
import { getRawWaveformSegmentsByChannels } from '~analyst-ui/react-apollo-components/get-raw-waveform-segments';
import { DEFAULT_INITIAL_WAVEFORM_CLIENT_STATE, IS_TRANSFER_WAVEFORM_GRAPHQL } from './constants';
import { WaveformClientState } from './types';
import {
  createZeroArray,
  createZeroDataChannelSegment,
  createZeroDataFilteredChannelSegment
} from './utils';
import { WaveformDataCache } from './waveform-data-cache';

export class WaveformClient {
  /**
   * A cache storing which channels we've already loaded data for.
   */
  private readonly waveformDataCache: WaveformDataCache = new WaveformDataCache();

  private cancelTokenSource: CancelTokenSource | undefined;

  private readonly client: Client;

  public state: WaveformClientState = {
    ...DEFAULT_INITIAL_WAVEFORM_CLIENT_STATE
  };

  public constructor(client: Client) {
    this.client = client;
  }

  /**
   * Retrieve a list of all the channel IDs in the cache.
   *
   * @returns string[] of channel ids
   */
  public readonly getWaveformChannelIds = (): string[] =>
    this.waveformDataCache.getWaveformChannelIds()

  /**
   *
   * Retrieve the cache entry associated with the provided channel ID/filter ID.
   * This method returns undefined if no cache entry exists for the provided
   * channel ID/filter ID.
   * @param channelId The channel ID associated with the cache entry to retrieve
   * @param filterId The filter ID associated with the cache entry to retrieve
   *
   * @returns either a list of ChannelSegments or a is of FilteredChannelSegment
   */
  public readonly getWaveformEntry = (
    channelId: string,
    filterId: string
  ): [
    | ChannelSegmentTypes.ChannelSegment<WaveformTypes.Waveform>
    | WaveformTypes.FilteredChannelSegment
  ] => this.waveformDataCache.getWaveformEntry(channelId, filterId)

  /**
   * Retrieves the cache entries associated with the provided channel ID.
   * This method returns undefined if no cache entries exists for the provided
   * channel ID.
   *
   * @param channelId The channel ID associated with the cache entry to retrieve
   *
   * @returns Map of id's (strings) to a list of either ChannelSegments or FilteredChannelSegments
   */
  public readonly getWaveformEntriesForChannelId = (
    channelId: string
  ): Map<
    string,
    [
      | ChannelSegmentTypes.ChannelSegment<WaveformTypes.Waveform>
      | WaveformTypes.FilteredChannelSegment
    ]
  > => this.waveformDataCache.getWaveformEntriesForChannelId(channelId)

  /**
   * Cancels the current fetch and clears the WF cache
   */
  public readonly stopAndClear = () => {
    if (this.cancelTokenSource) {
      this.cancelTokenSource.cancel('Operation canceled by the user');
      this.cancelTokenSource = undefined;
    }

    this.waveformDataCache.clearAllWaveformEntries();

    this.state = {
      ...DEFAULT_INITIAL_WAVEFORM_CLIENT_STATE
    };
  }

  /**
   * Retrieves ChannelSegments from the API gateway for the provided time range and channel IDs, and
   * updates the waveform data cache based on the results. If the overwrite parameter is provided and
   * set to true, cache entries will be overwritten with the retrieved ChannelSegments; otherwise
   * timeseries data from the retrieved ChannelSegments will be added to the existing cache entries
   * where they exist.
   *
   * @param channelIds list of channel ids to fetch for
   * @param filterIds list of filter ids to load data for
   * @param startTimeSecs start of interval to load waveforms for
   * @param endTimeSecs end of interval to load waveforms
   * @param notifyStateUpdated callback function that updates loading spinners/state about status of fetch
   * @param action callback to be executed after fetch
   * @param fetchChunksSize how many channels of data to load at a time, defaults to 2
   */
  public readonly fetchAndCacheWaveforms = (
    channelIds: string[],
    filterIds: string[],
    startTimeSecs: number,
    endTimeSecs: number,
    notifyStateUpdated: () => any,
    action: () => any,
    fetchChunksSize: number = 2
  ) => {
    if (channelIds && channelIds.length > 0) {
      if (fetchChunksSize < 1) {
        // tslint:disable-next-line:no-parameter-reassignment
        fetchChunksSize = 1;
      } else if (fetchChunksSize > channelIds.length) {
        // tslint:disable-next-line:no-parameter-reassignment
        fetchChunksSize = channelIds.length;
      }

      const fetchChunks: string[][] = [];
      for (let i = 0; i < channelIds.length; i += fetchChunksSize) {
        fetchChunks.push(channelIds.slice(i, i + fetchChunksSize));
      }

      if (!this.cancelTokenSource) {
        this.cancelTokenSource = Axios.CancelToken.source();
      }

      this.fetchAndCacheWaveformsLazy(
        channelIds,
        filterIds,
        startTimeSecs,
        endTimeSecs,
        notifyStateUpdated,
        action,
        fetchChunks
      );
    }

    notifyStateUpdated();
    action();
  }

  /**
   * Fetches channel segments and filters
   * Provides a layer of abstraction over fetching wf data
   *
   * @param channelIds list of channel ids to fetch for
   * @param filterIds list of filter ids to load data for
   * @param startTimeSecs start of interval to load waveforms for
   * @param endTimeSecs end of interval to load waveforms
   * @param notifyStateUpdated callback function that updates loading spinners/state about status of fetch
   * @param action callback to be executed after fetch
   * @param fetchChunks an array of lists of channel id's to fetch
   */
  public readonly fetchAndCacheWaveformsLazy = (
    channelIds: string[],
    filterIds: string[],
    startTimeSecs: number,
    endTimeSecs: number,
    notifyStateUpdated: () => any,
    action: () => any,
    fetchChunks: string[][]
  ) => {
    // Fetch and Cache Waveforms
    this.state.completed = 0;
    this.state.description = ` Loading waveforms...`;
    this.state.total = channelIds.length;
    this.state.percent = 0;
    this.state.isLoading = true;
    notifyStateUpdated();

    Promise.all(
      fetchChunks.map(async waveform =>
        this.internalFetchAndCacheRawWaveforms(waveform, filterIds, startTimeSecs, endTimeSecs)
          .then(result => {
            this.updateFetchResult(waveform.length, notifyStateUpdated);
            return result;
          })
          .catch(error => {
            throw error;
          })
      )
    )
      .then(receivedRawWaveformDataForChannelIds => {
        this.state.isLoading = false;
        action();

        this.state.completed = 0;
        this.state.description = ` Loading filters...`;
        this.state.total = receivedRawWaveformDataForChannelIds.reduce(
          (count, row) => Number(count) + row.length,
          0
        );
        this.state.percent = 0;
        this.state.isLoading = true;
        notifyStateUpdated();

        if (
          receivedRawWaveformDataForChannelIds &&
          receivedRawWaveformDataForChannelIds.length > 0 &&
          filterIds &&
          filterIds.length > 0
        ) {
          Promise.all(
            receivedRawWaveformDataForChannelIds.map(async filter =>
              this.internalFetchAndCacheFilteredWaveforms(
                filter,
                filterIds,
                startTimeSecs,
                endTimeSecs
              )
                .then(() => {
                  this.updateFetchResult(filter.length, notifyStateUpdated);
                })
                .catch(error => {
                  throw error;
                })
            )
          )
            .then(() => {
              this.updateFetchStatus(false, action, notifyStateUpdated);
            })
            .catch(error => {
              this.handleError(error);
            });
        }
      })
      .catch(error => {
        this.handleError(error);
      });
  }

  /**
   * Updates the fetch result state (completed and percent)
   *
   * @param completed the number of just completed fetches
   * @param notifyStateUpdated callback function that updates loading spinners/state about status of fetch
   */
  private readonly updateFetchResult = (completed: number, notifyStateUpdated: () => any) => {
    this.state.completed += completed;
    this.state.percent = this.state.completed / this.state.total;
    notifyStateUpdated();
  }

  /**
   * Updates the fetch status
   * @param isLoading true if loading; false otherwise
   * @param action the action to invoke when the fetch status changed
   * @param notifyStateUpdated the method to invoke to notify state changed
   */
  private updateFetchStatus(isLoading: boolean, action: () => any, notifyStateUpdated: () => any) {
    this.state.isLoading = isLoading;
    action();
    notifyStateUpdated();
  }

  /**
   * Handles a fetch error.
   *
   * @param error the error
   */
  private readonly handleError = (error: any) => {
    if (Axios.isCancel(error)) {
      UILogger.Instance().log(`Waveform request canceled ${error.message}`);
    } else {
      UILogger.Instance().error(`waveform client error: ${error}`);
    }
  }

  /**
   * Performs the graphql calls to retrieve raw waveform data
   *
   * @param channelIds a list of channelIds to fetch
   * @param filterIds a list of filter ids to apply and retrieve data for
   * @param startTimeSecs start of the interval to fetch wf data
   * @param endTimeSecs start of the interval to fetch wf data
   *
   * @returns a list of fetched channel ids'
   */
  private readonly internalFetchAndCacheRawWaveforms = async (
    channelIds: string[],
    filterIds: string[],
    startTimeSecs: number,
    endTimeSecs: number
  ): Promise<string[]> => {
    if (channelIds && channelIds.length > 0) {
      // Call API Gateway for raw waveform segments
      if (IS_TRANSFER_WAVEFORM_GRAPHQL) {
        // Request raw waveforms via graphql
        return getRawWaveformSegmentsByChannels({
          variables: {
            timeRange: {
              startTime: startTimeSecs,
              endTime: endTimeSecs
            },
            channelIds
          },
          client: this.client
        }).then(data => {
          if (data && data.data && data.data.getRawWaveformSegmentsByChannels) {
            const segments = data.data.getRawWaveformSegmentsByChannels;
            this.waveformDataCache.updateFromChannelSegments(segments);
            return uniq(segments.map(segment => segment.channelId));
          }
        });
      }

      // Request raw waveforms via web socket
      if (this.cancelTokenSource && this.cancelTokenSource.token) {
        return Axios.get(
          WAVEFORMS_PROXY_URI +
            `waveforms/raw?startTime=${startTimeSecs}&endTime=${endTimeSecs}&channelIds=${channelIds.join(
              ','
            )}`,
          {
            headers: {
              'Content-Type': 'application/json',
              Accept: 'application/json'
            },
            cancelToken: this.cancelTokenSource.token
          }
        )
          .then(resp => {
            const segments = resp.data as ChannelSegmentTypes.ChannelSegment<
              WaveformTypes.Waveform
            >[];
            this.waveformDataCache.updateFromChannelSegments(segments);
            return uniq(segments.map(segment => segment.channelId));
          })
          .catch(error => {
            throw error;
          });
      }
    }
  }
  /**
   * Performs the graphql calls to retrieve filtered waveform data
   *
   * @param channelIds a list of channelIds to fetch
   * @param filterIds a list of filter ids to apply and retrieve data for
   * @param startTimeSecs start of the interval to fetch wf data
   * @param endTimeSecs start of the interval to fetch wf data
   *
   */
  private readonly internalFetchAndCacheFilteredWaveforms = async (
    channelIds: string[],
    filterIds: string[],
    startTimeSecs: number,
    endTimeSecs: number
  ): Promise<void> => {
    if (channelIds && channelIds.length > 0 && filterIds && filterIds.length > 0) {
      // Call API Gateway for filtered waveform segments
      if (IS_TRANSFER_WAVEFORM_GRAPHQL) {
        // Request raw waveforms via graphql
        return getFilteredWaveformSegmentsByChannels({
          variables: {
            timeRange: {
              startTime: startTimeSecs,
              endTime: endTimeSecs
            },
            channelIds,
            filterIds
          },
          client: this.client
        }).then(data => {
          this.cacheFilteredWaveforms(
            channelIds,
            filterIds,
            startTimeSecs,
            endTimeSecs,
            data.data.getFilteredWaveformSegmentsByChannels
          );
        });
      }

      /* Request filtered waveforms via web socket */
      if (this.cancelTokenSource && this.cancelTokenSource.token) {
        return Axios.get(
          WAVEFORMS_PROXY_URI +
            `/waveforms/filtered?startTime=${startTimeSecs}&endTime=${endTimeSecs}&channelIds=${channelIds.join(
              ','
            )}&filterIds=${String(filterIds)}`,
          {
            headers: {
              'Content-Type': 'application/json',
              Accept: 'application/json'
            },
            cancelToken: this.cancelTokenSource.token
          }
        )
          .then(resp => {
            const segments = resp.data as WaveformTypes.FilteredChannelSegment[];
            this.cacheFilteredWaveforms(
              channelIds,
              filterIds,
              startTimeSecs,
              endTimeSecs,
              segments
            );
          })
          .catch(error => {
            throw error;
          });
      }
    }
  }
  /**
   * Caches fetched waveform data
   *
   * @param channelIds a list of channelIds to cache
   * @param filterIds a list of filter ids to cache
   * @param startTimeSecs start of the interval to cache wf data
   * @param endTimeSecs start of the interval to cache wf data
   *
   * @returns a list of cached channel ids'
   */
  private readonly cacheFilteredWaveforms = (
    channelIds: string[],
    filterIds: string[],
    startTimeSecs: number,
    endTimeSecs: number,
    filteredChannelSegments: WaveformTypes.FilteredChannelSegment[]
  ) => {
    if (channelIds && channelIds.length > 0 && filterIds && filterIds.length > 0) {
      this.waveformDataCache.updateFromFilteredChannelSegments(filteredChannelSegments);

      // insert flat line data if no data was received
      const dataZeroArray = createZeroArray(startTimeSecs, endTimeSecs);

      const sourceChannelIds = uniq(filteredChannelSegments.map(seg => seg.sourceChannelId));

      const zeroFilterChannelSegments: WaveformTypes.FilteredChannelSegment[] = [];
      channelIds.forEach(channelId => {
        // no data was received for the channel id, add entries for each filter
        if (!includes(sourceChannelIds, channelId)) {
          filterIds.forEach(filterId => {
            zeroFilterChannelSegments.push(
              createZeroDataFilteredChannelSegment(
                channelId,
                filterId,
                startTimeSecs,
                endTimeSecs,
                dataZeroArray
              )
            );
          });
        } else {
          if (filteredChannelSegments) {
            const receivedDataForFilterIds = uniq(
              filteredChannelSegments.map(channelSegment => channelSegment.wfFilterId)
            );
            filterIds.forEach(filterId => {
              if (!includes(receivedDataForFilterIds, filterId)) {
                this.waveformDataCache.updateChannelSegment(
                  channelId,
                  filterId,
                  createZeroDataChannelSegment(
                    channelId,
                    filterId,
                    startTimeSecs,
                    endTimeSecs,
                    dataZeroArray
                  )
                );
              }
            });
          }
        }
      });
      this.waveformDataCache.updateFromFilteredChannelSegments(zeroFilterChannelSegments);
    }
  }
}
