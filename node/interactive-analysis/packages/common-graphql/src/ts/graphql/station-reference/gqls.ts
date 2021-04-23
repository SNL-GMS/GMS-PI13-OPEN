import gql from 'graphql-tag';
import { locationFragment } from '../common/gqls';

export const referenceChannelFragment = gql`
  fragment ReferenceChannelFragment on ReferenceChannel {
    id
    name
    channelType
    sampleRate
    position {
      northDisplacementKm
      eastDisplacementKm
      verticalDisplacementKm
    }
    actualTime
    systemTime
    depth
  }
`;

export const referenceSiteFragment = gql`
  fragment ReferenceSiteFragment on ReferenceSite {
    id
    name
    channels {
      ...ReferenceChannelFragment
    }
    location {
      ...LocationFragment
    }
  }
  ${locationFragment}
  ${referenceChannelFragment}
`;

export const referenceStationFragment = gql`
  fragment ReferenceStationFragment on ReferenceStation {
    id
    name
    latitude
    longitude
    elevation
    stationType
    description
    defaultChannel {
      ...ReferenceChannelFragment
    }
    networks {
      id
      name
      monitoringOrganization
    }
    location {
      ...LocationFragment
    }
    sites {
      ...ReferenceSiteFragment
    }
    dataAcquisition {
      dataAcquisition
      interactiveProcessing
      automaticProcessing
    }
  }
  ${referenceSiteFragment}
  ${locationFragment}
  ${referenceChannelFragment}
`;
