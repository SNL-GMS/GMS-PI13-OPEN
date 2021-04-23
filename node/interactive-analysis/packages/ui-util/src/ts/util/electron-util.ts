import { isWindowDefined } from '@gms/common-util';

declare var require: any;

/**
 * Returns true if running in Electron Main Process; false otherwise.
 */
export const isElectronMainProcess = () => {
  // Main process
  if (
    typeof process !== 'undefined' &&
    typeof process.versions === 'object' &&
    !!(process.versions as any).electron
  ) {
    return true;
  }
  return false;
};

/**
 * Returns true if running in Electron Renderer Process; false otherwise.
 */
export const isElectronRendererProcess = () => {
  // Renderer process
  if (
    isWindowDefined() &&
    typeof (window as any).process === 'object' &&
    (window as any).process.type === 'renderer'
  ) {
    return true;
  }
  return false;
};

/**
 * Returns true if running in Electron; false otherwise.
 */
export const isElectron = () => {
  // Renderer process
  if (isElectronRendererProcess()) {
    // tslint:disable-next-line: no-console
    console.debug(`Running in electron main process`);
    return true;
  }

  // Main process
  if (isElectronMainProcess()) {
    // tslint:disable-next-line: no-console
    console.debug(`Running in electron renderer process`);
    return true;
  }

  // Detect the user agent when the `nodeIntegration` option is set to true
  if (
    typeof navigator === 'object' &&
    typeof navigator.userAgent === 'string' &&
    navigator.userAgent.indexOf('Electron') >= 0
  ) {
    // tslint:disable-next-line: no-console
    console.debug(`Running in electron in node integration`);
    return true;
  }

  // tslint:disable-next-line: no-console
  console.debug(`Not running in electron`);
  return false;
};

/**
 * Returns the electron instance; undefined if not running in electron.
 */
export const getElectron = () => {
  try {
    if (isElectron()) {
      // tslint:disable-next-line: no-console
      console.info(`Running in electron, attempting to 'require electron'`);
      // tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
      const electron = require('electron');
      if (typeof electron !== undefined) {
        return electron;
      }
    }
  } catch (error) {
    // tslint:disable-next-line: no-console
    console.error(`Failed to require electron: ${error}`);
  }
  return undefined;
};

/**
 * Returns the electron enhancer instance; undefined if not running in electron.
 */
export const getElectronEnhancer = () => {
  try {
    if (isElectron()) {
      // tslint:disable-next-line: no-console
      console.debug(`Running in electron, attempting to 'require redux-electron-store''`);
      // This requires that we remove all dependencies that expect the window from the Redux store
      // tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
      const electronEnhancer = require('redux-electron-store').electronEnhancer;
      if (typeof electronEnhancer !== undefined) {
        return electronEnhancer;
      }
    }
  } catch (error) {
    // tslint:disable-next-line: no-console
    console.error(`Failed to require electron enhancer: ${error}`);
  }
  return undefined;
};

/**
 * Reloads all of the windows for electron (main process and renderer processes)
 */
export const reload = (): void => {
  const electron = getElectron();
  if (electron) {
    // tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
    const BrowserWindow = electron.BrowserWindow || electron.remote.BrowserWindow;
    const windows = BrowserWindow.getAllWindows();
    if (windows) {
      windows.forEach(win => win.reload());
    }
  } else {
    // fail safe; not running in electron just reload the existing window
    window.location.reload();
  }
};
