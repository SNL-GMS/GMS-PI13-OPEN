/**
 * Composes a function that invokes the given functions from right to left.
 * @param funcs the functions to compose
 */
// TODO Replace this with lodash's flowRight function
export function compose(...funcs: Function[]) {
  funcs.reverse();
  return function(...args: any[]) {
    const [firstFunction, ...restFunctions] = funcs;
    let result = firstFunction.apply(null, args);
    restFunctions.forEach(fnc => {
      result = fnc.call(null, result);
    });
    return result;
  };
}
