import { CommonTypes } from '@gms/common-graphql';
import { getSecureRandomNumber } from '@gms/common-util';
import { SitechanFileFields, SiteFileFields } from './css-enums';

// const to use to indicate no valid number could be parsed
// (NaN, undefined, and null do not play nicely with the form these values are displayed
// in which is why those weren't used)
const invalidNumber = -999999999;

const offdateNeg1NaValue = '-1';
const offdateNeg999NaValue = '-999';
const preferredOffdateNaValue = 2286324;

/**
 * Determine the correct StationType
 *
 * @param refsta site refsta information
 * @param sta site sta information
 * @param staType site station type information
 * @returns StationType
 */
export const determineStationType = (
  refsta: string,
  sta: string,
  staType: string
): CommonTypes.StationType => {
  const infrasoundPattern = /^I[0-6]/;
  const hydroacousticPattern = /^H[0-2]/;
  if (infrasoundPattern.test(sta)) {
    return CommonTypes.StationType.INFRASOUND_ARRAY;
  }

  if (hydroacousticPattern.test(sta)) {
    return CommonTypes.StationType.HYDROACOUSTIC_ARRAY;
  }

  if (refsta === sta) {
    if (staType === 'ss') {
      return CommonTypes.StationType.SEISMIC_3_COMPONENT;
    }

    if (staType === 'ar') {
      return CommonTypes.StationType.SEISMIC_ARRAY;
    }
  }

  if (staType === 'ss') {
    return CommonTypes.StationType.SEISMIC_3_COMPONENT;
  }

  return undefined;
};

export const getRandomId = (): string => {
  const randomNumber = getSecureRandomNumber();
  const randomNumberLength = 36;
  const randomNumberSubstringStart = 2;
  const randomNumberSubstringEnd = 10;

  return randomNumber
    .toString(randomNumberLength)
    .substr(randomNumberSubstringStart, randomNumberSubstringEnd);
};

export const parseSitechanSta = (line: string): string => {
  try {
    return line.substring(SitechanFileFields.staStart, SitechanFileFields.staEnd).trim();
  } catch (err) {
    return 'Invalid CSS sitechan sta';
  }
};

export const parseSitechanOndate = (line: string): number => {
  try {
    return parseFloat(
      line.substring(SitechanFileFields.ondateStart, SitechanFileFields.ondateEnd).trim()
    );
  } catch (err) {
    return invalidNumber;
  }
};

export const parseSitechanOndateAsString = (line: string): string => {
  try {
    return line.substring(SitechanFileFields.ondateStart, SitechanFileFields.ondateEnd).trim();
  } catch (err) {
    return 'Invalid CSS sitechan ondate';
  }
};

export const parseSitechanOffdate = (line: string): number => {
  try {
    const offdate = line
      .substring(SitechanFileFields.offdateStart, SitechanFileFields.offdateEnd)
      .trim();
    if (offdate === offdateNeg1NaValue || offdate === offdateNeg999NaValue) {
      return preferredOffdateNaValue;
    }
    return parseFloat(offdate);
  } catch (err) {
    return invalidNumber;
  }
};

export const parseSitechanType = (line: string): string => {
  try {
    return line.substring(SitechanFileFields.chanTypeStart, SitechanFileFields.chanTypeEnd).trim();
  } catch (err) {
    return 'Invalid CSS sitechan type';
  }
};

export const parseSitechanDescription = (line: string): string => {
  try {
    return line
      .substring(SitechanFileFields.descriptionStart, SitechanFileFields.descriptionEnd)
      .trim();
  } catch (err) {
    return 'Invalid CSS sitechan description';
  }
};

export const parseSitechanDepth = (line: string): number => {
  try {
    return parseFloat(
      line.substring(SitechanFileFields.depthStart, SitechanFileFields.depthEnd).trim()
    );
  } catch (err) {
    return invalidNumber;
  }
};

export const parseSiteSta = (line: string): string => {
  try {
    return line.substring(SiteFileFields.staStart, SiteFileFields.staEnd).trim();
  } catch (err) {
    return `Invalid CSS site sta_${getRandomId()}`;
  }
};

export const parseSiteRefsta = (line: string): string => {
  try {
    return line.substring(SiteFileFields.refstaStart, SiteFileFields.refstaEnd).trim();
  } catch (err) {
    return 'Invalid CSS site refsta';
  }
};

export const parseSiteLatitude = (line: string): number => {
  try {
    return parseFloat(
      line.substring(SiteFileFields.latitudeStart, SiteFileFields.latitudeEnd).trim()
    );
  } catch (error) {
    return invalidNumber;
  }
};

export const parseSiteLongitude = (line: string): number => {
  try {
    return parseFloat(
      line.substring(SiteFileFields.longitudeStart, SiteFileFields.longitudeEnd).trim()
    );
  } catch (err) {
    return invalidNumber;
  }
};

export const parseSiteElevation = (line: string): number => {
  try {
    return parseFloat(
      line.substring(SiteFileFields.elevationStart, SiteFileFields.elevationEnd).trim()
    );
  } catch (err) {
    return invalidNumber;
  }
};

export const parseSiteOndate = (line: string): number => {
  try {
    return parseFloat(line.substring(SiteFileFields.ondateStart, SiteFileFields.ondateEnd).trim());
  } catch (err) {
    return invalidNumber;
  }
};

export const parseSiteOffdate = (line: string): number => {
  try {
    const offdate = line.substring(SiteFileFields.offdateStart, SiteFileFields.offdateEnd).trim();
    if (offdate === offdateNeg1NaValue || offdate === offdateNeg999NaValue) {
      return preferredOffdateNaValue;
    }
    return parseFloat(offdate);
  } catch (err) {
    return invalidNumber;
  }
};

export const parseSiteStatype = (line: string): string => {
  try {
    return line.substring(SiteFileFields.staTypeStart, SiteFileFields.staTypeEnd).trim();
  } catch (err) {
    return 'Invalid CSS site statype';
  }
};

export const parseSiteDescription = (line: string): string => {
  try {
    return line.substring(SiteFileFields.descriptionStart, SiteFileFields.descriptionEnd).trim();
  } catch (err) {
    return 'Invalid CSS site description';
  }
};

export const parseSiteDnorth = (line: string): number => {
  try {
    return parseFloat(line.substring(SiteFileFields.dnorthStart, SiteFileFields.dnorthEnd).trim());
  } catch (err) {
    return invalidNumber;
  }
};

export const parseSiteDeast = (line: string): number => {
  try {
    return parseFloat(line.substring(SiteFileFields.deastStart, SiteFileFields.deastEnd).trim());
  } catch (err) {
    return invalidNumber;
  }
};
