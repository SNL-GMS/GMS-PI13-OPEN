import * as StringUtils from '../../src/ts/util/string-util';

describe('String util', () => {
  it('can generate a unique number from a string seed', () => {
    const seedStr = 'This string is a seed';
    const firstResult = StringUtils.uniqueNumberFromString(seedStr);
    expect(firstResult).toBeDefined();
    const secondResult = StringUtils.uniqueNumberFromString(seedStr);
    expect(secondResult).toEqual(firstResult);
    const differentStr = 'This is a different seed string';
    const numberFromDifferentSeed = StringUtils.uniqueNumberFromString(differentStr);
    expect(numberFromDifferentSeed === firstResult).toBeFalsy();
  });
});
