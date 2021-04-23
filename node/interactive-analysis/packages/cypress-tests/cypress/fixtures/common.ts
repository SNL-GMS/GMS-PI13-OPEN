/// <reference types="Cypress" />
export const SHORT_WAIT_TIME_MS = 500;
export const COMMON_WAIT_TIME_MS = 3000;
export const LONG_WAIT_TIME = 5000;
export const VERY_LONG_WAIT_TIME = 10000;
export const DATA_REFRESH_TIME = 25000;
export const DATA_REFRESH_TIME_WITH_EXTRA_TIME = 35000;

export enum ModifierKeys {
  SHIFT = '{shift}',
  ALT = '{alt}',
  CTRL = '{ctrl}',
  META = '{meta}',
  ESC = '{esc}'
}

export enum SOHDisplays {
  STATION_STATISTICS = 'station-statistics',
  OVERVIEW = 'soh-overview',
  MISSING = 'soh-missing',
  LAG = 'soh-lag',
  ENVIRONMENT = 'soh-environment'
}

export enum CommonDisplays {
  SYSTEM_MESSAGES = 'system-messages'
}

export enum cypressItsOptions {
  LENGTH = 'length'
}

export enum cypressInvokeOptions {
  TEXT = 'text'
}

// List of commands for cypress in alphabetical order
// There may be others, if so add them here if you use a command not listed
export enum cypressShouldOptions {
  BE_AN = 'be.an',
  BE_CHECKED = 'be.checked',
  BE_DISABLED = 'be.disabled',
  BE_EMPTY = 'be.empty',
  BE_FOCUSED = 'be.focused', // equivalent to should('have.focus')
  BE_GREATER_THAN = 'be.gt', // equivalent to should('have.length.greaterThan')
  BE_HIDDEN = 'be.hidden',
  BE_NULL = 'be.null',
  BE_SELECTED = 'be.selected',
  BE_VISIBLE = 'be.visible',

  CONTAIN = 'contain',

  EQUAL = 'equal',
  EXIST = 'exist',

  HAVE_ATTRIBUTE = 'have.attr',
  HAVE_CSS = 'have.css',
  HAVE_CLASS = 'have.class',
  HAVE_BEEN_CALLED_TWICE = 'have.been.calledTwice',
  HAVE_FOCUS = 'have.focus', // equivalent to should('be.focused')
  HAVE_HTML = 'have.html',
  HAVE_ID = 'have.id',
  HAVE_KEYS = 'have.keys',
  HAVE_LENGTH = 'have.length',
  HAVE_LENGTH_GREATER_THAN = 'have.length.greaterThan', // equivalent to should('be.gt')
  HAVE_LENGTH_GREATER_THAN_OR_EQUAL = 'have.length.gte',
  HAVE_LENGTH_LESS_THAN = 'have.length.lessThan',
  HAVE_PROPERTY = 'have.property',
  HAVE_TEXT = 'have.text',
  HAVE_VALUE = 'have.value',

  INCLUDE = 'include',

  MATCH = 'match',

  NOT_BE_CHECKED = 'not.be.checked',
  NOT_BE_OK = 'not.be.ok',
  NOT_EXIST = 'not.exist',
  NOT_HAVE_CLASS = 'not.have.class',
  NOT_HAVE_VALUE = 'not.have.value',
  NOT_INCLUDE = 'not.include'
}

/**
 * OS type we are trying to detect for specific key commands
 */
export enum OSTypes {
  MAC = 'Mac',
  IOS = 'iOS',
  WINDOWS = 'Windows',
  ANDROID = 'Android',
  LINUX = 'Linux'
}

/**
 * @returns the os the browser is running on
 */
export const getOS = () => {
  const userAgent = window.navigator.userAgent;
  const platform = window.navigator.platform;
  const macosPlatforms = ['Macintosh', 'MacIntel', 'MacPPC', 'Mac68K'];
  const windowsPlatforms = ['Win32', 'Win64', 'Windows', 'WinCE'];
  const iosPlatforms = ['iPhone', 'iPad', 'iPod'];
  let os: OSTypes;

  macosPlatforms.indexOf(platform) !== -1
    ? (os = OSTypes.MAC)
    : iosPlatforms.indexOf(platform) !== -1
    ? (os = OSTypes.IOS)
    : windowsPlatforms.indexOf(platform) !== -1
    ? (os = OSTypes.WINDOWS)
    : /Android/.test(userAgent)
    ? (os = OSTypes.ANDROID)
    : /Linux/.test(platform)
    ? (os = OSTypes.LINUX)
    : (os = null);
  return os;
};

/**
 * Set of common methods other tests may use.
 */

/**
 * ----- Command -----
 * These functions interact with the UI, but do not verify the results
 * ie: clickThis, scrollThat, etc.
 */

export const holdKey = (key: string) => cy.get('body').type(`${key}`, { release: false });

export const releaseHeldKeys = () => cy.get('body').type('{shift}');

/**
 * ----- Verifiers -----
 * Verify some UI state
 * checkThis, confirmThat
 */

/**
 * ----- Capability -----
 * Perform an action and verify its result
 * ie: locate, reject
 */

export const login = () => {
  cy.get('[data-cy=username-input]', { timeout: LONG_WAIT_TIME * 2 }).type('cypress-user');
  return cy.get('[data-cy=login-btn]').click();
};

export const ensureJQueryIsThere = () =>
  cy.on('window:before:load', win => {
    if (!('jQuery' in win) && !('$' in win)) {
      Object.defineProperty(win, '$', {
        configurable: false,
        get: () => Cypress.$,
        set: () => {
          /* ignore empty block */
        }
      });
    }
  });

/**
 * Visits a display based on passed in parameter
 * @param display to visit
 */
export const openDisplay = (display: string) => {
  cy.visit(`#/${display}`);
};

/**
 * Using the passed in query selector, checks if data exists
 * @param selector selector option from query selectors
 */
export const verifyDataIsLoadedBySelector = (selector: string) => {
  cy.get(selector).should('have.length.greaterThan', 0);
};

export const visitApp = () => {
  // Base URL configured in cypress.json.
  cy.log('Test commencing ...');
  ensureJQueryIsThere();
  cy.visit('.').then(() => {
    cy.wait(LONG_WAIT_TIME);
    login();
  });
};

export const openFavoriteAnalystInterval = () => {
  cy.get('[data-cy="1274392801-AL1 - events"]').dblclick({ force: true });
};

export const hideMapDialog = () => {
  cy.get('.cesium-button.cesium-toolbar-button.cesium-navigation-help-button').click();
};

export const checkAreSignalDetectionsLoaded = () => {
  // tslint:disable-next-line: no-magic-numbers
  cy.get('.pick-marker', { timeout: 40000 }).should('have.length.greaterThan', 20);
};

export const clickGoldenLayoutTab = tabName => {
  cy.get(`li[title="${tabName}"]`).click({ force: true });
};

export const clearLayout = () => {
  cy.get('.app-menu-button').click({ force: true });
  cy.get('.bp3-menu-item')
    .contains('Developer Tools')
    .click({ force: true });
  cy.get('.bp3-menu')
    .contains('Clear Layout')
    .click({ force: true });
};
