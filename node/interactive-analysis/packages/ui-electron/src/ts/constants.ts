// default url populated at compile time by webpack
declare var DEFAULT_SERVER_URL;
export const SERVER_URL: string = process.env.SERVER_URL || DEFAULT_SERVER_URL;
