import { uniqueNumberFromString } from '@gms/common-util';
import * as Immutable from 'immutable';

// CONSTANTS
const GOLDEN_RATIO_CONJUGATE = 0.618033988749895;
const DEGREES_IN_A_CIRCLE = 360;
const HUNDRED_PERCENT = 100;
const DEFAULT_SATURATION = 0.5;
const DEFAULT_VALUE = 0.7;

/**
 * A HSV Color
 * hue: 0 to 360 degrees
 * saturation: 0 to 100 percent
 * value: 0 to 100 percent
 */
export interface HSL {
  hue: number;
  saturation: number;
  lightness: number;
}

/**
 * Converts HSV to HSL colors based on this Stack Overflow
 * discussion: https://stackoverflow.com/questions/3423214/convert-hsb-hsv-color-to-hsl
 * @param h hue between 0 and 360 degrees
 * @param s saturation between 0 and 100 percent
 * @param v value between 0 and 100 percent
 */
export function hsvToHSL(h: number, s: number, v: number): HSL {
  const lightness = ((2 - s) * v) / 2;
  let saturation;
  if (lightness !== 0) {
    if (lightness === 1) {
      saturation = 0;
      // tslint:disable-next-line: no-magic-numbers
    } else if (lightness < 0.5) {
      saturation = (s * v) / (lightness * 2);
    } else {
      saturation = (s * v) / (2 - lightness * 2);
    }
  }
  return {
    hue: h,
    saturation: saturation * HUNDRED_PERCENT,
    lightness: lightness * HUNDRED_PERCENT
  };
}

/**
 * Converts an HSL color into a css-formatted string of the form:
 *   hsl(<hue>deg, <saturation>%, <lightness>%)
 * @param color the HSL color to convert
 */
export const hslToString = (color: HSL) =>
  `hsl(${color.hue}deg, ${color.saturation}%, ${color.lightness}%)`;

export const hslToHex = (color: HSL) => {
  // see https://css-tricks.com/converting-color-spaces-in-javascript/
  // tslint:disable
  const h = color.hue;
  // Must be fractions of 1
  const s = color.saturation / 100;
  const l = color.lightness / 100;

  const c = (1 - Math.abs(2 * l - 1)) * s;
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
  const m = l - c / 2;
  let r = 0;
  let g = 0;
  let b = 0;

  if (0 <= h && h < 60) {
    r = c;
    g = x;
    b = 0;
  } else if (60 <= h && h < 120) {
    r = x;
    g = c;
    b = 0;
  } else if (120 <= h && h < 180) {
    r = 0;
    g = c;
    b = x;
  } else if (180 <= h && h < 240) {
    r = 0;
    g = x;
    b = c;
  } else if (240 <= h && h < 300) {
    r = x;
    g = 0;
    b = c;
  } else if (300 <= h && h < 360) {
    r = c;
    g = 0;
    b = x;
  }
  // Having obtained RGB, convert channels to hex
  let rStr = Math.round((r + m) * 255).toString(16);
  let gStr = Math.round((g + m) * 255).toString(16);
  let bStr = Math.round((b + m) * 255).toString(16);

  // Prepend 0s, if necessary
  if (rStr.length == 1) {
    rStr = '0' + r;
  }
  if (gStr.length == 1) {
    gStr = '0' + g;
  }
  if (bStr.length == 1) {
    bStr = '0' + b;
  }

  // tslint:enable
  return `#${rStr}${gStr}${bStr}`;
};

/**
 * Generates colors that are guaranteed to have a different hue.
 * Colors are generated in HSL format.
 */
export class DistinctColorPalette {
  /**
   * Start as random value. Each time generateRandomColorHSL
   * is called, this will update. Used to ensure that the new
   * color is sufficiently different from previously generated
   * colors.
   */
  private nextHue: number;

  /**
   * The internal map of colors in the palette
   */
  private colorMap: Immutable.Map<string | number, HSL>;

  /**
   * A color palette with distinct colors. Each color will have the same
   * saturation and lightness, and a unique hue.
   * @param size the number of colors to generate.
   * @param keys array of keys
   * @param seed an optional number or string that sets the starting hue.
   * The same seed will always generate the same palette
   */
  public constructor(keys: string[] | number[], seed?: number | string) {
    this.nextHue = this.getHueFromSeed(seed);
    this.colorMap = Immutable.Map<string | number, HSL>();

    this.generateNewListOfDistinctColorsWithKeysHSL(keys);
  }

  /**
   * Adds a new color to the internal color palette.
   * The new color will appear at the end of the list.
   * @returns the HSL color generated
   */
  public addColor(): HSL {
    this.colorMap = this.colorMap.set(this.colorMap.size, this.generateDistinctColorHSL());
    return this.colorMap.get(this.colorMap.size - 1);
  }

  /**
   * Gets all of the HSL colors from the palette.
   *
   * hue: 0 to 360
   * saturation: 0% to 100%
   * value: 0% to 100%
   *
   * @returns the a map of HSL colors index by predefined key or their index
   */
  public getColors(): Immutable.Map<string | number, HSL> {
    return this.colorMap;
  }

  /**
   * Gets the HSL color from the palette at the provided index.
   *
   * hue: 0 to 360
   * saturation: 0% to 100%
   * value: 0% to 100%
   *
   * @throws RangeError if an index out of bounds
   * @param index the index of the color in the palette list
   * @returns the HSL color at the given index
   */
  public getColor(index: number | string): HSL {
    if (index >= this.colorMap.size) {
      throw new RangeError(`index out of bounds: ${index}`);
    }
    if (index) {
      return this.colorMap.get(index);
    }
    return this.colorMap.get(index);
  }

  /**
   * Get an iterator for colorMap.
   *
   * hue: 0 to 360
   * saturation: 0% to 100%
   * value: 0% to 100%
   *
   * @returns the a iterator of color map
   */
  public getColorStrings(): IterableIterator<HSL> {
    return this.colorMap.values();
  }

  /**
   * Gets a css friendly HSL color string of the format:
   *
   * hsl(30deg, 80%, 95%);
   *
   * hue: 0 to 360
   * saturation: 0% to 100%
   * value: 0% to 100%
   *
   * @param index the index of the color from the palette
   * @returns the color in a css-friendly string
   */
  public getColorString(index: number | string): string {
    return hslToString(this.getColor(index));
  }

  /**
   * Returns the number of colors in the color palette.
   * @return the number of colors
   */
  public getSize(): number {
    return this.colorMap.size;
  }

  /**
   * Generates a color that is significantly different
   * from previously generated colors, starting from a
   * random seed value. Adds the color to the list
   * @returns HSL color value with fixed saturation and lightness
   */
  private readonly generateDistinctColorHSL = (): HSL => {
    this.nextHue += GOLDEN_RATIO_CONJUGATE * DEGREES_IN_A_CIRCLE;
    this.nextHue %= DEGREES_IN_A_CIRCLE;
    const hslColor = hsvToHSL(this.nextHue, DEFAULT_SATURATION, DEFAULT_VALUE);
    return hslColor;
  }

  /**
   * Generates a color palette where each color is as distinct.
   * @returns an array of HSL colors, each of which has a hue
   * that is the golden ratio away from the preceding color in the list
   * hue is 0 to 360 deg
   * saturation is 0 to 100 percent
   * value is 0 to 100 percent
   */
  private readonly generateNewListOfDistinctColorsWithKeysHSL = (
    keys: string[] | number[]
  ): void => {
    this.generateNewColorList(keys.length).forEach(
      (color, index) => (this.colorMap = this.colorMap.set(keys[index], color))
    );
  }

  private readonly generateNewColorList = (size: number): HSL[] => {
    const colorList = new Array(size).fill(0); // Fill it with 0s so the array is not an empty array of length n
    return colorList.map(c => this.generateDistinctColorHSL());
  }

  /**
   * Generates a number of degrees on the color wheel to use as our
   * starting hue.
   * @param seed a seed number or string from which to generate the hue
   * @returns a number between 0 and 360 representing a number of degrees
   * in a circle. If the seed is falsy, return 0;
   */
  private getHueFromSeed(seed: number | string): number {
    if (!seed) {
      return 0;
    }
    const seedNum: number = typeof seed === 'string' ? uniqueNumberFromString(seed) : seed;
    return seedNum % DEGREES_IN_A_CIRCLE;
  }
}
