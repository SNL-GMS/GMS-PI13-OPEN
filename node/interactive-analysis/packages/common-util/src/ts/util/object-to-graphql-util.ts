/**
 * Creates graphQL input strings for mutation/queries for generic objects
 * Does not work for objects containing arrays
 * @param object the object
 * @param resultString the result string
 * @returns a graphQL string
 */
export function objectToGraphQLString(object: any, resultString: string): string {
  Object.keys(object).forEach(key => {
    const value = object[key];
    if (value) {
      if (typeof value === 'object') {
        // tslint:disable-next-line: no-parameter-reassignment
        resultString = objectToGraphQLString(value, `{${resultString}`) + '},';
      } else if (typeof value === 'string') {
        // tslint:disable-next-line: no-parameter-reassignment
        resultString += `${key}: "${object[key]}",`;
      } else {
        // tslint:disable-next-line: no-parameter-reassignment
        resultString += `${key}: ${object[key]},`;
      }
    }
  });
  return resultString;
}
