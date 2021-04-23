// tslint:disable-next-line: no-default-import
import defaultConfig from './default';

// Overwrites default config for a deployed environment
const deployedConfig = {
  ...defaultConfig,
  // new config name
  configName: 'deployed',
  // session memory is from a DB not in memory
  inMemorySession: false,
  // Sets mock enable to false for all services
  configuration: {
    ...defaultConfig.configuration,
    backend: {
      ...defaultConfig.configuration.backend,
      mock: {
        enable: false
      }
    }
  },
  systemMessage: {
    ...defaultConfig.systemMessage,
    backend: {
      ...defaultConfig.systemMessage.backend,
      mock: {
        enable: false
      }
    }
  },
  referenceStation: {
    ...defaultConfig.referenceStation,
    backend: {
      ...defaultConfig.referenceStation.backend,
      mock: {
        enable: false
      }
    }
  },
  processingStation: {
    ...defaultConfig.processingStation,
    backend: {
      ...defaultConfig.processingStation.backend,
      mock: {
        enable: false
      }
    }
  },
  signalDetection: {
    ...defaultConfig.signalDetection,
    backend: {
      ...defaultConfig.signalDetection.backend,
      mock: {
        enable: false
      }
    }
  },
  qcMask: {
    ...defaultConfig.qcMask,
    backend: {
      ...defaultConfig.qcMask.backend,
      mock: {
        enable: false
      }
    }
  },
  userProfile: {
    ...defaultConfig.userProfile,
    backend: {
      ...defaultConfig.userProfile.backend,
      mock: {
        enable: false
      }
    }
  },
  filterWaveform: {
    ...defaultConfig.filterWaveform,
    backend: {
      ...defaultConfig.filterWaveform.backend,
      mock: {
        enable: false
      }
    }
  },
  event: {
    ...defaultConfig.event,
    backend: {
      ...defaultConfig.event.backend,
      mock: {
        enable: false
      }
    }
  },
  channelSegment: {
    ...defaultConfig.channelSegment,
    backend: {
      ...defaultConfig.channelSegment.backend,
      mock: {
        enable: false
      }
    }
  },
  fk: {
    ...defaultConfig.fk,
    backend: {
      ...defaultConfig.fk.backend,
      mock: {
        enable: false
      }
    }
  },
  dataAcquisition: {
    ...defaultConfig.dataAcquisition,
    backend: {
      ...defaultConfig.dataAcquisition.backend,
      mock: {
        enable: false
      }
    }
  },
  performanceMonitoring: {
    ...defaultConfig.performanceMonitoring,
    backend: {
      ...defaultConfig.performanceMonitoring.backend,
      mock: {
        enable: false
      }
    }
  },
  // Change test data location for mock mode - deprecated?
  testData: {
    ...defaultConfig.testData,
    standardTestDataSet: {
      ...defaultConfig.testData.standardTestDataSet,
      stdsDataHome: '/opt/app-root/src/Test_Data_Sets/Standard_Test_Data/'
    }
  },
  // Changes kafka brokers to be the deployed kafka containers
  kafka: {
    ...defaultConfig.kafka,
    brokers: ['kafka1:9092', 'kafka2:9092', 'kafka3:9092']
  }
};

// tslint:disable-next-line: no-default-export
export default deployedConfig;
