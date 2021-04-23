import { SohTypes } from '@gms/common-graphql/src/ts/graphql/soh';
import {
  compareCellValues,
  getWorseStatus
} from '../../../../../src/ts/components/data-acquisition-ui/shared/table/utils';

/**
 * Checks
 */
describe('Table Utils', () => {
  test('functions should be defined', () => {
    expect(getWorseStatus).toBeDefined();
    expect(compareCellValues).toBeDefined();
  });

  test('compareCellValues function should properly order numbers mixed with undefined values', () => {
    expect(compareCellValues(undefined, undefined)).toEqual(0);
    expect(compareCellValues(1, undefined)).toBeGreaterThan(0);
    expect(compareCellValues(undefined, 1)).toBeLessThan(0);
    expect(compareCellValues(1, 1)).toEqual(0);
    expect(compareCellValues(1, 2)).toBeLessThan(0);
    expect(compareCellValues(2, 1)).toBeGreaterThan(0);
  });

  test('getWorseStatus function should get the worst status from all permutations', () => {
    const statuses = [
      SohTypes.SohStatusSummary.GOOD,
      SohTypes.SohStatusSummary.BAD,
      SohTypes.SohStatusSummary.MARGINAL,
      SohTypes.SohStatusSummary.NONE
    ];
    let output: SohTypes.SohStatusSummary;
    statuses.forEach((statusA: SohTypes.SohStatusSummary) => {
      statuses.forEach((statusB: SohTypes.SohStatusSummary) => {
        output =
          statusA === SohTypes.SohStatusSummary.BAD || statusB === SohTypes.SohStatusSummary.BAD
            ? SohTypes.SohStatusSummary.BAD
            : statusA === SohTypes.SohStatusSummary.MARGINAL ||
              statusB === SohTypes.SohStatusSummary.MARGINAL
            ? SohTypes.SohStatusSummary.MARGINAL
            : statusA === SohTypes.SohStatusSummary.GOOD ||
              statusB === SohTypes.SohStatusSummary.GOOD
            ? SohTypes.SohStatusSummary.GOOD
            : SohTypes.SohStatusSummary.NONE;
        expect(getWorseStatus(statusA, statusB)).toEqual(output);
      });
    });
  });
});
