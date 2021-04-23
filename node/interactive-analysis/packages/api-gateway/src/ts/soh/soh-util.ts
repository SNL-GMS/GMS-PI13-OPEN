import {
  SohStatusSummary,
  StationGroupSohStatus,
  StationSohCapabilityStatus,
  UiStationSoh
} from '@gms/common-graphql/lib/graphql/soh/types';
import { SOHStationGroupNameWithPriority } from '@gms/common-graphql/lib/graphql/ui-configuration/types';
import { epochSecondsNow, MILLISECONDS_IN_SECOND, toOSDTime, uuid4 } from '@gms/common-util';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { AcknowledgedSohStatusChange, SohStatusChange } from './model';

export const LIKELY_HOOD_OF_DATE_CHANGE = 0.75;

/**
 * Creates a base status change to be modified later
 */
export const createAcknowledgedStatusChange = (
  userName: string,
  stationName: string,
  unacknowledgedChanges: SohStatusChange[],
  comment?: string
): AcknowledgedSohStatusChange => ({
  acknowledgedAt: toOSDTime(Date.now() / MILLISECONDS_IN_SECOND),
  acknowledgedBy: userName,
  comment,
  id: uuid4(),
  acknowledgedStation: stationName,
  acknowledgedChanges: unacknowledgedChanges
});

const getStationGroups = (stationName: string): StationSohCapabilityStatus[] => {
  const groups = ProcessingStationProcessor.Instance().getSohStationGroupNames(stationName);
  return groups.map(g => ({
    groupName: g.name,
    stationName,
    sohStationCapability: SohStatusSummary.NONE
  }));
};

export const createEmptyStationSoh = (stationName: string): UiStationSoh => ({
  id: stationName,
  uuid: uuid4(),
  stationName,
  sohStatusSummary: SohStatusSummary.NONE,
  needsAcknowledgement: false,
  needsAttention: false,
  time: epochSecondsNow(),
  statusContributors: [],
  stationGroups: getStationGroups(stationName),
  channelSohs: [],
  allStationAggregates: []
});

export const createStationGroupSohStatus = (
  stationGroupConfig: SOHStationGroupNameWithPriority[]
): StationGroupSohStatus[] => {
  const stationGroupSohMap = stationGroupConfig.map(sgc => ({
    stationGroupName: sgc.name,
    time: Date.now(), // change to timeMs
    groupCapabilityStatus: SohStatusSummary.NONE,
    id: sgc.name,
    priority: sgc.priority
  }));
  return stationGroupSohMap;
};
