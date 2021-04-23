import { BrowserWindow } from 'electron';
import { SERVER_URL } from './constants';

/**
 * Options used to create a popout-window
 */
export interface PopoutOptions {
  /**
   * The popoput bounds
   */
  bounds?: {
    /**
     * The x bounds
     */
    x: number;
    /**
     * The y bounds
     */
    y: number;
  };
  /**
   * The popoput configuration
   */
  config: {
    /**
     * The component
     */
    component: string;
    /**
     * The component name
     */
    componentName: 'lm-react-component';
    /**
     * Is closable flag
     */
    isClosable: true;
    /**
     * Reorder enabled flag
     */
    reorderEnabled: true;
    /**
     * The title
     */
    title: string;
    /**
     * The type
     */
    type: 'component';
  }[];
  /**
   * The popoput title
   */
  title: string;
  /**
   * The popoput url
   */
  url: string;
}

/**
 * Create a popout-window with the given configuration
 * @param options options
 * @param nextWindow next window
 */
export function createPopoutWindow(options: PopoutOptions, nextWindow: Electron.BrowserWindow) {
  const currentWindow = nextWindow;

  currentWindow.setTitle(options.title);

  currentWindow.on('show', () => {
    // nextWindow.setBounds({
    //     height: options.bounds.height,
    //     width: options.bounds.width,
    //     x: options.bounds.x,
    //     y: options.bounds.y,
    // });

    // set popout config on the window so that it can pop itself back in correctly.
    // TODO figure out how to do this properly in electron (set metadata on a BrowserWindow)
    (currentWindow.webContents as any).popoutConfig = options.config[0];

    // tslint:disable-next-line: no-console
    console.log(`Opening popout window ${options.url.substring(options.url.indexOf('#/'))}`);

    currentWindow.webContents.send('load-path', options.url.substring(options.url.indexOf('#/')));
  });

  currentWindow.show();

  // tslint:disable-next-line:no-parameter-reassignment
  nextWindow = new BrowserWindow({
    autoHideMenuBar: true,
    show: false,
    backgroundColor: '#182026',
    webPreferences: { nodeIntegration: true }
  });
  nextWindow.setMenuBarVisibility(false);

  // tslint:disable-next-line
  nextWindow.loadURL(`${SERVER_URL}/#/loading`);

  return nextWindow;
}
