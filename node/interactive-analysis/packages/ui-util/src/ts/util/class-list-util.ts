import { splitStringBySpace } from '@gms/common-util';

/**
 * A list of classes (as keys) to boolean values.
 * Classes can be toggled on and off by flipping the boolean.
 */
export interface ClassList {
  [className: string]: boolean;
}

/**
 * Creates a ClassDefinitions
 */
export const makeClassList = (c: string | string[]) => {
  const classes = typeof c === 'string' ? splitStringBySpace(c) : [...c];
  const classDefs: ClassList = classes.reduce<ClassList>(
    (list, theClassName) => ({ ...list, [theClassName]: true }),
    {}
  );
  return classDefs;
};

/**
 * Used to generate a browser-ready string containing the classes
 * (keys) that are set to true
 * @param classDefinitions a set of class definitions to process
 * @returns a space-separated string of the classNames that are
 * set to true
 */
export const classList = (cl: ClassList, additionalClasses?: string) =>
  Object.keys(cl)
    .filter(className => cl[className])
    .join(' ') + ` ${additionalClasses ?? ''}`;
