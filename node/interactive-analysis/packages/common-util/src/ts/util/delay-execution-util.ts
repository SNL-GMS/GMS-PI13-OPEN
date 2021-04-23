/**
 * Delay invoking a function for some number of milliseconds.
 *
 * @param func the function to call on a delayed time
 * @param delayMillis the number of milliseconds to delay
 */
export const delayExecution = async <T>(func: () => T, delayMillis: number = 50) =>
  new Promise<T>((resolve, reject) => {
    setTimeout(() => {
      try {
        resolve(func());
      } catch (e) {
        reject(e);
      }
    }, delayMillis);
  });
