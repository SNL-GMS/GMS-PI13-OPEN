import { ProcessingStationTypes, WaveformTypes } from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import Immutable from 'immutable';
import cloneDeep from 'lodash/cloneDeep';
import uniq from 'lodash/uniq';
import { KeyDirection } from '~analyst-ui/components/waveform-display/types';
import { systemConfig } from '~analyst-ui/config';
import { createUnfilteredWaveformFilter } from './instance-of-util';

/**
 * Returns the waveform for the provided mode.
 *
 * @param mode the mode
 * @param sampleRate the sampleRate of the channel
 * @param defaultWaveformFilters default waveform filters
 *
 * @returns filter of type WaveformFilter
 */
export function getWaveformFilterForMode(
  mode: AnalystWorkspaceTypes.WaveformDisplayMode,
  sampleRate: number,
  defaultWaveformFilters: WaveformTypes.WaveformFilter[]
): WaveformTypes.WaveformFilter {
  let waveformFilter: WaveformTypes.WaveformFilter;
  if (mode === AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT) {
    waveformFilter = defaultWaveformFilters.find(
      filter =>
        filter.filterType.includes(systemConfig.measurementMode.amplitudeFilter.filterType) &&
        filter.filterPassBandType.includes(
          systemConfig.measurementMode.amplitudeFilter.filterPassBandType
        ) &&
        filter.lowFrequencyHz === systemConfig.measurementMode.amplitudeFilter.lowFrequencyHz &&
        filter.highFrequencyHz === systemConfig.measurementMode.amplitudeFilter.highFrequencyHz &&
        filter.sampleRate === sampleRate
    );
  }
  return waveformFilter;
}

/**
 * Returns the selected Waveform Filter based on the mode, station id and
 * the channel filters.
 *
 * @param mode the mode
 * @param id id of channel filter
 * @param sampleRate the sampleRate of the channel
 * @param channelFilters channel filters
 * @param defaultWaveformFilters default waveform filters
 *
 * @returns filter of type WaveformFilter
 */
export function getSelectedWaveformFilter(
  mode: AnalystWorkspaceTypes.WaveformDisplayMode,
  id: string,
  sampleRate: number,
  channelFilters: Immutable.Map<string, WaveformTypes.WaveformFilter>,
  defaultWaveformFilters: WaveformTypes.WaveformFilter[]
): WaveformTypes.WaveformFilter {
  let selectedFilter: WaveformTypes.WaveformFilter;
  if (mode !== AnalystWorkspaceTypes.WaveformDisplayMode.DEFAULT) {
    selectedFilter = getWaveformFilterForMode(mode, sampleRate, defaultWaveformFilters);
  } else {
    selectedFilter = channelFilters.has(id)
      ? channelFilters.get(id)
      : createUnfilteredWaveformFilter();
  }
  return selectedFilter;
}

/**
 * Event handler for when a key is pressed
 * @param e mouse event as React.MouseEvent<HTMLDivElement>
 * @param selectedChannels selected channels to be filtered
 * @param defaultWaveformFilters the default waveform filters
 * @param selectedFilterIndex the selected filter index
 * @param channelFilters map of channel filters
 * @returns an object {channelFilters, newFilterIndex}
 */
export function toggleWaveformChannelFilters(
  direction: KeyDirection,
  selectedChannels: string[],
  defaultWaveformFilters: WaveformTypes.WaveformFilter[],
  defaultStations: ProcessingStationTypes.ProcessingStation[],
  selectedFilterIndex: number,
  channelFilters: Immutable.Map<string, WaveformTypes.WaveformFilter>
): { channelFilters: Immutable.Map<string, WaveformTypes.WaveformFilter>; newFilterIndex: number } {
  if (selectedChannels) {
    const filterNames = uniq(defaultWaveformFilters.map(filter => filter.name));

    if (filterNames.length > 0) {
      const waveformFilterLength = filterNames.length;
      let tempChannelFilters = cloneDeep(channelFilters);
      let tempSelectedFilterIndex = selectedFilterIndex;
      tempSelectedFilterIndex =
        direction === KeyDirection.UP ? tempSelectedFilterIndex + 1 : tempSelectedFilterIndex - 1;
      if (tempSelectedFilterIndex >= waveformFilterLength) tempSelectedFilterIndex = -1;
      if (tempSelectedFilterIndex < -1) tempSelectedFilterIndex = waveformFilterLength - 1;

      if (selectedChannels !== undefined && selectedChannels.length > 0) {
        // for every id check to see if a default station matches
        // if none match, add to id list for every channel
        selectedChannels.forEach(selectedId => {
          defaultStations.forEach(station => {
            // if the selected id is a default station,
            // set the filter on all of its non-default stations
            if (station.name === selectedId) {
              tempChannelFilters = tempChannelFilters.set(
                station.name,
                // tslint:disable-next-line:max-line-length
                findWaveformFilter(
                  station.channels[0].nominalSampleRateHz,
                  tempSelectedFilterIndex,
                  filterNames,
                  defaultWaveformFilters
                )
              );
            } else {
              // check each station's child channels to see if the id
              // matches one of them, if so, apply
              station.channels.forEach(childChannel => {
                if (childChannel.name === selectedId) {
                  tempChannelFilters = tempChannelFilters.set(
                    childChannel.name,
                    findWaveformFilter(
                      childChannel.nominalSampleRateHz,
                      tempSelectedFilterIndex,
                      filterNames,
                      defaultWaveformFilters
                    )
                  );
                }
              });
            }
          });
        });
      } else {
        // no selected channels, apply filter to all
        defaultStations.forEach(station => {
          tempChannelFilters = tempChannelFilters.set(
            station.name,
            findWaveformFilter(
              // FIXME: Need a better way of looking up sample rate
              station.channels[0].nominalSampleRateHz,
              tempSelectedFilterIndex,
              filterNames,
              defaultWaveformFilters
            )
          );
        });
      }

      return { channelFilters: tempChannelFilters, newFilterIndex: tempSelectedFilterIndex };
    }
  }
}

/**
 * Find the selected filter for the channel
 *
 * @param sampleRate Sample rate of the filter
 * @param selectedFilterIndex index of the current selected filter
 * @param filterNames names of all the filters
 * @param defaultWaveformFilters list of filters
 *
 * @returns the waveformFilter requested
 */
export function findWaveformFilter(
  sampleRate: number,
  selectedFilterIndex: number,
  filterNames: string[],
  defaultWaveformFilters: WaveformTypes.WaveformFilter[]
): WaveformTypes.WaveformFilter {
  // If no filter selected, return unfiltered
  if (selectedFilterIndex === -1) {
    return createUnfilteredWaveformFilter();
  }
  const selectedFilterName = filterNames[selectedFilterIndex];

  const filters = defaultWaveformFilters;
  let filter = filters.find(
    filt => filt.name === selectedFilterName && filt.sampleRate === sampleRate
  );
  if (!filter) {
    filter = filters.find(filt => filt.name === selectedFilterName);
  }
  return filter;
}
