import { MagnitudeType } from '../src/ts/event/model-and-schema/model';
import {
  getDefiningBehaviorsForEvent,
  getEventHypForComputeMagnitude,
  getFirstSdHypPerStationForMagnitude,
  getHypothesisIdToLocationMap,
  sortSignalDetectionHypsByArrivalTime
} from '../src/ts/event/utils/network-magnitude-utils';
import { gatewayLogger } from '../src/ts/log/gateway-logger';
import { systemConfig } from '../src/ts/system-config';
import {
  defBehaviors,
  fauxEmptyEvent,
  fauxStations,
  mockEvent,
  mockSdHyps,
  networkMagClientArgs,
  stationIdsForSdHypIds
} from './__data__/network-magnitude-data';

describe('Compute Magnitude query generation', () => {
  it('sorts signal detection hyps by arrival time', () => {
    const sorted = sortSignalDetectionHypsByArrivalTime(mockSdHyps);
    expect(sorted).toMatchSnapshot();
  });

  it('gets first sdHyp per station for magnitude', () => {
    const firstSdHypPerStationForMagnitude = getFirstSdHypPerStationForMagnitude(
      mockSdHyps,
      stationIdsForSdHypIds,
      systemConfig,
      fauxStations,
      MagnitudeType.MB
    );
    expect(firstSdHypPerStationForMagnitude.length).toEqual(2);
  });

  it('gets hypothesis id to location map', () => {
    // tslint:disable-next-line: no-magic-numbers
    const map = getHypothesisIdToLocationMap(
      mockSdHyps,
      stationIdsForSdHypIds,
      fauxStations,
      // tslint:disable-next-line: no-magic-numbers
      10
    );
    expect(map).toMatchSnapshot();
  });

  it('can get defining changes based on event, userDefChanges, and defaults', () => {
    expect(defBehaviors[0].defining).toBe(false);
  });
  it('can get event hyp to compute magnitude', () => {
    const newEventHyp = getEventHypForComputeMagnitude(mockEvent, MagnitudeType.MB);
    expect(newEventHyp).toMatchSnapshot();
  });

  it('getting defining behaviors from an event with an empty location solution logs an error', () => {
    const spyLogger = jest.spyOn(gatewayLogger, 'error');
    const definingBehaviorsForEmptyEvent = getDefiningBehaviorsForEvent(
      fauxEmptyEvent,
      MagnitudeType.MB
    );
    expect(definingBehaviorsForEmptyEvent).toEqual([]);
    expect(spyLogger).toBeCalledWith(
      'Attempting to get defining behaviors from event with empty location solution set'
    );
  });

  it('gets expected network magnitude client arguments', () => {
    expect(networkMagClientArgs).toBeInstanceOf(Map);
    expect(networkMagClientArgs).toMatchSnapshot();
  });
});
