import { BrowserWindow } from 'electron';
import * as storage from 'electron-json-storage';
import debounce from 'lodash/debounce';
import isEqual from 'lodash/isEqual';

const layoutStorageKey = 'user-layout';

/**
 *
 * @param nextPopout - a handle to the next popout window, which should be ignored during persistence.
 */
const persistLayout = nextPopout => {
  const windows = BrowserWindow.getAllWindows();
  storage.set(
    layoutStorageKey,
    windows
      .map(window => {
        // ignore the next popout window
        if (isEqual(window, nextPopout)) return undefined;
        const popoutConfig = (window.webContents as any).popoutConfig;
        const bounds = window.getBounds();
        const url = window.webContents.getURL();
        const title = popoutConfig?.title ?? window.getTitle();
        return {
          url,
          bounds,
          title,
          popoutConfig
        };
      })
      .filter(config => config !== undefined)
  );
};

// only allow for state saves every 500ms, since these events can fire pretty often
// tslint:disable-next-line: completed-docs
const persistLayoutDebounce: ((nextPopout: BrowserWindow) => void) & {
  cancel(): void;
  flush(): void;
} = debounce(
  (nextPopout: Electron.BrowserWindow) => {
    persistLayout(nextPopout);
  },
  // tslint:disable-next-line:no-magic-numbers
  500
);

export { persistLayoutDebounce as persistLayout };

/**
 * Load the layout. layout will be empty if there is no value set for the storage key.
 */
export async function loadLayout() {
  return new Promise<any>((resolve, reject) => {
    storage.get(layoutStorageKey, (error, layout) => {
      if (error) {
        reject(error);
      } else {
        resolve(layout);
      }
    });
  });
}

/**
 * Clear the layout
 */
export async function clearLayout() {
  return new Promise<any>((resolve, reject) => {
    storage.clear(error => {
      if (error) {
        reject(error);
      } else {
        resolve();
      }
    });
  });
}
