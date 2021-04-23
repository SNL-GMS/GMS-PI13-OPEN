import gql from 'graphql-tag';
import { fkFrequencyThumbnailBySDIdFragment } from '../../fk/gqls';

export const computeFkFrequencyThumbnailQuery = gql`
  query computeFkFrequencyThumbnails($fkInput: FkInput!) {
    computeFkFrequencyThumbnails(fkInput: $fkInput) {
      ...FkFrequencyThumbnailBySDIdFragment
    }
  }
  ${fkFrequencyThumbnailBySDIdFragment}
`;
