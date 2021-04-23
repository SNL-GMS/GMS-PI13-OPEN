import gql from 'graphql-tag';

/**
 * Represents gql SOHStationGroupWithPriority fragment.
 */
export const sohStationGroupNameWithPriorityFragment = gql`
  fragment SOHStationGroupNameWithPriorityFragment on SOHStationGroupNameWithPriority {
    name
    priority
  }
`;

export const waveformFilterFragment = gql`
  fragment WaveformFilterFragment on WaveformFilter {
    id
    name
    description
    filterType
    filterPassBandType
    lowFrequencyHz
    highFrequencyHz
    order
    filterSource
    filterCausality
    zeroPhase
    sampleRate
    sampleRateTolerance
    groupDelaySecs
    validForSampleRate
  }
`;

/**
 * Represents gql SOHStationGroupWithPriority fragment.
 */
export const analystConfigurationFragment = gql`
  fragment AnalystConfigurationFragment on AnalystConfiguration {
    logLevel
    defaultNetwork
    redisplayPeriod
    reprocessingPeriod
    acknowledgementQuietDuration
    availableQuietDurations
    sohHistoricalDurations
    sohStationStaleTimeMS
    sohStationGroupNames {
      ...SOHStationGroupNameWithPriorityFragment
    }
    defaultFilters {
      ...WaveformFilterFragment
    }
    systemMessageLimit
  }
  ${sohStationGroupNameWithPriorityFragment}
  ${waveformFilterFragment}
`;
