import Immutable from 'immutable';
import { WaveformFilterDefinition } from '../waveform-filter/model';

/**
 * Interface for analyst configuration used to determine fields based on roles
 */
export interface AnalystConfiguration {
  readonly defaultNetwork: string;
  readonly defaultFilters: WaveformFilterDefinition[];
}

/**
 * Soh specific configuration
 */
export interface SohConfiguration {
  stationSohControlConfiguration: {
    reprocessingPeriod: string;
    displayedStationGroups: string[];
    rollupStationSohTimeTolerance: string;
  };
  stationSohMonitoringDisplayParameters: {
    redisplayPeriod: string;
    acknowledgementQuietDuration: string;
    availableQuietDurations: string[];
    sohStationStaleDuration: string;
    sohHistoricalDurations: string[];
  };
}

/**
 * Common configuration
 */
export interface CommonConfiguration {
  systemMessageLimit: number;
}

/**
 * Overall configuration
 */
export interface Configuration {
  /**
   * AnalystConfiguration keyed on user role
   */
  analystConfiguration: Immutable.Map<string, AnalystConfiguration>;

  /**
   * SohConfigurationOld
   */
  sohConfiguration: SohConfiguration;

  /**
   * CommonConfiguration keyed on user role
   */
  commonConfiguration: Immutable.Map<string, CommonConfiguration>;
}

/**
 * Selector interface for config service
 */
export interface Selector {
  criterion: string;
  value: string;
}

/**
 * Analyst configurations loaded from service
 */
export enum AnalystConfigs {
  DEFAULT = 'ui.analyst-settings'
}

/**
 * SOH configurations loaded from service
 */
export const SohConfig = 'ui.soh-settings';

/**
 * Common configurations loaded from service
 */
export enum CommonConfigs {
  DEFAULT = 'ui.common-settings'
}

/**
 * Default user role
 */
export const defaultUserRole = 'DEFAULT';
