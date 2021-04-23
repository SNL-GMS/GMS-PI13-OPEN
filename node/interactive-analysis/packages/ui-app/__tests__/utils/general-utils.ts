export const testPermutationsUndefined = (testFunc, params: any[]) => {
  params.forEach((param, index) => {
    const customParams = [...params];
    customParams[index] = undefined;
    expect(testFunc(customParams)).toBeUndefined();
  });
};

export const testPermutationsFalsy = (testFunc, params: any[]) => {
  params.forEach((param, index) => {
    const customParams = [...params];
    customParams[index] = undefined;
    expect(testFunc(customParams)).toBeFalsy();
  });
};
