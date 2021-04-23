import {
  prettifyAllCapsEnumType,
  setDecimalPrecision,
  setDecimalPrecisionAsNumber,
  stripOutFirstOccurrence
} from '../../src/ts/util/formatting-util';

describe('Format utils', () => {
  test('Set Decimal Precision', () => {
    // tslint:disable: no-magic-numbers
    const numberToEdit = 4.89756;
    const one = setDecimalPrecision(numberToEdit, 1);
    const oneNumber = setDecimalPrecisionAsNumber(numberToEdit, 1);
    expect(one).toEqual('4.9');
    expect(oneNumber).toEqual(4.9);

    const two = setDecimalPrecision(numberToEdit, 2);
    const twoNumber = setDecimalPrecisionAsNumber(numberToEdit, 2);
    expect(two).toEqual('4.90');
    expect(twoNumber).toEqual(4.9);

    const three = setDecimalPrecision(numberToEdit, 3);
    const threeNumber = setDecimalPrecisionAsNumber(numberToEdit, 3);
    expect(three).toEqual('4.898');
    expect(threeNumber).toEqual(4.898);

    const four = setDecimalPrecision(numberToEdit, 4);
    const fourNumber = setDecimalPrecisionAsNumber(numberToEdit, 4);
    expect(four).toEqual('4.8976');
    expect(fourNumber).toEqual(4.8976);
  });

  test('Prettify All Caps Enum', () => {
    const singleWordEnum = 'MISSING';
    expect(prettifyAllCapsEnumType(singleWordEnum)).toEqual('Missing');

    const multiWordEnum = 'ENV_TYPE_TO_TEST';
    expect(prettifyAllCapsEnumType(multiWordEnum, true)).toEqual('Type To Test');
  });

  test('Strip station from channel name', () => {
    const channel = 'STATION.SITE.CHANNEL';
    expect(stripOutFirstOccurrence(channel)).toEqual('SITE.CHANNEL');
  });
});
