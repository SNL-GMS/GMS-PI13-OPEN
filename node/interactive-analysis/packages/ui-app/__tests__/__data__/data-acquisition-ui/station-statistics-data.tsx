import { SohStatusSummary } from '@gms/common-graphql/lib/graphql/soh/types';
import { cloneDeep } from 'apollo-utilities';
import uniqueId from 'lodash/uniqueId';
import { StationStatisticsRow } from '../../../src/ts/components/data-acquisition-ui/components/station-statistics/types';

const numRows = 100;

export const rowTemplate: StationStatisticsRow = {
  channelEnvironment: {
    isContributing: true,
    status: SohStatusSummary.GOOD,
    value: 12.34
  },
  channelLag: {
    isContributing: true,
    status: SohStatusSummary.MARGINAL,
    value: 2.34
  },
  channelMissing: {
    isContributing: true,
    status: SohStatusSummary.BAD,
    value: 98.76
  },
  channelTimeliness: {
    isContributing: true,
    status: SohStatusSummary.GOOD,
    value: 6.87
  },
  stationEnvironment: 76.29,
  stationLag: 2.34,
  stationMissing: 98.76,
  stationTimeliness: 5.62,
  id: uniqueId('rowTemplate_'),
  needsAcknowledgement: true,
  needsAttention: true,
  stationData: {
    stationName: uniqueId('Station '),
    stationStatus: SohStatusSummary.BAD,
    stationCapabilityStatus: SohStatusSummary.BAD
  },
  stationGroups: [
    {
      groupName: 'GroupA',
      sohStationCapability: SohStatusSummary.BAD,
      stationName: 'Station'
    },
    {
      groupName: 'GroupB',
      sohStationCapability: SohStatusSummary.BAD,
      stationName: 'Station'
    }
  ]
};

export const tableData = (() => {
  const testRows: StationStatisticsRow[] = [];
  for (let i = 0; i < numRows; i++) {
    const theRow = cloneDeep(rowTemplate);
    theRow.id = uniqueId('row_');
    theRow.stationData = {
      stationName: uniqueId('Station '),
      stationStatus: SohStatusSummary.BAD,
      stationCapabilityStatus: SohStatusSummary.BAD
    };
    theRow.channelEnvironment.value = i;
    theRow.channelLag.value = i;
    theRow.channelMissing.value = i;
    theRow.channelTimeliness.value = i;
    theRow.stationEnvironment = i;
    theRow.stationLag = i;
    theRow.stationMissing = i;
    theRow.stationTimeliness = i;
    testRows.push(theRow);
  }
  return testRows;
})();
