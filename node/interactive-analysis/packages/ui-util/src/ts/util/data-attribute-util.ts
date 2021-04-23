/**
 * A
 */
export interface DataAttributeList {
  [name: string]: string;
}
/**
 * Finds all parameters that start with data-, and returns them
 * @param props the props to search for data-* params
 * @returns an object containing 0 or more key-value pairs of the form:
 *  data-*: value
 * For example:
 *   { 'data-cy': 'example-component' }
 */
export const getDataAttributesFromProps = props => {
  const dataKeys = Object.keys(props).filter(param => param.slice(0, 'data-'.length) === 'data-');
  const dataAttributes: { [name: string]: string } = {};
  dataKeys.forEach(key => {
    dataAttributes[key] = props[key];
  });
  return dataAttributes;
};
