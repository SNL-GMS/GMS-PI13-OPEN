/** defines an undefined user; used for the graphql playground */
export const UNDEFINED_USER = 'undefined';

/**
 * Represents a user
 */
export interface ExpressUser {
  userName: string;
}

/** Represents a map that maps a unique key to an ExpressUser */
export type ExpressUserMap = Map<string, ExpressUser>;
