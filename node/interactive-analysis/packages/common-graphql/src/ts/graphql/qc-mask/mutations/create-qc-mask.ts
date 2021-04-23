import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const createQcMaskMutation = gql`
  mutation createQcMask($channelNames: [String]!, $input: QcMaskInput!) {
    createQcMask(channelNames: $channelNames, input: $input) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
