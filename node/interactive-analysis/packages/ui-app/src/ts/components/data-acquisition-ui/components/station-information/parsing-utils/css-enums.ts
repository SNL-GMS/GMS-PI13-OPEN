/**
 * enums used for parsing CSS site and sitechan files.
 */

export enum SiteFileFields {
  // 1-6
  staStart = 0,
  staEnd = 5,
  // 8-15
  ondateStart = 7,
  ondateEnd = 14,
  // 17-24
  offdateStart = 16,
  offdateEnd = 23,
  // 26-36
  latitudeStart = 25,
  latitudeEnd = 35,
  // 38-48
  longitudeStart = 37,
  longitudeEnd = 47,
  // 50-58
  elevationStart = 49,
  elevationEnd = 57,
  // 60-109
  descriptionStart = 59,
  descriptionEnd = 108,
  // 111-114
  staTypeStart = 110,
  staTypeEnd = 113,
  // 116-121
  refstaStart = 115,
  refstaEnd = 120,
  // 123-131
  dnorthStart = 122,
  dnorthEnd = 130,
  // 133-141
  deastStart = 132,
  deastEnd = 140
}

export enum SitechanFileFields {
  // 1-6
  staStart = 0,
  staEnd = 5,
  // 8-15
  chanTypeStart = 7,
  chanTypeEnd = 14,
  // 17-24
  ondateStart = 16,
  ondateEnd = 23,
  // 35-42
  offdateStart = 34,
  offdateEnd = 41,
  // 49-57
  depthStart = 48,
  depthEnd = 56,
  // 73-122
  descriptionStart = 72,
  descriptionEnd = 121
}
