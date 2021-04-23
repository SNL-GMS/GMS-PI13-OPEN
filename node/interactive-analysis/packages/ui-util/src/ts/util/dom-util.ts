/**
 * Returns true if it is a DOM element
 */
export function isDomElement(element): element is Element {
  return element instanceof Element || element instanceof HTMLDocument;
}
