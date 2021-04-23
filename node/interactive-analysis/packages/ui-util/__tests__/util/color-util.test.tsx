import * as ColorUtils from '../../src/ts/util/color-util';
// tslint:disable: no-magic-numbers

const expectValidColorValue = (value: number, maxValue: number) => {
  expect(value).toBeDefined();
  expect(value).toBeLessThanOrEqual(maxValue);
  expect(value).toBeGreaterThanOrEqual(0);
};

const expectValidColor = (color: ColorUtils.HSL) => {
  expectValidColorValue(color.hue, 360);
  expectValidColorValue(color.saturation, 100);
  expectValidColorValue(color.lightness, 100);
};

const expectColorsToBeDifferent = (a: ColorUtils.HSL, b: ColorUtils.HSL) => {
  expect(a.hue === b.hue).toEqual(false);
};

const expectColorsToMatch = (a: ColorUtils.HSL, b: ColorUtils.HSL) => {
  expect(a.hue === b.hue).toEqual(true);
};

const expectColorPalettesToMatch = (
  numColors: string[],
  a: ColorUtils.DistinctColorPalette,
  b: ColorUtils.DistinctColorPalette
) => {
  expect(a.getSize()).toEqual(b.getSize());
  numColors.forEach(color => expectColorsToMatch(a.getColor(color), b.getColor(color)));
};

const expectColorPalettesToBeDifferent = (
  numColors: string[],
  a: ColorUtils.DistinctColorPalette,
  b: ColorUtils.DistinctColorPalette
) => {
  numColors.forEach(color => expectColorsToBeDifferent(a.getColor(color), b.getColor(color)));
};

describe('Color utils', () => {
  const numColors = ['one', 'two', 'three'];
  const stationName = 'ASAR';
  const palette = new ColorUtils.DistinctColorPalette(numColors, stationName);
  it('can generate a color', () => {
    const color = palette.getColor('one');
    expectValidColor(color);
  });

  it('can generate a color string', () => {
    const color: ColorUtils.HSL = {
      hue: 211,
      saturation: 12,
      lightness: 11
    };
    const colorString = ColorUtils.hslToString(color);
    expect(colorString.replace(/\s+/g, '')).toEqual('hsl(211deg,12%,11%)');
    expect(typeof palette.getColorString('one') === 'string').toBeTruthy();
  });

  it('can generate a list of distinct colors', () => {
    numColors.forEach(firstColor => {
      expectValidColor(palette.getColor(firstColor));
      numColors.forEach(secondColor => {
        if (firstColor !== secondColor) {
          expectColorsToBeDifferent(palette.getColor(firstColor), palette.getColor(secondColor));
        }
      });
    });
  });

  it('gets the same color palette when provided the same number as a seed', () => {
    const seedNum = 123;
    const paletteA = new ColorUtils.DistinctColorPalette(numColors, seedNum);
    const paletteB = new ColorUtils.DistinctColorPalette(numColors, seedNum);
    expectColorPalettesToMatch(numColors, paletteA, paletteB);
  });

  it('produces a different palette for a different seed', () => {
    const seedNumA = 123;
    const seedNumB = 321;
    const paletteA = new ColorUtils.DistinctColorPalette(numColors, seedNumA);
    const paletteB = new ColorUtils.DistinctColorPalette(numColors, seedNumB);
    expectColorPalettesToBeDifferent(numColors, paletteA, paletteB);
  });

  it('can take a string for a seed', () => {
    const seedStr = 'this string gets converted into a number internally';
    const paletteA = new ColorUtils.DistinctColorPalette(numColors, seedStr);
    const paletteB = new ColorUtils.DistinctColorPalette(numColors, seedStr);
    expectColorPalettesToMatch(numColors, paletteA, paletteB);
    const differentSeed = 'this should produce a different palette';
    const differentPalette = new ColorUtils.DistinctColorPalette(numColors, differentSeed);
    expectColorPalettesToBeDifferent(numColors, paletteA, differentPalette);
  });
});
