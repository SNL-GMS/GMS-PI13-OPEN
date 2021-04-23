export interface MessageConfig {
  labels: {
    sohToolbar: {
      filterByStationGroup: string;
      filterStatuses: string;
      filterMonitorsByStatus: string;
      updateTimeDisplay: string;
      interval: string;
    };
    environmentalSubtitle: string;
    lagSubtitle: string;
    timelinessSubtitle: string;
    missingSubtitle: string;
    lagTrendsSubtitle: string;
    missingTrendsSubtitle: string;
  };
  table: {
    noDataMessage: string;
  };
  tooltipMessages: {
    sohToolbar: {
      filerByStationGroup: string;
      selectStatuses: string;
      lastUpdateTime: string;
      interval: string;
    };
    stationStatistics: {
      nonContributingCell: string;
      nullCell: string;
      notReceivedCell: string;
      channelTimelinessHeader: string;
      channelEnvironmentHeader: string;
      channelLagHeader: string;
      channelMissingHeader: string;
      stationTimelinessHeader: string;
      stationEnvironmentHeader: string;
      stationLagHeader: string;
      stationMissingHeader: string;
      stationHeader: string;
      topCount: string;
      bottomCount: string;
      stationCell: string;
      badge: string;
    };
  };
}
export const messageConfig: MessageConfig = {
  labels: {
    sohToolbar: {
      filterByStationGroup: 'Filter by Station Group',
      filterStatuses: 'Filter by Status',
      filterMonitorsByStatus: 'Filter Monitors By Status',
      updateTimeDisplay: 'Last Updated',
      interval: 'Update Interval'
    },
    environmentalSubtitle: 'Current percent environmental issues per channel',
    lagSubtitle: 'Current lag per channel',
    timelinessSubtitle: 'Current timeliness per channel',
    missingSubtitle: 'Current percent missing data per channel',
    lagTrendsSubtitle: 'Historical trends for lag',
    missingTrendsSubtitle: 'Historical trends for missing'
  },
  table: {
    noDataMessage: 'No SOH to display'
  },
  tooltipMessages: {
    sohToolbar: {
      filerByStationGroup: 'Set which station groups appear',
      selectStatuses: 'Set which statuses appear in the lower bin',
      lastUpdateTime: 'Most recent SOH data received',
      interval: 'Interval at which SOH data is processed'
    },
    stationStatistics: {
      nonContributingCell: 'This value did not contribute to the SOH status for this station',
      nullCell: 'This value was unknown in the latest SOH status update',
      notReceivedCell: 'This value was not received in the latest SOH status update',
      channelTimelinessHeader:
        'Longest time in seconds since the time of the latest data sample that has been acquired on a single channel (now - latest data sample time that has been acquired, worst channel)',
      channelEnvironmentHeader:
        "Largest percentage of 'bad' indicators on a single channel/environmental monitor pair over a configurable time window",
      channelLagHeader:
        'Longest transmission time for received data samples on a single channel over a configurable time window (reception time - latest data sample time, worst channel)',
      channelMissingHeader:
        'Largest percentage of missing data on a single channel over a configurable time window (worst channel)',
      stationTimelinessHeader:
        'Time in seconds since the time of the latest data sample that has been acquired on any channel (now - latest data sample time that has been acquired on any channel)',
      stationEnvironmentHeader:
        "Average percentage of 'bad' indicators across all channels (time windows are configurable and can vary by channel; station value is a straight average of the channel percentages)",
      stationLagHeader:
        'Average transmission time for received data samples across all channels over a configurable time window, e.g., last 10 minutes (reception time - latest data sample time)',
      stationMissingHeader:
        'Total percentage of missing data across all channels over a configurable time window',
      stationHeader: 'Name of the station',
      topCount: 'BAD group status count',
      bottomCount: 'MARGINAL group status count',
      stationCell: 'Worst of capability rollup',
      badge: 'Worst of monitor type rollup - '
    }
  }
};
