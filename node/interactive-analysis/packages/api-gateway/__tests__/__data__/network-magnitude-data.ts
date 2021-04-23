import { PhaseType, Units } from '../../src/ts/common/model';
import {
  Event,
  EventHypothesis,
  EventStatus,
  MagnitudeType
} from '../../src/ts/event/model-and-schema/model';
import {
  getDefiningSettings,
  getFirstSdHypPerStationForMagnitude,
  getNetworkMagClientArguments
} from '../../src/ts/event/utils/network-magnitude-utils';
import { FeatureMeasurementTypeName } from '../../src/ts/signal-detection/model';
import { systemConfig } from '../../src/ts/system-config';

export const mockSdHyps: any[] = [
  {
    id: '1',
    featureMeasurements: [
      {
        featureMeasurementType: FeatureMeasurementTypeName.ARRIVAL_TIME,
        measurementValue: {
          value: 3,
          standardDeviation: 0
        }
      },
      {
        featureMeasurementType: FeatureMeasurementTypeName.PHASE,
        measurementValue: {
          phase: PhaseType.P,
          confidence: 0
        }
      },
      {
        featureMeasurementType: FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2,
        measurementValue: {
          startTime: 1,
          period: 1,
          amplitude: {
            value: 1,
            standardDeviation: 1,
            units: Units.UNITLESS
          }
        }
      }
    ]
  },
  {
    id: '2',
    featureMeasurements: [
      {
        featureMeasurementType: FeatureMeasurementTypeName.ARRIVAL_TIME,
        measurementValue: {
          value: 2,
          standardDeviation: 0
        }
      },
      {
        featureMeasurementType: FeatureMeasurementTypeName.PHASE,
        measurementValue: {
          phase: PhaseType.P,
          confidence: 0
        }
      },
      {
        featureMeasurementType: FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2,
        measurementValue: {
          startTime: 1,
          period: 1,
          amplitude: {
            value: 1,
            standardDeviation: 1,
            units: Units.UNITLESS
          }
        }
      }
    ]
  },
  {
    id: '3',
    featureMeasurements: [
      {
        featureMeasurementType: FeatureMeasurementTypeName.ARRIVAL_TIME,
        measurementValue: {
          value: 1,
          standardDeviation: 0
        }
      },
      {
        featureMeasurementType: FeatureMeasurementTypeName.PHASE,
        measurementValue: {
          phase: PhaseType.P,
          confidence: 0
        }
      },
      {
        featureMeasurementType: FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2,
        measurementValue: {
          startTime: 1,
          period: 1,
          amplitude: {
            value: 1,
            standardDeviation: 1,
            units: Units.UNITLESS
          }
        }
      }
    ]
  },
  {
    id: '4',
    featureMeasurements: [
      {
        featureMeasurementType: FeatureMeasurementTypeName.ARRIVAL_TIME,
        measurementValue: {
          value: 4,
          standardDeviation: 0
        }
      },
      {
        featureMeasurementType: FeatureMeasurementTypeName.PHASE,
        measurementValue: {
          phase: PhaseType.P3KPbc,
          confidence: 0
        }
      }
    ]
  }
];

export const stationIdsForSdHypIds = new Map<string, string>([
  ['1', 'station1'],
  ['2', 'station2'],
  ['3', 'station2'],
  ['4', 'station3']
]);

export const fauxStations: any[] = [
  {
    name: 'station1',
    location: {
      depthKm: 1,
      elevationKm: 2,
      latitudeDegrees: 3,
      longitudeDegrees: 4
    }
  },
  {
    name: 'station2',
    location: {
      depthKm: 10,
      elevationKm: 2,
      latitudeDegrees: 3,
      longitudeDegrees: 4
    }
  },
  {
    name: 'station3',
    location: {
      depthKm: 100,
      elevationKm: 2,
      latitudeDegrees: 3,
      longitudeDegrees: 4
    }
  }
];

export const mockEventHyp: EventHypothesis = {
  id: 'eh1',
  eventId: 'e1',
  associations: [],
  modified: false,
  locationSolutionSets: [
    {
      count: 1,
      id: 'lss1',
      locationSolutions: [
        {
          id: 'ls1',
          featurePredictions: [],
          location: {
            depthKm: 1,
            latitudeDegrees: 1,
            longitudeDegrees: 1,
            time: 1
          },
          locationBehaviors: [],
          locationRestraint: undefined,
          snapshots: [],
          locationUncertainty: undefined,
          networkMagnitudeSolutions: [
            {
              magnitude: 1,
              magnitudeType: MagnitudeType.MB,
              networkMagnitudeBehaviors: [
                {
                  defining: true,
                  stationMagnitudeSolution: {
                    magnitude: 1,
                    magnitudeUncertainty: 1,
                    measurement: undefined,
                    model: undefined,
                    modelCorrection: undefined,
                    phase: undefined,
                    stationCorrection: undefined,
                    stationName: 'station1',
                    type: undefined
                  },
                  residual: 1,
                  weight: 1
                }
              ],
              uncertainty: 0
            },
            {
              magnitude: 2,
              magnitudeType: MagnitudeType.MS,
              networkMagnitudeBehaviors: [],
              uncertainty: 0
            }
          ]
        }
      ]
    }
  ],
  parentEventHypotheses: [],
  preferredLocationSolution: undefined,
  rejected: false
};

export const mockEvent: Event = {
  id: 'e1',
  monitoringOrganization: 'no one',
  status: EventStatus.AwaitingReview,
  preferredEventHypothesisHistory: [],
  hypotheses: [mockEventHyp],
  currentEventHypothesis: {
    eventHypothesis: mockEventHyp,
    processingStageId: 'ps1'
  },
  rejectedSignalDetectionAssociations: [],
  associations: [],
  signalDetectionIds: [],
  hasConflict: false
};

export const fauxEmptyEvent: Event = {
  id: 'e1',
  monitoringOrganization: 'no one',
  status: EventStatus.AwaitingReview,
  preferredEventHypothesisHistory: [],
  hypotheses: [mockEventHyp],
  currentEventHypothesis: {
    processingStageId: 'ps1',
    eventHypothesis: {
      id: 'eh1',
      eventId: 'e1',
      modified: false,
      associations: [],
      locationSolutionSets: [
        {
          count: 1,
          id: 'lss1',
          locationSolutions: []
        }
      ],
      parentEventHypotheses: [],
      preferredLocationSolution: undefined,
      rejected: false
    }
  },
  rejectedSignalDetectionAssociations: [],
  associations: [],
  signalDetectionIds: [],
  hasConflict: false
};

export const defBehaviors = getDefiningSettings(
  mockEvent,
  [
    {
      defining: false,
      stationName: 'station1',
      magnitudeType: MagnitudeType.MB
    }
  ],
  getFirstSdHypPerStationForMagnitude(
    mockSdHyps,
    stationIdsForSdHypIds,
    systemConfig,
    fauxStations,
    MagnitudeType.MB
  ),
  stationIdsForSdHypIds,
  MagnitudeType.MB,
  true
);

export const networkMagClientArgs = getNetworkMagClientArguments(
  mockEvent,
  defBehaviors,
  systemConfig.getCalculableMagnitudes(true),
  mockSdHyps,
  stationIdsForSdHypIds,
  fauxStations,
  systemConfig,
  true
);
