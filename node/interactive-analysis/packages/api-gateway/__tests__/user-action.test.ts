import { UserActionDescription } from '../src/ts/cache/model';
import {
  userActionCreatorForEventHypothesisChange,
  userActionCreatorForSignalDetectionHypothesisChange
} from '../src/ts/cache/user-action';
import { FeatureMeasurementTypeName } from '../src/ts/signal-detection/model';

// Test data for this test is defined below the tests

// Mostly empty signal detection for testing user actions
const dummyDetection: any = {
  stationName: 'TEST'
};

// Mostly empty event hypothesis for testing user actions
const newEventHypothesis: any = {
  preferredLocationSolution: {
    locationSolution: {
      location: {
        time: 1274349600
      }
    }
  }
};

// Mostly empty signal detection hypothesis for testing user actions
const oldSDHypothesis: any = {
  featureMeasurements: [
    {
      featureMeasurementType: FeatureMeasurementTypeName.PHASE,
      measurementValue: {
        phase: 'P'
      }
    },
    {
      featureMeasurementType: FeatureMeasurementTypeName.ARRIVAL_TIME,
      measurementValue: {
        value: 1274349600
      }
    },
    {
      featureMeasurementType: FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2,
      measurementValue: {
        amplitude: {
          value: 1.23
        }
      }
    }
  ]
};

// Mostly empty signal detection hypothesis for testing user actions
const newSDHypothesis: any = {
  featureMeasurements: [
    {
      featureMeasurementType: FeatureMeasurementTypeName.PHASE,
      measurementValue: {
        phase: 'S'
      }
    },
    {
      featureMeasurementType: FeatureMeasurementTypeName.ARRIVAL_TIME,
      measurementValue: {
        value: 12743496720
      }
    },
    {
      featureMeasurementType: FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2,
      measurementValue: {
        amplitude: {
          value: 0.23
        }
      }
    }
  ]
};

describe('User Action Tests', () => {
  it('Snapshot for signal detection actions', () => {
    Object.values(UserActionDescription).forEach(action => {
      const userAction = userActionCreatorForSignalDetectionHypothesisChange(
        action,
        dummyDetection,
        oldSDHypothesis,
        newSDHypothesis
      );
      expect(userAction.toString()).toMatchSnapshot();
    });
  });

  it('Snapshot for event actions', () => {
    Object.values(UserActionDescription).forEach(action => {
      const userAction = userActionCreatorForEventHypothesisChange(
        action,
        undefined,
        newEventHypothesis
      );
      expect(userAction.toString()).toMatchSnapshot();
    });
  });
});
