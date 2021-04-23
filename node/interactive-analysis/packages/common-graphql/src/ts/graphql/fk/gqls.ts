import gql from 'graphql-tag';

export const frequencyBandFragment = gql`
  fragment FrequencyBandFragment on FrequencyBand {
    minFrequencyHz
    maxFrequencyHz
  }
`;

export const fstatDataFragment = gql`
  fragment FstatDataFragment on FstatData {
    azimuthWf {
      startTime
      sampleRate
      sampleCount
      values
    }
    slownessWf {
      startTime
      sampleRate
      sampleCount
      values
    }
    fstatWf {
      startTime
      sampleRate
      sampleCount
      values
    }
  }
`;

export const fkPowerSpectrumFragment = gql`
  fragment FkPowerSpectrumFragment on FkPowerSpectrum {
    power
    fstat
    quality
    attributes {
      peakFStat
      azimuth
      slowness
      azimuthUncertainty
      slownessUncertainty
    }
  }
`;

export const fkPowerSpectraFragment = gql`
  fragment FkPowerSpectraFragment on FkPowerSpectra {
    id
    contribChannels {
      id
      name
      channelType
      sampleRate
    }
    startTime
    sampleRate
    sampleCount
    windowLead
    windowLength
    stepSize
    lowFrequency
    highFrequency
    metadata {
      phaseType
      slowStartX
      slowDeltaX
      slowStartY
      slowDeltaY
    }
    slowCountX
    slowCountY
    reviewed
    spectrums {
      ...FkPowerSpectrumFragment
    }
    fstatData {
      ...FstatDataFragment
    }
    configuration {
      maximumSlowness
      mediumVelocity
      numberOfPoints
      normalizeWaveforms
      useChannelVerticalOffset
      leadFkSpectrumSeconds
      contributingChannelsConfiguration {
        id
        enabled
        name
      }
    }
  }
  ${fkPowerSpectrumFragment}
  ${fstatDataFragment}
`;

export const fkFrequencyThumbnailBySDIdFragment = gql`
  fragment FkFrequencyThumbnailBySDIdFragment on FkFrequencyThumbnailBySDId {
    signalDetectionId
    fkFrequencyThumbnails {
      frequencyBand {
        ...FrequencyBandFragment
      }
      fkSpectra {
        ...FkPowerSpectraFragment
      }
    }
  }
  ${frequencyBandFragment}
  ${fkPowerSpectraFragment}
`;
