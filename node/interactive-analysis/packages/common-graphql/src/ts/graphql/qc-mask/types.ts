import { QueryControls } from 'react-apollo';
import { DataPayload } from '../cache/types';
import { TimeRange } from '../common/types';

// ***************************************
// Mutations
// ***************************************

export interface QcMaskInput {
  timeRange: TimeRange;
  category: string;
  type: string;
  rationale: string;
}

export interface CreateQcMaskMutationArgs {
  channelNames: string[];
  input: QcMaskInput;
}

export interface CreateQcMaskMutationResult {
  data: {
    createQcMask: DataPayload;
  };
}

export interface RejectQcMaskMutationArgs {
  maskId: string;
  inputRationale: string;
}

export interface RejectQcMaskMutationResult {
  data: {
    rejectQcMask: DataPayload;
  };
}

export interface UpdateQcMaskMutationArgs {
  maskId: string;
  input: QcMaskInput;
}

export interface UpdateQcMaskMutationResult {
  data: {
    updateQcMask: DataPayload;
  };
}

// ***************************************
// Subscriptions
// ***************************************

export interface QcMasksCreatedSubscription {
  qcMasksCreated: QcMask[];
}

// ***************************************
// Queries
// ***************************************

export interface QcMasksByChannelNameQueryArgs {
  timeRange: TimeRange;
  channelNames: string[];
}

// tslint:disable-next-line:max-line-length interface-over-type-literal
export type QcMasksByChannelNameQueryProps = {
  qcMasksByChannelNameQuery: QueryControls<{}> & { qcMasksByChannelName: QcMask[] };
};

// ***************************************
// Model
// ***************************************

export interface QcMaskVersion {
  startTime: number;
  endTime: number;
  category: string;
  type: string;
  rationale: string;
  version: string;
  channelSegmentIds: string[];
}

export interface QcMask {
  id: string;
  channelName: string;
  currentVersion: QcMaskVersion;
  qcMaskVersions: QcMaskVersion[];
}
