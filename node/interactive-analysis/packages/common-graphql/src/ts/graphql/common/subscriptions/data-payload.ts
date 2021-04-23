import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const dataPayloadSubscription = gql`
  subscription dataPayload {
    dataPayload {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
