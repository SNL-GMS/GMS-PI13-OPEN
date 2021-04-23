import { CommonTypes, SignalDetectionTypes } from '@gms/common-graphql';

// From event id: 65775069-1212-4924-8252-2c2b53f40835
export const signalDetectionsData: SignalDetectionTypes.SignalDetection[] = [
  {
    id: 'acbd4ff2-f6e1-4a50-bc43-8ccc4f274b05',
    monitoringOrganization: 'TEST',
    conflictingHypotheses: [],
    reviewed: {
      amplitudeMeasurement: false
    },
    requiresReview: {
      amplitudeMeasurement: false
    },
    stationName: 'USRK',
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
    hasConflict: false
  },
  {
    id: 'e4ae55cc-a395-471c-8a98-bb2cf08411cd',
    monitoringOrganization: 'TEST',
    conflictingHypotheses: [],
    reviewed: {
      amplitudeMeasurement: false
    },
    requiresReview: {
      amplitudeMeasurement: false
    },
    stationName: 'SONM',
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
    hasConflict: false
  }
];
export const signalDetectionsSortedByStationName: SignalDetectionTypes.SignalDetection[] = [
  {
    id: 'e4ae55cc-a395-471c-8a98-bb2cf08411cd',
    monitoringOrganization: 'TEST',
    conflictingHypotheses: [],
    reviewed: {
      amplitudeMeasurement: false
    },
    requiresReview: {
      amplitudeMeasurement: false
    },
    stationName: 'SONM',
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
    hasConflict: false
  },
  {
    id: 'acbd4ff2-f6e1-4a50-bc43-8ccc4f274b05',
    monitoringOrganization: 'TEST',
    conflictingHypotheses: [],
    reviewed: {
      amplitudeMeasurement: false
    },
    requiresReview: {
      amplitudeMeasurement: false
    },
    stationName: 'USRK',
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
    hasConflict: false
  }
];
