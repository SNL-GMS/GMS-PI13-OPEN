import { createStore } from '@gms/ui-state';
import { getElectron, isElectron } from '@gms/ui-util';
import { app, BrowserWindow, ipcMain } from 'electron';
import delay from 'lodash/delay';
import isEmpty from 'lodash/isEmpty';
import process from 'process';
import { SERVER_URL } from './constants';
import { createMainWindow } from './main-window';
import { clearLayout, loadLayout, persistLayout } from './persist-layout';
import { createPopoutWindow, PopoutOptions } from './popout-window';

// for debugging purposes, add context menu for inspecting elements;
/* tslint:disable */
// require('electron-context-menu')({});
/* tslint:enable */

// tslint:disable
console.info(`Process is running on electron: ${isElectron()}`);
console.info(`Required electron successfully: ${getElectron() !== undefined}`);
console.info(`Setting up Redux store for electron`);
createStore(); // set up redux in the main process
console.info(`Completed setup for Redux store for electron`);
// tslint:enable

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let mainWindow: Electron.BrowserWindow;
export let nextPopoutWindow: Electron.BrowserWindow;

/**
 * Set up the application.
 * Load layout and re-hydrate stored layout, or initialize a blank layout
 */
async function initialize() {
  if (process.argv.indexOf('--clear') > -1) {
    await clearLayout();
  }

  const layout = await loadLayout();
  if (isEmpty(layout)) {
    mainWindow = createMainWindow();
  } else {
    loadSavedConfiguration(layout);
  }

  mainWindow.on('close', () => {
    app.quit();
  });

  nextPopoutWindow = new BrowserWindow({
    autoHideMenuBar: true,
    show: false,
    backgroundColor: '#182026',
    webPreferences: { nodeIntegration: true }
  });
  nextPopoutWindow.setMenuBarVisibility(false);

  // tslint:disable-next-line
  nextPopoutWindow.loadURL(`${SERVER_URL}/#/loading`);

  // pop-out events from the main window will broadcast on this channel.
  ipcMain.on('popout-window', (event, args) => {
    nextPopoutWindow = createPopoutWindow(args, nextPopoutWindow);
  });

  // pop-in events from pop-out windows will broadcast on this channel.
  ipcMain.on('popin-window', (event, args) => {
    // fire a popin-resolve event to the main window
    mainWindow.webContents.send('popin-window-resolve', args);
  });

  ipcMain.on('state-changed', (event, args) => {
    persistLayout(nextPopoutWindow);
  });
}

/**
 * Load & rehydrate a saved configuration
 * @param layout layout
 */
function loadSavedConfiguration(layout) {
  layout.forEach(windowLayout => {
    if (windowLayout === null) return;
    const { bounds, url, title, popoutConfig } = windowLayout;
    const window = new BrowserWindow({
      title,
      x: bounds.x,
      y: bounds.y,
      height: bounds.height,
      width: bounds.width,
      backgroundColor: '#182026',
      webPreferences: { nodeIntegration: true }
    });

    // tslint:disable-next-line
    window.loadURL(url);

    if (popoutConfig) {
      window.setMenuBarVisibility(false);

      delay(() => {
        // if the HTML tag <title> is defined in the HTML file loaded by loadURL();
        // the `title` property will be ignored. force update
        window.setTitle(title);
      }, 1000);

      (window.webContents as any).popoutConfig = popoutConfig;
    } else {
      mainWindow = window;
    }
  });
}

/**
 * Create a pop-out window
 * @param options options
 */
export function popout(options: PopoutOptions) {
  nextPopoutWindow = createPopoutWindow(options, nextPopoutWindow);
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', initialize);

app.on('browser-window-created', (event, window) => {
  if (SERVER_URL.includes('localhost') && process.env.ELECTRON_DEV_TOOLS) {
    window.webContents.openDevTools();
  }
  window.on('move', e => {
    if (nextPopoutWindow) {
      persistLayout(nextPopoutWindow);
    }
  });
});

app.on('window-all-closed', () => {
  app.quit();
});
