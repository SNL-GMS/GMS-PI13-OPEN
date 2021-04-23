// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const random = require('math-random');

/**
 * Returns a random number that is a
 * cryptographically secure random number generation.
 *
 * The number returned will be between 0 - 1.
 *
 * A Cryptographically Secure Pseudo-Random Number Generator.
 * This is what produces unpredictable data that you need for security purposes.
 * use of Math.random throughout codebase is considered high criticality
 * At present, the only required use is a simple random number.
 * If additional functionality is required in the future,
 * a random number library can be created to support more
 * sophisticated usage.
 */
// tslint:disable-next-line:no-unnecessary-callback-wrapper
export const getSecureRandomNumber = (): number => random();

/**
 * Random Number Generator (used for Lat/Lon)
 * @param from lower bound
 * @param to upper bound
 * @param fixed decimal places to generate
 * @returns a secure random number
 */
export function getRandomInRange(from, to, fixed) {
  // tslint:disable-next-line: restrict-plus-operands
  return (getSecureRandomNumber() * (to - from) + from).toFixed(fixed) * 1;
}

/**
 * Returns a random index based on array length
 * @param arrayLength length of array
 */
export function getRandomIndexForArray(arrayLength: number) {
  return getRandomInRange(0, arrayLength - 1, 0);
}
