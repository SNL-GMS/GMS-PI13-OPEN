import gql from 'graphql-tag';
import { locationFragment } from '../common/gqls';

export const processingChannelFragment = gql`
  fragment ProcessingChannelFragment on ProcessingChannel {
    name
    displayName
    canonicalName
    description
    station
    channelDataType
    nominalSampleRateHz
    location {
      ...LocationFragment
    }
    orientationAngles {
      horizontalAngleDeg
      verticalAngleDeg
    }
  }
  ${locationFragment}
`;

export const processingChannelGroupFragment = gql`
  fragment ProcessingChannelGroupFragment on ProcessingChannelGroup {
    name
    description
    type
    location {
      ...LocationFragment
    }
    channels {
      ...ProcessingChannelFragment
    }
  }
  ${locationFragment}
  ${processingChannelFragment}
`;

export const processingStationFragment = gql`
  fragment ProcessingStationFragment on ProcessingStation {
    name
    type
    description
    location {
      ...LocationFragment
    }
    channelGroups {
      ...ProcessingChannelGroupFragment
    }
    channels {
      ...ProcessingChannelFragment
    }
  }
  ${locationFragment}
  ${processingChannelGroupFragment}
  ${processingChannelFragment}
`;
