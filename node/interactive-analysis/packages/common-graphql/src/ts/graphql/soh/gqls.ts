import gql from 'graphql-tag';

export const sohStatusFragment = gql`
  fragment SohStatusFragment on SohStatus {
    stationAcquisitionSohStatus {
      completeness
      completenessSummary
      lag
      lagSummary
    }
    environmentSohStatus {
      countBySohType {
        CLOCK_LOCKED
        POSITIVE_LEAP_SECOND_DETECTED
        QUESTIONABLE_TIME_TAG
        START_TIME_SERIES_BLOCKETTE
        EVENT_IN_PROGRESS
        STATION_EVENT_DETRIGGER
        DIGITAL_FILTER_MAY_BE_CHARGING
        SPIKE_DETECTED
        GLITCHES_DETECTED
        STATION_EVENT_TRIGGER
        END_TIME_SERIES_BLOCKETTE
        SHORT_DATA_RECORD
        NEGATIVE_LEAP_SECOND_DETECTED
        LONG_DATA_RECORD
      }
      summaryBySohType {
        CLOCK_LOCKED
        POSITIVE_LEAP_SECOND_DETECTED
        QUESTIONABLE_TIME_TAG
        START_TIME_SERIES_BLOCKETTE
        EVENT_IN_PROGRESS
        STATION_EVENT_DETRIGGER
        DIGITAL_FILTER_MAY_BE_CHARGING
        SPIKE_DETECTED
        GLITCHES_DETECTED
        STATION_EVENT_TRIGGER
        END_TIME_SERIES_BLOCKETTE
        SHORT_DATA_RECORD
        NEGATIVE_LEAP_SECOND_DETECTED
        LONG_DATA_RECORD
      }
    }
  }
`;

export const sohContributorFragment = gql`
  fragment SohContributorFragment on SohContributor {
    value
    valuePresent
    statusSummary
    contributing
    type
  }
`;

export const channelSohFragment = gql`
  fragment ChannelSohFragment on ChannelSoh {
    channelName
    channelSohStatus
    allSohMonitorValueAndStatuses {
      status
      value
      valuePresent
      monitorType
      hasUnacknowledgedChanges
      contributing
      quietUntilMs
      quietDurationMs
      thresholdBad
      thresholdMarginal
    }
  }
`;
export const uiStationSohFragment = gql`
  fragment UiStationSohFragment on UiStationSoh {
    id
    uuid
    stationName
    sohStatusSummary
    needsAcknowledgement
    needsAttention
    time
    statusContributors {
      ...SohContributorFragment
    }
    stationGroups {
      groupName
      stationName
      sohStationCapability
    }
    channelSohs {
      ...ChannelSohFragment
    }
    allStationAggregates {
      value
      valuePresent
      aggregateType
    }
  }
  ${sohContributorFragment}
  ${channelSohFragment}
`;

export const stationGroupSohStatusFragment = gql`
  fragment StationGroupSohStatusFragment on StationGroupSohStatus {
    stationGroupName
    time
    groupCapabilityStatus
    id
    priority
  }
  ${uiStationSohFragment}
`;

export const stationAndStationGroupSohFragment = gql`
  fragment StationAndStationGroupSohFragment on StationAndStationGroupSoh {
    stationGroups {
      ...StationGroupSohStatusFragment
    }
    stationSoh {
      ...UiStationSohFragment
    }
    isUpdateResponse
  }
  ${uiStationSohFragment}
  ${stationGroupSohStatusFragment}
`;
