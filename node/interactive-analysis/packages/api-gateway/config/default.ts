// To point at a deployment run with DEPLOYED_URL variable like
// this: DEPLOYED_URL='thud-config.orca.gms.test.com' yarn start
const baseURL = process.env.DEPLOYED_URL ? `.${process.env.DEPLOYED_URL}` : ':8080';

// To set mock enabled false run with MOCK=false or MOCK=FALSE
// all other values for MOCK ('undefined', 'true', 't', '', random string) will result in mock true
const mockEnabled =
  process.env.MOCK === undefined ||
  process.env.MOCK === '' ||
  process.env.MOCK.toLocaleLowerCase() !== 'false';

// tslint:disable: max-line-length
// Default Config
const defaultConfig = {
  configName: 'default',
  logLevel: 'info',
  inMemorySession: true,

  // Server configuration
  server: {
    graphql: {
      http: {
        host: 'localhost',
        port: 3000,
        graphqlPath: '/graphql' // GraphQL query/mutation/subscription path
      },
      ws: {
        host: 'localhost',
        port: 4000,
        path: '/subscriptions'
      }
    }
  },

  // Kafka configuration
  kafka: {
    clientId: 'api-gateway',
    groupId: 'user-interface',
    brokers: ['kafka:9092'],
    connectionTimeout: 3000,
    maxWaitTimeInMs: 100,
    heartbeatInterval: 500, // ms
    consumerTopics: {
      systemMessagesTopic: 'system.system-messages',
      uiStationSoh: 'soh.ui-materialized-view'
    },
    producerTopics: {
      acknowledgedTopic: 'soh.ack-station-soh',
      quietedTopic: 'soh.quieted-list'
    }
  },

  // Common - data payload subscription
  common: {
    resolvers: {},
    subscriptions: {
      channels: {
        dataPayload: 'dataPayload'
      }
    }
  },

  // System Messages Configuration
  systemMessage: {
    backend: {
      mock: {
        enable: mockEnabled
      },
      // Service endpoints for this component
      services: {
        getSystemMessageDefinitions: {
          requestConfig: {
            method: 'post',
            url: `http://smds-service${baseURL}/retrieve-system-message-definitions`,
            headers: {
              accept: 'text/plain',
              'content-type': 'text/plain'
            },
            proxy: false,
            timeout: 60000
          }
        }
      }
    },
    subscriptions: {
      systemMessages: 'systemMessages'
    }
  },

  // Workflow configuration
  workflow: {
    resolvers: {},
    subscriptions: {
      channels: {
        stagesChanged: 'stagesChanged'
      }
    },
    intervalService: {
      intervalCreationStartTimeSec: 1274400000, // Friday, May 21, 2010 12:00:00 AM GMT
      mockedIntervalStartTimeSec: 1274299201, // Wednesday, Mat 19, 2010 20:00:00 GMT
      createIntervalDurationSec: 7200, // How often to create new interval
      intervalDurationSec: 7200, // Duration of an interval i.e. 2 hours
      intervalCreationFrequencyMillis: 60000 // Wake every 60 seconds to check new interval
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        workflowData: {
          requestConfig: {
            method: 'get',
            url: 'http://WORKFLOW_I_HAVE_NO_IDEA_URL',
            headers: {
              accept: 'application/json',
              'content-Type': 'application/json'
            },
            proxy: false,
            timeout: 60000
          }
        }
      }
    }
  },

  // Reference Station configuration
  referenceStation: {
    resolvers: {},
    subscriptions: {
      channels: {}
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        networkByName: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/station-reference/reference-networks/name`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 120000
          }
        },
        stationsByIds: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/station-reference/reference-stations/version-ids`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 120000
          }
        },
        channelsByIds: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/station-reference/reference-channels`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 120000
          }
        }
      }
    }
  },
  // Processing Station configuration
  processingStation: {
    resolvers: {},
    subscriptions: {
      channels: {}
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        stationGroupByName: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/station-groups`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 120000
          }
        },
        stationsByNames: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service:8080/osd/stations`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 120000
          }
        },
        channelsByNames: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL ? baseURL : ':8080'}/osd/channels`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 120000
          }
        }
      }
    }
  },

  // Signal detection configuration
  signalDetection: {
    resolvers: {},
    subscriptions: {
      channels: {
        detectionsCreated: 'detectionsCreated'
      }
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        sdsByStation: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/signal-detection/stations-time`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 120000
          }
        },
        saveSds: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/signal-detections/new`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        }
      }
    }
  },

  // QC mask configuration
  qcMask: {
    resolvers: {},
    subscriptions: {
      channels: {
        qcMasksCreated: 'qcMasksCreated'
      }
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        masksByChannelIds: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/qc-masks/channels-time`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        saveMasks: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL ? baseURL : ':8080'}/osd/qc-masks/new`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        }
      }
    }
  },

  // Waveform configuration
  waveform: {
    waveformMode: 'fromFile',
    resolvers: {},
    subscriptions: {
      channels: {
        waveformUpdated: 'waveformUpdated',
        waveformChannelSegmentsAdded: 'waveformChannelSegmentsAdded'
      }
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      services: {}
    }
  },

  // Filtered waveform configuration
  filterWaveform: {
    numberFilterSamples: 100000,
    resolvers: {},
    subscriptions: {
      channels: {}
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        calculateWaveformSegments: {
          requestConfig: {
            method: 'post',
            url: `http://filter-control-service${baseURL}/signal-enhancement/waveform-filtering/streaming`,
            responseType: 'arraybuffer',
            proxy: false,
            headers: {
              accept: 'application/msgpack',
              'content-type': 'application/msgpack'
            },
            timeout: 300000
          }
        }
      }
    }
  },

  // Event configuration
  event: {
    resolvers: {},
    subscriptions: {
      channels: {
        eventsCreated: 'eventsCreated'
      }
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 20
      },
      // Service endpoints for this component
      services: {
        getEventsByTimeAndLatLong: {
          requestConfig: {
            method: 'post',
            url: `http://osd-signaldetection-repository-service${baseURL}/coi/events/query/time-lat-lon`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 180000
          }
        },
        getEventsByIds: {
          requestConfig: {
            method: 'post',
            url: `http://osd-signaldetection-repository-service${baseURL}/coi/events/query/ids`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 10000
          }
        },
        computeFeaturePredictions: {
          requestConfig: {
            method: 'post',
            url: `http://fp-service${baseURL}/feature-measurement/prediction/for-location-solution-and-channel`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        locateEvent: {
          requestConfig: {
            method: 'post',
            url: `http://event-location-control-service${baseURL}/event/location/locate/interactive`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        saveEvents: {
          requestConfig: {
            method: 'post',
            url: `http://osd-signaldetection-repository-service${baseURL}/coi/events`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        computeNetworkMagnitudeSolution: {
          requestConfig: {
            method: 'post',
            url: `http://event-magnitude-service${baseURL ? baseURL : ':8080'}/event/update`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        }
      }
    }
  },

  // Configuration configuration
  configuration: {
    resolvers: {},
    subscriptions: {
      channels: {}
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        getAnalystConfiguration: {
          requestConfig: {
            method: 'post',
            url: `http://interactive-analysis-config-service${baseURL}/resolve`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        getSohConfiguration: {
          requestConfig: {
            method: 'post',
            url: `http://ssam-control${baseURL}/retrieve-station-soh-monitoring-ui-client-parameters`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'text/plain',
              'content-type': 'text/plain'
            },
            timeout: 120000
          }
        }
      }
    }
  },

  // Config configuration
  config: {
    rootKey: 'ui',
    resolvers: {},
    subscriptions: {
      channels: {}
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 20
      },
      // Service endpoints for this component
      services: {
        configByKey: {
          requestConfig: {
            method: 'post',
            url: '',
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        }
      }
    }
  },

  // Channel segment configuration
  channelSegment: {
    resolvers: {},
    subscriptions: {
      channels: {}
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        channelSegmentsById: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/channel-segments/segment-ids`,
            responseType: 'arraybuffer',
            proxy: false,
            headers: {
              accept: 'application/msgpack',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        channelSegmentsByTimeRange: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/channel-segments/channels-time`,
            responseType: 'arraybuffer',
            proxy: false,
            headers: {
              accept: 'application/msgpack',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        computeBeam: {
          requestConfig: {
            method: 'post',
            url: `http://beam-control-service${baseURL}/signal-enhancement/beam/streaming`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        saveWaveforms: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/channel-segments/new`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        saveFks: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/channel-segments/fk/new`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        }
      }
    }
  },

  // FK configuration
  fk: {
    resolvers: {},
    subscriptions: {
      channels: {
        fksCreated: 'fksCreated'
      }
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        computeFk: {
          requestConfig: {
            method: 'post',
            url: `http://fk-control-service${baseURL}/signal-enhancement/fk/spectra/interactive`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 120000
          }
        }
      }
    }
  },

  // Data acquisition configuration
  dataAcquisition: {
    resolvers: {},
    subscriptions: {
      channels: {}
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        transferredFilesByTimeRange: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/data-acquisition/transferred-files/by-transfer-time`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 480000
          }
        },
        saveReferenceStation: {
          requestConfig: {
            method: 'post',
            url: `http://osd-stationreference-coi-service${baseURL}/coi/reference-stations`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        }
      }
    }
  },

  // Performance monitoring configuration
  performanceMonitoring: {
    resolvers: {},
    subscriptions: {
      channels: {
        sohStatus: 'sohStatus'
      }
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        stationGroupSohStatusLatest: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/performance-monitoring/station-group-soh-status/by-time`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        stationSohLatest: {
          requestConfig: {
            method: 'post',
            url: 'http://not-a-real-endpoint',
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        saveStationGroupSohStatus: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/performance-monitoring/station-group-soh-status/new`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        getHistoricalSohData: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/coi/performance-monitoring/station-soh/query/historical-by-station-id-time-and-soh-monitor-types`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 120000
          }
        },
        getHistoricalAceiData: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/coi/acquired-channel-environment-issues/query/station-id-time-and-type`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 120000
          }
        }
      }
    }
  },

  // User profile configuration
  userProfile: {
    resolvers: {},
    subscriptions: {
      channels: {}
    },
    backend: {
      mock: {
        enable: mockEnabled,
        serviceDelayMs: 0
      },
      // Service endpoints for this component
      services: {
        getUserProfile: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/user-preferences`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        },
        setUserProfile: {
          requestConfig: {
            method: 'post',
            url: `http://frameworks-osd-service${baseURL}/osd/user-preferences/store`,
            responseType: 'json',
            proxy: false,
            headers: {
              accept: 'application/json',
              'content-type': 'application/json'
            },
            timeout: 60000
          }
        }
      }
    }
  },

  // Easy copy and paste to add a new section
  // generic: {
  //   resolvers: {},
  //   subscriptions: {
  //     channels: {}
  //   },
  //   backend: {
  //     mock: {
  //       enable: mockEnabled,
  //       serviceDelayMs: 0
  //     },
  //     services: {
  //       genericService: {
  //         requestConfig: {
  //           method: 'post',
  //           url: '',
  //           responseType: 'json',
  //           proxy: false,
  //           headers: {
  //             accept: 'application/json',
  //             'content-type': 'application/json'
  //           },
  //           timeout: 60000
  //         }
  //       }
  //     }
  //   }
  // },

  // Other Info - test data files
  testData: {
    standardTestDataSet: {
      // tslint:disable-next-line: no-invalid-template-strings
      stdsDataHome: '${HOME}/Test_Data_Sets/Standard_Test_Data',
      featurePredictions: 'feature-prediction',
      stdsJsonDir: 'gms_test_data_set',
      stationReference: {
        networkFileName: 'reference-network.json',
        networkMembershipFileName: 'reference-network-memberships.json',
        stationFileName: 'reference-station.json',
        stationMembershipFileName: 'reference-station-memberships.json',
        siteFileName: 'reference-site.json',
        siteMembershipFileName: 'reference-site-memberships.json',
        channelFileName: 'reference-channel.json'
      },
      stationProcessing: {
        stationGroupsFileName: 'processing-station-group.json'
      },
      signalDetection: {
        signalDetectionFileName: 'signal-detections.json'
      },
      qcMask: {
        qcMaskFileName: 'converted-qc-masks.json'
      },
      events: {
        eventsFileName: 'events.json'
      },
      fk: {
        fkDataPath: 'FkSpectra/ChanSeg',
        fkSpectraDefinition: 'FkSpectra/FkSpectraDefinition.json'
      },
      channelSegment: {
        channelSegmentSubDir: 'segments-and-soh',
        channelSegmentIdToW: 'segment-claim-checks.json'
      },
      waveform: {
        files: 'w'
      },

      filterMappings: 'resources/test_data/filterIdToNameMapping.json'
    },
    // Test data not part of STDS
    additionalTestData: {
      dataPath: 'resources/test_data/additional-test-data/',
      systemMessageDefinitionsFileName: 'systemMessageDefinitions.json',
      filterChannelFileName: 'filterProcessingChannels.json',
      waveformFilterFileName: 'filterParameters.json',
      workflowFilename: 'workflow.json',
      qcMaskFileName: 'qcMasks.json',
      uiConfigFileName: 'uiConfig.json',
      stationGroupsFileName: 'processing-station-group.json',
      stationGroupSohStatus: 'stationGroupSohStatus.json',
      stationSoh: 'stationSoh.json',
      stationSohOld: 'stationSohOld.json',
      transferredFileName: 'transferredFile.json',
      featurePredictionAzimuth: 'featurePredictionAzimuth.json',
      featurePredictionSlowness: 'featurePredictionSlowness.json',
      featurePredictionArrival: 'featurePredictionArrival.json',
      networkMagnitudeSolutions: 'networkMagnitudeSolutions.json',
      channelCalibrationFile: 'calibration.json',
      defaultUserProfileFile: 'defaultUserProfile.json',
      analystConfigurationFilename: 'analystConfiguration.json',
      commonConfigurationFilename: 'commonConfiguration.json',
      uiSohConfigurationFileName: 'ui-soh-settings.json',
      uiSohConfigurationOldFileName: 'ui-soh-settings-old.json',
      uiSohStationGroupFileName: 'soh.station-groups.json',
      sohControlFileName: 'soh-control.json',
      historicalSohFilename: 'historicalSohResponse.json',
      historicalAceiFilename: 'historicalAceiResponse.json'
    },
    // PI 9 temporary file folder - TODO not needed?
    tempTigerTeamData: {
      dataPath: 'resources/test_data/temp-tiger-team-data/'
    },
    // Inputs for integration tests - currently deprecated
    integrationInputs: {
      dataPath: 'resources/test_data/integration-inputs/',
      featurePrediction: 'feature-prediction-input.json',
      filterWaveform: 'filter-waveform-input.json',
      saveQcMask: 'save-qcmask-input.json',
      saveSignalDetection: 'save-sd-input.json',
      locateEvent: 'locate-event-input.json',
      computeFk: 'compute-fk-input.json',
      computeBeam: 'compute-beam-input.json',
      saveEvent: 'save-event-input.json',
      saveWaveformChannelSegment: 'save-waveform-channel-segment-input.json',
      saveFkChannelSegment: 'save-fk-channel-segment-input.json',
      computeMagSolution: 'compute-mag-solution.json'
    }
  },
  lateData: {
    detectionCount: 10,
    channelSegmentCount: 4,
    delayMillis: 10000,
    preStartDelay: 40000
  }
};

// tslint:disable-next-line: no-default-export
export default defaultConfig;
