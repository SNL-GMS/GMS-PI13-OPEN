import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const markActivityIntervalMutation = gql`
  mutation markActivityInterval($activityIntervalId: String!, $input: IntervalStatusInput!) {
    markActivityInterval(activityIntervalId: $activityIntervalId, input: $input) {
      activityInterval {
        id
      }
      dataPayload {
        ...DataPayloadFragment
      }
    }
  }
  ${dataPayloadFragment}
`;
