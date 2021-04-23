export const uniqueNumberFromString = input => {
  let hash = 0;
  for (let i = 0; i < input.length; i++) {
    // tslint:disable: no-bitwise
    // tslint:disable: no-magic-numbers
    // tslint:disable-next-line: restrict-plus-operands
    hash = (hash << 5) - hash + input.charCodeAt(i);
    hash |= 0; // to 32bit integer
    // tslint:enable: no-magic-numbers
    // tslint:enable: no-bitwise
  }
  return Math.abs(hash);
};

/**
 * Returns a new string that has been converted to sentence case
 * @param s the string on which to operate
 */
export const toSentenceCase = (s: string) => {
  if (typeof s !== 'string') return '';
  return s?.charAt(0).toUpperCase() + s?.slice(1).toLowerCase();
};

/**
 * Creates an array containing each substring, split along spaces.
 * @param input the string on which to operate
 * @returns an array of strings
 */
export const splitStringBySpace = (input: string) =>
  input.split(/(s+)/).filter((e: string) => e.trim().length > 0);
