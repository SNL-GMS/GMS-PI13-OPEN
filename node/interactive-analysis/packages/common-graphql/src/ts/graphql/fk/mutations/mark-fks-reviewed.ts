import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const markFksReviewedMutation = gql`
  mutation markFksReviewed($markFksReviewedInput: MarkFksReviewedInput!) {
    markFksReviewed(markFksReviewedInput: $markFksReviewedInput) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
