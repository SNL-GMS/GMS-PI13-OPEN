import { EventTypes, FkTypes, SignalDetectionTypes } from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import {
  convertGraphicsXYtoCoordinate,
  convertPolarToXY,
  convertXYtoPolar,
  getSortedSignalDetections
} from '../../../../../src/ts/components/analyst-ui/components/azimuth-slowness/components/fk-util';
import { signalDetectionsData } from '../../../../__data__/signal-detections-data';

// tslint:disable: no-magic-numbers
// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

export const fkSpectra: FkTypes.FkPowerSpectra = {
  id: undefined,
  spectrums: [],
  contribChannels: [],
  startTime: undefined,
  sampleRate: undefined,
  sampleCount: undefined,
  windowLead: undefined,
  windowLength: undefined,
  stepSize: undefined,
  lowFrequency: undefined,
  highFrequency: undefined,
  metadata: {
    phaseType: undefined,
    slowStartX: -20,
    slowDeltaX: undefined,
    slowStartY: -20,
    slowDeltaY: undefined
  },
  slowCountX: undefined,
  slowCountY: undefined,
  reviewed: undefined,
  fstatData: undefined,
  configuration: undefined
};
const incrementAmt = 10;
const canvasDimension = 40;
const sqrtOfFifty = 7.0710678118654755;

/**
 * Tests helper function that converts graphic space to xy coordinates
 */
describe('convertGraphicsXYtoCoordinate', () => {
  test('tests calling function with valid inputs and known output', () => {
    let xyValue = 0;

    let xy = convertGraphicsXYtoCoordinate(
      xyValue,
      xyValue,
      fkSpectra,
      canvasDimension,
      canvasDimension
    );
    expect(xy.x).toEqual(-20);
    expect(xy.y).toEqual(20);

    xyValue += incrementAmt;
    xy = convertGraphicsXYtoCoordinate(
      xyValue,
      xyValue,
      fkSpectra,
      canvasDimension,
      canvasDimension
    );
    expect(xy.x).toEqual(-10);
    expect(xy.y).toEqual(10);

    xyValue += incrementAmt;
    xy = convertGraphicsXYtoCoordinate(
      xyValue,
      xyValue,
      fkSpectra,
      canvasDimension,
      canvasDimension
    );
    expect(xy.x).toEqual(0);
    expect(xy.y).toEqual(0);

    xyValue += incrementAmt;
    xy = convertGraphicsXYtoCoordinate(
      xyValue,
      xyValue,
      fkSpectra,
      canvasDimension,
      canvasDimension
    );
    expect(xy.x).toEqual(10);
    expect(xy.y).toEqual(-10);

    xyValue += incrementAmt;
    xy = convertGraphicsXYtoCoordinate(
      xyValue,
      xyValue,
      fkSpectra,
      canvasDimension,
      canvasDimension
    );
    expect(xy.x).toEqual(20);
    expect(xy.y).toEqual(-20);
  });

  test('test bad input', () => {
    const xyValue = 0;
    let xy = convertGraphicsXYtoCoordinate(
      undefined,
      xyValue,
      fkSpectra,
      canvasDimension,
      canvasDimension
    );
    expect(xy).toBeUndefined();
    xy = convertGraphicsXYtoCoordinate(
      xyValue,
      undefined,
      fkSpectra,
      canvasDimension,
      canvasDimension
    );
    expect(xy).toBeUndefined();
    xy = convertGraphicsXYtoCoordinate(
      xyValue,
      xyValue,
      undefined,
      canvasDimension,
      canvasDimension
    );
    expect(xy).toBeUndefined();
    xy = convertGraphicsXYtoCoordinate(xyValue, xyValue, fkSpectra, undefined, canvasDimension);
    expect(xy).toBeUndefined();
    xy = convertGraphicsXYtoCoordinate(xyValue, xyValue, fkSpectra, canvasDimension, undefined);
    expect(xy).toBeUndefined();
  });
});

describe('convertXYtoPolar', () => {
  test('test valid inputs to xy to polar conversion', () => {
    const polar = convertXYtoPolar(5, 5);
    expect(polar.azimuthDeg).toEqual(45);
    expect(polar.radialSlowness).toEqual(sqrtOfFifty);
  });

  test('bad inputs for conversion', () => {
    const polar = convertXYtoPolar(undefined, undefined);
    expect(polar.azimuthDeg).toBeUndefined();
    expect(polar.radialSlowness).toBeUndefined();
  });
});

describe('convertPolarToXY', () => {
  test('test valid inputs to polar to xy conversion', () => {
    const xy = convertPolarToXY(sqrtOfFifty, 45);
    expect(xy.x).toBeCloseTo(5, 5);
    expect(xy.y).toBeCloseTo(-5, 5);
  });

  test('bad inputs for conversion', () => {
    const xy = convertPolarToXY(undefined, undefined);
    expect(xy.x).toBeNaN();
    expect(xy.y).toBeNaN();
  });
});

describe('getSortedSignalDetections', () => {
  it('test getSortedSignalDetections returns empty list with empty lists', () => {
    const emptySds: SignalDetectionTypes.SignalDetection[] = [];
    const emptyDistanceToSource: EventTypes.LocationToStationDistance[] = [];
    const sortedSds = getSortedSignalDetections(
      emptySds,
      AnalystWorkspaceTypes.WaveformSortType.distance,
      emptyDistanceToSource
    );
    expect(sortedSds).toEqual([]);
  });
  it('test getSortedSignalDetections correctly sorts by station', () => {
    const sds: SignalDetectionTypes.SignalDetection[] = signalDetectionsData;
    const emptyDistanceToSource: EventTypes.LocationToStationDistance[] = [];
    const sortedSds = getSortedSignalDetections(
      sds,
      AnalystWorkspaceTypes.WaveformSortType.stationName,
      emptyDistanceToSource
    );
    expect(sortedSds).toMatchSnapshot();
  });
});
