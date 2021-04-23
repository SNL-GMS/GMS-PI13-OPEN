import gql from 'graphql-tag';

export const waveformSegmentsAddedSubscription = gql`
  subscription waveformChannelSegmentsAdded {
    waveformChannelSegmentsAdded {
      channelId
      startTime
      endTime
    }
  }
`;
