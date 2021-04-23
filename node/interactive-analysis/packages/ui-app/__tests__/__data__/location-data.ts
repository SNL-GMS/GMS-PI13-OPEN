import { CommonTypes, EventTypes, SignalDetectionTypes } from '@gms/common-graphql';
import { FeatureMeasurementTypeName } from '@gms/common-graphql/lib/graphql/signal-detection/types';

export const mockDistanceToStations: any[] = [
  {
    stationName: 'fd3dbadc-72fd-36c6-a3cc-ca4f7a4f58be',
    distance: 5
  },
  {
    stationName: '7a481f10-e4d3-3687-9efa-622a82eb92cf',
    distance: 13
  }
];

export const signalDetections: SignalDetectionTypes.SignalDetection[] = [
  {
    id: 'acbd4ff2-f6e1-4a50-bc43-8ccc4f274b05',
    monitoringOrganization: 'TEST',
    currentHypothesis: {
      id: 'acbd4ff2-f6e1-4a50-bc43-8ccc4f274b05',
      rejected: false,
      featureMeasurements: [
        {
          id: 'dbdd8420-a448-44a0-9fea-d39fe55b0136',
          measurementValue: {
            value: 1274393237.85,
            standardDeviation: 0.685
          },
          featureMeasurementType: SignalDetectionTypes.FeatureMeasurementTypeName.ARRIVAL_TIME
        },
        {
          id: '3f9b417b-bc12-44ad-855d-132355708ec5',
          measurementValue: {
            referenceTime: 0,
            measurementValue: 0
          },
          featureMeasurementType:
            SignalDetectionTypes.FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH
        },
        {
          id: '3f9b417b-bc12-44ad-855d-132355708ec5',
          measurementValue: {
            referenceTime: 0,
            measurementValue: 0
          },
          featureMeasurementType: SignalDetectionTypes.FeatureMeasurementTypeName.SLOWNESS
        },
        {
          id: '3f9b417b-bc12-44ad-855d-132355708ec5',
          measurementValue: {
            phase: CommonTypes.PhaseType.P,
            confidence: 0
          },
          featureMeasurementType: SignalDetectionTypes.FeatureMeasurementTypeName.PHASE
        }
      ]
    },
    signalDetectionHypothesisHistory: [
      {
        id: 'b90ee9c8-3fc9-4ccd-95ea-5d1d206c35a7',
        phase: 'tx',
        rejected: false,
        arrivalTimeSecs: 1274393334.1,
        arrivalTimeUncertainty: 0.685
      },
      {
        id: 'e4ae55cc-a395-471c-8a98-bb2cf08411cd',
        phase: 'P',
        rejected: false,
        arrivalTimeSecs: 1274393334.1,
        arrivalTimeUncertainty: 0.685
      }
    ],
    modified: false,
    hasConflict: false,
    requiresReview: {
      amplitudeMeasurement: true
    },
    reviewed: {
      amplitudeMeasurement: false
    },
    conflictingHypotheses: [],
    stationName: ''
  },
  {
    id: 'e4ae55cc-a395-471c-8a98-bb2cf08411cd',
    monitoringOrganization: 'TEST',
    currentHypothesis: {
      id: 'e4ae55cc-a395-471c-8a98-bb2cf08411cd',
      rejected: false,
      featureMeasurements: [
        {
          id: 'e8d95a93-382d-4d84-85ce-983e87bdcb0c',
          measurementValue: {
            value: 1274393334.1,
            standardDeviation: 0.685
          },
          featureMeasurementType: SignalDetectionTypes.FeatureMeasurementTypeName.ARRIVAL_TIME
        },
        {
          id: '8cdefd4e-803f-48f1-9e51-4229066f9991',
          measurementValue: {
            referenceTime: 0,
            measurementValue: 0
          },
          featureMeasurementType:
            SignalDetectionTypes.FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH
        },
        {
          id: '8cdefd4e-803f-48f1-9e51-4229066f9991',
          measurementValue: {
            referenceTime: 0,
            measurementValue: 0
          },
          featureMeasurementType: SignalDetectionTypes.FeatureMeasurementTypeName.SLOWNESS
        },
        {
          id: '8cdefd4e-803f-48f1-9e51-4229066f9991',
          measurementValue: {
            phase: CommonTypes.PhaseType.P,
            confidence: 0
          },
          featureMeasurementType: SignalDetectionTypes.FeatureMeasurementTypeName.PHASE
        }
      ]
    },
    signalDetectionHypothesisHistory: [
      {
        id: 'b90ee9c8-3fc9-4ccd-95ea-5d1d206c35a7',
        phase: 'tx',
        rejected: false,
        arrivalTimeSecs: 1274393334.1,
        arrivalTimeUncertainty: 0.685
      },
      {
        id: 'e4ae55cc-a395-471c-8a98-bb2cf08411cd',
        phase: 'P',
        rejected: false,
        arrivalTimeSecs: 1274393334.1,
        arrivalTimeUncertainty: 0.685
      }
    ],
    modified: false,
    hasConflict: false,
    requiresReview: {
      amplitudeMeasurement: true
    },
    reviewed: {
      amplitudeMeasurement: false
    },
    conflictingHypotheses: [],
    stationName: ''
  }
];
export const eventHypothesisWithLocationSets: EventTypes.EventHypothesis = {
  id: '3a06fac7-46ad-337e-a8da-090a1cc801a1',
  rejected: false,
  event: {
    id: '3a06fac7-46ad-337e-a8da-090a1cc801a1',
    status: EventTypes.EventStatus.OpenForRefinement
  },
  preferredLocationSolution: {
    locationSolution: {
      id: '3a06fac7-46ad-337e-a8da-090a1cc801a1',
      locationType: 'standard',
      location: {
        latitudeDegrees: 44.94862,
        longitudeDegrees: -106.38442,
        depthKm: 0,
        time: 1274392890.238
      },
      locationToStationDistances: [
        {
          distance: {
            degrees: 10,
            km: 10
          },
          azimuth: 1,
          stationId: '3308666b-f9d8-3bff-a59e-928730ffa797'
        }
      ],
      snapshots: [
        {
          signalDetectionId: 'db31fbe6-322f-3e91-911c-578f22f4234b',
          signalDetectionHypothesisId: 'db31fbe6-322f-3e91-911c-578f22f4234b',
          stationName: 'PDAR',
          channelName: 'fkb',
          phase: CommonTypes.PhaseType.Pg,
          time: {
            defining: true,
            observed: 1274392950.85,
            residual: null,
            correction: null
          },
          slowness: {
            defining: false,
            observed: 18.65,
            residual: -0.4,
            correction: null
          },
          azimuth: {
            defining: true,
            observed: 52.56,
            residual: -1.4,
            correction: null
          }
        }
      ],
      featurePredictions: [],
      locationRestraint: {
        depthRestraintType: EventTypes.DepthRestraintType.UNRESTRAINED,
        depthRestraintKm: null,
        latitudeRestraintType: EventTypes.RestraintType.UNRESTRAINED,
        latitudeRestraintDegrees: null,
        longitudeRestraintType: EventTypes.RestraintType.UNRESTRAINED,
        longitudeRestraintDegrees: null,
        timeRestraintType: EventTypes.RestraintType.UNRESTRAINED,
        timeRestraint: null
      },
      locationUncertainty: {
        xy: -272.5689,
        xz: -1,
        xt: -33.5027,
        yy: 525.9998,
        yz: -1,
        yt: -30.6263,
        zz: -1,
        zt: -1,
        tt: 9.7391,
        stDevOneObservation: 0,
        ellipses: [
          {
            scalingFactorType: EventTypes.ScalingFactorType.CONFIDENCE,
            kWeight: 0,
            confidenceLevel: 0.9,
            majorAxisLength: '59.7138',
            majorAxisTrend: 137.57,
            minorAxisLength: '32.3731',
            minorAxisTrend: -1,
            depthUncertainty: -1,
            timeUncertainty: 'PT5.137S'
          }
        ],
        ellipsoids: []
      },
      locationBehaviors: [
        {
          residual: 0,
          weight: 1.01,
          defining: true,
          signalDetectionId: '00000000-0000-0000-0000-000000000000',
          featureMeasurementType: FeatureMeasurementTypeName.AMPLITUDE
        }
      ],
      networkMagnitudeSolutions: []
    }
  },
  associationsMaxArrivalTime: 1274392950.85,
  signalDetectionAssociations: [
    {
      id: '1522eea6-b188-421e-9c1f-e40c6066b841',
      rejected: false,
      eventHypothesisId: '3a06fac7-46ad-337e-a8da-090a1cc801a1',
      signalDetectionHypothesis: {
        id: 'db31fbe6-322f-3e91-911c-578f22f4234b',
        rejected: false,
        parentSignalDetectionId: 'db31fbe6-322f-3e91-911c-578f22f4234b'
      }
    }
  ],
  locationSolutionSets: [
    {
      id: '5fa862f1-2e2d-4d87-bc66-00177de18c5e',
      count: 0,
      locationSolutions: [
        {
          id: '3a06fac7-46ad-337e-a8da-090a1cc801a1',
          locationType: 'standard',
          location: {
            latitudeDegrees: 44.94862,
            longitudeDegrees: -106.38442,
            depthKm: 0,
            time: 1274392890.238
          },
          locationToStationDistances: [
            {
              distance: {
                degrees: 10,
                km: 10
              },
              azimuth: 1,
              stationId: '3308666b-f9d8-3bff-a59e-928730ffa797'
            }
          ],
          snapshots: [
            {
              signalDetectionId: 'db31fbe6-322f-3e91-911c-578f22f4234b',
              signalDetectionHypothesisId: 'db31fbe6-322f-3e91-911c-578f22f4234b',
              stationName: 'PDAR',
              channelName: 'fkb',
              phase: CommonTypes.PhaseType.Pg,
              time: {
                defining: true,
                observed: 1274392950.85,
                residual: null,
                correction: null
              },
              slowness: {
                defining: false,
                observed: 18.65,
                residual: -0.4,
                correction: null
              },
              azimuth: {
                defining: true,
                observed: 52.56,
                residual: -1.4,
                correction: null
              }
            }
          ],
          featurePredictions: [],
          locationRestraint: {
            depthRestraintType: EventTypes.DepthRestraintType.UNRESTRAINED,
            depthRestraintKm: null,
            latitudeRestraintType: EventTypes.RestraintType.UNRESTRAINED,
            latitudeRestraintDegrees: null,
            longitudeRestraintType: EventTypes.RestraintType.UNRESTRAINED,
            longitudeRestraintDegrees: null,
            timeRestraintType: EventTypes.RestraintType.UNRESTRAINED,
            timeRestraint: null
          },
          locationUncertainty: {
            xy: -272.5689,
            xz: -1,
            xt: -33.5027,
            yy: 525.9998,
            yz: -1,
            yt: -30.6263,
            zz: -1,
            zt: -1,
            tt: 9.7391,
            stDevOneObservation: 0,
            ellipses: [
              {
                scalingFactorType: EventTypes.ScalingFactorType.CONFIDENCE,
                kWeight: 0,
                confidenceLevel: 0.9,
                majorAxisLength: '59.7138',
                majorAxisTrend: 137.57,
                minorAxisLength: '32.3731',
                minorAxisTrend: -1,
                depthUncertainty: -1,
                timeUncertainty: 'PT5.137S'
              }
            ],
            ellipsoids: []
          },
          locationBehaviors: [
            {
              residual: 0,
              weight: 1.01,
              defining: true,
              signalDetectionId: '00000000-0000-0000-0000-000000000000',
              featureMeasurementType: FeatureMeasurementTypeName.AMPLITUDE
            }
          ],
          networkMagnitudeSolutions: []
        }
      ]
    }
  ]
};

export const event: EventTypes.Event = {
  id: '186f997b-7d7d-3151-8b4d-5609f7a8f31f',
  status: EventTypes.EventStatus.ReadyForRefinement,
  modified: false,
  hasConflict: false,
  conflictingSdIds: [],
  currentEventHypothesis: {
    eventHypothesis: eventHypothesisWithLocationSets,
    processingStage: {
      id: 'wowow'
    }
  }
};
