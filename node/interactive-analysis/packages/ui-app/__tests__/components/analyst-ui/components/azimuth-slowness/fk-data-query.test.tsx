import { SignalDetectionQueries } from '@gms/common-graphql';

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

it('should be the correct query', () => {
  expect(SignalDetectionQueries.signalDetectionsByStationQuery).toMatchSnapshot();
});
