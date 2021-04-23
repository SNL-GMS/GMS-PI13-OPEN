import { QueryControls } from 'react-apollo';
import { LogLevel } from '../common/types';
import { WaveformFilter } from '../waveform/types';

export interface UIConfigurationQueryProps {
  uiConfigurationQuery: QueryControls<{}> & { uiAnalystConfiguration: AnalystConfiguration };
}

/**
 * Interface for analyst configuration used to determine fields based on roles
 */
export interface AnalystConfiguration {
  readonly logLevel: LogLevel;
  readonly defaultNetwork: string;
  readonly sohStationGroupNames: SOHStationGroupNameWithPriority[];
  readonly defaultFilters: WaveformFilter[];
  readonly redisplayPeriod: number;
  readonly reprocessingPeriod: number;
  readonly acknowledgementQuietDuration: number;
  readonly availableQuietDurations: number[];
  readonly sohStationStaleTimeMS: number;
  readonly sohHistoricalDurations: number[];
  readonly systemMessageLimit: number;
}

/**
 * SOH StationGroup and Priority interface definition
 */
export interface SOHStationGroupNameWithPriority {
  name: string;
  priority: number;
}
