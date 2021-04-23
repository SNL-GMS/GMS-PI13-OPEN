import gql from 'graphql-tag';

export const qcMaskVersionFragment = gql`
  fragment QcMaskVersionFragment on QcMaskVersion {
    startTime
    endTime
    category
    type
    rationale
    version
    channelSegmentIds
  }
`;

export const qcMaskFragment = gql`
  fragment QcMaskFragment on QcMask {
    id
    channelName
    currentVersion {
      ...QcMaskVersionFragment
    }
    qcMaskVersions {
      ...QcMaskVersionFragment
    }
  }
  ${qcMaskVersionFragment}
`;
