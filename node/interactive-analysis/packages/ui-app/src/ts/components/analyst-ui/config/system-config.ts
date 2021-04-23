import { CommonTypes, EventTypes, SignalDetectionTypes } from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { WeavessTypes } from '@gms/weavess';
import Immutable from 'immutable';

/**
 * Validate if an object is defined, throw an error if the object is `undefined`.
 * @param value the object to validate
 * @param description a description of what was being done
 * @param name the item name
 */
const throwErrorIfUndefined = <T>(value: T, description: string, name: string) => {
  if (!value) {
    throw new Error(`Failed to get ${description}, ${name} is undefined`);
  }
};

export enum MagnitudeCategory {
  SURFACE = 'Surface Wave Magnitudes',
  BODY = 'Body Wave Magnitudes'
}

export interface SystemConfig {
  defaultSdPhases: CommonTypes.PhaseType[];
  defaultTablePrecision: number;
  // Phases to display at top of phase selection list
  prioritySdPhases: CommonTypes.PhaseType[];
  nonFkSdPhases: CommonTypes.PhaseType[];
  defaultWeavessConfiguration: WeavesConfiguration;
  defaultWeavessHotKeyOverrides: WeavesHotKeyOverrides;
  defaultFkConfig: FkConfig;
  eventGlobalScan: EventConfig;
  eventRefinement: EventConfig;
  additionalSubscriptionTime: number;
  // The padding around a signal detection in FKPlots
  fkPlotTimePadding: number;
  numberOfDefiningLocationBehaviorsRequiredForLocate: number;
  measurementMode: MeasurementModeConfig;
  continousFkConfiguration: {
    minMediumVelocity: number;
    maxMediumVelocity: number;
    defaultMediumVelocity: number;
    minMaximumSlowness: number;
    maxMaximumSlowness: number;
    defaultMaximumSlowness: number;
    minNumberOfPoints: number;
    maxNumberOfPoints: number;
    defaultNumberOfPoints: number;
    normalizeWaveforms: boolean;
    useChannelVerticalOffsets: boolean;
  };
  amplitudeTypeForPhase: Map<CommonTypes.PhaseType, SignalDetectionTypes.AmplitudeType>;
  amplitudeTypeForMagnitude: Map<EventTypes.MagnitudeType, SignalDetectionTypes.AmplitudeType>;
  magnitudeTypesForPhase: Map<CommonTypes.PhaseType, EventTypes.MagnitudeType[]>;
  magnitudeTypeToDisplayName: Map<EventTypes.MagnitudeType, string>;
  amplitudeTypeToDisplayName: Map<SignalDetectionTypes.AmplitudeType, string>;
  displayedMagnitudesForCategory: Map<
    MagnitudeCategory,
    AnalystWorkspaceTypes.DisplayedMagnitudeTypes
  >;
  fkNeedsReviewRuleSet: {
    phasesNeedingReview: CommonTypes.PhaseType[];
  };
  interactionDelay: {
    extraFast: number;
    fast: number;
    medium: number;
    slow: number;
    extraSlow: number;
  };

  /**
   * Returns the additional amount of data (in seconds) that should be loaded
   * on the initial load (added to the view time interval)
   */
  getDefaultTimeRange(
    timeRange: CommonTypes.TimeRange,
    analystActivity: AnalystWorkspaceTypes.AnalystActivity
  ): CommonTypes.TimeRange;
  getSignalDetectionTimeRange(
    timeRange: CommonTypes.TimeRange,
    analystActivity: AnalystWorkspaceTypes.AnalystActivity
  ): CommonTypes.TimeRange;
  getEventsTimeRange(
    timeRange: CommonTypes.TimeRange,
    analystActivity: AnalystWorkspaceTypes.AnalystActivity
  ): CommonTypes.TimeRange;
}

export interface AmplitudeFilter {
  filterType: string;
  filterPassBandType: string;
  lowFrequencyHz: number;
  highFrequencyHz: number;
}

export interface WeavesConfiguration {
  stationHeightPx: number;
}
export interface WeavesHotKeyOverrides {
  amplitudeScale: string;
  amplitudeScaleSingleReset: string;
  amplitudeScaleReset: string;
  qcMaskCreate: string;
}

export interface EventConfig {
  numberOfWaveforms: number;
  sortType: AnalystWorkspaceTypes.WaveformSortType;
  // specifies the additional data (in seconds) to load
  // when the interval is initially loaded for the event type
  // this only effects the view time interval
  additionalDataToLoad: number;
  additionalEventDataToLoad: number;
}

export interface FkConfig {
  fkPowerSpectrumDefinition: FKPowerSpectrumDefinition;
  fkProcessingContext: FkProcessingContext;
  fkCreationInfo: FkCreationInfo;
}

export interface FKPowerSpectrumDefinition {
  windowLead: number;
  windowLength: number;
  startTimeStep: number;
  lowFrequency: number;
  highFrequency: number;
  useChannelVerticalOffsets: boolean;
  phaseType: string;
  mediumVelocityKmPerSec: number;
  xSlowStart: number;
  xSlowDelta: number;
  xSlowCount: number;
  ySlowStart: number;
  ySlowDelta: number;
  ySlowCount: number;
  waveformSampleRateHz: number;
  waveformSampleRateToleranceHz: number;
}

export interface FkProcessingContext {
  analystId: string;
  storageVisibility: string;
}

export interface FkCreationInfo {
  creatorName: string;
  softwareInfo: {
    name: string;
    version: string;
  };
}

export enum QcMaskCategory {
  ANALYST_DEFINED = 'Analyst defined',
  CHANNEL_PROCESSING = 'Channel processing',
  DATA_AUTHENTICATION = 'Data authentication',
  REJECTED = 'Rejected',
  STATION_SOH = 'Station SOH',
  WAVEFORM_QUALITY = 'Waveform quality'
}

export enum QcMaskType {
  SENSOR_PROBLEM = 'Sensor problem',
  STATION_PROBLEM = 'Station problem',
  CALIBRATION = 'Calibration',
  STATION_SECURITY = 'Station security',
  TIMING = 'Timing',
  REPAIRABLE_GAP = 'Repairable gap',
  LONG_GAP = 'Long gap',
  REPEATED_ADJACENT_AMPLITUDE_VALUE = 'Repeated adjacent amplitude value',
  SPIKE = 'Spike'
}

/**
 * Defines the configuration for the measurement mode
 * within the waveform display.
 */
export interface MeasurementModeConfig {
  phases: CommonTypes.PhaseType[];
  amplitudeFilter: AmplitudeFilter;
  // In measurement mode, selecting a signal detection automatically zooms in on a ~20
  // second (configurable) time range around that detection in the measure window
  // 7.5 seconds before the detection and 12.5 seconds after
  displayTimeRange: {
    startTimeOffsetFromSignalDetection: number;
    endTimeOffsetFromSignalDetection: number;
  };
  // A ~5 second (configurable) window, determined by lead and lag from the selected signal
  // detection, that indicates the portion of the waveform within which measurements can be made
  selection: {
    id: string;
    startTimeOffsetFromSignalDetection: number;
    endTimeOffsetFromSignalDetection: number;
    lineStyle: WeavessTypes.LineStyle;
    borderColor: string;
    color: string;
    isMoveable: boolean;
  };
  peakTroughSelection: {
    id: string;
    lineStyle: WeavessTypes.LineStyle;
    nonMoveableLineStyle: WeavessTypes.LineStyle;
    borderColor: string;
    color: string;
    isMoveable: boolean; // and the mode is in MEASUREMENT
    warning: {
      // The analyst is warned if they select a peak/trough that
      // is not within the specified range.
      min: number;
      max: number;
      borderColor: string;
      color: string;
      textColor: string;
    };
  };
}
const amplitudeTypeForPhase = new Map<CommonTypes.PhaseType, SignalDetectionTypes.AmplitudeType>([
  [CommonTypes.PhaseType.P, SignalDetectionTypes.AmplitudeType.AMPLITUDE_A5_OVER_2],
  [CommonTypes.PhaseType.LR, SignalDetectionTypes.AmplitudeType.AMPLITUDE_ALR_OVER_2]
]);
const amplitudeTypeForMagnitude = new Map<
  EventTypes.MagnitudeType,
  SignalDetectionTypes.AmplitudeType
>([
  [EventTypes.MagnitudeType.MB, SignalDetectionTypes.AmplitudeType.AMPLITUDE_A5_OVER_2],
  [EventTypes.MagnitudeType.MBMLE, SignalDetectionTypes.AmplitudeType.AMPLITUDE_A5_OVER_2],
  [EventTypes.MagnitudeType.MS, SignalDetectionTypes.AmplitudeType.AMPLITUDE_ALR_OVER_2],
  [EventTypes.MagnitudeType.MSMLE, SignalDetectionTypes.AmplitudeType.AMPLITUDE_ALR_OVER_2]
]);
const magnitudeTypesForPhase = new Map<CommonTypes.PhaseType, EventTypes.MagnitudeType[]>([
  [CommonTypes.PhaseType.P, [EventTypes.MagnitudeType.MB, EventTypes.MagnitudeType.MBMLE]],
  [CommonTypes.PhaseType.LR, [EventTypes.MagnitudeType.MS, EventTypes.MagnitudeType.MSMLE]]
]);

const displayedMagnitudesForCategory = new Map<
  MagnitudeCategory,
  AnalystWorkspaceTypes.DisplayedMagnitudeTypes
>([
  [
    MagnitudeCategory.BODY,
    Immutable.Map<any, boolean>([
      [EventTypes.MagnitudeType.MB, true],
      [EventTypes.MagnitudeType.MBMLE, true],
      [EventTypes.MagnitudeType.MS, false],
      [EventTypes.MagnitudeType.MSMLE, false]
    ])
  ],
  [
    MagnitudeCategory.SURFACE,
    Immutable.Map<any, boolean>([
      [EventTypes.MagnitudeType.MB, false],
      [EventTypes.MagnitudeType.MBMLE, false],
      [EventTypes.MagnitudeType.MS, true],
      [EventTypes.MagnitudeType.MSMLE, true]
    ])
  ]
]);

const magnitudeTypeToDisplayName = new Map<EventTypes.MagnitudeType, string>([
  [EventTypes.MagnitudeType.MB, 'Mb'],
  [EventTypes.MagnitudeType.MBMLE, 'Mb MLE'],
  [EventTypes.MagnitudeType.MS, 'Ms'],
  [EventTypes.MagnitudeType.MSMLE, 'Ms MLE']
]);

const amplitudeTypeToDisplayName = new Map<SignalDetectionTypes.AmplitudeType, string>([
  [SignalDetectionTypes.AmplitudeType.AMPLITUDE_A5_OVER_2, 'A5/2'],
  [SignalDetectionTypes.AmplitudeType.AMPLITUDE_ALR_OVER_2, 'ARL/2']
]);

// TODO hard-coded here for now, eventually this will be read in from the server.
export const systemConfig: SystemConfig = {
  numberOfDefiningLocationBehaviorsRequiredForLocate: 4,
  defaultTablePrecision: 3,
  continousFkConfiguration: {
    minMediumVelocity: 0.1,
    maxMediumVelocity: 10,
    defaultMediumVelocity: 5,
    minMaximumSlowness: 0.01,
    maxMaximumSlowness: 100,
    defaultMaximumSlowness: 40,
    minNumberOfPoints: 4,
    maxNumberOfPoints: 162,
    defaultNumberOfPoints: 81,
    normalizeWaveforms: false,
    useChannelVerticalOffsets: false
  },
  displayedMagnitudesForCategory,
  amplitudeTypeForPhase,
  amplitudeTypeForMagnitude,
  magnitudeTypesForPhase,
  magnitudeTypeToDisplayName,
  amplitudeTypeToDisplayName,
  fkNeedsReviewRuleSet: {
    phasesNeedingReview: [
      CommonTypes.PhaseType.P,
      CommonTypes.PhaseType.Pg,
      CommonTypes.PhaseType.Pn
    ]
  },
  defaultSdPhases: [
    CommonTypes.PhaseType.P,
    CommonTypes.PhaseType.S,
    CommonTypes.PhaseType.P3KPbc,
    CommonTypes.PhaseType.P4KPdf_B,
    CommonTypes.PhaseType.P7KPbc,
    CommonTypes.PhaseType.P7KPdf_D,
    CommonTypes.PhaseType.PKiKP,
    CommonTypes.PhaseType.PKKSab,
    CommonTypes.PhaseType.PKP2bc,
    CommonTypes.PhaseType.PKP3df_B,
    CommonTypes.PhaseType.PKSab,
    CommonTypes.PhaseType.PP_1,
    CommonTypes.PhaseType.pPKPbc,
    CommonTypes.PhaseType.PS,
    CommonTypes.PhaseType.Rg,
    CommonTypes.PhaseType.SKiKP,
    CommonTypes.PhaseType.SKKSac,
    CommonTypes.PhaseType.SKPdf,
    CommonTypes.PhaseType.SKSdf,
    CommonTypes.PhaseType.sPdiff,
    CommonTypes.PhaseType.SS,
    CommonTypes.PhaseType.sSKSdf,
    CommonTypes.PhaseType.Lg,
    CommonTypes.PhaseType.P3KPbc_B,
    CommonTypes.PhaseType.P5KPbc,
    CommonTypes.PhaseType.P7KPbc_B,
    CommonTypes.PhaseType.Pb,
    CommonTypes.PhaseType.PKKP,
    CommonTypes.PhaseType.PKKSbc,
    CommonTypes.PhaseType.PKP2df,
    CommonTypes.PhaseType.PKPab,
    CommonTypes.PhaseType.PKSbc,
    CommonTypes.PhaseType.PP_B,
    CommonTypes.PhaseType.pPKPdf,
    CommonTypes.PhaseType.PS_1,
    CommonTypes.PhaseType.SKKP,
    CommonTypes.PhaseType.SKKSac_B,
    CommonTypes.PhaseType.SKS,
    CommonTypes.PhaseType.SKSSKS,
    CommonTypes.PhaseType.sPKiKP,
    CommonTypes.PhaseType.SS_1,
    CommonTypes.PhaseType.SSS,
    CommonTypes.PhaseType.nNL,
    CommonTypes.PhaseType.P3KPdf,
    CommonTypes.PhaseType.P5KPbc_B,
    CommonTypes.PhaseType.P7KPbc_C,
    CommonTypes.PhaseType.PcP,
    CommonTypes.PhaseType.PKKPab,
    CommonTypes.PhaseType.PKKSdf,
    CommonTypes.PhaseType.PKP3,
    CommonTypes.PhaseType.PKPbc,
    CommonTypes.PhaseType.PKSdf,
    CommonTypes.PhaseType.pPdiff,
    CommonTypes.PhaseType.PPP,
    CommonTypes.PhaseType.pSdiff,
    CommonTypes.PhaseType.Sb,
    CommonTypes.PhaseType.SKKPab,
    CommonTypes.PhaseType.SKKSdf,
    CommonTypes.PhaseType.SKS2,
    CommonTypes.PhaseType.Sn,
    CommonTypes.PhaseType.sPKP,
    CommonTypes.PhaseType.SS_B,
    CommonTypes.PhaseType.SSS_B,
    CommonTypes.PhaseType.NP,
    CommonTypes.PhaseType.P3KPdf_B,
    CommonTypes.PhaseType.P5KPdf,
    CommonTypes.PhaseType.P7KPdf,
    CommonTypes.PhaseType.PcS,
    CommonTypes.PhaseType.PKKPbc,
    CommonTypes.PhaseType.PKP,
    CommonTypes.PhaseType.PKP3ab,
    CommonTypes.PhaseType.PKPdf,
    CommonTypes.PhaseType.Pn,
    CommonTypes.PhaseType.pPKiKP,
    CommonTypes.PhaseType.PPP_B,
    CommonTypes.PhaseType.pSKS,
    CommonTypes.PhaseType.ScP,
    CommonTypes.PhaseType.SKKPbc,
    CommonTypes.PhaseType.SKP,
    CommonTypes.PhaseType.SKS2ac,
    CommonTypes.PhaseType.SnSn,
    CommonTypes.PhaseType.sPKPab,
    CommonTypes.PhaseType.sSdiff,
    CommonTypes.PhaseType.NP_1,
    CommonTypes.PhaseType.P4KPbc,
    CommonTypes.PhaseType.P5KPdf_B,
    CommonTypes.PhaseType.P7KPdf_B,
    CommonTypes.PhaseType.Pdiff,
    CommonTypes.PhaseType.PKKPdf,
    CommonTypes.PhaseType.PKP2,
    CommonTypes.PhaseType.PKP3bc,
    CommonTypes.PhaseType.PKPPKP,
    CommonTypes.PhaseType.PnPn,
    CommonTypes.PhaseType.pPKP,
    CommonTypes.PhaseType.PPS,
    CommonTypes.PhaseType.pSKSac,
    CommonTypes.PhaseType.ScS,
    CommonTypes.PhaseType.SKKPdf,
    CommonTypes.PhaseType.SKPab,
    CommonTypes.PhaseType.SKS2df,
    CommonTypes.PhaseType.SP,
    CommonTypes.PhaseType.sPKPbc,
    CommonTypes.PhaseType.sSKS,
    CommonTypes.PhaseType.P4KPdf,
    CommonTypes.PhaseType.P5KPdf_C,
    CommonTypes.PhaseType.P7KPdf_C,
    CommonTypes.PhaseType.Pg,
    CommonTypes.PhaseType.PKKS,
    CommonTypes.PhaseType.PKP2ab,
    CommonTypes.PhaseType.PKP3df,
    CommonTypes.PhaseType.PKS,
    CommonTypes.PhaseType.PP,
    CommonTypes.PhaseType.pPKPab,
    CommonTypes.PhaseType.PPS_B,
    CommonTypes.PhaseType.pSKSdf,
    CommonTypes.PhaseType.Sdiff,
    CommonTypes.PhaseType.SKKS,
    CommonTypes.PhaseType.SKPbc,
    CommonTypes.PhaseType.SKSac,
    CommonTypes.PhaseType.SP_1,
    CommonTypes.PhaseType.sPKPdf,
    CommonTypes.PhaseType.sSKSac,
    CommonTypes.PhaseType.Sx,
    CommonTypes.PhaseType.tx,
    CommonTypes.PhaseType.N,
    CommonTypes.PhaseType.Px,
    CommonTypes.PhaseType.PKhKP,
    CommonTypes.PhaseType.LR
  ],
  prioritySdPhases: [CommonTypes.PhaseType.P, CommonTypes.PhaseType.S],
  nonFkSdPhases: [],
  defaultWeavessConfiguration: {
    stationHeightPx: 75
  },
  defaultWeavessHotKeyOverrides: {
    amplitudeScale: 'KeyS',
    amplitudeScaleSingleReset: 'Alt+KeyS',
    amplitudeScaleReset: 'Alt+Shift+KeyS',
    qcMaskCreate: 'KeyM'
  },
  defaultFkConfig: {
    fkPowerSpectrumDefinition: {
      windowLead: 5,
      windowLength: 10,
      startTimeStep: 2,
      lowFrequency: 1.25,
      highFrequency: 3.25,
      useChannelVerticalOffsets: true,
      phaseType: 'P',
      mediumVelocityKmPerSec: 1,
      xSlowStart: -0.4,
      xSlowDelta: 0.008,
      xSlowCount: 100,
      ySlowStart: -0.4,
      ySlowDelta: 0.008,
      ySlowCount: 100,
      waveformSampleRateHz: 10,
      waveformSampleRateToleranceHz: 0.05
    },
    fkProcessingContext: {
      analystId: 'ANALYST ID',
      storageVisibility: 'PRIVATE'
    },
    fkCreationInfo: {
      creatorName: 'FK CREATOR',
      softwareInfo: {
        name: 'interactive-analysis-ui',
        version: '0.1.0'
      }
    }
  },
  eventGlobalScan: {
    numberOfWaveforms: 20,
    sortType: AnalystWorkspaceTypes.WaveformSortType.stationName,
    additionalDataToLoad: 900, // 15 minutes
    additionalEventDataToLoad: 2700 // 45 minutes
  },
  eventRefinement: {
    numberOfWaveforms: 10,
    sortType: AnalystWorkspaceTypes.WaveformSortType.distance,
    additionalDataToLoad: 900, // 15 minutes
    additionalEventDataToLoad: 2700 // 45 minutes
  },
  additionalSubscriptionTime: 1800,
  interactionDelay: {
    extraFast: 100,
    fast: 250,
    medium: 500,
    slow: 750,
    extraSlow: 1000
  },

  measurementMode: {
    phases: [CommonTypes.PhaseType.P, CommonTypes.PhaseType.Pg, CommonTypes.PhaseType.Pn],
    amplitudeFilter: {
      filterType: 'FIR_HAMMING',
      filterPassBandType: 'BAND_PASS',
      lowFrequencyHz: 1,
      highFrequencyHz: 3
    },
    displayTimeRange: {
      startTimeOffsetFromSignalDetection: -7.5, // 7.5 seconds before
      endTimeOffsetFromSignalDetection: 12.5 // 12.5 seconds
    },
    selection: {
      id: 'selection-measurement-selection-',
      startTimeOffsetFromSignalDetection: -0.5, // 0.5 seconds before
      endTimeOffsetFromSignalDetection: 5, // 5 seconds after
      lineStyle: WeavessTypes.LineStyle.SOLID,
      borderColor: 'rgba(150, 150, 150, 1)',
      color: 'rgba(150, 150, 150, 0.3)',
      isMoveable: false
    },
    peakTroughSelection: {
      id: 'selection-measurement-peaktrough-',
      lineStyle: WeavessTypes.LineStyle.DASHED,
      nonMoveableLineStyle: WeavessTypes.LineStyle.SOLID,
      borderColor: 'rgb(41, 166, 52, 1)', // $forest3 color
      color: 'rgb(41, 166, 52, 0.05)', // $forest3 color
      isMoveable: true,
      warning: {
        // The analyst is warned if they select a peak/trough that
        // is not within the specified range.
        min: 0.4,
        max: 1,
        borderColor: 'rgb(255, 255, 0, 1)',
        color: 'rgb(255, 255, 0, 0.05)',
        textColor: 'rgb(255, 255, 0, 1)'
      }
    }
  },

  /**
   * !!!WARNING
   * The apollo cache uses the time ranges for caching of data. If this additional data to load ever
   * becomes variable, the apollo caching solution will need to become more sophisticated.
   */
  getDefaultTimeRange: (
    timeRange: CommonTypes.TimeRange,
    analystActivity: AnalystWorkspaceTypes.AnalystActivity
  ): CommonTypes.TimeRange => {
    const description = 'default time range';
    throwErrorIfUndefined(timeRange, description, 'time range');
    throwErrorIfUndefined(timeRange.startTime, description, 'start time');
    throwErrorIfUndefined(timeRange.endTime, description, 'end time');

    const timeRangeOffset =
      analystActivity === AnalystWorkspaceTypes.AnalystActivity.eventRefinement
        ? systemConfig.eventRefinement.additionalDataToLoad
        : systemConfig.eventGlobalScan.additionalDataToLoad;

    return {
      startTime: Number(timeRange.startTime) - timeRangeOffset,
      endTime: Number(timeRange.endTime) + timeRangeOffset
    };
  },
  getEventsTimeRange: (
    timeRange: CommonTypes.TimeRange,
    analystActivity: AnalystWorkspaceTypes.AnalystActivity
  ): CommonTypes.TimeRange => {
    const description = 'events time range';
    throwErrorIfUndefined(timeRange, description, 'time range');
    throwErrorIfUndefined(timeRange.startTime, description, 'start time');
    throwErrorIfUndefined(timeRange.endTime, description, 'end time');
    throwErrorIfUndefined(analystActivity, description, 'analyst activity');

    const startTimeRangeOffset =
      analystActivity === AnalystWorkspaceTypes.AnalystActivity.eventRefinement
        ? systemConfig.eventRefinement.additionalEventDataToLoad
        : systemConfig.eventGlobalScan.additionalEventDataToLoad;
    const endTimeRangeOffset =
      analystActivity === AnalystWorkspaceTypes.AnalystActivity.eventRefinement
        ? systemConfig.eventRefinement.additionalDataToLoad
        : systemConfig.eventGlobalScan.additionalDataToLoad;

    return {
      startTime: Number(timeRange.startTime) - startTimeRangeOffset,
      endTime: Number(timeRange.endTime) + endTimeRangeOffset
    };
  },
  getSignalDetectionTimeRange: (
    timeRange: CommonTypes.TimeRange,
    analystActivity: AnalystWorkspaceTypes.AnalystActivity
  ): CommonTypes.TimeRange => {
    const description = 'signal detection time range';
    throwErrorIfUndefined(timeRange, description, 'time range');
    throwErrorIfUndefined(timeRange.startTime, description, 'start time');
    throwErrorIfUndefined(timeRange.endTime, description, 'end time');
    throwErrorIfUndefined(analystActivity, description, 'analyst activity');

    const startTimeRangeOffset =
      analystActivity === AnalystWorkspaceTypes.AnalystActivity.eventRefinement
        ? systemConfig.eventRefinement.additionalDataToLoad
        : systemConfig.eventGlobalScan.additionalDataToLoad;
    const endTimeRangeOffset =
      analystActivity === AnalystWorkspaceTypes.AnalystActivity.eventRefinement
        ? systemConfig.eventRefinement.additionalEventDataToLoad
        : systemConfig.eventGlobalScan.additionalEventDataToLoad;

    return {
      startTime: Number(timeRange.startTime) - startTimeRangeOffset,
      endTime: Number(timeRange.endTime) + endTimeRangeOffset
    };
  },
  fkPlotTimePadding: 60
};
