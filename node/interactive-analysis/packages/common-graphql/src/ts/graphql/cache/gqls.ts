import gql from 'graphql-tag';
import { workspaceStateFragment } from '../common/gqls';
import { eventFragment } from '../event/gqls';
import { qcMaskFragment } from '../qc-mask/gqls';
import { signalDetectionFragment } from '../signal-detection/gqls';

/**
 * Represents gql fragment the history change.
 */
export const invalidDataFragment = gql`
  fragment InvalidDataFragment on InvalidData {
    eventIds
    signalDetectionIds
  }
`;

/**
 * Represents gql fragment the hypothesis change information.
 */
const hypothesisChangeInformationFragment = gql`
  fragment HypothesisChangeInformationFragment on HypothesisChangeInformation {
    id
    hypothesisId
    type
    parentId
    userAction
  }
`;

/**
 * Represents gql fragment the history change.
 */
export const historyChangeFragment = gql`
  fragment HistoryChangeFragment on HistoryChange {
    id
    active
    eventId
    conflictCreated
    hypothesisChangeInformation {
      ...HypothesisChangeInformationFragment
    }
  }
  ${hypothesisChangeInformationFragment}
`;

/**
 * Represents gql fragment the history summary.
 */
export const historyFragment = gql`
  fragment HistoryFragment on History {
    id
    description
    changes {
      ...HistoryChangeFragment
    }
    redoPriorityOrder
  }
  ${historyChangeFragment}
`;

export const dataPayloadFragment = gql`
  fragment DataPayloadFragment on DataPayload {
    events {
      ...EventFragment
    }
    sds {
      ...SignalDetectionFragment
    }
    qcMasks {
      ...QcMaskFragment
    }
    invalid {
      ...InvalidDataFragment
    }
    workspaceState {
      ...WorkspaceStateFragment
    }
    history {
      ...HistoryFragment
    }
  }
  ${eventFragment}
  ${signalDetectionFragment}
  ${qcMaskFragment}
  ${invalidDataFragment}
  ${workspaceStateFragment}
  ${historyFragment}
`;
